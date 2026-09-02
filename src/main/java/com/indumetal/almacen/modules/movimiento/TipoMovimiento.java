package com.indumetal.almacen.modules.movimiento;

/** RF-06, RF-08, RF-17, RF-18: tipos de movimiento que afectan el stock. */
public enum TipoMovimiento {
    INGRESO,        // recepcion de materia prima/insumos (RF-06)
    SALIDA,         // despacho hacia produccion (RF-08)
    DEVOLUCION,     // devolucion de produccion hacia almacen (RF-17)
    TRANSFERENCIA_SALIDA, // sale de la ubicacion/almacen origen (RF-18)
    TRANSFERENCIA_ENTRADA, // entra a la ubicacion/almacen destino (RF-18)
    AJUSTE_POSITIVO,  // ajuste por inventario fisico (RF-12)
    AJUSTE_NEGATIVO
}
