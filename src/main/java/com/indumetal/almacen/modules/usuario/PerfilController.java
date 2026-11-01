package com.indumetal.almacen.modules.usuario;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.common.exception.BusinessException;
import com.indumetal.almacen.modules.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Perfil del usuario autenticado (cualquier rol): consultar y editar sus
 * propios datos y cambiar su contrasena. Separado de UsuarioController porque
 * aquel es exclusivo del Administrador.
 */
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<UsuarioResponse> obtener() {
        return ApiResponse.ok(UsuarioResponse.fromEntity(usuarioActual()));
    }

    @PutMapping
    @Transactional
    public ApiResponse<UsuarioResponse> actualizar(@Valid @RequestBody ActualizarPerfilRequest request) {
        Usuario usuario = usuarioActual();
        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        return ApiResponse.ok("Perfil actualizado", UsuarioResponse.fromEntity(usuarioRepository.save(usuario)));
    }

    @PutMapping("/password")
    @Transactional
    public ApiResponse<Void> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request) {
        Usuario usuario = usuarioActual();
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new BusinessException("La contrasena actual es incorrecta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuarioRepository.save(usuario);
        return ApiResponse.ok("Contrasena actualizada correctamente", null);
    }

    private Usuario usuarioActual() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no encontrado"));
    }

    @Getter
    @Setter
    public static class ActualizarPerfilRequest {
        @NotBlank @Size(max = 150)
        private String nombres;
        @NotBlank @Size(max = 150)
        private String apellidos;
    }

    @Getter
    @Setter
    public static class CambiarPasswordRequest {
        @NotBlank
        private String passwordActual;
        @NotBlank
        @Size(min = 8, message = "La nueva contrasena debe tener al menos 8 caracteres")
        private String passwordNueva;
    }
}
