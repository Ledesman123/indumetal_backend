package com.indumetal.almacen.modules.auth.dto;

import com.indumetal.almacen.modules.rol.RolNombre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tipo; // "Bearer"
    private Long usuarioId;
    private String nombreCompleto;
    private RolNombre rol;
}
