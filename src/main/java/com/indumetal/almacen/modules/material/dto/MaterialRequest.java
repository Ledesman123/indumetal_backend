package com.indumetal.almacen.modules.material.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MaterialRequest {

    @NotBlank
    private String sku;

    @NotBlank
    private String nombre;

    private String descripcion;

    @NotBlank
    private String unidadMedida;

    @NotBlank
    private String categoria;

    @NotNull @DecimalMin("0")
    private BigDecimal stockMinimo;

    @NotNull @DecimalMin("0")
    private BigDecimal stockMaximo;

    // RF-14: costo unitario para valorizacion del inventario. Opcional; si se omite, se asume 0.
    @DecimalMin("0")
    private BigDecimal costoUnitario;

    private Boolean requiereLote;

    private String imagenUrl;
}
