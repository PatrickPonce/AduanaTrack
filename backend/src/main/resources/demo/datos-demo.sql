-- Datos de demostración (demo del 19/11/2026). NO es una migración de Flyway: vive en demo/ y se carga a mano
-- (ver docs/despliegue.md, «Datos de demostración»). Requiere que las migraciones V001–V017 ya estén aplicadas.
--
-- Deja en la base:
--   · 2 empresas (Importadora Andina S.A.C. y Pacífico Trade E.I.R.L.), cada una con su usuario cliente asignado
--     (jperez y mrojas) y su carta poder (la de Pacífico vence en 10 días: sale como alerta).
--   · 12 despachos numerados + 1 borrador, uno por cada etapa del flujo, con embarque, hitos, liquidación, pago,
--     DAM, canal, citas, inspecciones (conforme y con observaciones) y levante registrados.
--   · Notificaciones de ejemplo para jperez, mrojas y el agente.
--
-- Sin contraseñas: los usuarios nuevos copian el hash del usuario demo «cliente» (V003), así que entran con la misma
-- contraseña que ya se comparte por el grupo. Los PDF (cartas poder, documentos, actas) no se cargan: su descarga
-- responde 404 hasta que se suban desde la aplicación.
--
-- Se puede ejecutar más de una vez: si las empresas ya existen, no hace nada. Todo ocurre en una sola transacción.

DO $demo$
DECLARE
    v_agente    BIGINT;
    v_hash      TEXT;
    v_rol       BIGINT;
    v_andina    BIGINT;
    v_pacifico  BIGINT;
    v_jperez    BIGINT;
    v_mrojas    BIGINT;
    v_dep1      BIGINT;
    v_dep2      BIGINT;
    v_ahora     TIMESTAMP := date_trunc('minute', now() AT TIME ZONE 'America/Lima');
    v_hoy       DATE      := (now() AT TIME ZONE 'America/Lima')::date;
