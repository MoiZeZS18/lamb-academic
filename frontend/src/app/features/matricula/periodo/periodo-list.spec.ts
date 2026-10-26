import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { API, PERIODO_ACTIVO, PERIODO_CERRADO } from '../../../../testing/datos-matricula';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../../testing/sesion-prueba';
import { PeriodoList } from './periodo-list';

describe('PeriodoList (CRUD independiente, S7)', () => {
  let backend: HttpTestingController;
  let confirmacion: boolean;
  let dialogos: number;

  async function crear(roles: string[]) {
    iniciarSesionDePrueba(roles);
    await TestBed.configureTestingModule({
      imports: [PeriodoList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: MatDialog,
          useValue: {
            open: () => {
              dialogos++;
              return { afterClosed: () => of(confirmacion) };
            },
          },
        },
      ],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PeriodoList);
    fixture.detectChanges();
    backend.expectOne(`${API}/periodos`).flush([PERIODO_CERRADO, PERIODO_ACTIVO]);
    await fixture.whenStable();
    return fixture;
  }

  function html(fixture: { nativeElement: unknown }) {
    return fixture.nativeElement as HTMLElement;
  }

  function eliminarDe(fixture: { nativeElement: unknown }, codigo: string) {
    const fila = [...html(fixture).querySelectorAll('tbody tr')].find(
      (tr) => tr.querySelector('td')!.textContent!.trim() === codigo,
    )!;
    return [...fila.querySelectorAll('button')].find((b) => b.textContent!.includes('Eliminar'))!;
  }

  beforeEach(() => {
    confirmacion = true;
    dialogos = 0;
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });
  afterEach(() => backend.verify());

  it('lista los periodos del mas reciente al mas antiguo', async () => {
    const fixture = await crear(['ADMIN']);
    const codigos = [...html(fixture).querySelectorAll('tbody tr')].map((tr) => tr.querySelector('td')!.textContent!.trim());
    expect(codigos).toEqual(['2026-II', '2026-I']);
  });

  it('ADMIN ve crear, editar y eliminar', async () => {
    const fixture = await crear(['ADMIN']);
    const texto = html(fixture).textContent!;
    expect(texto).toContain('Nuevo periodo');
    expect(texto).toContain('Editar');
    expect(eliminarDe(fixture, '2026-I')).toBeDefined();
  });

  it('COORDINADOR solo consulta (la escritura de periodos es de ADMIN en SecurityConfig)', async () => {
    const fixture = await crear(['COORDINADOR']);
    const texto = html(fixture).textContent!;
    expect(texto).not.toContain('Nuevo periodo');
    expect(html(fixture).querySelectorAll('tbody button').length).toBe(0);
  });

  it('si se cancela la confirmacion no envia el DELETE', async () => {
    const fixture = await crear(['ADMIN']);
    confirmacion = false;
    eliminarDe(fixture, '2026-I').click();
    expect(dialogos).toBe(1);
    backend.expectNone((r) => r.method === 'DELETE');
  });

  it('explica el rechazo por matriculas registradas con el mensaje real del backend', async () => {
    const fixture = await crear(['ADMIN']);
    eliminarDe(fixture, '2026-I').click();
    backend
      .expectOne((r) => r.method === 'DELETE' && r.url === `${API}/periodos/24`)
      .flush(
        { status: 409, message: 'El periodo 2026-I tiene matriculas registradas y no puede eliminarse' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();
    expect(html(fixture).querySelector('.mensaje-error')!.textContent).toContain('tiene matriculas registradas');
  });

  it('traduce el rechazo de integridad de Oracle (secciones del periodo) a un motivo comprensible', async () => {
    const fixture = await crear(['ADMIN']);
    eliminarDe(fixture, '2026-I').click();
    backend
      .expectOne((r) => r.method === 'DELETE')
      .flush(
        { status: 409, message: 'La operacion viola una restriccion de integridad de la base de datos' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();
    expect(html(fixture).querySelector('.mensaje-error')!.textContent).toContain(
      'No se puede eliminar 2026-I: tiene secciones programadas.',
    );
  });
});
