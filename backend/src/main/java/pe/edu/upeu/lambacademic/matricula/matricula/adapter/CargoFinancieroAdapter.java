package pe.edu.upeu.lambacademic.matricula.matricula.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoGenerado;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoMatriculaCommand;
import pe.edu.upeu.lambacademic.finanzas.cargo.service.CargoService;
import pe.edu.upeu.lambacademic.matricula.matricula.port.CargoFinancieroPort;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CargoFinancieroAdapter implements CargoFinancieroPort {

    private final CargoService cargoService;

    @Override
    public CargoInfo generarCargo(Long matriculaId, Long estudianteId, Long periodoId, String periodoCodigo,
                                  Integer totalCreditos, Integer numeroCuotas) {
        return toInfo(cargoService.generarCargoMatricula(new CargoMatriculaCommand(
                matriculaId, estudianteId, periodoId, periodoCodigo, totalCreditos, numeroCuotas)));
    }

    @Override
    public void anularCargo(Long matriculaId) {
        cargoService.anularCargoDeMatricula(matriculaId);
    }

    @Override
    public Optional<CargoInfo> consultarCargo(Long matriculaId) {
        return cargoService.buscarCargoDeMatricula(matriculaId).map(CargoFinancieroAdapter::toInfo);
    }

    @Override
    public Map<Long, CargoInfo> consultarCargos(Collection<Long> matriculaIds) {
        Map<Long, CargoInfo> resultado = new HashMap<>();
        cargoService.buscarCargosDeMatriculas(matriculaIds).forEach((id, cargo) -> resultado.put(id, toInfo(cargo)));
        return resultado;
    }

    private static CargoInfo toInfo(CargoGenerado cargo) {
        return new CargoInfo(cargo.cargoId(), cargo.numeroCargo(), cargo.montoTotal(), cargo.numeroCuotas(),
                cargo.estado());
    }
}
