-- ============================================================
-- V2__add_costo_unitario_material.sql
-- RF-14: se necesita el costo unitario de cada material para poder
-- calcular la valorizacion del inventario en el dashboard.
-- ============================================================

ALTER TABLE materiales
    ADD COLUMN costo_unitario NUMERIC(14,2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN materiales.costo_unitario IS
    'Costo unitario de referencia (S/) usado para calcular la valorizacion del inventario (RF-14).';
