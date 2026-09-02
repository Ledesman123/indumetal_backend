package com.indumetal.almacen.modules.auth;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.modules.auth.dto.LoginRequest;
import com.indumetal.almacen.modules.auth.dto.LoginResponse;
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

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Inicio de sesion exitoso", authService.login(request));
    }
}
