package pe.edu.upeu.lambacademic.finanzas.cargo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CuotaResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.PagoResponse;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.Cargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.DetalleCargo;
import pe.edu.upeu.lambacademic.finanzas.cargo.entity.Pago;

@Mapper(componentModel = "spring")
public interface CargoMapper {

    CargoResponse toResponse(Cargo cargo);

    CuotaResponse toResponse(DetalleCargo cuota);

    @Mapping(target = "cargoId", source = "cargo.id")
    PagoResponse toResponse(Pago pago);
}
