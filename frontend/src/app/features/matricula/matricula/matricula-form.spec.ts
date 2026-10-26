import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { pagina } from '../../../../testing/datos-matricula';
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

const ALUMNOS = [
  alumno(1001, '2026100001', 'Quispe Mamani Ana Lucia', '70000001'),
  alumno(1033, '2026100033', 'Apaza Condori Jose Luis', '70000033'),
];

function alumno(id: number, codigoEstudiante: string, nombreCompleto: string, numeroDocumento: string) {
  return {
    id,
    codigoEstudiante,
    estado: 'ACTIVO',
    fechaIngreso: '2026-03-01',
    habilitado: true,
    persona: { id, numeroDocumento, nombreCompleto, email: null, idEscuela: 1, escuelaNombre: 'Sistemas' },
  };
}

/** Plan vigente de la escuela 1 (forma de PlanEstudioResponse); IS103 queda fuera a proposito. */
const PLAN = {
  id: 1,
  idEscuela: 1,
  escuelaNombre: 'Escuela Profesional de Ingenieria de Sistemas',
  codigo: 'PLAN-EPIS-2026',
  nombre: 'Plan EPIS 2026',
  anio: 2026,
  version: 1,
  estado: 'VIGENTE',
  totalCreditos: 11,
  totalCursos: 3,
  detalles: [
    { id: 1, idCurso: 1, cursoCodigo: 'IS101', cursoNombre: 'Curso IS101', ciclo: 3, creditos: 4, tipo: 'OBLIGATORIO', idPrerrequisito: null, prerrequisitoCodigo: null },
    { id: 2, idCurso: 2, cursoCodigo: 'IS102', cursoNombre: 'Curso IS102', ciclo: 3, creditos: 4, tipo: 'OBLIGATORIO', idPrerrequisito: null, prerrequisitoCodigo: null },
    { id: 3, idCurso: 4, cursoCodigo: 'IS104', cursoNombre: 'Curso IS104', ciclo: 4, creditos: 3, tipo: 'ELECTIVO', idPrerrequisito: 1, prerrequisitoCodigo: 'IS101' },
  ],
};

function contexto(estudianteId: number, extra: object = {}) {
  return { estudianteId, existe: true, habilitado: true, escuelaId: 1, ...extra };
}

