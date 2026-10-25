import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { API, PERIODO_ACTIVO, matriculaDePrueba, seccionDePrueba } from '../../../../testing/datos-matricula';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { MatriculaDetalle } from './matricula-detalle';

describe('MatriculaDetalle', () => {
  let backend: HttpTestingController;

  async function crear(id: string) {
    iniciarSesionDePrueba(['ESTUDIANTE'], 1001);
    await TestBed.configureTestingModule({
      imports: [MatriculaDetalle],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: MatDialog, useValue: {} }],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MatriculaDetalle);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    return fixture;
  }

  function responderDatos() {
    backend.expectOne(`${API}/periodos/4`).flush(PERIODO_ACTIVO);
    backend.expectOne((r) => r.url === `${API}/secciones`).flush([seccionDePrueba(1001, 'IS101')]);
    backend.expectOne((r) => r.url === `${API}/cursos`).flush([]);
  }

  beforeEach(() => {
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
});
