package com.indumetal.almacen.modules.auth;

import com.indumetal.almacen.common.audit.Auditable;
import com.indumetal.almacen.modules.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Soporta el flujo de "olvide mi password" (3 pasos: pedir codigo, verificar
 * codigo, resetear password). Cada fila representa un intento de recuperacion
 * para un usuario; se descarta (se borra) apenas se usa con exito o se pide
 * un codigo nuevo.
 */
@Entity
@Table(name = "password_reset_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetCode extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash; // BCrypt del codigo de 6 digitos

    @Column(name = "codigo_expiracion", nullable = false)
    private Instant codigoExpiracion;

    @Column(name = "codigo_verificado", nullable = false)
    @Builder.Default
    private Boolean codigoVerificado = false;

    @Column(name = "token_hash")
    private String tokenHash; // BCrypt del token de reset, emitido tras verificar el codigo

    @Column(name = "token_expiracion")
    private Instant tokenExpiracion;
}