type Interno = {
  cicloFiltro: { set(v: number | null): void };
  soloDisponibles: { set(v: boolean): void };
  ofertaFiltrada(): Seccion[];
  noHabilitado(): boolean;
  busquedaAlumno: { set(v: string): void };
  alumnosFiltrados(): { id: number }[];
  bloqueosEnvio(): string[];
  form: { controls: { estudianteId: { setValue(v: number): void } } };
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

  async function crear(roles = ['ESTUDIANTE'], contextoEstudiante: object = contexto(1033)) {
    const estudiante = roles.includes('ESTUDIANTE');
    iniciarSesionDePrueba(roles, estudiante ? 1033 : undefined);
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
    if (estudiante) {
      backend.expectOne((r) => r.url === `${API}/matriculas`).flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
      // El ESTUDIANTE no envia estudianteId: el backend usa el alumno_id del token.
      const ctx = backend.expectOne(`${API}/matriculas/contexto`);
      expect(ctx.request.params.has('estudianteId')).toBe(false);
      ctx.flush(contextoEstudiante);
      backend
        .match((r) => r.url === `${API}/planes-estudio`)
        .forEach((r) => {
          expect(r.request.params.get('idEscuela')).toBe('1');
          expect(r.request.params.get('estado')).toBe('VIGENTE');
          r.flush([PLAN]);
        });
    } else {
      backend.expectOne((r) => r.url === `${API}/alumnos` && r.params.get('estado') === 'ACTIVO').flush(ALUMNOS);
    }
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
    const { fixture, c } = await crear();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('#alumno')).toBeNull();
    expect(html.querySelector('#buscar-alumno')).toBeNull();
    expect(html.querySelector('.panel-lateral')!.textContent).toContain('Usuario Prueba');
    expect(c.bloqueosEnvio()).not.toContain('Elige el alumno.');
  });

  it('la COORDINACION busca al alumno por codigo, nombre o documento', async () => {
    const { fixture, c } = await crear(['COORDINADOR']);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('#alumno')).not.toBeNull();
    expect(c.bloqueosEnvio()).toContain('Elige el alumno.');
    c.busquedaAlumno.set('apaza');
    expect(c.alumnosFiltrados().map((a) => a.id)).toEqual([1033]);
    c.busquedaAlumno.set('70000001');
    expect(c.alumnosFiltrados().map((a) => a.id)).toEqual([1001]);
    c.form.controls.estudianteId.setValue(1033);
    backend.match((r) => r.url === `${API}/matriculas`).forEach((r) => r.flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } }));
    // El alumno elegido se conserva en la lista aunque ya no coincida con la busqueda.
    expect(c.alumnosFiltrados().map((a) => a.id)).toEqual([1001, 1033]);
    expect(c.bloqueosEnvio()).not.toContain('Elige el alumno.');
  });

  it('el panel lista lo que falta antes de registrar', async () => {
    const { c } = await crear();
    expect(c.bloqueosEnvio()).toEqual(['Elige al menos un curso.']);
    c.elegirSeccion(OFERTA[0]);
    c.elegirSeccion(OFERTA[2]);
    responderPrerrequisitos();
    expect(c.bloqueosEnvio()).toEqual(['1 cruce(s) de horario.']);
  });

  it('consulta los prerrequisitos con la escuela del alumno (la misma que valida el backend)', async () => {
    const { c } = await crear();
    c.elegirSeccion(OFERTA[2]);
    const req = backend.expectOne((r) => r.url === `${API}/cursos/4/prerrequisitos`);
    expect(req.request.params.get('idEscuela')).toBe('1');
    req.flush([]);
  });

  it('ubica cada curso en el ciclo de su plan y permite filtrar por ciclo', async () => {
    const { c, fixture } = await crear();
    const html = fixture.nativeElement as HTMLElement;
    const texto = html.textContent!.replace(/\s+/g, ' ');
    expect(texto).toContain('PLAN-EPIS-2026');
    expect(texto).toContain('Ciclo 4 · Electivo · Requiere IS101');
    c.soloDisponibles.set(false);
    await fixture.whenStable();
    expect(html.textContent).toContain('No figura en su plan de estudios');
    // Orden por ciclo: los del ciclo 3 primero; IS103 (fuera del plan) al final.
    expect(c.ofertaFiltrada().map((s) => s.curso.codigo)).toEqual(['IS101', 'IS102', 'IS104', 'IS103']);
    c.cicloFiltro.set(4);
    expect(c.ofertaFiltrada().map((s) => s.curso.codigo)).toEqual(['IS104']);
  });

  it('si Base Universal indica que el alumno no esta habilitado, no deja registrar', async () => {
    const { c, fixture } = await crear(['ESTUDIANTE'], contexto(1033, { habilitado: false }));
    await fixture.whenStable();
    expect(c.noHabilitado()).toBe(true);
    expect(c.bloqueosEnvio()).toContain('El alumno no está habilitado para matricularse (Base Universal).');
    const boton = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) =>
      b.textContent!.includes('Revisar y registrar'),
    )!;
    expect(boton.disabled).toBe(true);
  });

  it('la COORDINACION, al elegir alumno, consulta su escuela y los prerrequisitos de esa escuela', async () => {
    const { c, fixture } = await crear(['COORDINADOR']);
    const select = (fixture.nativeElement as HTMLElement).querySelector<HTMLSelectElement>('#alumno')!;
    select.value = [...select.options].find((o) => o.text.includes('Apaza'))!.value;
    select.dispatchEvent(new Event('change'));
    backend.match((r) => r.url === `${API}/matriculas`).forEach((r) => r.flush(pagina([])));
    const ctx = backend.expectOne((r) => r.url === `${API}/matriculas/contexto`);
    expect(ctx.request.params.get('estudianteId')).toBe('1033');
    ctx.flush(contexto(1033));
    backend.expectOne((r) => r.url === `${API}/planes-estudio`).flush([PLAN]);
    c.elegirSeccion(OFERTA[0]);
    const req = backend.expectOne((r) => r.url === `${API}/cursos/1/prerrequisitos`);
    expect(req.request.params.get('idEscuela')).toBe('1');
    req.flush([]);
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
