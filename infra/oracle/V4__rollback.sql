-- =============================================================================
-- LAMB Academic - REVERSION de V4 (NO SE EJECUTA AUTOMATICAMENTE)
-- Elimina los resultados registrados por curso. Solo con autorizacion y respaldo previo.
-- =============================================================================

WHENEVER SQLERROR CONTINUE
SET ECHO ON

ALTER TABLE DETALLE_MATRICULA DROP CONSTRAINT CK_DET_MATRICULA_RES_FECHA;
ALTER TABLE DETALLE_MATRICULA DROP CONSTRAINT CK_DET_MATRICULA_RESULTADO;
ALTER TABLE DETALLE_MATRICULA DROP (RESULTADO, FECHA_RESULTADO);

EXIT
