package com.indumetal.almacen.modules.usuario;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.modules.usuario.dto.UsuarioRequest;
import com.indumetal.almacen.modules.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-03: Solo el rol ADMINISTRADOR puede administrar usuarios. */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ApiResponse<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.ok("Usuario creado correctamente", usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UsuarioResponse> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.ok("Usuario actualizado", usuarioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ApiResponse<UsuarioResponse> cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        return ApiResponse.ok(usuarioService.activarDesactivar(id, activo));
    }

    @GetMapping("/{id}")
    public ApiResponse<UsuarioResponse> obtener(@PathVariable Long id) {
        return ApiResponse.ok(usuarioService.obtenerPorId(id));
    }

    @GetMapping
    public ApiResponse<List<UsuarioResponse>> listar() {
        return ApiResponse.ok(usuarioService.listarTodos());
    }
}
