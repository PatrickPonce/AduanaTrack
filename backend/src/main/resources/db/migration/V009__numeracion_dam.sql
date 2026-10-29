-- Numeración de la DAM (HU-09 · T-09.1 · SCRUM-67): el agente numera la declaración en el sistema
-- externo de SUNAT y aquí solo deja constancia del número oficial, su fecha y el cargo en PDF.
-- Las columnas son nulas hasta que el despacho llega a DAM_NUMERADA.

ALTER TABLE despacho
    ADD COLUMN numero_dam        VARCHAR(30) UNIQUE,
    ADD COLUMN fecha_numeracion  DATE,
    -- Cargo de numeración (PDF opcional): nombre original y ruta relativa al directorio de documentos.
    ADD COLUMN cargo_nombre      VARCHAR(255),
    ADD COLUMN cargo_ruta        VARCHAR(255);
