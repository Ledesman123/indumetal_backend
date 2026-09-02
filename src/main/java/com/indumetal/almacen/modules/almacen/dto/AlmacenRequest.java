package com.indumetal.almacen.modules.almacen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlmacenRequest {
    @NotBlank
    private String codigo;
    @NotBlank
    private String nombre;
    private String direccion;
}
