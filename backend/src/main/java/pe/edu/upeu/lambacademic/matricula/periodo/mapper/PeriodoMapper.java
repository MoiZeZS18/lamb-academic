package pe.edu.upeu.lambacademic.matricula.periodo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoRequest;
import pe.edu.upeu.lambacademic.matricula.periodo.dto.PeriodoResponse;
import pe.edu.upeu.lambacademic.matricula.periodo.entity.Periodo;

@Mapper(componentModel = "spring")
public interface PeriodoMapper {

    @Mapping(target = "id", ignore = true)
    Periodo toEntity(PeriodoRequest request);

    PeriodoResponse toResponse(Periodo periodo);
}