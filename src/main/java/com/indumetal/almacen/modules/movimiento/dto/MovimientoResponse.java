package com.indumetal.almacen.modules.movimiento.dto;

import com.indumetal.almacen.modules.movimiento.MovimientoAlmacen;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Vista plana de un movimiento para el frontend. Evita serializar las entidades
 * relacionadas completas (ej. Usuario, que incluye el hash de la contrasena).
 */
@Getter
@Builder
public class MovimientoResponse {
    private Long id;
    private String tipo;
    private Long materialId;
    private String materialSku;
    private String materialNombre;
    private String unidadMedida;
    private Long ubicacionId;
    private String ubicacionCodigo;
    private BigDecimal cantidad;
    private BigDecimal saldoResultante;
    private String lote;
    private LocalDate fechaVencimiento;
    private String ordenCompra;
    private String proveedor;
    private String ordenProduccion;
    private String areaSolicitante;
    private Long movimientoRelacionadoId;
    private String observaciones;
    private String usuario;
    private Instant creadoEn;

    public static MovimientoResponse fromEntity(MovimientoAlmacen m) {
        return MovimientoResponse.builder()
                .id(m.getId())
                .tipo(m.getTipo().name())
                .materialId(m.getMaterial().getId())
                .materialSku(m.getMaterial().getSku())
                .materialNombre(m.getMaterial().getNombre())
                .unidadMedida(m.getMaterial().getUnidadMedida())
                .ubicacionId(m.getUbicacion().getId())
                .ubicacionCodigo(m.getUbicacion().getCodigo())
                .cantidad(m.getCantidad())
                .saldoResultante(m.getSaldoResultante())
                .lote(m.getLote())
                .fechaVencimiento(m.getFechaVencimiento())
                .ordenCompra(m.getOrdenCompra())
                .proveedor(m.getProveedor())
                .ordenProduccion(m.getOrdenProduccion())
                .areaSolicitante(m.getAreaSolicitante())
                .movimientoRelacionadoId(m.getMovimientoRelacionadoId())
                .observaciones(m.getObservaciones())
                .usuario(m.getUsuario().getNombres() + " " + m.getUsuario().getApellidos())
                .creadoEn(m.getCreadoEn())
                .build();
    }
}
