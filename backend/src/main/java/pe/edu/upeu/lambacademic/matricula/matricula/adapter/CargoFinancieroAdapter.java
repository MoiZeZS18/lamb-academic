package pe.edu.upeu.lambacademic.matricula.matricula.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoGenerado;
import pe.edu.upeu.lambacademic.finanzas.cargo.dto.CargoMatriculaCommand;
import pe.edu.upeu.lambacademic.finanzas.cargo.service.CargoService;
import pe.edu.upeu.lambacademic.matricula.matricula.port.CargoFinancieroPort;

@Component
@RequiredArgsConstructor
public class CargoFinancieroAdapter implements CargoFinancieroPort {

    private final CargoService cargoService;

    @Override
    public CargoInfo generarCargo(Long matriculaId, Long estudianteId, Long periodoId, String periodoCodigo,
                                  Integer totalCreditos, Integer numeroCuotas) {
        CargoGenerado cargo = cargoService.generarCargoMatricula(new CargoMatriculaCommand(
                matriculaId, estudianteId, periodoId, periodoCodigo, totalCreditos, numeroCuotas));
        return new CargoInfo(cargo.cargoId(), cargo.numeroCargo(), cargo.montoTotal(), cargo.numeroCuotas(),
                cargo.estado());
    }

    @Override
    public void anularCargo(Long matriculaId) {
        cargoService.anularCargoDeMatricula(matriculaId);
    }
}
