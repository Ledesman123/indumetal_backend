package com.indumetal.almacen.modules.stock;

import com.indumetal.almacen.modules.material.Material;
import com.indumetal.almacen.modules.ubicacion.Ubicacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * RF-10: saldo de stock en tiempo real, por material y por ubicacion.
 * Se actualiza de forma transaccional cada vez que se registra un movimiento
 * (ver MovimientoService), evitando tener que recalcular el kardex completo
 * cada vez que se consulta el stock disponible.
 */
@Entity
@Table(name = "stock_ubicacion", uniqueConstraints = @UniqueConstraint(columnNames = {"material_id", "ubicacion_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUbicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal cantidad = BigDecimal.ZERO;

    @Version
    private Long version; // control de concurrencia optimista (varios almaceneros a la vez)
}
