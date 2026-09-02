package com.indumetal.almacen.modules.movimiento;

import com.indumetal.almacen.modules.material.Material;
import com.indumetal.almacen.modules.ubicacion.Ubicacion;
import com.indumetal.almacen.modules.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * RF-06, RF-08, RF-09, RF-13, RF-17, RF-18: registro cronologico de todo
 * movimiento de almacen. Es la fuente de verdad del Kardex (RF-09): el
 * kardex se construye consultando esta tabla ordenada por fecha.
 */
@Entity
@Table(name = "movimientos_almacen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoAlmacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoMovimiento tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "saldo_resultante")
    private BigDecimal saldoResultante; // saldo de la ubicacion luego del movimiento (para el kardex)

    // --- Trazabilidad RF-13 ---
    @Column(length = 50)
    private String lote;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    // --- Datos de ingreso (RF-06) ---
    @Column(name = "orden_compra", length = 50)
    private String ordenCompra;

    @Column(length = 150)
    private String proveedor;

    // --- Datos de salida (RF-08) ---
    @Column(name = "orden_produccion", length = 50)
    private String ordenProduccion;

    @Column(name = "area_solicitante", length = 100)
    private String areaSolicitante;

    // --- Transferencias (RF-18): referencia al movimiento pareja ---
    @Column(name = "movimiento_relacionado_id")
    private Long movimientoRelacionadoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // quien registro el movimiento

    @Column(length = 500)
    private String observaciones;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    void prePersist() {
        this.creadoEn = Instant.now();
    }
}
