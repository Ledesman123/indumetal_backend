package com.indumetal.almacen.modules.inventariofisico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RegistrarConteoRequest {
    @NotNull private Long detalleId;
    @NotNull private BigDecimal stockFisico;
    private String observaciones;
}
