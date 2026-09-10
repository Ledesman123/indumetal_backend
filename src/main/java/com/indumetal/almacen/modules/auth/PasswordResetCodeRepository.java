package com.indumetal.almacen.modules.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findFirstByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    @Modifying
    @Query("DELETE FROM PasswordResetCode p WHERE p.usuario.id = :usuarioId")
    void eliminarPorUsuario(Long usuarioId);
}