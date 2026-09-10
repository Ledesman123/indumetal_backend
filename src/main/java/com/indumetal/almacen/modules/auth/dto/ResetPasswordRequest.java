package com.indumetal.almacen.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Paso 3 de "olvide mi password": cambia la contrasena usando el resetToken del paso 2. */
@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank @Email
    private String correo;

    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 8, message = "La nueva contrasena debe tener al menos 8 caracteres")
    private String nuevaPassword;
}