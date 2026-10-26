"""Claves temporales de demostracion para los usuarios de prueba del realm lamb-academic (solo local).

Genera claves distintas por usuario, faciles de escribir frente al docente (Palabra-Palabra-NNN),
y las guarda en infra/secretos/keycloak-usuarios.env (ignorado por Git). Antes respalda las claves
vigentes en infra/secretos/keycloak-usuarios.anterior.env (tambien ignorado). Nunca imprime claves.

Despues de generar, se aplican en Keycloak (puerto 48080) con el mecanismo administrativo existente:
    python -I infra/keycloak/sembrar_usuarios.py

Uso:
    python -I infra/keycloak/claves_demostracion.py            # genera claves de demostracion
    python -I infra/keycloak/claves_demostracion.py --restaurar # vuelve a las claves respaldadas
    python -I infra/keycloak/claves_demostracion.py --nuevas    # claves largas aleatorias (despues de la demo)
"""
import os
import secrets
import shutil
import string
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ACTUAL = os.path.join(RAIZ, "secretos", "keycloak-usuarios.env")
RESPALDO = os.path.join(RAIZ, "secretos", "keycloak-usuarios.anterior.env")
USUARIOS = ["ESTUDIANTE_ANA", "ESTUDIANTE_JOSE", "COORDINADOR", "ADMIN", "CAJERO"]

# Palabras comunes sin tildes ni enies (se escriben igual en cualquier teclado).
PALABRAS = [
    "Arena", "Bosque", "Cedro", "Cielo", "Cobre", "Coral", "Cumbre", "Delta", "Duna", "Faro",
    "Fuente", "Glaciar", "Granito", "Isla", "Jade", "Lago", "Laurel", "Luna", "Marea", "Mesa",
    "Monte", "Nube", "Olivo", "Onda", "Palma", "Pampa", "Perla", "Pino", "Prisma", "Puerto",
    "Quinua", "Rayo", "Rio", "Roble", "Ruta", "Sauce", "Selva", "Sierra", "Sol", "Tierra",
    "Trigo", "Valle", "Viento", "Volcan", "Yunque", "Zafiro", "Ambar", "Brisa", "Condor", "Llama",
]


def clave_legible():
    return f"{secrets.choice(PALABRAS)}-{secrets.choice(PALABRAS)}-{secrets.randbelow(900) + 100}"


def clave_larga(longitud=20):
    alfabeto = string.ascii_letters + string.digits
    return "".join(secrets.choice(alfabeto) for _ in range(longitud))


def escribir(claves):
    temporal = ACTUAL.replace(".env", ".tmp.env")  # tambien ignorado por Git (*.env)
    with open(temporal, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Claves locales de los usuarios de prueba (nunca en Git). Ver docs/SEGURIDAD_KEYCLOAK.md.\n")
        for usuario in USUARIOS:
            f.write(f"{usuario}_PASSWORD={claves[usuario]}\n")
    os.replace(temporal, ACTUAL)


def main():
    modo = sys.argv[1] if len(sys.argv) > 1 else ""
    if modo == "--restaurar":
        if not os.path.exists(RESPALDO):
            sys.exit("No hay respaldo de claves anteriores (keycloak-usuarios.anterior.env).")
        shutil.copyfile(RESPALDO, ACTUAL)
        print("Claves anteriores restauradas en keycloak-usuarios.env. Aplica con sembrar_usuarios.py.")
        return
    if modo not in ("", "--nuevas"):
        sys.exit(__doc__)
    if os.path.exists(ACTUAL) and not os.path.exists(RESPALDO):
        shutil.copyfile(ACTUAL, RESPALDO)
        print("Respaldo de las claves vigentes: infra/secretos/keycloak-usuarios.anterior.env")
    generador = clave_larga if modo == "--nuevas" else clave_legible
    claves = {}
    while len(set(claves.values())) < len(USUARIOS):  # todas distintas
        claves = {usuario: generador() for usuario in USUARIOS}
    escribir(claves)
    tipo = "largas aleatorias" if modo == "--nuevas" else "de demostracion"
    print(f"Claves {tipo} distintas para {len(USUARIOS)} usuarios guardadas en infra/secretos/keycloak-usuarios.env.")
    print("Aplicalas en Keycloak con: python -I infra/keycloak/sembrar_usuarios.py")


if __name__ == "__main__":
    main()
