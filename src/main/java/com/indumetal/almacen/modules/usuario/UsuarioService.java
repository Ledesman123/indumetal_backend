package com.indumetal.almacen.modules.usuario;

import com.indumetal.almacen.modules.usuario.dto.UsuarioRequest;
import com.indumetal.almacen.modules.usuario.dto.UsuarioResponse;

import java.util.List;

public interface UsuarioService {
    UsuarioResponse crear(UsuarioRequest request);
    UsuarioResponse actualizar(Long id, UsuarioRequest request);
    UsuarioResponse activarDesactivar(Long id, boolean activo);
    UsuarioResponse obtenerPorId(Long id);
    List<UsuarioResponse> listarTodos();
}
