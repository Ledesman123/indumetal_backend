package com.indumetal.almacen.modules.movimiento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** RF-17: devolucion de materiales sobrantes desde produccion hacia el almacen. */
@Getter
@Setter
public class DevolucionRequest {
    @NotNull private Long materialId;
    @NotNull private Long ubicacionId; // ubicacion destino donde se reingresa
    @NotNull @DecimalMin("0.0001") private java.math.BigDecimal cantidad;
    @NotBlank private String ordenProduccion;
    private String observaciones;
}
