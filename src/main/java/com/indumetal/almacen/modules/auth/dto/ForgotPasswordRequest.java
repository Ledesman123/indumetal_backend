package com.indumetal.almacen.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Paso 1 de "olvide mi password": el usuario pide que le envien un codigo. */
@Getter
@Setter
public class ForgotPasswordRequest {
    @NotBlank @Email
    private String correo;
}