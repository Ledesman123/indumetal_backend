package com.indumetal.almacen.modules.auth.dto;

import lombok.Builder;
import lombok.Getter;

/** Respuesta del paso 2: entrega un token temporal para poder cambiar la password en el paso 3. */
@Getter
@Builder
public class VerifyResetCodeResponse {
    private String resetToken;
    private int expiraEnMinutos;
}