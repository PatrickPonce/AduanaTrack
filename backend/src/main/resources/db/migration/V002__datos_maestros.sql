-- Datos maestros del Sprint 1 (T-00.4 · SCRUM-24): roles, parámetros y catálogos.

INSERT INTO rol (codigo, nombre) VALUES
    ('ADMIN', 'Administrador'),
    ('AGENTE', 'Agente de aduana'),
    ('CLIENTE', 'Cliente');

INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('DIAS_ALMACENAJE', '30', 'Días calendario de almacenaje contados desde la ETA'),
    ('DIAS_ALERTA_ALMACENAJE', '3', 'Días restantes de almacenaje desde los que se alerta'),
    ('DIAS_ALERTA_CARTA_PODER', '15', 'Días antes del vencimiento de la carta poder desde los que se alerta');

-- Listas maestras de HU-03 (T-03.2 · SCRUM-36).
INSERT INTO regimen_aduanero (codigo, descripcion) VALUES
    ('10', 'Importación para el consumo'),
    ('20', 'Admisión temporal para reexportación en el mismo estado'),
    ('21', 'Admisión temporal para perfeccionamiento activo'),
    ('70', 'Depósito aduanero');

INSERT INTO aduana (codigo, nombre) VALUES
    ('118', 'Marítima del Callao'),
    ('046', 'Paita'),
    ('082', 'Salaverry'),
    ('091', 'Chimbote'),
    ('163', 'Ilo');
