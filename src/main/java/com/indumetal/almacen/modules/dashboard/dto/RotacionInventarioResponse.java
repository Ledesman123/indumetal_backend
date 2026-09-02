package com.indumetal.almacen.modules.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * RF-14: indice de rotacion de un material en el periodo consultado.
 * rotacion = unidades despachadas (SALIDA) en el periodo / stock actual disponible.
 * Un valor alto indica un material de "alta rotacion" (se mueve rapido);
 * un valor cercano a 0 indica baja rotacion (material casi inmovil).
 */
@Getter
@Builder
public class RotacionInventarioResponse {
    private Long materialId;
    private String sku;
    private String nombre;
    private BigDecimal unidadesDespachadas;
    private BigDecimal stockActual;
    private BigDecimal indiceRotacion; // null si el material no tiene stock actual (no aplica)
}
