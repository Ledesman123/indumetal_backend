package com.indumetal.almacen.modules.inventariofisico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IniciarInventarioRequest {
    @NotNull private Long almacenId;
    private List<Long> materialIds; // materiales a contar en este ciclo; vacio = todo el almacen
}
