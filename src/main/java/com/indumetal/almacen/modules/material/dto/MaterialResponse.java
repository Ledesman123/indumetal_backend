package com.indumetal.almacen.modules.material.dto;

import com.indumetal.almacen.modules.material.Material;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MaterialResponse {
    private Long id;
    private String sku;
    private String nombre;
    private String descripcion;
    private String unidadMedida;
    private String categoria;
    private BigDecimal stockMinimo;
    private BigDecimal stockMaximo;
    private BigDecimal costoUnitario;
    private Boolean requiereLote;
    private String imagenUrl;
    private Boolean activo;

    public static MaterialResponse fromEntity(Material m) {
        return MaterialResponse.builder()
                .id(m.getId())
                .sku(m.getSku())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .unidadMedida(m.getUnidadMedida())
                .categoria(m.getCategoria())
                .stockMinimo(m.getStockMinimo())
                .stockMaximo(m.getStockMaximo())
                .costoUnitario(m.getCostoUnitario())
                .requiereLote(m.getRequiereLote())
                .imagenUrl(m.getImagenUrl())
                .activo(m.getActivo())
                .build();
    }
}
