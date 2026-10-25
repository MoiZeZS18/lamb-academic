import { generarCodeChallenge, generarCodeVerifier, generarState } from './pkce';

describe('PKCE (RFC 7636)', () => {
  it('calcula el code_challenge S256 del ejemplo oficial del RFC 7636 (apendice B)', async () => {
    const challenge = await generarCodeChallenge('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk');
    expect(challenge).toBe('E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM');
  });

  it('genera verifier de 43 caracteres base64url y distinto en cada llamada', () => {
    const a = generarCodeVerifier();
    const b = generarCodeVerifier();
    expect(a).toMatch(/^[A-Za-z0-9_-]{43}$/);
    expect(a).not.toBe(b);
  });

  it('genera un state impredecible sin relleno base64', () => {
    expect(generarState()).toMatch(/^[A-Za-z0-9_-]{22}$/);
  });
});
