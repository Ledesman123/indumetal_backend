package com.indumetal.almacen.modules.movimiento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** RF-18: transferencia de materiales entre almacenes o zonas. */
@Getter
@Setter
public class TransferenciaRequest {
    @NotNull private Long materialId;
    @NotNull private Long ubicacionOrigenId;
    @NotNull private Long ubicacionDestinoId;
    @NotNull @DecimalMin("0.0001") private java.math.BigDecimal cantidad;
    private String observaciones;
}
