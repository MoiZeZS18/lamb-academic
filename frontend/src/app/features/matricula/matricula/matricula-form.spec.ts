import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { Seccion } from '../seccion/seccion.model';
import { MatriculaForm } from './matricula-form';
import { hoyIso } from './contexto-matricula';

const API = 'http://localhost:8080/api/v1';
const HOY = hoyIso();
const PERIODO = { id: 4, codigo: '2026-II', nombre: 'P', fechaInicio: '2000-01-01', fechaFin: '2999-12-31', estado: 'ACTIVO' };

function seccion(id: number, cursoId: number, codigo: string, creditos: number, dia: string, inicio: string, fin: string,
  extra: Partial<Seccion> = {}): Seccion {
  return {
    id,
    curso: { id: cursoId, codigo, nombre: `Curso ${codigo}`, creditos },
    periodoId: 4,
    grupo: 'A',
    docenteId: 4,
    docenteNombre: 'Mendoza Ramos Carlos',
    limiteVacantes: 30,
    cuposOcupados: 1,
    vacantesDisponibles: 29,
    estado: 'ABIERTA',
    horarios: [{ id: id * 10, diaSemana: dia as Seccion['horarios'][0]['diaSemana'], horaInicio: inicio, horaFin: fin, aula: 'A-101' }],
    ...extra,
  };
}

const OFERTA = [
  seccion(1001, 1, 'IS101', 4, 'LUNES', '08:00', '10:00'),
  seccion(1002, 2, 'IS102', 4, 'MARTES', '08:00', '10:00'),
  seccion(1004, 4, 'IS104', 3, 'LUNES', '09:00', '11:00'),
  seccion(1003, 3, 'IS103', 3, 'LUNES', '10:00', '12:00', { vacantesDisponibles: 0, cuposOcupados: 2, limiteVacantes: 2 }),
];

type Interno = {
  elegirSeccion(s: Seccion): void;
  quitarLinea(i: number): void;
  confirmarYRegistrar(): void;
  totalCreditos(): number;
  cruces(): unknown[];
  error(): string | null;
  enviando(): boolean;
  lineasForm: { length: number; hasError(e: string): boolean; push(x: unknown): void };
};

describe('MatriculaForm (cabecera-detalle, S9)', () => {
  let backend: HttpTestingController;
  let confirmacion: boolean;

  async function crear() {
    iniciarSesionDePrueba(['ESTUDIANTE'], 1033);
    await TestBed.configureTestingModule({
      imports: [MatriculaForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(confirmacion) }) } },
      ],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MatriculaForm);
    fixture.detectChanges();
    backend.expectOne(`${API}/periodos`).flush([PERIODO]);
    backend.expectOne((r) => r.url === `${API}/secciones`).flush(OFERTA);
    backend.expectOne((r) => r.url === `${API}/matriculas`).flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
    await fixture.whenStable();
    return { fixture, c: fixture.componentInstance as unknown as Interno };
  }

  function responderPrerrequisitos() {
    backend.match((r) => r.url.includes('/prerrequisitos')).forEach((r) => r.flush([]));
  }

  beforeEach(() => {
    confirmacion = true;
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('el ESTUDIANTE no elige alumno: usa el alumno_id de su token', async () => {
    const { fixture } = await crear();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector<HTMLInputElement>('input[aria-label=Alumno]')!.readOnly).toBe(true);
  });

  it('calcula los creditos en vivo al agregar y quitar lineas', async () => {
    const { c } = await crear();
    c.elegirSeccion(OFERTA[0]);
    c.elegirSeccion(OFERTA[1]);
    responderPrerrequisitos();
    expect(c.totalCreditos()).toBe(8);
    c.quitarLinea(0);
    expect(c.totalCreditos()).toBe(4);
  });

  it('exige al menos un curso', async () => {
    const { c } = await crear();
    c.quitarLinea(0);
    expect(c.lineasForm.hasError('sinLineas')).toBe(true);
    c.confirmarYRegistrar();
    expect(c.error()).toContain('Revisa la matrícula');
    backend.expectNone(`${API}/matriculas`);
  });

  it('detecta el cruce de horarios y no envia la matricula', async () => {
    const { c } = await crear();
    c.elegirSeccion(OFERTA[0]);
    c.elegirSeccion(OFERTA[2]);
    responderPrerrequisitos();
    expect(c.cruces().length).toBe(1);
    c.confirmarYRegistrar();
    expect(c.error()).toContain('cruces de horario');
    backend.expectNone((r) => r.method === 'POST');
  });

  it('una seccion sin vacantes deja la linea invalida', async () => {
    const { c, fixture } = await crear();
    c.elegirSeccion(OFERTA[3]);
    responderPrerrequisitos();
    c.confirmarYRegistrar();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('La sección no tiene vacantes.');
    backend.expectNone((r) => r.method === 'POST');
  });

  it('si el usuario cancela la confirmacion no se envia nada', async () => {
    const { c } = await crear();
    confirmacion = false;
    c.elegirSeccion(OFERTA[0]);
    responderPrerrequisitos();
    c.confirmarYRegistrar();
    backend.expectNone((r) => r.method === 'POST');
  });

  it('confirma y envia UNA sola peticion cabecera-detalle; no permite doble envio', async () => {
    const { c } = await crear();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    c.elegirSeccion(OFERTA[0]);
    c.elegirSeccion(OFERTA[1]);
    responderPrerrequisitos();
    c.confirmarYRegistrar();
    c.confirmarYRegistrar();
    const req = backend.expectOne((r) => r.method === 'POST' && r.url === `${API}/matriculas`);
    expect(c.enviando()).toBe(true);
    expect(req.request.body).toEqual({
      matricula: { estudianteId: 1033, periodoId: 4, fecha: HOY, estado: 'INICIADA', numeroCuotas: 5 },
      detalles: [{ seccionId: 1001 }, { seccionId: 1002 }],
    });
    req.flush({ id: 218, cargo: { cargoId: 9, numeroCargo: 'CRG-9', montoTotal: 550, numeroCuotas: 5, estado: 'EMITIDO' } });
    expect(navegar).toHaveBeenCalledWith(['/matricula/consulta', 218]);
  });

  it('muestra el error real de negocio del backend (409 prerrequisito) y permite corregir', async () => {
    const { c } = await crear();
    c.elegirSeccion(OFERTA[1]);
    responderPrerrequisitos();
    c.confirmarYRegistrar();
    backend
      .expectOne((r) => r.method === 'POST')
      .flush(
        { status: 409, message: 'Para matricularse en IS102 debe haber aprobado antes IS101' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(c.error()).toBe('Para matricularse en IS102 debe haber aprobado antes IS101');
    expect(c.enviando()).toBe(false);
  });

  it('409 por cupos agotados recarga la oferta con las vacantes actuales', async () => {
    const { c } = await crear();
    c.elegirSeccion(OFERTA[0]);
    responderPrerrequisitos();
    c.confirmarYRegistrar();
    backend
      .expectOne((r) => r.method === 'POST')
      .flush({ status: 409, message: 'La seccion 1001 no tiene cupos disponibles' }, { status: 409, statusText: 'Conflict' });
    backend.expectOne((r) => r.url === `${API}/secciones`).flush(OFERTA);
    expect(c.error()).toContain('Se actualizó la oferta');
  });
});
