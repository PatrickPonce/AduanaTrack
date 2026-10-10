package pe.edu.usmp.aduanatrack.cliente.validacion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** RUC peruano: prefijo 10, 15, 16, 17 o 20 y dígito verificador módulo 11 (HU-04, CA1). */
@Documented
@Constraint(validatedBy = RucValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface RucValido {

    String message() default "RUC no válido: revise el dígito verificador";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
