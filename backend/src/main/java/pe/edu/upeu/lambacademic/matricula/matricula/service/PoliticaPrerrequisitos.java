package pe.edu.upeu.lambacademic.matricula.matricula.service;

/**
 * Como se valida un prerrequisito al matricular (propiedad lamb.matricula.prerrequisitos.politica).
 */
public enum PoliticaPrerrequisitos {
    /** Por defecto: el prerrequisito debe tener resultado APROBADO en un periodo anterior. */
    APROBADO,
    /**
     * TEMPORAL: acepta un curso llevado en un periodo anterior sin resultado registrado
     * (excluye DESAPROBADO y RETIRADO). Solo para transicion mientras se cargan resultados.
     */
    CURSADO_TEMPORAL,
    NINGUNA
}
