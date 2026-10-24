-- Huella de los datos de LAMB_APP: filas y hash del contenido por tabla, mas roles, cuotas y objetos.
-- Solo lectura. Ejecutar como SYSDBA dentro del contenedor (autenticacion del sistema operativo):
--   docker exec -i lamb-academic-oracle sqlplus -s / as sysdba < infra/oracle/verificacion/huella_datos.sql
-- Sirve para comparar el estado antes y despues de una operacion de mantenimiento.
SET FEEDBACK OFF HEADING OFF PAGESIZE 0 LINESIZE 200
ALTER SESSION SET CONTAINER = FREEPDB1;
SET SERVEROUTPUT ON SIZE UNLIMITED
DECLARE
    v_expr  CLOB;
    v_filas NUMBER;
    v_hash  NUMBER;
BEGIN
    FOR t IN (SELECT table_name FROM dba_tables WHERE owner = 'LAMB_APP' ORDER BY table_name) LOOP
        v_expr := NULL;
        FOR c IN (SELECT column_name FROM dba_tab_columns
                  WHERE owner = 'LAMB_APP' AND table_name = t.table_name ORDER BY column_id) LOOP
            v_expr := v_expr || CASE WHEN v_expr IS NULL THEN '' ELSE ' || ''|'' || ' END
                      || 'TO_CHAR("' || c.column_name || '")';
        END LOOP;
        EXECUTE IMMEDIATE 'SELECT COUNT(*), NVL(SUM(ORA_HASH(' || v_expr || ')), 0) FROM LAMB_APP."'
                          || t.table_name || '"' INTO v_filas, v_hash;
        DBMS_OUTPUT.PUT_LINE('TABLA ' || RPAD(t.table_name, 22) || ' filas=' || v_filas || ' hash=' || v_hash);
    END LOOP;
END;
/
SELECT 'ROL ' || granted_role FROM dba_role_privs WHERE grantee = 'LAMB_APP' ORDER BY 1;
SELECT 'CUOTA ' || tablespace_name || ' ' || max_bytes FROM dba_ts_quotas WHERE username = 'LAMB_APP';
SELECT 'OBJETOS ' || COUNT(*) FROM dba_objects WHERE owner = 'LAMB_APP';
SELECT 'RESTRICCIONES_INVALIDAS ' || COUNT(*) FROM dba_constraints
 WHERE owner = 'LAMB_APP' AND (status <> 'ENABLED' OR validated <> 'VALIDATED');
SELECT 'CUENTA ' || username || ' ' || account_status FROM dba_users
 WHERE username IN ('LAMB_APP', 'PDBADMIN', 'SYSTEM', 'SYS') ORDER BY 1;
EXIT
