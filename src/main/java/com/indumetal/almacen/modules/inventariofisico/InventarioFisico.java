package com.indumetal.almacen.modules.inventariofisico;

import com.indumetal.almacen.modules.almacen.Almacen;
import com.indumetal.almacen.modules.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** RF-12: cabecera de un proceso de inventario fisico/ciclico. */
@Entity
@Table(name = "inventarios_fisicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioFisico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoInventario estado = EstadoInventario.ABIERTO;

    @Column(name = "fecha_inicio", nullable = false)
    @Builder.Default
    private Instant fechaInicio = Instant.now();

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @OneToMany(mappedBy = "inventarioFisico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleInventarioFisico> detalles = new ArrayList<>();

    public enum EstadoInventario { ABIERTO, CERRADO }
}
