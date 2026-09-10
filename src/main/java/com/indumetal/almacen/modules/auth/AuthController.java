package com.indumetal.almacen.modules.auth;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.modules.auth.dto.ForgotPasswordRequest;
import com.indumetal.almacen.modules.auth.dto.LoginRequest;
import com.indumetal.almacen.modules.auth.dto.LoginResponse;
import com.indumetal.almacen.modules.auth.dto.ResetPasswordRequest;
import com.indumetal.almacen.modules.auth.dto.VerifyResetCodeRequest;
import com.indumetal.almacen.modules.auth.dto.VerifyResetCodeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Inicio de sesion exitoso", authService.login(request));
    }

    // Paso 1 de "olvide mi password": envia un codigo de 6 digitos al correo.
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String mensaje = passwordResetService.solicitarCodigo(request.getCorreo());
        return ApiResponse.ok(mensaje, null);
    }

    // Paso 2: valida el codigo recibido y entrega un token temporal para el paso 3.
    @PostMapping("/verify-reset-code")
    public ApiResponse<VerifyResetCodeResponse> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        VerifyResetCodeResponse respuesta = passwordResetService.verificarCodigo(request.getCorreo(), request.getCodigo());
        return ApiResponse.ok("Codigo verificado correctamente", respuesta);
    }

    // Paso 3: cambia la contrasena usando el resetToken obtenido en el paso 2.
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetearPassword(request.getCorreo(), request.getResetToken(), request.getNuevaPassword());
        return ApiResponse.ok("Contrasena actualizada correctamente", null);
    }
}