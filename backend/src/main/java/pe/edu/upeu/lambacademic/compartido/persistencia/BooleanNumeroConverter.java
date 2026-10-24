package pe.edu.upeu.lambacademic.compartido.persistencia;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Oracle 23ai tiene tipo BOOLEAN nativo y Hibernate lo espera por defecto para Boolean,
 * pero los scripts BD2 del equipo usan NUMBER(1) con CHECK (0, 1). Este convertidor se
 * aplica a todos los atributos Boolean para que ddl-auto: validate acepte NUMBER(1).
 */
@Converter(autoApply = true)
public class BooleanNumeroConverter implements AttributeConverter<Boolean, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Boolean valor) {
        if (valor == null) {
            return null;
        }
        return valor ? 1 : 0;
    }

    @Override
    public Boolean convertToEntityAttribute(Integer valor) {
        if (valor == null) {
            return null;
        }
        return valor == 1;
    }
}
