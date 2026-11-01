package com.indumetal.almacen.modules.inventariofisico.dto;

import com.indumetal.almacen.modules.inventariofisico.DetalleInventarioFisico;
import com.indumetal.almacen.modules.inventariofisico.InventarioFisico;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * RF-12: vista de un inventario fisico para el frontend. Evita la referencia
 * circular cabecera <-> detalle de las entidades JPA al serializar a JSON.
 */
@Getter
@Builder
public class InventarioFisicoResponse {
    private Long id;
    private Long almacenId;
    private String almacenNombre;
    private String estado;
    private String responsable;
    private Instant fechaInicio;
    private Instant fechaCierre;
    private List<Detalle> detalles;

    @Getter
    @Builder
    public static class Detalle {
        private Long id;
        private Long materialId;
        private String materialSku;
        private String materialNombre;
        private Long ubicacionId;
        private String ubicacionCodigo;
        private BigDecimal stockSistema;
        private BigDecimal stockFisico;
        private BigDecimal diferencia;
        private String observaciones;

        public static Detalle fromEntity(DetalleInventarioFisico d) {
            return Detalle.builder()
                    .id(d.getId())
                    .materialId(d.getMaterial().getId())
                    .materialSku(d.getMaterial().getSku())
                    .materialNombre(d.getMaterial().getNombre())
                    .ubicacionId(d.getUbicacion().getId())
                    .ubicacionCodigo(d.getUbicacion().getCodigo())
                    .stockSistema(d.getStockSistema())
                    .stockFisico(d.getStockFisico())
                    .diferencia(d.getDiferencia())
                    .observaciones(d.getObservaciones())
                    .build();
        }
    }

    public static InventarioFisicoResponse fromEntity(InventarioFisico i) {
        return InventarioFisicoResponse.builder()
                .id(i.getId())
                .almacenId(i.getAlmacen().getId())
                .almacenNombre(i.getAlmacen().getNombre())
                .estado(i.getEstado().name())
                .responsable(i.getResponsable().getNombres() + " " + i.getResponsable().getApellidos())
                .fechaInicio(i.getFechaInicio())
                .fechaCierre(i.getFechaCierre())
                .detalles(i.getDetalles().stream()
                        .sorted(Comparator.comparing(DetalleInventarioFisico::getId))
                        .map(Detalle::fromEntity)
                        .toList())
                .build();
    }
}
