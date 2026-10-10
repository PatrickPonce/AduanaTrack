package pe.edu.usmp.aduanatrack.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import pe.edu.usmp.aduanatrack.config.SeguridadProperties;

/** Emite y valida el JWT (HS256) que viaja en la cookie de sesión. */
@Service
public class JwtService {

    private static final String CLAIM_USUARIO = "usr";
    private static final String CLAIM_NOMBRE = "nom";
    private static final String CLAIM_ROL = "rol";

    private final SecretKey clave;
    private final Clock clock;

    public JwtService(SeguridadProperties propiedades, Clock clock) {
        this.clave = Keys.hmacShaKeyFor(propiedades.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String generar(UsuarioAutenticado usuario, Duration duracion) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(usuario.id()))
                .claim(CLAIM_USUARIO, usuario.usuario())
                .claim(CLAIM_NOMBRE, usuario.nombre())
                .claim(CLAIM_ROL, usuario.rol())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /** Devuelve el usuario del token, o vacío si la firma no es válida o el token expiró. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new UsuarioAutenticado(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_USUARIO, String.class),
                    claims.get(CLAIM_NOMBRE, String.class),
                    claims.get(CLAIM_ROL, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
