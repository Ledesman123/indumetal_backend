package com.indumetal.almacen.modules.inventariofisico;

import com.indumetal.almacen.modules.material.Material;
import com.indumetal.almacen.modules.ubicacion.Ubicacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** RF-12: detalle por material/ubicacion: stock del sistema vs. conteo fisico y su diferencia. */
@Entity
@Table(name = "detalle_inventario_fisico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleInventarioFisico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_fisico_id", nullable = false)
    private InventarioFisico inventarioFisico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(name = "stock_sistema", nullable = false)
    private BigDecimal stockSistema;

    @Column(name = "stock_fisico")
    private BigDecimal stockFisico; // se completa durante el conteo

    @Column(name = "diferencia")
    private BigDecimal diferencia; // stockFisico - stockSistema

    @Column(length = 300)
    private String observaciones;
}
