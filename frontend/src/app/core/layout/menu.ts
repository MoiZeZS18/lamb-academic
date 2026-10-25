import {
  LucideCalendarDays,
  LucideCalendarRange,
  LucideChartColumn,
  LucideClipboardPen,
  LucideIcon,
  LucideLayers,
  LucideLayoutDashboard,
  LucideListChecks,
} from '@lucide/angular';
import { ROLES_COORDINACION, ROLES_MATRICULA } from '../auth/roles';

/**
 * Menu lateral declarativo (patron reutilizado de Cristian, core/menu.ts). `roles` repite las
 * mismas reglas de las rutas (app.routes.ts) y de SecurityConfig: el menu es UX, no seguridad.
 * Sin `roles`: cualquier usuario con sesion.
 */
export interface ItemMenu {
  etiqueta: string;
  ruta: string;
  icono: LucideIcon;
  roles?: string[];
  exacto?: boolean;
}

export interface GrupoMenu {
  titulo: string;
  items: ItemMenu[];
}

export const MENU: GrupoMenu[] = [
  {
    titulo: 'Matrícula',
    items: [
      { etiqueta: 'Portal de matrícula', ruta: '/matricula', icono: LucideLayoutDashboard, exacto: true },
      { etiqueta: 'Inscripción', ruta: '/matricula/inscripcion', icono: LucideClipboardPen, roles: ROLES_MATRICULA },
      { etiqueta: 'Matrículas', ruta: '/matricula/consulta', icono: LucideListChecks, roles: ROLES_MATRICULA },
      { etiqueta: 'Horario', ruta: '/matricula/horario', icono: LucideCalendarDays, roles: ROLES_MATRICULA },
    ],
  },
  {
    titulo: 'Gestión académica',
    items: [
      { etiqueta: 'Periodos', ruta: '/matricula/periodos', icono: LucideCalendarRange },
      { etiqueta: 'Secciones', ruta: '/matricula/secciones', icono: LucideLayers },
      { etiqueta: 'Reportes', ruta: '/matricula/reportes', icono: LucideChartColumn, roles: ROLES_COORDINACION },
    ],
  },
];
