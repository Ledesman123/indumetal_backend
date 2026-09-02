package com.indumetal.almacen.modules.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** RF-14: fila del ranking "materiales de mayor movimiento". */
@Getter
@Builder
public class MaterialMovimientoResponse {
    private Long materialId;
    private String sku;
    private String nombre;
    private long cantidadMovimientos;
    private BigDecimal unidadesMovidas;
}
