package com.indumetal.almacen.modules.inventariofisico;

import com.indumetal.almacen.common.exception.BusinessException;
import com.indumetal.almacen.common.exception.ResourceNotFoundException;
import com.indumetal.almacen.modules.almacen.Almacen;
import com.indumetal.almacen.modules.almacen.AlmacenRepository;
import com.indumetal.almacen.modules.inventariofisico.dto.IniciarInventarioRequest;
import com.indumetal.almacen.modules.inventariofisico.dto.RegistrarConteoRequest;
import com.indumetal.almacen.modules.material.Material;
import com.indumetal.almacen.modules.material.MaterialRepository;
import com.indumetal.almacen.modules.stock.StockUbicacion;
import com.indumetal.almacen.modules.stock.StockUbicacionRepository;
import com.indumetal.almacen.modules.usuario.Usuario;
import com.indumetal.almacen.modules.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * RF-12: gestiona el ciclo de vida de un inventario fisico:
 *  1) iniciar()      -> toma una "foto" del stock del sistema por cada material/ubicacion
 *  2) registrarConteo() -> el almacenero digita el conteo fisico real
 *  3) cerrar()        -> calcula diferencias y AJUSTA el stock del sistema al stock fisico
 */
@Service
@RequiredArgsConstructor
public class InventarioFisicoService {

    private final InventarioFisicoRepository inventarioRepository;
    private final DetalleInventarioFisicoRepository detalleRepository;
    private final AlmacenRepository almacenRepository;
    private final MaterialRepository materialRepository;
    private final StockUbicacionRepository stockRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public InventarioFisico iniciar(IniciarInventarioRequest req) {
        Almacen almacen = almacenRepository.findById(req.getAlmacenId())
                .orElseThrow(() -> ResourceNotFoundException.of("Almacen", req.getAlmacenId()));

        InventarioFisico inventario = InventarioFisico.builder()
                .almacen(almacen)
                .responsable(usuarioActual())
                .estado(InventarioFisico.EstadoInventario.ABIERTO)
                .fechaInicio(Instant.now())
                .build();
        inventario = inventarioRepository.save(inventario);

        for (Long materialId : req.getMaterialIds()) {
            Material material = materialRepository.findById(materialId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Material", materialId));

            List<StockUbicacion> stocks = stockRepository.findByMaterialId(materialId);
            for (StockUbicacion s : stocks) {
                if (!s.getUbicacion().getAlmacen().getId().equals(almacen.getId())) continue;

                detalleRepository.save(DetalleInventarioFisico.builder()
                        .inventarioFisico(inventario)
                        .material(material)
                        .ubicacion(s.getUbicacion())
                        .stockSistema(s.getCantidad())
                        .build());
            }
        }
        return inventario;
    }

    @Transactional
    public DetalleInventarioFisico registrarConteo(RegistrarConteoRequest req) {
        DetalleInventarioFisico detalle = detalleRepository.findById(req.getDetalleId())
                .orElseThrow(() -> ResourceNotFoundException.of("Detalle de inventario", req.getDetalleId()));

        detalle.setStockFisico(req.getStockFisico());
        detalle.setDiferencia(req.getStockFisico().subtract(detalle.getStockSistema()));
        detalle.setObservaciones(req.getObservaciones());

        return detalleRepository.save(detalle);
    }

    /** Cierra el inventario y AJUSTA el stock real segun lo contado (RF-12). */
    @Transactional
    public InventarioFisico cerrar(Long inventarioId) {
        InventarioFisico inventario = inventarioRepository.findById(inventarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inventario fisico", inventarioId));

        if (inventario.getEstado() == InventarioFisico.EstadoInventario.CERRADO) {
            throw new BusinessException("El inventario ya se encuentra cerrado");
        }

        for (DetalleInventarioFisico d : inventario.getDetalles()) {
            if (d.getStockFisico() == null) {
                throw new BusinessException("Existen materiales sin conteo fisico registrado (ej. "
                        + d.getMaterial().getSku() + "). Complete el conteo antes de cerrar.");
            }
            StockUbicacion stock = stockRepository
                    .findByMaterialIdAndUbicacionId(d.getMaterial().getId(), d.getUbicacion().getId())
                    .orElseThrow();
            stock.setCantidad(d.getStockFisico()); // el stock del sistema pasa a ser el fisico
            stockRepository.save(stock);
        }

        inventario.setEstado(InventarioFisico.EstadoInventario.CERRADO);
        inventario.setFechaCierre(Instant.now());
        return inventarioRepository.save(inventario);
    }

    private Usuario usuarioActual() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
    }
}
