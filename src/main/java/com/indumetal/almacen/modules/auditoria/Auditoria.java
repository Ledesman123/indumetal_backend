package com.indumetal.almacen.modules.auditoria;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** RF-20: bitacora de auditoria (quien hizo que y cuando). */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_correo", length = 150)
    private String usuarioCorreo;

    @Column(nullable = false, length = 100)
    private String entidad; // ej: "Material", "MovimientoAlmacen"

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false, length = 20)
    private String accion; // CREAR, ACTUALIZAR, ELIMINAR

    @Column(length = 2000)
    private String detalle;

    @Column(nullable = false)
    @Builder.Default
    private Instant fecha = Instant.now();
}
