package com.indumetal.almacen.modules.alerta;

import com.indumetal.almacen.modules.material.Material;
import com.indumetal.almacen.modules.material.MaterialRepository;
import com.indumetal.almacen.modules.stock.StockUbicacionRepository;
import com.indumetal.almacen.modules.usuario.UsuarioRepository;
import com.indumetal.almacen.modules.rol.RolNombre;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF-11: revisa periodicamente el stock de cada material y envia una alerta
 * por correo a Supervisores de Almacen y Administradores cuando el stock
 * total cae por debajo del stock minimo configurado.
 *
 * La frecuencia se define en application.yml -> app.stock.revisar-cron
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertaStockService {

    private final MaterialRepository materialRepository;
    private final StockUbicacionRepository stockRepository;
    private final UsuarioRepository usuarioRepository;
    private final JavaMailSender mailSender;

    @Scheduled(cron = "${app.stock.revisar-cron}")
    public void revisarStockMinimo() {
        List<Long> materialesEnAlerta = stockRepository.buscarMaterialesEnStockMinimo();
        if (materialesEnAlerta.isEmpty()) {
            return;
        }
        List<String> correosDestino = usuarioRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .filter(u -> u.getRol().getNombre() == RolNombre.SUPERVISOR_ALMACEN
                        || u.getRol().getNombre() == RolNombre.ADMINISTRADOR)
                .map(u -> u.getCorreo())
                .toList();

        for (Long materialId : materialesEnAlerta) {
            materialRepository.findById(materialId).ifPresent(m -> enviarAlerta(m, correosDestino));
        }
    }

    private void enviarAlerta(Material material, List<String> destinatarios) {
        if (destinatarios.isEmpty()) return;

        BigDecimal stockActual = stockRepository.sumarStockPorMaterial(material.getId());

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatarios.toArray(new String[0]));
        mensaje.setSubject("[Almacen INDUMETAL] Stock minimo: " + material.getSku() + " - " + material.getNombre());
        mensaje.setText("""
                El material %s (%s) alcanzo el stock minimo.

                Stock actual: %s %s
                Stock minimo configurado: %s %s

                Por favor, coordine el reabastecimiento con el area de Compras.
                """.formatted(material.getSku(), material.getNombre(),
                stockActual, material.getUnidadMedida(),
                material.getStockMinimo(), material.getUnidadMedida()));

        try {
            mailSender.send(mensaje);
        } catch (Exception e) {
            // No se detiene el job por un error de envio de correo; solo se registra.
            log.warn("No se pudo enviar la alerta de stock minimo para {}: {}", material.getSku(), e.getMessage());
        }
    }
}
