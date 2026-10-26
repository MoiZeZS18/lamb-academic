import { Routes } from '@angular/router';
import { ROLES_COORDINACION, ROLES_MATRICULA, Rol } from './core/auth/roles';
import { authGuard } from './core/guards/auth-guard';
import { rolGuard } from './core/guards/rol-guard';

/**
 * Rutas de la SPA (S7 + S11 3.7). auth/callback queda fuera del layout y sin guard. Inicio es
 * publico. authGuard va en cada ruta con sesion y rolGuard, despues, en las que exigen rol; los
 * roles son los mismos de SecurityConfig (el backend decide de verdad).
 */
export const routes: Routes = [
  {
    path: 'auth/callback',
    loadComponent: () => import('./core/auth/auth-callback').then((m) => m.AuthCallback),
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/layout').then((m) => m.Layout),
    children: [
      {
        path: '',
        loadComponent: () => import('./core/inicio/inicio').then((m) => m.Inicio),
      },
      {
        path: 'matricula',
        canActivate: [authGuard],
        loadComponent: () => import('./features/matricula/matricula/portal-matricula').then((m) => m.PortalMatricula),
      },
      {
        path: 'matricula/inscripcion',
        canActivate: [authGuard, rolGuard(...ROLES_MATRICULA)],
        loadComponent: () => import('./features/matricula/matricula/matricula-form').then((m) => m.MatriculaForm),
      },
      {
        path: 'matricula/consulta',
        canActivate: [authGuard, rolGuard(...ROLES_MATRICULA)],
        loadComponent: () => import('./features/matricula/matricula/matricula-list').then((m) => m.MatriculaList),
      },
      {
        path: 'matricula/consulta/:id',
        canActivate: [authGuard, rolGuard(...ROLES_MATRICULA)],
        loadComponent: () =>
          import('./features/matricula/matricula/matricula-detalle').then((m) => m.MatriculaDetalle),
      },
      {
        path: 'matricula/horario',
        canActivate: [authGuard, rolGuard(...ROLES_MATRICULA)],
        loadComponent: () =>
          import('./features/matricula/matricula/horario-matricula').then((m) => m.HorarioMatricula),
      },
      {
        path: 'matricula/reportes',
        canActivate: [authGuard, rolGuard(...ROLES_COORDINACION)],
        loadComponent: () =>
          import('./features/matricula/matricula/reporte-matricula').then((m) => m.ReporteMatricula),
      },
      {
        path: 'matricula/periodos',
        // Pantallas de gestion: el estudiante ve la oferta en Inscripcion y su periodo en el portal.
        canActivate: [authGuard, rolGuard(...ROLES_COORDINACION)],
        loadComponent: () => import('./features/matricula/periodo/periodo-list').then((m) => m.PeriodoList),
      },
      {
        path: 'matricula/periodos/nuevo',
        canActivate: [authGuard, rolGuard(Rol.ADMIN)],
        loadComponent: () => import('./features/matricula/periodo/periodo-form').then((m) => m.PeriodoForm),
      },
      {
        path: 'matricula/periodos/:id/editar',
        canActivate: [authGuard, rolGuard(Rol.ADMIN)],
        loadComponent: () => import('./features/matricula/periodo/periodo-form').then((m) => m.PeriodoForm),
      },
      {
        path: 'matricula/secciones',
        // Pantallas de gestion: el estudiante ve la oferta en Inscripcion y su periodo en el portal.
        canActivate: [authGuard, rolGuard(...ROLES_COORDINACION)],
        loadComponent: () => import('./features/matricula/seccion/seccion-list').then((m) => m.SeccionList),
      },
      {
        path: 'matricula/secciones/nueva',
        canActivate: [authGuard, rolGuard(...ROLES_COORDINACION)],
        loadComponent: () => import('./features/matricula/seccion/seccion-form').then((m) => m.SeccionForm),
      },
      {
        path: 'matricula/secciones/:id/editar',
        canActivate: [authGuard, rolGuard(...ROLES_COORDINACION)],
        loadComponent: () => import('./features/matricula/seccion/seccion-form').then((m) => m.SeccionForm),
      },
      {
        path: 'sin-permiso',
        loadComponent: () => import('./core/sin-permiso/sin-permiso').then((m) => m.SinPermiso),
      },
      {
        path: '**',
        loadComponent: () => import('./core/no-encontrado/no-encontrado').then((m) => m.NoEncontrado),
      },
    ],
  },
];
