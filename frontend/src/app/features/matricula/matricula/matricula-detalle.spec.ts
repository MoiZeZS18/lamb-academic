import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import {
  API,
  PERIODO_ACTIVO,
  alumnosDePrueba,
  matriculaDePrueba,
  matriculaHistorica,
  seccionDePrueba,
} from '../../../../testing/datos-matricula';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { MatriculaDetalle } from './matricula-detalle';

describe('MatriculaDetalle', () => {
  let backend: HttpTestingController;
  let confirmacion: boolean;

  async function crear(id: string, roles = ['ESTUDIANTE']) {
    iniciarSesionDePrueba(roles, roles.includes('ESTUDIANTE') ? 1001 : undefined);
    await TestBed.configureTestingModule({
      imports: [MatriculaDetalle],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(confirmacion) }) } },
      ],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MatriculaDetalle);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    return fixture;
  }

  function responderDatos(periodo: object = PERIODO_ACTIVO, coordinacion = false) {
    backend.expectOne(`${API}/periodos/4`).flush(periodo);
    backend.expectOne((r) => r.url === `${API}/secciones`).flush([seccionDePrueba(1001, 'IS101')]);
    backend.expectOne((r) => r.url === `${API}/cursos`).flush([]);
    if (coordinacion) backend.expectOne(`${API}/alumnos`).flush(alumnosDePrueba([1001]));
  }

  function botones(fixture: { nativeElement: unknown }) {
    return [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].map((b) => b.textContent!.trim());
  }

  beforeEach(() => {
    confirmacion = true;
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('al cambiar el :id de la ruta (componente reutilizado) vuelve a cargar la matricula', async () => {
    const fixture = await crear('93');
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93));
    responderDatos();
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('24/10/2026');

    fixture.componentRef.setInput('id', '94');
    fixture.detectChanges();
    backend.expectOne(`${API}/matriculas/94`).flush(matriculaDePrueba(94, { fecha: '2026-10-20' }));
    responderDatos();
    await fixture.whenStable();
    expect(html.textContent).toContain('20/10/2026');
    expect(html.textContent).not.toContain('24/10/2026');
  });

  it('descarta la respuesta de una carga anterior que llega tarde', async () => {
    const fixture = await crear('93');
    const anterior = backend.expectOne(`${API}/matriculas/93`);
    fixture.componentRef.setInput('id', '94');
    fixture.detectChanges();
    backend.expectOne(`${API}/matriculas/94`).flush(matriculaDePrueba(94, { fecha: '2026-10-20' }));
    responderDatos();
    // La respuesta de la 93 llega despues y no debe reemplazar a la 94 ni pedir mas datos.
    anterior.flush(matriculaDePrueba(93));
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('20/10/2026');
    expect(html.textContent).not.toContain('24/10/2026');
  });

  it('el ESTUDIANTE consulta su matricula sin acciones de gestion', async () => {
    const fixture = await crear('93');
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93, { totalCreditos: 4 }));
    responderDatos();
    await fixture.whenStable();
    const lista = botones(fixture);
    expect(lista).toContain('Imprimir constancia');
    expect(lista).not.toContain('Anular');
    expect(lista.some((b) => b.includes('Generar cargo'))).toBe(false);
    expect((fixture.nativeElement as HTMLElement).querySelector('select')).toBeNull();
    // El estudiante no pide la lista de alumnos (403 en el backend).
    backend.expectNone(`${API}/alumnos`);
  });

  it('la COORDINACION anula con confirmacion y recarga la matricula', async () => {
    const fixture = await crear('93', ['COORDINADOR']);
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93, { totalCreditos: 4 }));
    responderDatos(PERIODO_ACTIVO, true);
    await fixture.whenStable();
    const anular = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find(
      (b) => b.textContent!.trim() === 'Anular',
    )!;
    anular.click();
    backend.expectOne((r) => r.method === 'PATCH' && r.url === `${API}/matriculas/93/anular`).flush(
      matriculaDePrueba(93, { estado: 'ANULADA' }),
    );
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93, { estado: 'ANULADA' }));
    responderDatos(PERIODO_ACTIVO, true);
    await fixture.whenStable();
    expect(botones(fixture)).not.toContain('Anular');
  });

  it('si la COORDINACION cancela la confirmacion no anula', async () => {
    const fixture = await crear('93', ['COORDINADOR']);
    confirmacion = false;
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93, { totalCreditos: 4 }));
    responderDatos(PERIODO_ACTIVO, true);
    await fixture.whenStable();
    [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) => b.textContent!.trim() === 'Anular')!.click();
    backend.expectNone((r) => r.method === 'PATCH');
  });

  it('la COORDINACION genera el cargo que falta (POST /cargo) solo si hay creditos', async () => {
    const fixture = await crear('93', ['COORDINADOR']);
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93, { totalCreditos: 4, cargo: null }));
    responderDatos(PERIODO_ACTIVO, true);
    await fixture.whenStable();
    const generar = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) =>
      b.textContent!.includes('Generar cargo'),
    )!;
    generar.click();
    backend.expectOne((r) => r.method === 'POST' && r.url === `${API}/matriculas/93/cargo`).flush(matriculaDePrueba(93));
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93, { totalCreditos: 4 }));
    responderDatos(PERIODO_ACTIVO, true);
  });

  it('el resultado por curso solo se registra con el periodo CERRADO', async () => {
    const fixture = await crear('93', ['COORDINADOR']);
    backend.expectOne(`${API}/matriculas/93`).flush(matriculaDePrueba(93, { totalCreditos: 4 }));
    responderDatos({ ...PERIODO_ACTIVO, estado: 'CERRADO' }, true);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('select')).not.toBeNull();
  });

  it('un registro historico muestra a la COORDINACION por que requiere revision', async () => {
    const fixture = await crear('25', ['COORDINADOR']);
    backend.expectOne(`${API}/matriculas/25`).flush(matriculaHistorica(25, [100]));
    responderDatos(PERIODO_ACTIVO, true);
    await fixture.whenStable();
    const aviso = (fixture.nativeElement as HTMLElement).querySelector('.mensaje-alerta')!.textContent!;
    expect(aviso).toContain('Registro histórico que requiere revisión');
    expect(aviso).toContain('El alumno 1 no existe en Base Universal.');
    expect(aviso).toContain('La sección 100 no existe en la oferta del periodo.');
  });
});
