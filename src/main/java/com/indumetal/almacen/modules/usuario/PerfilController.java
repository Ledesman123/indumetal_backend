package com.indumetal.almacen.modules.usuario;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.common.exception.BusinessException;
import com.indumetal.almacen.modules.rol.RolNombre;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
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

    // La foto llega comprimida desde el frontend (~30-80 KB); se limita para no inflar la BD.
    private static final int MAX_FOTO_CARACTERES = 700_000;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<PerfilResponse> obtener() {
        return ApiResponse.ok(PerfilResponse.fromEntity(usuarioActual()));
    }

    @PutMapping
    @Transactional
    public ApiResponse<PerfilResponse> actualizar(@Valid @RequestBody ActualizarPerfilRequest request) {
        String foto = request.getFotoUrl();
        if (foto != null && !foto.isBlank()) {
            if (!foto.startsWith("data:image/")) {
                throw new BusinessException("La foto de perfil debe ser una imagen.");
            }
            if (foto.length() > MAX_FOTO_CARACTERES) {
                throw new BusinessException("La foto de perfil es demasiado grande.");
            }
        }

        Usuario usuario = usuarioActual();
        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());
        usuario.setFotoUrl(foto == null || foto.isBlank() ? null : foto);
        return ApiResponse.ok("Perfil actualizado", PerfilResponse.fromEntity(usuarioRepository.save(usuario)));
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
    @Builder
    public static class PerfilResponse {
        private Long id;
        private String nombres;
        private String apellidos;
        private String correo;
        private RolNombre rol;
        private String telefono;
        private String fotoUrl;

        static PerfilResponse fromEntity(Usuario u) {
            return PerfilResponse.builder()
                    .id(u.getId())
                    .nombres(u.getNombres())
                    .apellidos(u.getApellidos())
                    .correo(u.getCorreo())
                    .rol(u.getRol().getNombre())
                    .telefono(u.getTelefono())
                    .fotoUrl(u.getFotoUrl())
                    .build();
        }
    }

    @Getter
    @Setter
    public static class ActualizarPerfilRequest {
        @NotBlank @Size(max = 150)
        private String nombres;
        @NotBlank @Size(max = 150)
        private String apellidos;
        @Size(max = 20)
        private String telefono;
        private String fotoUrl; // data URL (base64) o null para quitar la foto
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
