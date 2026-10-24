package pe.edu.upeu.lambacademic.baseuniversal.institucional.dto;

import java.util.List;

/** Arbol institucional Sede > Facultades > Escuelas (navegacion controlada por DTO, S3 2.5). */
public record ArbolResponse(Long id, String codigo, String nombre, List<FacultadArbol> facultades) {

    public record FacultadArbol(Long id, String codigo, String nombre, List<EscuelaArbol> escuelas) {
    }

    public record EscuelaArbol(Long id, String codigo, String nombre, String nivel) {
    }
}
