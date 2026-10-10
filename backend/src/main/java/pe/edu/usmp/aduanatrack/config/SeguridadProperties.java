package pe.edu.usmp.aduanatrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "aduanatrack.seguridad")
public record SeguridadProperties(
        @NotBlank @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres") String jwtSecret,
        boolean cookieSecure) {
}
