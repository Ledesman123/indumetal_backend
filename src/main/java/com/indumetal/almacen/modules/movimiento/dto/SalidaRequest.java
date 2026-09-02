package com.indumetal.almacen.modules.movimiento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** RF-08: registro de salida de materiales hacia una orden de produccion. */
@Getter
@Setter
public class SalidaRequest {
    @NotNull private Long materialId;
    @NotNull private Long ubicacionId;
    @NotNull @DecimalMin("0.0001") private java.math.BigDecimal cantidad;
    @NotBlank private String ordenProduccion;
    @NotBlank private String areaSolicitante;
    private String observaciones;
}
