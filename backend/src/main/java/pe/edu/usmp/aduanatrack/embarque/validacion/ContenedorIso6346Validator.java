package pe.edu.usmp.aduanatrack.embarque.validacion;

import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContenedorIso6346Validator implements ConstraintValidator<ContenedorIso6346, String> {

    private static final Pattern FORMATO = Pattern.compile("[A-Z]{3}[UJZ]\\d{7}");
    private static final int POSICIONES_CON_PESO = 10;

    /**
     * Los nulos y la forma del número los validan {@code @NotBlank} y {@code @Pattern}; aquí
     * solo se rechaza el que tiene la forma correcta y el dígito de control errado, para que
     * cada fila muestre un único mensaje.
     */
    @Override
    public boolean isValid(String numero, ConstraintValidatorContext context) {
        if (numero == null) {
            return true;
        }
        String normalizado = normalizar(numero);
        return !FORMATO.matcher(normalizado).matches() || digitoDeControl(normalizado) == ultimoDigito(normalizado);
    }

    /** Acepta minúsculas y espacios alrededor, igual que el guardado del embarque. */
    public static boolean esValido(String numero) {
        if (numero == null) {
            return false;
        }
        String normalizado = normalizar(numero);
        return FORMATO.matcher(normalizado).matches() && digitoDeControl(normalizado) == ultimoDigito(normalizado);
    }

    /**
     * Suma de los 10 primeros caracteres por 2^posición, módulo 11; si el resto es 10, el
     * dígito es 0.
     */
    static int digitoDeControl(String numero) {
        int suma = 0;
        for (int i = 0; i < POSICIONES_CON_PESO; i++) {
            suma += valor(numero.charAt(i)) << i;
        }
        return suma % 11 % 10;
    }

    /** Dígitos: su valor. Letras: A=10 … Z=38, saltando los múltiplos de 11 (11, 22 y 33). */
    private static int valor(char c) {
        if (Character.isDigit(c)) {
            return c - '0';
        }
        int valor = 10 + (c - 'A');
        return valor + (c - 'A' + 9) / 10;
    }

    private static int ultimoDigito(String numero) {
        return numero.charAt(POSICIONES_CON_PESO) - '0';
    }

    private static String normalizar(String numero) {
        return numero.trim().toUpperCase(Locale.ROOT);
    }
}
