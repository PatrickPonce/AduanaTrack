package pe.edu.usmp.aduanatrack.cliente.validacion;

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RucValidator implements ConstraintValidator<RucValido, String> {

    private static final Set<String> PREFIJOS = Set.of("10", "15", "16", "17", "20");
    private static final int[] FACTORES = { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };

    /** Los nulos y el formato de 11 dígitos los validan {@code @NotBlank} y {@code @Pattern}. */
    @Override
    public boolean isValid(String ruc, ConstraintValidatorContext context) {
        if (ruc == null || !ruc.matches("\\d{11}")) {
            return true;
        }
        return esValido(ruc);
    }

    public static boolean esValido(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}") || !PREFIJOS.contains(ruc.substring(0, 2))) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < FACTORES.length; i++) {
            suma += Character.getNumericValue(ruc.charAt(i)) * FACTORES[i];
        }
        int digito = 11 - (suma % 11);
        if (digito == 10) {
            digito = 0;
        } else if (digito == 11) {
            digito = 1;
        }
        return digito == Character.getNumericValue(ruc.charAt(10));
    }
}
