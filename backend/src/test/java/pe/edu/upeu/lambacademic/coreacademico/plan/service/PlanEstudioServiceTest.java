package pe.edu.upeu.lambacademic.coreacademico.plan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.dto.EscuelaResponse;
import pe.edu.upeu.lambacademic.baseuniversal.institucional.service.InstitucionalService;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;
import pe.edu.upeu.lambacademic.coreacademico.curso.mapper.CursoMapper;
import pe.edu.upeu.lambacademic.coreacademico.curso.repository.CursoRepository;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanEstudioRequest;
import pe.edu.upeu.lambacademic.coreacademico.plan.dto.PlanEstudioResponse;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.DetallePlanEstudio;
import pe.edu.upeu.lambacademic.coreacademico.plan.entity.PlanEstudio;
import pe.edu.upeu.lambacademic.coreacademico.plan.repository.DetallePlanEstudioRepository;
import pe.edu.upeu.lambacademic.coreacademico.plan.repository.PlanEstudioRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Criterio 4 (cabecera-detalle) de la malla: exito, calculos y reglas que fuerzan rollback. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanEstudioServiceTest {

    @Mock private PlanEstudioRepository planRepository;
    @Mock private DetallePlanEstudioRepository detalleRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private InstitucionalService institucionalService;
    @Spy private CursoMapper cursoMapper = Mappers.getMapper(CursoMapper.class);

    @InjectMocks private PlanEstudioService service;

    @BeforeEach
    void setUp() {
        when(institucionalService.obtenerEscuela(1L))
                .thenReturn(new EscuelaResponse(1L, 1L, "FIA", "EPIS", "Ing. de Sistemas", "PREGRADO", true));
        when(planRepository.existsByCodigo(any())).thenReturn(false);
        when(planRepository.save(any(PlanEstudio.class))).thenAnswer(inv -> inv.getArgument(0));
        for (long id = 1; id <= 6; id++) {
            Curso curso = Curso.builder().id(id).codigo("IS10" + id).nombre("Curso " + id).creditos(4).activo(true).build();
            when(cursoRepository.findById(id)).thenReturn(Optional.of(curso));
        }
    }

    @Test
    @DisplayName("Exito: cabecera + detalles, totales calculados desde los detalles")
    void creaMallaValida() {
        PlanEstudioResponse plan = service.crear(request(
                detalle(1L, 3, 4, null), detalle(2L, 3, 4, null), detalle(3L, 4, 4, 1L), detalle(4L, 4, 3, null)));

        assertThat(plan.totalCursos()).isEqualTo(4);
        assertThat(plan.totalCreditos()).isEqualTo(15);
        assertThat(plan.detalles()).filteredOn(d -> d.idCurso() == 3L)
                .singleElement().extracting(PlanEstudioResponse.DetallePlanResponse::idPrerrequisito).isEqualTo(1L);
    }

    @Test
    @DisplayName("Regla 1: curso repetido en la malla -> rollback (no se guarda nada)")
    void cursoRepetido() {
        assertThatThrownBy(() -> service.crear(request(detalle(1L, 3, 4, null), detalle(1L, 4, 4, null))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("regla 1");
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regla 2: un ciclo con menos de 6 creditos se rechaza")
    void creditosPorCicloFueraDeRango() {
        assertThatThrownBy(() -> service.crear(request(detalle(1L, 3, 4, null))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("regla 2");
    }

    @Test
    @DisplayName("Un prerrequisito debe estar en un ciclo anterior del mismo plan")
    void prerrequisitoEnCicloPosterior() {
        assertThatThrownBy(() -> service.crear(request(detalle(1L, 3, 4, 2L), detalle(2L, 3, 4, null))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("ciclo anterior");
    }

    @Test
    void prerrequisitoFueraDelPlan() {
        assertThatThrownBy(() -> service.crear(request(detalle(1L, 3, 4, 6L), detalle(2L, 3, 4, null))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("mismo plan");
    }

    @Test
    @DisplayName("Regla 3: un electivo no puede ser prerrequisito")
    void electivoComoPrerrequisito() {
        PlanEstudioRequest.DetallePlanRequest electivo =
                new PlanEstudioRequest.DetallePlanRequest(1L, 3, 4, DetallePlanEstudio.Tipo.ELECTIVO, null);
        assertThatThrownBy(() -> service.crear(request(electivo, detalle(2L, 3, 4, null), detalle(3L, 4, 6, 1L))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("regla 3");
    }

    @Test
    void cursoInexistente() {
        assertThatThrownBy(() -> service.crear(request(detalle(99L, 3, 6, null))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void codigoDuplicado() {
        when(planRepository.existsByCodigo("PLAN-T")).thenReturn(true);
        assertThatThrownBy(() -> service.crear(request(detalle(1L, 3, 6, null))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Ya existe");
    }

    private static PlanEstudioRequest request(PlanEstudioRequest.DetallePlanRequest... detalles) {
        return new PlanEstudioRequest(1L, "PLAN-T", "Plan de prueba", 2026, List.of(detalles));
    }

    private static PlanEstudioRequest.DetallePlanRequest detalle(Long curso, int ciclo, int creditos, Long prerrequisito) {
        return new PlanEstudioRequest.DetallePlanRequest(curso, ciclo, creditos, DetallePlanEstudio.Tipo.OBLIGATORIO,
                prerrequisito);
    }
}
