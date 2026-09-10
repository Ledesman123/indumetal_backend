package com.indumetal.almacen.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** Paso 2 de "olvide mi password": el usuario escribe el codigo de 6 digitos recibido por correo. */
@Getter
@Setter
public class VerifyResetCodeRequest {
    @NotBlank @Email
    private String correo;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "El codigo debe tener exactamente 6 digitos")
    private String codigo;
}