package com.indumetal.almacen.modules.usuario.dto;

import com.indumetal.almacen.modules.rol.RolNombre;
import com.indumetal.almacen.modules.usuario.Usuario;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UsuarioResponse {
    private Long id;
    private String nombres;
    private String apellidos;
    private String correo;
    private RolNombre rol;
    private Boolean activo;

    public static UsuarioResponse fromEntity(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .correo(u.getCorreo())
                .rol(u.getRol().getNombre())
                .activo(u.getActivo())
                .build();
    }
}
