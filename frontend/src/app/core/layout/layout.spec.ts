import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { cerrarSesionDePrueba, iniciarSesionDePrueba } from '../../../testing/sesion-prueba';
import { Layout } from './layout';

async function enlacesDelMenu(): Promise<string[]> {
  await TestBed.configureTestingModule({ imports: [Layout], providers: [provideRouter([])] }).compileComponents();
  const fixture = TestBed.createComponent(Layout);
  await fixture.whenStable();
  const nav = fixture.nativeElement.querySelector('nav') as HTMLElement | null;
  return nav ? [...nav.querySelectorAll('a')].map((a) => a.textContent!.trim()) : [];
}

describe('Layout: menu por rol (S11 2.7)', () => {
  beforeEach(() => {
    cerrarSesionDePrueba();
    TestBed.resetTestingModule();
  });

  it('sin sesion no muestra menu y ofrece "Iniciar sesión"', async () => {
    await TestBed.configureTestingModule({ imports: [Layout], providers: [provideRouter([])] }).compileComponents();
    const fixture = TestBed.createComponent(Layout);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('nav')).toBeNull();
    expect(html.textContent).toContain('Iniciar sesión');
  });

  it('ESTUDIANTE: solo su informacion, sin la gestion academica', async () => {
    iniciarSesionDePrueba(['ESTUDIANTE'], 1001);
    const enlaces = await enlacesDelMenu();
    expect(enlaces).toEqual(['Portal de matrícula', 'Inscripción', 'Mis matrículas', 'Mi horario']);
  });

  it('COORDINADOR: gestion academica completa con Reportes', async () => {
    iniciarSesionDePrueba(['COORDINADOR']);
    expect(await enlacesDelMenu()).toEqual([
      'Portal de matrícula',
      'Inscripción',
      'Matrículas',
      'Horario',
      'Periodos',
      'Secciones',
      'Reportes',
    ]);
  });

  it('CAJERO: solo el portal (su trabajo esta en Finanzas)', async () => {
    iniciarSesionDePrueba(['CAJERO']);
    expect(await enlacesDelMenu()).toEqual(['Portal de matrícula']);
  });
});
