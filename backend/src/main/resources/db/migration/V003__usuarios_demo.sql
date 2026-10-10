-- Usuarios de demostración del Sprint 1 (T-00.4 · SCRUM-24): uno por rol.
-- Solo se guarda el hash BCrypt; la contraseña de demo se comparte fuera del repositorio.
-- Semgrep marca los hashes (detected-bcrypt-hash): hallazgo aceptado, son cuentas de demostración
-- (docs/seguridad/sprint-1/02-hallazgos-y-correcciones.md, #8).

INSERT INTO usuario (usuario, correo, nombre, password_hash, rol_id)
SELECT d.usuario, d.correo, d.nombre, d.password_hash, r.id
FROM (VALUES
    ('admin', 'admin@aduanatrack.pe', 'Administrador Demo',
        '$2a$10$cPrmZU0Vo9Cm0BgAPXF.seGHKFGSAElnH7XnD9yzytpcHDiExR9YG', 'ADMIN'), -- nosemgrep: generic.secrets.security.detected-bcrypt-hash.detected-bcrypt-hash
    ('agente', 'agente@aduanatrack.pe', 'Agente Demo',
        '$2a$10$p3LoSMCivpbYnCmThrs7vegZK9VQLkcmKuUoh170fds52elxl1fD.', 'AGENTE'), -- nosemgrep: generic.secrets.security.detected-bcrypt-hash.detected-bcrypt-hash
    ('cliente', 'cliente@aduanatrack.pe', 'Cliente Demo',
        '$2a$10$FE451xVk/41P/Thq0QRTRu/eVXBsLuZ5i1HsmjErcGzgFczMmVFqm', 'CLIENTE') -- nosemgrep: generic.secrets.security.detected-bcrypt-hash.detected-bcrypt-hash
) AS d (usuario, correo, nombre, password_hash, rol)
JOIN rol r ON r.codigo = d.rol;
