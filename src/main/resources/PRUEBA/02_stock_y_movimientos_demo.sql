-- ============================================================================
-- 02_stock_y_movimientos_demo.sql
-- Stock inicial + movimientos historicos para que el Kardex, el Dashboard y los
-- Reportes tengan datos desde el primer minuto de la demo.
--
-- REQUISITO: haber ejecutado antes 01_datos_base_prueba.sql
--
-- Cada fila crea: 1 INGRESO (hace 10 dias) + 1 SALIDA (hace 3 dias) + el saldo
-- en stock_ubicacion. Asi stock_ubicacion siempre coincide con el ultimo
-- saldo_resultante del kardex. Es idempotente: si ya existe stock para el par
-- material/ubicacion, esa fila se omite.
--
-- Stock final que deja (con una BD limpia):
--   MP-ACE-001 = 1550 | MP-ALU-001 = 280 | MP-COB-001 = 30  (BAJO minimo 50)
--   INS-SOL-001 = 110 | INS-PIN-001 = 45 | REP-ROD-001 = 6  (BAJO minimo 10)
--   REP-DIS-001 = 140 | REP-LUB-001 = sin stock (sirve para probar la
--   asignacion automatica de ubicacion en el primer ingreso)
-- Valorizacion total esperada: S/ 19,075.00
-- ============================================================================

DO $$
DECLARE
    r        RECORD;
    v_user   BIGINT;
    v_mat    BIGINT;
    v_ubi    BIGINT;
    v_saldo  NUMERIC(14,3);
BEGIN
    SELECT id INTO v_user FROM usuarios WHERE correo = 'almacenero@indumetal.pe';
    IF v_user IS NULL THEN
        RAISE EXCEPTION 'Falta el usuario almacenero@indumetal.pe. Ejecuta primero 01_datos_base_prueba.sql';
    END IF;

    FOR r IN
        SELECT *
        FROM (VALUES
            -- sku           almacen  ubicacion        ingreso salida oc             proveedor                  op             area              lote          vence
            ('MP-ACE-001',  'MP',  'Z1-P01-E01-N1',  2000,   450, 'OC-2026-001', 'Aceros del Peru SAC',     'OP-2026-010', 'Corte y Plegado', 'L-ACE-0901', DATE '2027-12-31'),
            ('MP-ALU-001',  'MP',  'Z1-P01-E02-N1',   400,   120, 'OC-2026-002', 'Perfiles Andinos SAC',    'OP-2026-011', 'Ensamblaje',      NULL::text,   NULL::date),
            ('MP-COB-001',  'MP',  'Z2-P01-E01-N1',   120,    90, 'OC-2026-003', 'Metales del Sur SRL',     'OP-2026-012', 'Fundicion',       NULL::text,   NULL::date),
            ('INS-SOL-001', 'MP',  'Z1-P01-E02-N1',   150,    40, 'OC-2026-004', 'Soldaduras Lima SAC',     'OP-2026-013', 'Soldadura',       NULL::text,   NULL::date),
            ('INS-PIN-001', 'MP',  'Z2-P01-E01-N1',    60,    15, 'OC-2026-005', 'Pinturas Industriales SA','OP-2026-014', 'Pintura',         'L-PIN-0902', DATE '2027-06-30'),
            ('REP-ROD-001', 'REP', 'Z1-P01-E01-N1',    40,    34, 'OC-2026-006', 'Rodamientos Peru SAC',    'OP-2026-015', 'Mantenimiento',   NULL::text,   NULL::date),
            ('REP-DIS-001', 'REP', 'Z1-P01-E01-N1',   200,    60, 'OC-2026-007', 'Abrasivos del Centro SAC','OP-2026-016', 'Corte y Plegado', NULL::text,   NULL::date)
        ) AS t(sku, alm, ubi, ingreso, salida, oc, proveedor, op, area, lote, vence)
    LOOP
        SELECT id INTO v_mat FROM materiales WHERE sku = r.sku;

        SELECT u.id INTO v_ubi
        FROM ubicaciones u
        JOIN almacenes a ON a.id = u.almacen_id
        WHERE a.codigo = r.alm AND u.codigo = r.ubi;

        IF v_mat IS NULL OR v_ubi IS NULL THEN
            RAISE NOTICE 'Se omite % (material o ubicacion no encontrados)', r.sku;
            CONTINUE;
        END IF;

        -- Idempotencia: si ya hay stock para este material/ubicacion, no se vuelve a sembrar.
        IF EXISTS (SELECT 1 FROM stock_ubicacion WHERE material_id = v_mat AND ubicacion_id = v_ubi) THEN
            CONTINUE;
        END IF;

        INSERT INTO movimientos_almacen
            (tipo, material_id, ubicacion_id, cantidad, saldo_resultante, lote, fecha_vencimiento,
             orden_compra, proveedor, usuario_id, observaciones, creado_en)
        VALUES
            ('INGRESO', v_mat, v_ubi, r.ingreso, r.ingreso, r.lote, r.vence,
             r.oc, r.proveedor, v_user, 'Dato demo: ingreso inicial', now() - INTERVAL '10 days');

        v_saldo := r.ingreso - r.salida;

        INSERT INTO movimientos_almacen
            (tipo, material_id, ubicacion_id, cantidad, saldo_resultante,
             orden_produccion, area_solicitante, usuario_id, observaciones, creado_en)
        VALUES
            ('SALIDA', v_mat, v_ubi, r.salida, v_saldo,
             r.op, r.area, v_user, 'Dato demo: despacho a produccion', now() - INTERVAL '3 days');

        INSERT INTO stock_ubicacion (material_id, ubicacion_id, cantidad, version)
        VALUES (v_mat, v_ubi, v_saldo, 0);
    END LOOP;
END $$;

-- ---------- Comprobacion: stock total por material ----------
SELECT m.sku,
       m.nombre,
       m.stock_minimo,
       COALESCE(SUM(s.cantidad), 0) AS stock_total,
       CASE WHEN COALESCE(SUM(s.cantidad), 0) < m.stock_minimo THEN 'BAJO MINIMO' ELSE 'OK' END AS estado
FROM materiales m
LEFT JOIN stock_ubicacion s ON s.material_id = m.id
GROUP BY m.id, m.sku, m.nombre, m.stock_minimo
ORDER BY m.id;