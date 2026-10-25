import { tokenDePrueba } from '../../../testing/sesion-prueba';
import { decodeJwtPayload } from './jwt';

describe('decodeJwtPayload', () => {
  it('lee roles, alumno_id y nombres con tildes', () => {
    const payload = decodeJwtPayload(tokenDePrueba(['ESTUDIANTE'], { alumno_id: 1033, name: 'José Apaza' }));
    expect(payload?.realm_access?.roles).toContain('ESTUDIANTE');
    expect(payload?.alumno_id).toBe(1033);
    expect(payload?.name).toBe('José Apaza');
  });

  it('devuelve null con un token mal formado', () => {
    expect(decodeJwtPayload('no-es-un-jwt')).toBeNull();
    expect(decodeJwtPayload('a.%%%.c')).toBeNull();
  });
});
