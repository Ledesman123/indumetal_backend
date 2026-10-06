-- ============================================================================
-- 03_consultas_verificacion.sql
-- Consultas de SOLO LECTURA para comprobar en Supabase lo que hizo cada
-- endpoint probado en Swagger. En el SQL Editor, selecciona UNA consulta y
-- pulsa "Run" (o Ctrl+Enter) para ejecutar solo esa.
-- ============================================================================

-- [V01] Ids de referencia (para completar los JSON de Swagger)
SELECT 'usuario' AS tipo, id, correo AS referencia FROM usuarios
UNION ALL SELECT 'almacen', id, codigo FROM almacenes
UNION ALL SELECT 'ubicacion', u.id, a.codigo || ' / ' || u.codigo FROM ubicaciones u JOIN almacenes a ON a.id = u.almacen_id
UNION ALL SELECT 'material', id, sku FROM materiales
ORDER BY tipo, id;

-- [V02] Usuarios, rol, estado y bloqueo (prueba de login / bloqueo por 5 intentos)
SELECT u.id, u.correo, r.nombre AS rol, u.activo, u.intentos_fallidos, u.bloqueado_hasta
FROM usuarios u JOIN roles r ON r.id = u.rol_id
ORDER BY u.id;

-- [V03] Stock actual por material y ubicacion (RF-10)
SELECT m.sku, a.codigo AS almacen, ub.codigo AS ubicacion, s.cantidad
FROM stock_ubicacion s
JOIN materiales m ON m.id = s.material_id
JOIN ubicaciones ub ON ub.id = s.ubicacion_id
JOIN almacenes a ON a.id = ub.almacen_id
ORDER BY m.sku, a.codigo, ub.codigo;

-- [V04] Stock total por material vs. stock minimo (alertas, RF-11)
SELECT m.sku,
       m.stock_minimo,
       COALESCE(SUM(s.cantidad), 0) AS stock_total,
       CASE WHEN COALESCE(SUM(s.cantidad), 0) < m.stock_minimo THEN 'BAJO MINIMO' ELSE 'OK' END AS estado
FROM materiales m
LEFT JOIN stock_ubicacion s ON s.material_id = m.id
GROUP BY m.id, m.sku, m.stock_minimo
ORDER BY m.id;

-- [V05] Ultimos 20 movimientos (ingresos, salidas, devoluciones, transferencias)
SELECT mv.id, mv.creado_en, mv.tipo, m.sku, ub.codigo AS ubicacion,
       mv.cantidad, mv.saldo_resultante,
       COALESCE(mv.orden_compra, mv.orden_produccion) AS referencia,
       mv.movimiento_relacionado_id AS relacionado, us.correo AS usuario
FROM movimientos_almacen mv
JOIN materiales m ON m.id = mv.material_id
JOIN ubicaciones ub ON ub.id = mv.ubicacion_id
JOIN usuarios us ON us.id = mv.usuario_id
ORDER BY mv.id DESC
LIMIT 20;

-- [V06] Kardex de un material (cambia el 1 por el id que quieras revisar)
SELECT mv.creado_en, mv.tipo, ub.codigo AS ubicacion,
       CASE WHEN mv.tipo IN ('INGRESO','DEVOLUCION','TRANSFERENCIA_ENTRADA','AJUSTE_POSITIVO') THEN mv.cantidad ELSE 0 END AS entrada,
       CASE WHEN mv.tipo IN ('INGRESO','DEVOLUCION','TRANSFERENCIA_ENTRADA','AJUSTE_POSITIVO') THEN 0 ELSE mv.cantidad END AS salida,
       mv.saldo_resultante AS saldo
FROM movimientos_almacen mv
JOIN ubicaciones ub ON ub.id = mv.ubicacion_id
WHERE mv.material_id = 1
ORDER BY mv.creado_en, mv.id;

-- [V07] Coherencia: el stock de cada ubicacion debe igualar el ultimo saldo del kardex.
--       Si el inventario fisico ajusto el stock, aparecera una diferencia (esperado).
SELECT s.material_id, s.ubicacion_id, s.cantidad AS stock_actual,
       (SELECT mv.saldo_resultante FROM movimientos_almacen mv
         WHERE mv.material_id = s.material_id AND mv.ubicacion_id = s.ubicacion_id
         ORDER BY mv.creado_en DESC, mv.id DESC LIMIT 1) AS ultimo_saldo_kardex
FROM stock_ubicacion s
ORDER BY s.material_id, s.ubicacion_id;

-- [V08] Valorizacion del inventario por categoria (debe coincidir con GET /api/dashboard/valorizacion)
SELECT m.categoria, SUM(s.cantidad * m.costo_unitario) AS valorizacion
FROM stock_ubicacion s JOIN materiales m ON m.id = s.material_id
GROUP BY m.categoria
ORDER BY valorizacion DESC;

SELECT COALESCE(SUM(s.cantidad * m.costo_unitario), 0) AS valorizacion_total
FROM stock_ubicacion s JOIN materiales m ON m.id = s.material_id;

-- [V09] Inventarios fisicos y sus detalles
--       IMPORTANTE: Swagger NO devuelve los detalleId al iniciar el inventario;
--       sacalos de aqui para usarlos en POST /api/inventarios-fisicos/conteos.
SELECT i.id AS inventario_id, i.estado, a.codigo AS almacen, i.fecha_inicio, i.fecha_cierre
FROM inventarios_fisicos i JOIN almacenes a ON a.id = i.almacen_id
ORDER BY i.id DESC;

SELECT d.id AS detalle_id, d.inventario_fisico_id, m.sku, ub.codigo AS ubicacion,
       d.stock_sistema, d.stock_fisico, d.diferencia
FROM detalle_inventario_fisico d
JOIN materiales m ON m.id = d.material_id
JOIN ubicaciones ub ON ub.id = d.ubicacion_id
ORDER BY d.inventario_fisico_id DESC, d.id;

-- [V10] Bitacora de auditoria (ultimos 20 registros)
SELECT id, fecha, usuario_correo, entidad, entidad_id, accion, detalle
FROM auditoria
ORDER BY fecha DESC
LIMIT 20;

-- [V11] Codigos de recuperacion de contrasena (los hashes no son legibles; sirve para ver estado/expiracion)
SELECT p.id, u.correo, p.codigo_expiracion, p.codigo_verificado, p.token_expiracion, p.creado_en
FROM password_reset_codes p JOIN usuarios u ON u.id = p.usuario_id
ORDER BY p.id DESC;