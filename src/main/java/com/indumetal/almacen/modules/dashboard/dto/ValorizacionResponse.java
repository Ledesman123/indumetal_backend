package com.indumetal.almacen.modules.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** RF-14: valorizacion total del inventario y su desglose por categoria de material. */
@Getter
@Builder
public class ValorizacionResponse {
    private BigDecimal valorizacionTotal;
    private List<ValorizacionCategoria> porCategoria;

    @Getter
    @Builder
    public static class ValorizacionCategoria {
        private String categoria;
        private BigDecimal valorizacion;
    }
}
