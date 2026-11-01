package com.indumetal.almacen.modules.stock;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** RF-10: fila de stock por material/ubicacion, aplanada para el frontend. */
@Getter
@Builder
public class StockResponse {
    private Long materialId;
    private String materialSku;
    private String materialNombre;
    private String unidadMedida;
    private Long ubicacionId;
    private String ubicacionCodigo;
    private Long almacenId;
    private String almacenNombre;
    private BigDecimal cantidad;
    private BigDecimal stockMinimo;
    private BigDecimal stockMaximo;

    public static StockResponse fromEntity(StockUbicacion s) {
        return StockResponse.builder()
                .materialId(s.getMaterial().getId())
                .materialSku(s.getMaterial().getSku())
                .materialNombre(s.getMaterial().getNombre())
                .unidadMedida(s.getMaterial().getUnidadMedida())
                .ubicacionId(s.getUbicacion().getId())
                .ubicacionCodigo(s.getUbicacion().getCodigo())
                .almacenId(s.getUbicacion().getAlmacen().getId())
                .almacenNombre(s.getUbicacion().getAlmacen().getNombre())
                .cantidad(s.getCantidad())
                .stockMinimo(s.getMaterial().getStockMinimo())
                .stockMaximo(s.getMaterial().getStockMaximo())
                .build();
    }
}
