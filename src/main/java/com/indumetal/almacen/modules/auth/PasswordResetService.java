package com.indumetal.almacen.modules.auth;

import com.indumetal.almacen.common.exception.UnauthorizedException;
import com.indumetal.almacen.modules.auditoria.AuditoriaService;
import com.indumetal.almacen.modules.auth.dto.VerifyResetCodeResponse;
import com.indumetal.almacen.modules.usuario.Usuario;
import com.indumetal.almacen.modules.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * "Olvide mi password": flujo de 3 pasos.
 *  1) solicitarCodigo:   el usuario pide un codigo -> se le envia por correo (6 digitos, expira en 10 min)
 *  2) verificarCodigo:   el usuario escribe el codigo -> si es correcto, se le entrega un resetToken (expira en 10 min)
 *  3) resetearPassword:  el usuario envia el resetToken + su nueva contrasena -> se actualiza
 *
 * Decisiones de seguridad (documentadas para poder explicarlas):
 *  - Nunca se revela si un correo existe o no en el sistema: los 3 metodos
 *    responden siempre el mismo mensaje generico, exista o no la cuenta.
 *  - Ni el codigo ni el resetToken se guardan en texto plano; se guardan
 *    con BCrypt igual que las contrasenas.
 *  - El resetToken es distinto del codigo de 6 digitos: el codigo solo sirve
 *    para "demostrar que el correo es tuyo"; el token es el que de verdad
 *    autoriza el cambio de password, y solo se entrega una vez validado el codigo.
 *  - Cada solicitud nueva de codigo invalida (borra) los intentos anteriores
 *    de ese usuario, para que no queden codigos viejos utilizables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int CODIGO_EXPIRA_MINUTOS = 10;
    private static final int TOKEN_EXPIRA_MINUTOS = 10;
    private static final String MENSAJE_GENERICO_SOLICITUD =
            "Si el correo esta registrado, se enviara un codigo de verificacion.";
    private static final String MENSAJE_GENERICO_INVALIDO = "Codigo o token invalido o expirado.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public String solicitarCodigo(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoIgnoreCase(correo);
        if (usuarioOpt.isEmpty()) {
            // No revelamos si el correo existe; simplemente no hacemos nada mas.
            log.debug("Solicitud de codigo para un correo no registrado: {}", correo);
            return MENSAJE_GENERICO_SOLICITUD;
        }
        Usuario usuario = usuarioOpt.get();

        // Invalida cualquier codigo/token anterior sin usar de este usuario.
        resetCodeRepository.eliminarPorUsuario(usuario.getId());

        String codigo = generarCodigoDeSeisDigitos();
        PasswordResetCode registro = PasswordResetCode.builder()
                .usuario(usuario)
                .codigoHash(passwordEncoder.encode(codigo))
                .codigoExpiracion(Instant.now().plus(CODIGO_EXPIRA_MINUTOS, ChronoUnit.MINUTES))
                .codigoVerificado(false)
                .build();
        resetCodeRepository.save(registro);

        enviarCorreoConCodigo(usuario, codigo);
        return MENSAJE_GENERICO_SOLICITUD;
    }

    @Transactional
    public VerifyResetCodeResponse verificarCodigo(String correo, String codigo) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new UnauthorizedException(MENSAJE_GENERICO_INVALIDO));

        PasswordResetCode registro = resetCodeRepository.findFirstByUsuarioIdOrderByCreadoEnDesc(usuario.getId())
                .orElseThrow(() -> new UnauthorizedException(MENSAJE_GENERICO_INVALIDO));

        boolean expirado = registro.getCodigoExpiracion().isBefore(Instant.now());
        boolean yaVerificado = Boolean.TRUE.equals(registro.getCodigoVerificado());
        boolean coincide = passwordEncoder.matches(codigo, registro.getCodigoHash());

        if (expirado || yaVerificado || !coincide) {
            throw new UnauthorizedException(MENSAJE_GENERICO_INVALIDO);
        }

        String resetToken = UUID.randomUUID().toString();
        registro.setCodigoVerificado(true);
        registro.setTokenHash(passwordEncoder.encode(resetToken));
        registro.setTokenExpiracion(Instant.now().plus(TOKEN_EXPIRA_MINUTOS, ChronoUnit.MINUTES));
        resetCodeRepository.save(registro);

        return VerifyResetCodeResponse.builder()
                .resetToken(resetToken)
                .expiraEnMinutos(TOKEN_EXPIRA_MINUTOS)
                .build();
    }

    @Transactional
    public void resetearPassword(String correo, String resetToken, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new UnauthorizedException(MENSAJE_GENERICO_INVALIDO));

        PasswordResetCode registro = resetCodeRepository.findFirstByUsuarioIdOrderByCreadoEnDesc(usuario.getId())
                .orElseThrow(() -> new UnauthorizedException(MENSAJE_GENERICO_INVALIDO));

        boolean codigoVerificado = Boolean.TRUE.equals(registro.getCodigoVerificado());
        boolean tokenPresente = registro.getTokenHash() != null && registro.getTokenExpiracion() != null;
        boolean tokenExpirado = tokenPresente && registro.getTokenExpiracion().isBefore(Instant.now());
        boolean tokenCoincide = tokenPresente && passwordEncoder.matches(resetToken, registro.getTokenHash());

        if (!codigoVerificado || !tokenPresente || tokenExpirado || !tokenCoincide) {
            throw new UnauthorizedException(MENSAJE_GENERICO_INVALIDO);
        }

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        // Si la cuenta estaba bloqueada por intentos fallidos, un cambio de
        // password legitimo (via correo) es una buena oportunidad para desbloquearla.
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        resetCodeRepository.delete(registro);

        auditoriaService.registrar("Usuario", usuario.getId(), "RESET_PASSWORD",
                "Contrasena restablecida via codigo de verificacion enviado a " + usuario.getCorreo());
    }

    private String generarCodigoDeSeisDigitos() {
        int numero = random.nextInt(1_000_000); // 0 a 999999
        return String.format("%06d", numero);
    }

    private void enviarCorreoConCodigo(Usuario usuario, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(usuario.getCorreo());
        mensaje.setSubject("[Almacen INDUMETAL] Codigo de recuperacion de contrasena");
        mensaje.setText("""
                Hola %s,

                Recibimos una solicitud para restablecer tu contrasena.
                Tu codigo de verificacion es: %s

                Este codigo vence en %d minutos. Si tu no solicitaste este cambio,
                puedes ignorar este correo; tu contrasena actual seguira funcionando.
                """.formatted(usuario.getNombres(), codigo, CODIGO_EXPIRA_MINUTOS));

        try {
            mailSender.send(mensaje);
        } catch (Exception e) {
            // Igual que en las alertas de stock: no se interrumpe el flujo por un
            // fallo de correo, pero queda registrado para poder investigarlo.
            log.warn("No se pudo enviar el codigo de recuperacion a {}: {}", usuario.getCorreo(), e.getMessage());
        }
    }
}