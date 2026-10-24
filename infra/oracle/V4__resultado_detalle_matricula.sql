-- =============================================================================
-- LAMB Academic - V4: resultado final del curso en cada detalle de matricula
-- Ejecutar como LAMB_APP despues de un respaldo Data Pump.
--
-- Motivo (PENDIENTES A1): un prerrequisito debe estar APROBADO, no solo "cursado".
-- No se crea un modulo de notas: solo se registra el resultado final por curso llevado
-- (APROBADO, DESAPROBADO, RETIRADO), una vez cerrado el periodo.
--
-- Script ADITIVO: columnas NULL. Las filas existentes (incluidas las 7 matriculas
-- previas) quedan con RESULTADO NULL = "sin resultado registrado".
-- Reversion: V4__rollback.sql (requiere autorizacion; pierde los resultados registrados).
-- =============================================================================

WHENEVER SQLERROR EXIT FAILURE ROLLBACK
SET ECHO ON

ALTER TABLE DETALLE_MATRICULA ADD (
    RESULTADO        VARCHAR2(15),
    FECHA_RESULTADO  TIMESTAMP
);

ALTER TABLE DETALLE_MATRICULA ADD CONSTRAINT CK_DET_MATRICULA_RESULTADO
    CHECK (RESULTADO IS NULL OR RESULTADO IN ('APROBADO', 'DESAPROBADO', 'RETIRADO'));

ALTER TABLE DETALLE_MATRICULA ADD CONSTRAINT CK_DET_MATRICULA_RES_FECHA
    CHECK ((RESULTADO IS NULL AND FECHA_RESULTADO IS NULL) OR (RESULTADO IS NOT NULL AND FECHA_RESULTADO IS NOT NULL));

EXIT SUCCESS
