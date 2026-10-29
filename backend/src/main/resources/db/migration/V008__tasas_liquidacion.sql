-- Tasas de la liquidación referencial (HU-08 · T-08.1 · SCRUM-63). Valores de referencia que
-- confirma el PO; en porcentaje (16 = 16 %). Nada de tasas fijas en el código.
--
-- T-08.2 (SCRUM-64, Patrick) agrega aquí abajo las tablas liquidacion y pago_tributos.

INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('TASA_IGV', '16', 'IGV de la liquidación referencial, en porcentaje'),
    ('TASA_IPM', '2', 'Impuesto de Promoción Municipal de la liquidación referencial, en porcentaje'),
    ('TASA_PERCEPCION', '3.5', 'Percepción del IGV de la liquidación referencial, en porcentaje');
