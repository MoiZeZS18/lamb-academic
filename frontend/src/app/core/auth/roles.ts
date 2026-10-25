/**
 * Roles del realm lamb-academic. Son los mismos que exige SecurityConfig en el backend:
 * el menu y los guards solo reflejan esas reglas, nunca las reemplazan (S11 2.7).
 */
export const Rol = {
  ESTUDIANTE: 'ESTUDIANTE',
  COORDINADOR: 'COORDINADOR',
  ADMIN: 'ADMIN',
  CAJERO: 'CAJERO',
} as const;

export type Rol = (typeof Rol)[keyof typeof Rol];

/** Pueden registrar matriculas (el ESTUDIANTE solo la suya: lo valida el backend). */
export const ROLES_MATRICULA: Rol[] = [Rol.ESTUDIANTE, Rol.COORDINADOR, Rol.ADMIN];
/** Coordinacion academica: secciones, matriculas de cualquier alumno, anulacion y reportes. */
export const ROLES_COORDINACION: Rol[] = [Rol.COORDINADOR, Rol.ADMIN];
