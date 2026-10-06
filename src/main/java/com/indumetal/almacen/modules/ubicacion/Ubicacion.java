package com.indumetal.almacen.modules.ubicacion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // <--- NUEVO IMPORT
import com.indumetal.almacen.modules.almacen.Almacen;
import jakarta.persistence.*;
import lombok.*;

/** RF-05, RF-07: ubicacion fisica (zona-pasillo-estante-nivel) dentro de un almacen. */
@Entity
@Table(name = "ubicaciones", uniqueConstraints = @UniqueConstraint(columnNames = {"almacen_id", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <--- NUEVA ANOTACIÓN
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(nullable = false, length = 20)
    private String codigo; // ej: Z1-P02-E03-N1

    private String zona;
    private String pasillo;
    private String estante;
    private String nivel;

    @Column(name = "capacidad_maxima")
    private Integer capacidadMaxima;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}