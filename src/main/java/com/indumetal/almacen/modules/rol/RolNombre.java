package com.indumetal.almacen.modules.rol;

/**
 * Roles soportados por el sistema (RF-02).
 * Se mapean 1 a 1 con la tabla "roles" sembrada por Flyway (V1__init_schema.sql).
 */
public enum RolNombre {
    ALMACENERO,
    SUPERVISOR_ALMACEN,
    JEFE_PRODUCCION,
    ADMINISTRADOR
}
