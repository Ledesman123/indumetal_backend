package com.indumetal.almacen.modules.kardex;

import com.indumetal.almacen.modules.movimiento.MovimientoAlmacen;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class KardexItem {
    private Instant fecha;
    private String tipo;
    private String ubicacion;
    private BigDecimal entrada;
    private BigDecimal salida;
    private BigDecimal saldo;
    private String referencia; // orden de compra / orden de produccion
    private String usuario;

    public static KardexItem fromMovimiento(MovimientoAlmacen m) {
        boolean esEntrada = switch (m.getTipo()) {
            case INGRESO, DEVOLUCION, TRANSFERENCIA_ENTRADA, AJUSTE_POSITIVO -> true;
            default -> false;
        };
        String referencia = m.getOrdenCompra() != null ? m.getOrdenCompra() : m.getOrdenProduccion();

        return KardexItem.builder()
                .fecha(m.getCreadoEn())
                .tipo(m.getTipo().name())
                .ubicacion(m.getUbicacion().getCodigo())
                .entrada(esEntrada ? m.getCantidad() : BigDecimal.ZERO)
                .salida(!esEntrada ? m.getCantidad() : BigDecimal.ZERO)
                .saldo(m.getSaldoResultante())
                .referencia(referencia)
                .usuario(m.getUsuario().getNombres() + " " + m.getUsuario().getApellidos())
                .build();
    }
}
