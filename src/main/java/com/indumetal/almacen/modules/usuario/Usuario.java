package com.indumetal.almacen.modules.usuario;

import com.indumetal.almacen.common.audit.Auditable;
import com.indumetal.almacen.modules.rol.Rol;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombres;

    @Column(nullable = false, length = 150)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 150)
    private String correo; // correo institucional (RF-01)

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // BCrypt

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private Integer intentosFallidos = 0; // RF-01: bloqueo tras 5 intentos

    @Column(name = "bloqueado_hasta")
    private java.time.Instant bloqueadoHasta;
}
