package pe.edu.upeu.lambacademic.compartido.persistencia;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Oracle no tiene tipo TIME: las horas de clase se guardan como VARCHAR2(5) "HH:mm"
 * (validadas con CHECK REGEXP_LIKE en HORARIOS_AULA) y en Java se usan como LocalTime.
 */
@Converter
public class HoraConverter implements AttributeConverter<LocalTime, String> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String convertToDatabaseColumn(LocalTime hora) {
        return hora == null ? null : hora.format(FORMATO);
    }

    @Override
    public LocalTime convertToEntityAttribute(String valor) {
        return valor == null ? null : LocalTime.parse(valor, FORMATO);
    }
}
