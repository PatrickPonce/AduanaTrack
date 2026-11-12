-- Registro manual del levante (HU-13 · T-13.1 · SCRUM-86): el agente obtiene el levante en SUNAT Operaciones
-- en Línea y aquí deja constancia del número, la fecha (despacho.fecha_levante ya existe desde V001) y la
-- constancia en PDF. Las columnas son nulas hasta que el despacho llega a LEVANTE_OTORGADO.
-- Es V014 y no V013: la numeración se corrió una posición porque V011 ya la ocupa el Arancel de Aduanas 2022.

ALTER TABLE despacho
    ADD COLUMN numero_levante            VARCHAR(30) UNIQUE,
    -- Constancia del levante (PDF opcional): nombre original y ruta relativa al directorio de documentos.
    ADD COLUMN constancia_levante_nombre VARCHAR(255),
    ADD COLUMN constancia_levante_ruta   VARCHAR(255),
    -- true si el agente confirmó registrar el levante sin pago de tributos o sin inspección conforme.
    ADD COLUMN levante_con_advertencia   BOOLEAN NOT NULL DEFAULT FALSE;
