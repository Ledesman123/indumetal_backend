-- ============================================================================
-- 04_utilidades_y_limpieza.sql
-- Atajos para preparar y repetir la demo. Ejecuta CADA bloque por separado
-- (selecciona el bloque y pulsa Run). No corras el archivo completo.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- [U1] Desbloquear un usuario tras la prueba de "5 intentos fallidos" (RF-01)
-- ----------------------------------------------------------------------------
UPDATE usuarios
SET intentos_fallidos = 0,
    bloqueado_hasta   = NULL
WHERE correo = 'almacenero@indumetal.pe';

-- ----------------------------------------------------------------------------
-- [U2] Reactivar un usuario desactivado
-- ----------------------------------------------------------------------------
UPDATE usuarios
SET activo = TRUE
WHERE correo = 'almacenero@indumetal.pe';

-- ----------------------------------------------------------------------------
-- [U3] Recuperacion de contrasena SIN configurar SMTP
--      1) Primero llama a POST /api/auth/forgot-password con el correo del usuario
--         (eso crea el registro en password_reset_codes).
--      2) Ejecuta este bloque: fija el codigo 123456 con vigencia de 10 minutos.
--      3) Usa 123456 en POST /api/auth/verify-reset-code.
--      (El codigo real solo existe hasheado y se envia por correo; esto es solo para pruebas.)
-- ----------------------------------------------------------------------------
UPDATE password_reset_codes
SET codigo_hash       = extensions.crypt('123456', extensions.gen_salt('bf', 10)),
    codigo_expiracion = now() + INTERVAL '10 minutes',
    codigo_verificado = FALSE
WHERE id = (
    SELECT p.id
    FROM password_reset_codes p
    JOIN usuarios u ON u.id = p.usuario_id
    WHERE u.correo = 'almacenero@indumetal.pe'
    ORDER BY p.creado_en DESC
    LIMIT 1
);

-- ----------------------------------------------------------------------------
-- [U4] Volver a poner la contrasena de prueba (Test#2026) despues de probar reset-password
-- ----------------------------------------------------------------------------
UPDATE usuarios
SET password_hash     = extensions.crypt('Test#2026', extensions.gen_salt('bf', 10)),
    intentos_fallidos = 0,
    bloqueado_hasta   = NULL
WHERE correo = 'almacenero@indumetal.pe';

-- ----------------------------------------------------------------------------
-- [U5] Contrasena nueva para el admin si la olvidas / la cambiaste (queda Admin#2026)
-- ----------------------------------------------------------------------------
-- UPDATE usuarios
-- SET password_hash = '$2a$10$gE8CdhspgdQ.X.n0FlpRBuxZRjI/5KBFKjY6IpMm36yRxqdpAgVVO',
--     intentos_fallidos = 0, bloqueado_hasta = NULL
-- WHERE correo = 'admin@indumetal.pe';

-- ============================================================================
-- LIMPIEZA  (DESTRUCTIVO — esta comentado a proposito)
-- Quita el comentario SOLO del bloque que necesites.
-- ============================================================================

-- [L1] Borra TODAS las operaciones (movimientos, stock, inventarios, auditoria,
--      codigos de recuperacion) y reinicia sus contadores de id.
--      NO borra usuarios, almacenes, ubicaciones ni materiales.
--      Despues de esto, vuelve a correr 02_stock_y_movimientos_demo.sql.
-- TRUNCATE TABLE detalle_inventario_fisico,
--                inventarios_fisicos,
--                movimientos_almacen,
--                stock_ubicacion,
--                auditoria,
--                password_reset_codes
--      RESTART IDENTITY;

-- [L2] Borra los materiales creados durante las pruebas por Swagger (los demo se conservan).
--      Solo funciona si esos materiales no tienen movimientos/stock.
-- DELETE FROM materiales
-- WHERE sku IN ('MP-LAT-001')
--   AND NOT EXISTS (SELECT 1 FROM movimientos_almacen mv WHERE mv.material_id = materiales.id)
--   AND NOT EXISTS (SELECT 1 FROM stock_ubicacion s WHERE s.material_id = materiales.id);

-- [L3] Borra los usuarios creados durante las pruebas por Swagger (ej. ana.rojas@...).
--      Solo funciona si el usuario no registro movimientos ni inventarios.
-- DELETE FROM usuarios
-- WHERE correo = 'ana.rojas@indumetal.pe';