package com.indumetal.almacen.modules.auditoria;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-20: servicio de bitacora de auditoria. Se invoca explicitamente desde
 * los servicios de negocio (Material, Usuario, Movimiento, etc.) despues de
 * una operacion de creacion/edicion/eliminacion relevante.
 *
 * Se ejecuta en una transaccion NUEVA (REQUIRES_NEW) para que el registro de
 * auditoria quede guardado incluso si, mas adelante en el mismo request,
 * ocurriera un rollback de la operacion de negocio.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String entidad, Long entidadId, String accion, String detalle) {
        String correo = obtenerCorreoActual();
        auditoriaRepository.save(Auditoria.builder()
                .usuarioCorreo(correo)
                .entidad(entidad)
                .entidadId(entidadId)
                .accion(accion)
                .detalle(detalle)
                .build());
    }

    private String obtenerCorreoActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }
}
