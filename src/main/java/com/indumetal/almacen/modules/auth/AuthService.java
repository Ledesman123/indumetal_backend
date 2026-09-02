package com.indumetal.almacen.modules.auth;

import com.indumetal.almacen.common.exception.UnauthorizedException;
import com.indumetal.almacen.modules.auth.dto.LoginRequest;
import com.indumetal.almacen.modules.auth.dto.LoginResponse;
import com.indumetal.almacen.modules.usuario.Usuario;
import com.indumetal.almacen.modules.usuario.UsuarioRepository;
import com.indumetal.almacen.security.JwtService;
import com.indumetal.almacen.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * RF-01: inicio de sesion con JWT.
 * Restriccion: maximo 5 intentos fallidos consecutivos -> bloqueo temporal (15 min).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INTENTOS = 5;
    private static final long BLOQUEO_MINUTOS = 15;

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.getCorreo())
                .orElseThrow(() -> new UnauthorizedException("Correo o contrasena incorrectos"));

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(Instant.now())) {
            throw new LockedException("Cuenta bloqueada temporalmente por intentos fallidos. Intente mas tarde.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            registrarIntentoFallido(usuario);
            throw new UnauthorizedException("Correo o contrasena incorrectos");
        } catch (DisabledException ex) {
            throw new UnauthorizedException("El usuario se encuentra inactivo. Contacte al administrador.");
        }

        // login correcto: reiniciar contador de intentos
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        SecurityUser securityUser = new SecurityUser(usuario);
        String token = jwtService.generarToken(securityUser);

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .usuarioId(usuario.getId())
                .nombreCompleto(usuario.getNombres() + " " + usuario.getApellidos())
                .rol(usuario.getRol().getNombre())
                .build();
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS) {
            usuario.setBloqueadoHasta(Instant.now().plus(BLOQUEO_MINUTOS, ChronoUnit.MINUTES));
        }
        usuarioRepository.save(usuario);
    }
}
