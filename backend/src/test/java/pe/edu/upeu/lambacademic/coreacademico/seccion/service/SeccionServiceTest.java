package pe.edu.upeu.lambacademic.coreacademico.seccion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.edu.upeu.lambacademic.baseuniversal.persona.service.PersonaService;
import pe.edu.upeu.lambacademic.coreacademico.curso.entity.Curso;
import pe.edu.upeu.lambacademic.coreacademico.curso.repository.CursoRepository;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.HorarioAulaRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionActualizacionRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.dto.SeccionRequest;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.DiaSemana;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.EstadoSeccion;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.HorarioAula;
import pe.edu.upeu.lambacademic.coreacademico.seccion.entity.Seccion;
import pe.edu.upeu.lambacademic.coreacademico.seccion.mapper.SeccionMapper;
import pe.edu.upeu.lambacademic.coreacademico.seccion.port.PeriodoAcademicoPort;
import pe.edu.upeu.lambacademic.coreacademico.seccion.port.SeccionEnUsoPort;
import pe.edu.upeu.lambacademic.coreacademico.seccion.repository.SeccionRepository;
import pe.edu.upeu.lambacademic.exception.ReglaNegocioException;
import pe.edu.upeu.lambacademic.exception.ResourceNotFoundException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeccionServiceTest {

    @Mock private SeccionRepository seccionRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private SeccionMapper mapper;
    @Mock private PersonaService personaService;
    @Mock private PeriodoAcademicoPort periodoPort;
    @Mock private SeccionEnUsoPort seccionEnUsoPort;

    @InjectMocks private SeccionService service;

    @BeforeEach
    void setUp() {
        Curso curso = Curso.builder().id(1L).codigo("IS101").nombre("LP II").creditos(4).activo(true).build();
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        when(periodoPort.buscarPeriodo(4L))
                .thenReturn(Optional.of(new PeriodoAcademicoPort.PeriodoInfo(4L, "2026-II", "2026-II", "ACTIVO")));
        when(mapper.toEntity(any(HorarioAulaRequest.class))).thenAnswer(inv -> {
            HorarioAulaRequest r = inv.getArgument(0);
            HorarioAula h = new HorarioAula();
            h.setDiaSemana(r.diaSemana());
            h.setHoraInicio(r.horaInicio());
            h.setHoraFin(r.horaFin());
            h.setAula(r.aula());
            return h;
        });
        when(seccionRepository.cruceDeAula(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(seccionRepository.cruceDeDocente(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void periodoInexistente() {
        when(periodoPort.buscarPeriodo(4L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.programar(request(horario(DiaSemana.LUNES, 8, 10, "A-1"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void periodoCerrado() {
        when(periodoPort.buscarPeriodo(4L))
                .thenReturn(Optional.of(new PeriodoAcademicoPort.PeriodoInfo(4L, "2026-II", "2026-II", "CERRADO")));
        assertThatThrownBy(() -> service.programar(request(horario(DiaSemana.LUNES, 8, 10, "A-1"))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cerrado");
    }

    @Test
    void horaInicioPosteriorAFin() {
        assertThatThrownBy(() -> service.programar(request(horario(DiaSemana.LUNES, 10, 8, "A-1"))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("anterior");
        verify(seccionRepository, never()).save(any());
    }

    @Test
    void horarioFueraDeFranja() {
        assertThatThrownBy(() -> service.programar(request(horario(DiaSemana.LUNES, 6, 8, "A-1"))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("entre");
    }

    @Test
    void horariosDeLaSeccionSeCruzan() {
        assertThatThrownBy(() -> service.programar(request(
                horario(DiaSemana.LUNES, 8, 10, "A-1"), horario(DiaSemana.LUNES, 9, 11, "A-2"))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("se cruzan");
    }

    @Test
    @DisplayName("El aula ya esta ocupada por otra seccion del periodo en ese rango")
    void cruceDeAula() {
        Seccion otra = new Seccion();
        otra.setId(1005L);
        otra.setCurso(Curso.builder().id(2L).codigo("IS102").build());
        HorarioAula ocupado = new HorarioAula();
        ocupado.setSeccion(otra);
        ocupado.setHoraInicio(LocalTime.of(9, 0));
        ocupado.setHoraFin(LocalTime.of(11, 0));
        when(seccionRepository.cruceDeAula(eq(4L), any(), eq(DiaSemana.LUNES), eq("A-1"), any(), any(), any()))
                .thenReturn(List.of(ocupado));

        assertThatThrownBy(() -> service.programar(request(horario(DiaSemana.LUNES, 8, 10, "A-1"))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("ya esta ocupada");
    }

    @Test
    void grupoDuplicado() {
        when(seccionRepository.existsByPeriodoIdAndCursoIdAndGrupo(4L, 1L, "A")).thenReturn(true);
        assertThatThrownBy(() -> service.programar(request(horario(DiaSemana.LUNES, 8, 10, "A-1"))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Ya existe el grupo");
    }

    @Test
    @DisplayName("El limite de vacantes no puede bajar por debajo de los cupos ocupados")
    void limiteMenorQueOcupados() {
        Seccion seccion = new Seccion();
        seccion.setId(1001L);
        seccion.setCuposOcupados(10);
        seccion.setEstado(EstadoSeccion.ABIERTA);
        when(seccionRepository.findDetalladaById(1001L)).thenReturn(Optional.of(seccion));

        assertThatThrownBy(() -> service.actualizar(1001L, new SeccionActualizacionRequest(null, 5)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cupos ya ocupados");
    }

    @Test
    @DisplayName("Reserva sin vacantes: el UPDATE atomico no afecta filas y se informa sin cupos")
    void reservaSinCupos() {
        Seccion seccion = new Seccion();
        seccion.setId(1003L);
        seccion.setEstado(EstadoSeccion.ABIERTA);
        when(seccionRepository.reservarCupo(1003L)).thenReturn(0);
        when(seccionRepository.findDetalladaById(1003L)).thenReturn(Optional.of(seccion));

        assertThatThrownBy(() -> service.reservarCupo(1003L))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no tiene cupos");
    }

    @Test
    void cancelarConMatriculadosSeRechaza() {
        Seccion seccion = new Seccion();
        seccion.setId(1001L);
        seccion.setCuposOcupados(3);
        seccion.setEstado(EstadoSeccion.ABIERTA);
        when(seccionRepository.findDetalladaById(anyLong())).thenReturn(Optional.of(seccion));

        assertThatThrownBy(() -> service.cambiarEstado(1001L, EstadoSeccion.CANCELADA))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("matriculados");
    }

    private static SeccionRequest request(HorarioAulaRequest... horarios) {
        return new SeccionRequest(1L, 4L, "A", null, 30, List.of(horarios));
    }

    @Test
    @DisplayName("[U2, S8] Seccion sin matriculas en periodo activo: se elimina")
    void eliminarSeccionSinUso() {
        Seccion seccion = seccionGuardada(0);
        when(seccionRepository.findDetalladaById(9L)).thenReturn(Optional.of(seccion));
        when(seccionEnUsoPort.tieneMatriculas(9L)).thenReturn(false);

        service.eliminar(9L);

        verify(seccionRepository).delete(seccion);
    }

    @Test
    @DisplayName("[U2, S8] Seccion que aparece en una matricula (aunque anulada y sin cupos): 409, se cancela")
    void eliminarSeccionUsadaEnMatricula() {
        when(seccionRepository.findDetalladaById(9L)).thenReturn(Optional.of(seccionGuardada(0)));
        when(seccionEnUsoPort.tieneMatriculas(9L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(9L))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Cancelala");
        verify(seccionRepository, never()).delete(any(Seccion.class));
    }

    @Test
    @DisplayName("[U2, S8] Seccion con cupos ocupados: 409 sin consultar Matricula")
    void eliminarSeccionConCupos() {
        when(seccionRepository.findDetalladaById(9L)).thenReturn(Optional.of(seccionGuardada(2)));

        assertThatThrownBy(() -> service.eliminar(9L)).isInstanceOf(ReglaNegocioException.class);
        verify(seccionRepository, never()).delete(any(Seccion.class));
    }

    @Test
    @DisplayName("[U2, S8] Seccion de un periodo cerrado: 409 (conserva la historia)")
    void eliminarSeccionDePeriodoCerrado() {
        when(periodoPort.buscarPeriodo(4L))
                .thenReturn(Optional.of(new PeriodoAcademicoPort.PeriodoInfo(4L, "2026-I", "2026-I", "CERRADO")));
        when(seccionRepository.findDetalladaById(9L)).thenReturn(Optional.of(seccionGuardada(0)));

        assertThatThrownBy(() -> service.eliminar(9L))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cerrado");
        verify(seccionRepository, never()).delete(any(Seccion.class));
    }

    private static Seccion seccionGuardada(int cuposOcupados) {
        Seccion seccion = new Seccion();
        seccion.setId(9L);
        seccion.setCurso(Curso.builder().id(1L).codigo("IS101").nombre("LP II").creditos(4).activo(true).build());
        seccion.setPeriodoId(4L);
        seccion.setGrupo("Z");
        seccion.setCuposOcupados(cuposOcupados);
        return seccion;
    }

    private static HorarioAulaRequest horario(DiaSemana dia, int desde, int hasta, String aula) {
        return new HorarioAulaRequest(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), aula);
    }
}
