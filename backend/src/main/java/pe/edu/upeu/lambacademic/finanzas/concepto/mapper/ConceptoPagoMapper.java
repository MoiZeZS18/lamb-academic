package pe.edu.upeu.lambacademic.finanzas.concepto.mapper;

import org.mapstruct.Mapper;
import pe.edu.upeu.lambacademic.finanzas.concepto.dto.ConceptoPagoResponse;
import pe.edu.upeu.lambacademic.finanzas.concepto.entity.ConceptoPago;

@Mapper(componentModel = "spring")
public interface ConceptoPagoMapper {

    ConceptoPagoResponse toResponse(ConceptoPago concepto);
}