BEGIN
    SELECT id INTO v_agente FROM usuario WHERE usuario = 'agente';
    SELECT password_hash INTO v_hash FROM usuario WHERE usuario = 'cliente';
    SELECT id INTO v_rol FROM rol WHERE codigo = 'CLIENTE';
    SELECT id INTO v_dep1 FROM deposito WHERE codigo = 'DT-01';
    SELECT id INTO v_dep2 FROM deposito WHERE codigo = 'AL-01';
    IF v_agente IS NULL OR v_hash IS NULL OR v_rol IS NULL OR v_dep1 IS NULL THEN
        RAISE EXCEPTION 'Faltan los usuarios demo (agente, cliente) o los depósitos: aplique primero las migraciones.';
    END IF;
    IF EXISTS (SELECT 1 FROM cliente WHERE ruc IN ('20512345671', '20605544330')) THEN
        RAISE NOTICE 'Los datos de demostración ya están cargados: no se hace nada.';
        RETURN;
    END IF;

    -- ---------- Empresas, cartas poder y usuarios cliente ----------
    INSERT INTO cliente (ruc, razon_social, direccion, correo, telefono, contacto, creado_en, actualizado_en)
    VALUES ('20512345671', 'Importadora Andina S.A.C.', 'Av. Javier Prado Este 1450, San Isidro',
            'compras@andina.pe', '014445566', 'Jorge Pérez', v_ahora - INTERVAL '40 days', v_ahora - INTERVAL '40 days')
    RETURNING id INTO v_andina;
    INSERT INTO cliente (ruc, razon_social, direccion, correo, telefono, contacto, creado_en, actualizado_en)
    VALUES ('20605544330', 'Pacífico Trade E.I.R.L.', 'Calle Los Cedros 320, Miraflores',
            'operaciones@pacificotrade.pe', '017778899', 'María Rojas', v_ahora - INTERVAL '35 days',
            v_ahora - INTERVAL '35 days')
    RETURNING id INTO v_pacifico;

    INSERT INTO carta_poder (cliente_id, fecha_emision, fecha_vencimiento, ruta_archivo, nombre_archivo,
                             tamano_bytes, sha256)
    VALUES (v_andina, v_hoy - 120, v_hoy + 245, 'demo/carta-poder-andina.pdf', 'carta-poder-andina.pdf', 1024,
            repeat('0', 64)),
           (v_pacifico, v_hoy - 355, v_hoy + 10, 'demo/carta-poder-pacifico.pdf', 'carta-poder-pacifico.pdf', 1024,
            repeat('0', 64));

    INSERT INTO usuario (usuario, correo, nombre, password_hash, rol_id, canal_notificacion, cliente_id,
                         cliente_asignado_en)
    VALUES ('jperez', 'jperez@andina.pe', 'Jorge Pérez', v_hash, v_rol, 'AMBOS', v_andina, v_ahora - INTERVAL '30 days')
    RETURNING id INTO v_jperez;
    INSERT INTO usuario (usuario, correo, nombre, password_hash, rol_id, canal_notificacion, cliente_id,
                         cliente_asignado_en)
    VALUES ('mrojas', 'mrojas@pacificotrade.pe', 'María Rojas', v_hash, v_rol, 'SISTEMA', v_pacifico,
            v_ahora - INTERVAL '30 days')
    RETURNING id INTO v_mrojas;

    -- ---------- Despachos: uno por etapa ----------
    -- rango: posición del estado en el flujo (1 = APERTURA … 11 = RETIRO_PROGRAMADO; 0 = borrador).
    CREATE TEMP TABLE d_demo (
        n INT, numero TEXT, cli BIGINT, estado TEXT, canal TEXT, dias_atras INT, eta_dias INT, limite_dias INT,
        aduana TEXT, descr TEXT, ref TEXT, bl TEXT, contenedor TEXT, buque TEXT, rango INT
    ) ON COMMIT DROP;

    INSERT INTO d_demo (n, numero, cli, estado, canal, dias_atras, eta_dias, limite_dias, aduana, descr, ref, bl,
                        contenedor, buque)
    VALUES
        (1,  'DESP-2026-0901', v_andina,   'APERTURA',              NULL,      4, 10,  40, '118',
             'Repuestos de maquinaria industrial', 'OC-ANDINA-1101', 'MSCU-BL-61001', 'MSCU1234567', 'MSC Aurora'),
        (2,  'DESP-2026-0902', v_andina,   'CLASIFICADO',           NULL,      6, 8,   38, '118',
             'Textiles de poliéster en rollos', 'OC-ANDINA-1102', 'HLCU-BL-61002', 'HLCU6543215', 'Hapag Express'),
        (3,  'DESP-2026-0903', v_andina,   'LIQUIDADO',             NULL,      8, 5,   35, '118',
             'Herramientas manuales y accesorios', 'OC-ANDINA-1103', 'MAEU-BL-61003', 'MAEU2468109', 'Maersk Lima'),
        (4,  'DESP-2026-0904', v_andina,   'DAM_NUMERADA',          NULL,      10, 3,  33, '118',
             'Equipos de refrigeración comercial', 'OC-ANDINA-1104', 'CMAU-BL-61004', 'CMAU1357901', 'CMA CGM Pacific'),
        (5,  'DESP-2026-0905', v_andina,   'INSPECCION_PROGRAMADA', 'NARANJA', 12, -3, 27, '118',
             'Autopartes y repuestos de motor', 'OC-ANDINA-1105', 'MSCU-BL-55120', 'TGHU1122336', 'MSC Aurora'),
        (6,  'DESP-2026-0906', v_andina,   'LEVANTE_OTORGADO',      'VERDE',   16, -8, 22, '118',
             'Material de oficina y papelería', 'OC-ANDINA-1106', 'HLCU-BL-88213', 'OOLU4455665', 'Hapag Express'),
        (7,  'DESP-2026-0907', v_andina,   'RETIRO_PROGRAMADO',     'VERDE',   18, -10, 20, '046',
             'Equipos de cómputo y periféricos', 'OC-ANDINA-1107', 'MAEU-BL-30477', 'ZIMU7788996', 'Maersk Lima'),
        (8,  'DESP-2026-0908', v_pacifico, 'DOCUMENTACION_COMPLETA', NULL,     5, 7,   37, '118',
             'Conservas de pescado en lata', 'PT-2026-021', 'ONEY-BL-61008', 'MSKU1020300', 'ONE Trust'),
        (9,  'DESP-2026-0909', v_pacifico, 'TRIBUTOS_PAGADOS',      NULL,      9, 4,   34, '118',
             'Maquinaria de empaque automático', 'PT-2026-022', 'COSU-BL-61009', 'TGHU1122336', 'Cosco Shipping'),
        (10, 'DESP-2026-0910', v_pacifico, 'CANAL_ASIGNADO',        'ROJO',    14, -28, 2, '118',
             'Insumos químicos para limpieza', 'PT-2026-023', 'MSCU-BL-61010', 'HLCU6543215', 'MSC Aurora'),
        (11, 'DESP-2026-0911', v_pacifico, 'INSPECCION_CONFORME',   'NARANJA', 15, -5, 25, '118',
             'Juguetes plásticos para el mercado local', 'PT-2026-024', 'OOLU-BL-61011', 'OOLU4455665', 'OOCL Peru'),
        (12, 'DESP-2026-0912', v_pacifico, 'LEVANTE_OTORGADO',      'NARANJA', 20, -12, 18, '118',
             'Mobiliario de oficina desarmado', 'PT-2026-025', 'ZIMU-BL-61012', 'MAEU2468109', 'ZIM Pacific'),
        (13, NULL,             v_pacifico, 'BORRADOR',              NULL,      1, 0,   0,  '118',
             'Borrador: accesorios de iluminación', 'PT-2026-026', NULL, NULL, NULL);

    UPDATE d_demo SET rango = COALESCE(array_position(ARRAY['APERTURA', 'DOCUMENTACION_COMPLETA', 'CLASIFICADO',
        'LIQUIDADO', 'TRIBUTOS_PAGADOS', 'DAM_NUMERADA', 'CANAL_ASIGNADO', 'INSPECCION_PROGRAMADA',
        'INSPECCION_CONFORME', 'LEVANTE_OTORGADO', 'RETIRO_PROGRAMADO'], estado), 0);

    INSERT INTO despacho (numero, cliente_id, regimen_id, aduana_id, referencia_cliente, descripcion_mercancia,
                          estado, canal_control, fecha_levante, agente_id, creado_en, tipo_operacion,
                          modalidad_despacho, numero_dam, fecha_numeracion, numero_levante)
    SELECT d.numero, d.cli, (SELECT id FROM regimen_aduanero WHERE codigo = '10'),
           (SELECT id FROM aduana WHERE codigo = d.aduana), d.ref, d.descr, d.estado,
           CASE WHEN d.rango >= 7 THEN d.canal END,
           CASE WHEN d.rango >= 10 THEN LEAST(v_hoy, (v_ahora - d.dias_atras * INTERVAL '1 day' + INTERVAL '8 days')::date) END,
           v_agente, v_ahora - d.dias_atras * INTERVAL '1 day', 'IMPORTACION_DEFINITIVA', 'ANTICIPADO',
           CASE WHEN d.rango >= 6 THEN '118-2026-10-' || lpad((700 + d.n)::text, 6, '0') END,
           CASE WHEN d.rango >= 6 THEN (v_ahora - d.dias_atras * INTERVAL '1 day' + INTERVAL '2 days')::date END,
           CASE WHEN d.rango >= 10 THEN 'LEV-2026-' || lpad((900 + d.n)::text, 6, '0') END
    FROM d_demo d;

    -- El correlativo continúa después de los números de demostración.
    INSERT INTO correlativo_despacho (anio, ultimo_numero) VALUES (2026, 912)
    ON CONFLICT (anio) DO UPDATE SET ultimo_numero = GREATEST(correlativo_despacho.ultimo_numero, 912);

    -- Embarque y contenedor (todos menos el borrador).
    INSERT INTO embarque (despacho_id, numero_bl, tipo_bl, naviera, buque, numero_viaje, puerto_embarque,
                          puerto_descarga, fecha_embarque, fecha_eta, fecha_limite_almacenaje)
    SELECT x.id, d.bl, 'MASTER', split_part(d.bl, '-', 1), d.buque, 'V' || (200 + d.n), 'Shanghái', 'Callao',
           v_hoy + d.eta_dias - 32, v_hoy + d.eta_dias, v_hoy + d.limite_dias
    FROM d_demo d JOIN despacho x ON x.numero = d.numero
    WHERE d.rango > 0;

    INSERT INTO contenedor (embarque_id, numero, tipo, precinto, peso_bruto_kg)
    SELECT e.id, d.contenedor, '40HC', 'PR-' || (7000 + d.n), 18000 + d.n * 150
    FROM d_demo d JOIN despacho x ON x.numero = d.numero JOIN embarque e ON e.despacho_id = x.id
    WHERE d.rango > 0;

    -- Hitos de la línea de tiempo: todos los que ya alcanzó (el canal verde no pasa por inspección).
    INSERT INTO hito_despacho (despacho_id, tipo, descripcion, sugerido, usuario_id, fecha)
    SELECT x.id, h.tipo,
           CASE h.tipo
               WHEN 'APERTURA' THEN 'Despacho abierto (' || d.numero || ')'
               WHEN 'DOCUMENTACION_COMPLETA' THEN 'Documentación completa'
               WHEN 'CLASIFICADO' THEN 'Clasificación arancelaria completa'
               WHEN 'LIQUIDADO' THEN 'Liquidación referencial generada'
               WHEN 'TRIBUTOS_PAGADOS' THEN 'Pago de tributos registrado'
               WHEN 'DAM_NUMERADA' THEN 'DAM numerada: ' || x.numero_dam
               WHEN 'CANAL_ASIGNADO' THEN 'Canal de control asignado: ' || d.canal
               WHEN 'INSPECCION_PROGRAMADA' THEN 'Inspección programada'
               WHEN 'INSPECCION_CONFORME' THEN 'Inspección conforme: el levante queda habilitado.'
               WHEN 'LEVANTE_OTORGADO' THEN 'Levante otorgado: ' || x.numero_levante
               WHEN 'RETIRO_PROGRAMADO' THEN 'Retiro programado'
           END,
           FALSE, v_agente, x.creado_en + h.orden * INTERVAL '4 hours'
    FROM d_demo d
    JOIN despacho x ON x.numero = d.numero
    JOIN (VALUES (1, 'APERTURA'), (2, 'DOCUMENTACION_COMPLETA'), (3, 'CLASIFICADO'), (4, 'LIQUIDADO'),
                 (5, 'TRIBUTOS_PAGADOS'), (6, 'DAM_NUMERADA'), (7, 'CANAL_ASIGNADO'), (8, 'INSPECCION_PROGRAMADA'),
                 (9, 'INSPECCION_CONFORME'), (10, 'LEVANTE_OTORGADO'), (11, 'RETIRO_PROGRAMADO')) AS h (orden, tipo)
      ON h.orden <= d.rango
    WHERE NOT (d.canal = 'VERDE' AND h.orden IN (8, 9));

    -- 0905: la primera inspección terminó con observaciones y el despacho sigue «Inspección programada».
    INSERT INTO hito_despacho (despacho_id, tipo, descripcion, sugerido, usuario_id, fecha)
    SELECT x.id, 'INSPECCION_CON_OBSERVACIONES',
           'Inspección con observaciones: Rotulado y marcas (Rotulado incompleto)', FALSE, v_agente,
           v_ahora - INTERVAL '1 day'
    FROM despacho x WHERE x.numero = 'DESP-2026-0905';

    -- ---------- Liquidación y pago (desde LIQUIDADO) ----------
    INSERT INTO liquidacion (despacho_id, fob, flete, seguro, cif, ad_valorem, igv, ipm, percepcion, total,
                             tasa_igv, tasa_ipm, tasa_percepcion, generado_por, generado_en)
    SELECT x.id, b.fob, b.flete, b.seguro, c.cif, a.adv, i.igv, i.ipm, p.perc, a.adv + i.igv + i.ipm + p.perc,
           16, 2, 3.5, v_agente, x.creado_en + INTERVAL '16 hours'
    FROM d_demo d
    JOIN despacho x ON x.numero = d.numero
    CROSS JOIN LATERAL (SELECT (18000 + d.n * 1500)::numeric(15, 2) AS fob, 1800::numeric(15, 2) AS flete,
                               250::numeric(15, 2) AS seguro) b
    CROSS JOIN LATERAL (SELECT b.fob + b.flete + b.seguro AS cif) c
    CROSS JOIN LATERAL (SELECT ROUND(c.cif * 0.06, 2) AS adv) a
    CROSS JOIN LATERAL (SELECT ROUND((c.cif + a.adv) * 0.16, 2) AS igv, ROUND((c.cif + a.adv) * 0.02, 2) AS ipm) i
    CROSS JOIN LATERAL (SELECT ROUND((c.cif + a.adv + i.igv + i.ipm) * 0.035, 2) AS perc) p
    WHERE d.rango >= 4;

    INSERT INTO pago_tributos (despacho_id, fecha, monto, medio_pago, numero_operacion, registrado_por, registrado_en)
    SELECT x.id, (x.creado_en + INTERVAL '1 day')::date, l.total, 'TRANSFERENCIA', 'OP-DEMO-' || d.n, v_agente,
           x.creado_en + INTERVAL '20 hours'
    FROM d_demo d JOIN despacho x ON x.numero = d.numero JOIN liquidacion l ON l.despacho_id = x.id
    WHERE d.rango >= 5;

    -- ---------- Citas ----------
    INSERT INTO cita_despacho (despacho_id, deposito_id, numero_cita, tipo, fecha_hora_confirmada, estado,
                               registrado_por, registrado_en)
    SELECT x.id, v_dep1, c.numero, 'INSPECCION', c.fecha, c.estado, v_agente, c.registrada
    FROM (VALUES ('DESP-2026-0905', 'TK-2026-004512', v_hoy + 1 + TIME '10:00', 'PROGRAMADA', v_ahora - INTERVAL '3 days'),
                 ('DESP-2026-0911', 'TK-2026-004530', v_hoy - 2 + TIME '09:30', 'REALIZADA', v_ahora - INTERVAL '4 days'),
                 ('DESP-2026-0912', 'TK-2026-004488', v_hoy - 9 + TIME '11:00', 'REALIZADA', v_ahora - INTERVAL '11 days'))
         AS c (despacho, numero, fecha, estado, registrada)
    JOIN despacho x ON x.numero = c.despacho;

    INSERT INTO cita_despacho (despacho_id, deposito_id, numero_cita, tipo, fecha_hora_confirmada, estado,
                               transportista_ruc, transportista_razon_social, transportista_placa,
                               transportista_conductor, registrado_por, registrado_en)
    SELECT x.id, v_dep2, 'TK-2026-004999', 'RETIRO', v_hoy + 2 + TIME '08:00', 'PROGRAMADA', '20123456786',
           'Transportes Demo SAC', 'ABC-123', 'Juan Pérez', v_agente, v_ahora - INTERVAL '1 day'
    FROM despacho x WHERE x.numero = 'DESP-2026-0907';

    INSERT INTO hito_despacho (despacho_id, tipo, descripcion, sugerido, usuario_id, fecha)
    SELECT x.id, 'CITA_REGISTRADA', 'Cita de ' || lower(c.tipo) || ' registrada: ' || c.numero_cita, FALSE,
           v_agente, c.registrado_en
    FROM cita_despacho c JOIN despacho x ON x.id = c.despacho_id
    WHERE x.numero IN ('DESP-2026-0905', 'DESP-2026-0907', 'DESP-2026-0911', 'DESP-2026-0912');

    -- ---------- Inspecciones (6 ítems cada una) ----------
    INSERT INTO inspeccion_despacho (despacho_id, resultado, observaciones, registrada_por, registrada_en)
    SELECT x.id, i.resultado, i.observaciones, v_agente, i.cuando
    FROM (VALUES ('DESP-2026-0905', 'CON_OBSERVACIONES', 'Rotulado incompleto en 3 bultos', v_ahora - INTERVAL '1 day'),
                 ('DESP-2026-0911', 'CONFORME', NULL, v_ahora - INTERVAL '2 days'),
                 ('DESP-2026-0912', 'CONFORME', NULL, v_ahora - INTERVAL '9 days'))
         AS i (despacho, resultado, observaciones, cuando)
    JOIN despacho x ON x.numero = i.despacho;

    INSERT INTO inspeccion_item (inspeccion_id, orden, descripcion, conforme, observacion)
    SELECT ins.id, o.orden, o.descripcion,
           NOT (ins.resultado = 'CON_OBSERVACIONES' AND o.orden = 5),
           CASE WHEN ins.resultado = 'CON_OBSERVACIONES' AND o.orden = 5 THEN 'Rotulado incompleto' END
    FROM inspeccion_despacho ins
    JOIN despacho x ON x.id = ins.despacho_id AND x.numero IN ('DESP-2026-0905', 'DESP-2026-0911', 'DESP-2026-0912')
    CROSS JOIN (VALUES (1, 'Cantidad de bultos'), (2, 'Peso y volumen declarados'),
                       (3, 'Descripción de la mercancía'), (4, 'Estado y embalaje de la mercancía'),
                       (5, 'Rotulado y marcas'), (6, 'Precintos y contenedor')) AS o (orden, descripcion);

    -- ---------- Notificaciones de ejemplo ----------
    INSERT INTO notificacion (usuario_id, despacho_id, tipo, severidad, titulo, mensaje, leida, creada_en, leida_en)
    SELECT u.id, x.id, n.tipo, n.severidad, n.titulo, n.mensaje, n.leida, v_ahora - n.horas * INTERVAL '1 hour',
           CASE WHEN n.leida THEN v_ahora - n.horas * INTERVAL '1 hour' + INTERVAL '30 minutes' END
    FROM (VALUES
        -- Cliente jperez (Andina)
        ('jperez', 'DESP-2026-0905', 'INSPECCION_CON_OBSERVACIONES', 'ALTA', 'Inspección con observaciones',
         'DESP-2026-0905: la inspección terminó con observaciones: Rotulado y marcas (Rotulado incompleto). Su agente de aduanas se pondrá en contacto.', FALSE, 20),
        ('jperez', 'DESP-2026-0905', 'CAMBIO_ESTADO', 'ALTA', 'Canal de control asignado',
         'DESP-2026-0905: canal naranja. Se programará la inspección en el depósito.', TRUE, 70),
        ('jperez', 'DESP-2026-0906', 'CAMBIO_ESTADO', 'INFO', 'Levante otorgado',
         'DESP-2026-0906: se otorgó el levante LEV-2026-000906.', TRUE, 150),
        ('jperez', 'DESP-2026-0907', 'CAMBIO_ESTADO', 'INFO', 'Retiro programado',
         'DESP-2026-0907: retiro de mercancía programado (cita TK-2026-004999).', FALSE, 22),
        -- Cliente mrojas (Pacífico)
        ('mrojas', 'DESP-2026-0910', 'CAMBIO_ESTADO', 'ALTA', 'Canal de control asignado',
         'DESP-2026-0910: canal rojo. Se programará la inspección en el depósito.', FALSE, 30),
        ('mrojas', 'DESP-2026-0911', 'INSPECCION_CONFORME', 'INFO', 'Inspección conforme',
         'DESP-2026-0911: la inspección terminó conforme y el levante quedó habilitado.', FALSE, 44),
        ('mrojas', 'DESP-2026-0912', 'CAMBIO_ESTADO', 'INFO', 'Levante otorgado',
         'DESP-2026-0912: se otorgó el levante LEV-2026-000912.', TRUE, 200),
        -- Agente
        ('agente', 'DESP-2026-0910', 'PLAZO_ALMACENAJE_POR_VENCER', 'CRITICA', 'Almacenaje por vencer',
         'DESP-2026-0910 vence en 2 días', FALSE, 6),
        ('agente', 'DESP-2026-0910', 'CAMBIO_ESTADO', 'ALTA', 'Canal de control asignado',
         'DESP-2026-0910: canal rojo, programe la inspección en el depósito', FALSE, 30),
        ('agente', 'DESP-2026-0905', 'INSPECCION_CON_OBSERVACIONES', 'ALTA', 'Inspección con observaciones',
         'DESP-2026-0905: Rotulado y marcas (Rotulado incompleto)', FALSE, 20),
        ('agente', 'DESP-2026-0911', 'INSPECCION_CONFORME', 'INFO', 'Inspección conforme',
         'DESP-2026-0911: el levante quedó habilitado.', TRUE, 44),
        ('agente', NULL, 'CARTA_PODER_POR_VENCER', 'CRITICA', 'Carta poder por vencer',
         'La carta poder de Pacífico Trade E.I.R.L. vence en 10 días', FALSE, 8))
        AS n (usuario, despacho, tipo, severidad, titulo, mensaje, leida, horas)
    JOIN usuario u ON u.usuario = n.usuario
    LEFT JOIN despacho x ON x.numero = n.despacho;

    RAISE NOTICE 'Datos de demostración cargados: 2 empresas, 12 despachos y 1 borrador.';
END
$demo$;
