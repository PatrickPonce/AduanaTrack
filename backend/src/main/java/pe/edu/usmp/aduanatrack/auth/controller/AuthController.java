package pe.edu.usmp.aduanatrack.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.auth.dto.LoginRequest;
import pe.edu.usmp.aduanatrack.auth.dto.UsuarioSesionResponse;
import pe.edu.usmp.aduanatrack.auth.service.AuthService;
import pe.edu.usmp.aduanatrack.auth.service.SesionIniciada;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SesionCookieService cookieService;

    public AuthController(AuthService authService, SesionCookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioSesionResponse> login(@Valid @RequestBody LoginRequest solicitud,
            HttpServletRequest request) {
        // getRemoteAddr ya refleja X-Forwarded-For (server.forward-headers-strategy=native).
        SesionIniciada sesion = authService.login(solicitud, request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.crear(sesion.token(), sesion.duracion()).toString())
                .body(UsuarioSesionResponse.de(sesion.usuario()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.eliminar().toString())
                .build();
    }

    @GetMapping("/me")
    public UsuarioSesionResponse me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioSesionResponse.de(usuario);
    }
}
