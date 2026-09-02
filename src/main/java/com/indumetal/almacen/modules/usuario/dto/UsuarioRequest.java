package com.indumetal.almacen.modules.usuario.dto;

import com.indumetal.almacen.modules.rol.RolNombre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequest {

    @NotBlank
    private String nombres;

    @NotBlank
    private String apellidos;

    @NotBlank
    @Email
    private String correo;

    // Solo obligatorio al crear; en edicion puede omitirse
    @Size(min = 8, message = "La contrasena debe tener minimo 8 caracteres")
    private String password;

    @NotNull
    private RolNombre rol;
}
