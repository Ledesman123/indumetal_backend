package com.indumetal.almacen.modules.movimiento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** RF-06: registro de ingreso de materia prima/insumos. */
@Getter
@Setter
public class IngresoRequest {
    @NotNull private Long materialId;

    // RF-07: si se omite (null), MovimientoService asigna la ubicacion automaticamente:
    // 1) reutiliza una ubicacion donde el material ya tenga stock, o
    // 2) si es la primera vez, usa la primera ubicacion activa disponible.
    private Long ubicacionId;

    @NotNull @DecimalMin("0.0001") private java.math.BigDecimal cantidad;
    private String ordenCompra;
    private String proveedor;
    private String lote;
    private LocalDate fechaVencimiento;
    private String observaciones;
}
