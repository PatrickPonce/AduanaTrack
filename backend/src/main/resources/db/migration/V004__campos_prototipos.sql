-- Campos de los prototipos de HU-03 y HU-05 (Historias de Usuario v2.0, figuras 3 y 5)
-- que no estaban en el esquema inicial. Son opcionales: los obligatorios siguen siendo los
-- de la Guía de Programación del Sprint 1.

ALTER TABLE despacho
    ADD COLUMN tipo_operacion     VARCHAR(30)
        CHECK (tipo_operacion IN ('IMPORTACION_DEFINITIVA', 'ADMISION_TEMPORAL', 'REIMPORTACION')),
    ADD COLUMN modalidad_despacho VARCHAR(20)
        CHECK (modalidad_despacho IN ('ANTICIPADO', 'DIFERIDO', 'URGENTE'));

ALTER TABLE embarque
    ADD COLUMN numero_viaje VARCHAR(30);

ALTER TABLE contenedor
    ADD COLUMN peso_bruto_kg NUMERIC(12, 2) CHECK (peso_bruto_kg > 0);
