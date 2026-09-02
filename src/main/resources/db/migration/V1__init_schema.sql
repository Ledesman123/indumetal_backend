-- ============================================================
-- V1__init_schema.sql
-- Intranet de Gestion de Almacen - INDUMETAL PERU S.A.C.
-- Motor: PostgreSQL (Supabase)
-- ============================================================

-- ---------- ROLES (RF-02) ----------
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(40) NOT NULL UNIQUE,
    descripcion VARCHAR(200)
);

INSERT INTO roles (nombre, descripcion) VALUES
    ('ALMACENERO', 'Registra ingresos, salidas y conteos fisicos'),
    ('SUPERVISOR_ALMACEN', 'Supervisa el almacen, aprueba y cierra inventarios'),
    ('JEFE_PRODUCCION', 'Solicita y consulta materiales para produccion'),
    ('ADMINISTRADOR', 'Administra usuarios, roles y configuracion del sistema');

-- ---------- USUARIOS (RF-01, RF-02, RF-03) ----------
CREATE TABLE usuarios (
    id               BIGSERIAL PRIMARY KEY,
    nombres          VARCHAR(150) NOT NULL,
    apellidos        VARCHAR(150) NOT NULL,
    correo           VARCHAR(150) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    rol_id           BIGINT NOT NULL REFERENCES roles(id),
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta  TIMESTAMPTZ,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en   TIMESTAMPTZ
);
CREATE INDEX idx_usuarios_correo ON usuarios (correo);

-- Usuario administrador inicial. Password: Admin#2026 (cambiar en produccion)
-- Hash BCrypt real generado para "Admin#2026" (verificar con BCryptPasswordEncoder de Spring Security)
INSERT INTO usuarios (nombres, apellidos, correo, password_hash, rol_id, activo)
VALUES ('Administrador', 'General', 'admin@indumetal.pe',
        '$2a$10$gE8CdhspgdQ.X.n0FlpRBuxZRjI/5KBFKjY6IpMm36yRxqdpAgVVO',
        (SELECT id FROM roles WHERE nombre = 'ADMINISTRADOR'), TRUE);

-- ---------- ALMACENES (RF-05) ----------
CREATE TABLE almacenes (
    id        BIGSERIAL PRIMARY KEY,
    codigo    VARCHAR(20) NOT NULL UNIQUE,
    nombre    VARCHAR(150) NOT NULL,
    direccion VARCHAR(200),
    activo    BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO almacenes (codigo, nombre, direccion) VALUES
    ('MP', 'Almacen de Materia Prima', 'Planta Callao - Nave 1'),
    ('PT', 'Almacen de Productos en Proceso y Terminados', 'Planta Callao - Nave 2'),
    ('REP', 'Almacen de Repuestos y Herramientas', 'Planta Callao - Nave 3');

-- ---------- UBICACIONES (RF-05, RF-07) ----------
CREATE TABLE ubicaciones (
    id               BIGSERIAL PRIMARY KEY,
    almacen_id       BIGINT NOT NULL REFERENCES almacenes(id),
    codigo           VARCHAR(20) NOT NULL,
    zona             VARCHAR(50),
    pasillo          VARCHAR(50),
    estante          VARCHAR(50),
    nivel            VARCHAR(50),
    capacidad_maxima INTEGER,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (almacen_id, codigo)
);

-- ---------- MATERIALES (RF-04, RF-13, RF-16, RF-19) ----------
CREATE TABLE materiales (
    id             BIGSERIAL PRIMARY KEY,
    sku            VARCHAR(30) NOT NULL UNIQUE,
    nombre         VARCHAR(200) NOT NULL,
    descripcion    VARCHAR(1000),
    unidad_medida  VARCHAR(20) NOT NULL,
    categoria      VARCHAR(60) NOT NULL,
    stock_minimo   NUMERIC(14,3) NOT NULL DEFAULT 0,
    stock_maximo   NUMERIC(14,3) NOT NULL DEFAULT 0,
    requiere_lote  BOOLEAN NOT NULL DEFAULT FALSE,
    imagen_url     VARCHAR(500),
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ
);
CREATE INDEX idx_materiales_categoria ON materiales (categoria);
CREATE INDEX idx_materiales_nombre ON materiales (nombre);

-- ---------- STOCK POR UBICACION (RF-10) ----------
CREATE TABLE stock_ubicacion (
    id          BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materiales(id),
    ubicacion_id BIGINT NOT NULL REFERENCES ubicaciones(id),
    cantidad    NUMERIC(14,3) NOT NULL DEFAULT 0,
    version     BIGINT NOT NULL DEFAULT 0,
    UNIQUE (material_id, ubicacion_id)
);
CREATE INDEX idx_stock_material ON stock_ubicacion (material_id);

-- ---------- MOVIMIENTOS DE ALMACEN (RF-06,08,09,13,17,18) ----------
CREATE TABLE movimientos_almacen (
    id                       BIGSERIAL PRIMARY KEY,
    tipo                     VARCHAR(25) NOT NULL,
    material_id              BIGINT NOT NULL REFERENCES materiales(id),
    ubicacion_id             BIGINT NOT NULL REFERENCES ubicaciones(id),
    cantidad                 NUMERIC(14,3) NOT NULL,
    saldo_resultante         NUMERIC(14,3),
    lote                     VARCHAR(50),
    fecha_vencimiento        DATE,
    orden_compra             VARCHAR(50),
    proveedor                VARCHAR(150),
    orden_produccion         VARCHAR(50),
    area_solicitante         VARCHAR(100),
    movimiento_relacionado_id BIGINT,
    usuario_id               BIGINT NOT NULL REFERENCES usuarios(id),
    observaciones            VARCHAR(500),
    creado_en                TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_movimientos_material_fecha ON movimientos_almacen (material_id, creado_en);
CREATE INDEX idx_movimientos_fecha ON movimientos_almacen (creado_en);

-- ---------- INVENTARIO FISICO (RF-12) ----------
CREATE TABLE inventarios_fisicos (
    id            BIGSERIAL PRIMARY KEY,
    almacen_id    BIGINT NOT NULL REFERENCES almacenes(id),
    usuario_id    BIGINT NOT NULL REFERENCES usuarios(id),
    estado        VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    fecha_inicio  TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_cierre  TIMESTAMPTZ
);

CREATE TABLE detalle_inventario_fisico (
    id                    BIGSERIAL PRIMARY KEY,
    inventario_fisico_id  BIGINT NOT NULL REFERENCES inventarios_fisicos(id) ON DELETE CASCADE,
    material_id           BIGINT NOT NULL REFERENCES materiales(id),
    ubicacion_id          BIGINT NOT NULL REFERENCES ubicaciones(id),
    stock_sistema         NUMERIC(14,3) NOT NULL,
    stock_fisico          NUMERIC(14,3),
    diferencia            NUMERIC(14,3),
    observaciones         VARCHAR(300)
);

-- ---------- AUDITORIA (RF-20) ----------
CREATE TABLE auditoria (
    id             BIGSERIAL PRIMARY KEY,
    usuario_correo VARCHAR(150),
    entidad        VARCHAR(100) NOT NULL,
    entidad_id     BIGINT,
    accion         VARCHAR(20) NOT NULL,
    detalle        VARCHAR(2000),
    fecha          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auditoria_entidad ON auditoria (entidad, fecha);
