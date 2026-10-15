package pe.edu.usmp.aduanatrack.embarque.validacion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Número de contenedor ISO 6346: 3 letras del propietario, categoría U/J/Z, 6 dígitos de
 * serie y el dígito de control (HU-05 · T-05.2).
 */
@Documented
@Constraint(validatedBy = ContenedorIso6346Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContenedorIso6346 {

    String message() default "Número de contenedor no válido: revise el dígito de control ISO 6346";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
