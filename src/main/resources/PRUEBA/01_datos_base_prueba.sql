-- ============================================================================
-- 01_datos_base_prueba.sql
-- Intranet de Gestion de Almacen - INDUMETAL PERU S.A.C.
-- Datos BASE de prueba para Supabase (PostgreSQL): usuarios, ubicaciones, materiales.
--
-- ANTES DE EJECUTAR:
--   * Levanta el backend al menos una vez. Flyway crea las tablas y siembra
--     roles, el usuario admin y los 3 almacenes (MP, PT, REP) con V1__init_schema.sql.
--   * Ejecuta este script en: Supabase > SQL Editor > New query > Run.
--   * Es idempotente: se puede correr varias veces sin duplicar datos.
--
-- Contrasena de TODOS los usuarios de prueba:  Test#2026
-- (el hash BCrypt se genera con pgcrypto; Spring Security lo valida sin problema)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

-- ---------- USUARIOS DE PRUEBA (uno por rol; el ADMIN ya existe por Flyway) ----------
INSERT INTO usuarios (nombres, apellidos, correo, password_hash, rol_id, activo)
SELECT v.nombres,
       v.apellidos,
       v.correo,
       extensions.crypt('Test#2026', extensions.gen_salt('bf', 10)),
       r.id,
       TRUE
FROM (VALUES
        ('Carlos', 'Quispe Ramos',  'almacenero@indumetal.pe',      'ALMACENERO'),
        ('Lucia',  'Mendoza Torres','supervisor@indumetal.pe',      'SUPERVISOR_ALMACEN'),
        ('Jorge',  'Paredes Salas', 'jefe.produccion@indumetal.pe', 'JEFE_PRODUCCION')
     ) AS v(nombres, apellidos, correo, rol)
JOIN roles r ON r.nombre = v.rol
ON CONFLICT (correo) DO NOTHING;

-- ---------- UBICACIONES (RF-05 / RF-07) ----------
-- Con una BD limpia los ids quedan: 1,2,3 = MP | 4 = PT | 5 = REP
INSERT INTO ubicaciones (almacen_id, codigo, zona, pasillo, estante, nivel, capacidad_maxima, activo)
VALUES
    ((SELECT id FROM almacenes WHERE codigo = 'MP'),  'Z1-P01-E01-N1', 'Zona 1', 'Pasillo 01', 'Estante 01', 'Nivel 1', 5000, TRUE),
    ((SELECT id FROM almacenes WHERE codigo = 'MP'),  'Z1-P01-E02-N1', 'Zona 1', 'Pasillo 01', 'Estante 02', 'Nivel 1', 5000, TRUE),
    ((SELECT id FROM almacenes WHERE codigo = 'MP'),  'Z2-P01-E01-N1', 'Zona 2', 'Pasillo 01', 'Estante 01', 'Nivel 1', 3000, TRUE),
    ((SELECT id FROM almacenes WHERE codigo = 'PT'),  'Z1-P01-E01-N1', 'Zona 1', 'Pasillo 01', 'Estante 01', 'Nivel 1', 4000, TRUE),
    ((SELECT id FROM almacenes WHERE codigo = 'REP'), 'Z1-P01-E01-N1', 'Zona 1', 'Pasillo 01', 'Estante 01', 'Nivel 1', 2000, TRUE)
ON CONFLICT (almacen_id, codigo) DO NOTHING;

-- ---------- MATERIALES (RF-04) ----------
-- Con una BD limpia los ids quedan 1..8 en este mismo orden.
INSERT INTO materiales
    (sku, nombre, descripcion, unidad_medida, categoria, stock_minimo, stock_maximo, costo_unitario, requiere_lote, activo)
VALUES
    ('MP-ACE-001',  'Plancha de acero ASTM A36 3mm',      'Plancha laminada en caliente 1.22 x 2.44 m',  'KG',  'Materia Prima',            500, 5000,  4.50, TRUE,  TRUE),
    ('MP-ALU-001',  'Perfil de aluminio 6063',            'Perfil estructural, longitud 6 m',            'UND', 'Materia Prima',            100, 1000, 18.00, FALSE, TRUE),
    ('MP-COB-001',  'Barra de cobre 1/2 pulg',            'Barra redonda de cobre electrolitico',        'KG',  'Materia Prima',             50,  400, 38.00, FALSE, TRUE),
    ('INS-SOL-001', 'Electrodo de soldadura E6011 1/8',   'Electrodo revestido, caja de 5 kg',           'KG',  'Insumos',                   40,  300, 12.50, FALSE, TRUE),
    ('INS-PIN-001', 'Pintura anticorrosiva gris',         'Pintura base epoxica, galon',                 'GLN', 'Insumos',                   20,  150, 85.00, TRUE,  TRUE),
    ('REP-ROD-001', 'Rodamiento 6204-2RS',                'Rodamiento rigido de bolas sellado',          'UND', 'Repuestos y Herramientas',  10,   80, 22.00, FALSE, TRUE),
    ('REP-DIS-001', 'Disco de corte 4 1/2 pulg',          'Disco abrasivo para acero',                   'UND', 'Repuestos y Herramientas',  30,  300,  4.20, FALSE, TRUE),
    ('REP-LUB-001', 'Grasa lubricante industrial',        'Grasa multiproposito EP-2, balde de 1 kg',    'KG',  'Repuestos y Herramientas',   5,   40, 28.00, FALSE, TRUE)
ON CONFLICT (sku) DO NOTHING;

-- ---------- Comprobacion: ids que usara la guia de pruebas ----------
SELECT 'usuario'   AS tipo, id, correo AS referencia FROM usuarios
UNION ALL
SELECT 'almacen',   id, codigo FROM almacenes
UNION ALL
SELECT 'ubicacion', u.id, a.codigo || ' / ' || u.codigo FROM ubicaciones u JOIN almacenes a ON a.id = u.almacen_id
UNION ALL
SELECT 'material',  id, sku FROM materiales
ORDER BY tipo, id;
