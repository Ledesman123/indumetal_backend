package com.indumetal.almacen.modules.usuario;

import com.indumetal.almacen.common.exception.BusinessException;
import com.indumetal.almacen.common.exception.ResourceNotFoundException;
import com.indumetal.almacen.modules.rol.Rol;
import com.indumetal.almacen.modules.rol.RolRepository;
import com.indumetal.almacen.modules.usuario.dto.UsuarioRequest;
import com.indumetal.almacen.modules.usuario.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** RF-03: administracion de usuarios y roles (solo Administrador). */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByCorreoIgnoreCase(request.getCorreo())) {
            throw new BusinessException("Ya existe un usuario registrado con ese correo");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("La contrasena es obligatoria al crear un usuario");
        }
        Rol rol = rolRepository.findByNombre(request.getRol())
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", request.getRol()));

        Usuario usuario = Usuario.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .correo(request.getCorreo().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(rol)
                .activo(true)
                .build();

        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscar(id);
        Rol rol = rolRepository.findByNombre(request.getRol())
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", request.getRol()));

        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setCorreo(request.getCorreo().toLowerCase());
        usuario.setRol(rol);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse activarDesactivar(Long id, boolean activo) {
        Usuario usuario = buscar(id);
        usuario.setActivo(activo);
        if (activo) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    @Override
    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.fromEntity(buscar(id));
    }

    @Override
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::fromEntity).toList();
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }
}
