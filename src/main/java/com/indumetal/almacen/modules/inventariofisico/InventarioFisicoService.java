package com.indumetal.almacen.modules.inventariofisico;

import com.indumetal.almacen.common.exception.BusinessException;
import com.indumetal.almacen.common.exception.ResourceNotFoundException;
import com.indumetal.almacen.modules.almacen.Almacen;
import com.indumetal.almacen.modules.almacen.AlmacenRepository;
import com.indumetal.almacen.modules.inventariofisico.dto.IniciarInventarioRequest;
import com.indumetal.almacen.modules.inventariofisico.dto.InventarioFisicoResponse;
import com.indumetal.almacen.modules.inventariofisico.dto.RegistrarConteoRequest;
import com.indumetal.almacen.modules.stock.StockUbicacion;
import com.indumetal.almacen.modules.stock.StockUbicacionRepository;
import com.indumetal.almacen.modules.usuario.Usuario;
import com.indumetal.almacen.modules.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
    private final StockUbicacionRepository stockRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<InventarioFisicoResponse> listar() {
        return inventarioRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaInicio")).stream()
                .map(InventarioFisicoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventarioFisicoResponse obtener(Long id) {
        return InventarioFisicoResponse.fromEntity(buscar(id));
    }

    /**
     * Si 'materialIds' viene vacio o nulo, se cuentan TODOS los materiales con
     * stock registrado en el almacen (inventario completo); si no, solo los indicados (ciclico).
     */
    @Transactional
    public InventarioFisicoResponse iniciar(IniciarInventarioRequest req) {
        Almacen almacen = almacenRepository.findById(req.getAlmacenId())
                .orElseThrow(() -> ResourceNotFoundException.of("Almacen", req.getAlmacenId()));

        if (inventarioRepository.existsByAlmacenIdAndEstado(almacen.getId(), InventarioFisico.EstadoInventario.ABIERTO)) {
            throw new BusinessException("El almacen " + almacen.getNombre()
                    + " ya tiene un inventario en proceso. Cierrelo antes de iniciar otro.");
        }

        boolean todos = req.getMaterialIds() == null || req.getMaterialIds().isEmpty();
        List<StockUbicacion> stocks = stockRepository.findByUbicacionAlmacenId(almacen.getId()).stream()
                .filter(s -> todos || req.getMaterialIds().contains(s.getMaterial().getId()))
                .toList();
        if (stocks.isEmpty()) {
            throw new BusinessException("No hay stock registrado en el almacen " + almacen.getNombre()
                    + " para los materiales seleccionados.");
        }

        InventarioFisico inventario = InventarioFisico.builder()
                .almacen(almacen)
                .responsable(usuarioActual())
                .estado(InventarioFisico.EstadoInventario.ABIERTO)
                .fechaInicio(Instant.now())
                .build();

        for (StockUbicacion s : stocks) {
            inventario.getDetalles().add(DetalleInventarioFisico.builder()
                    .inventarioFisico(inventario)
                    .material(s.getMaterial())
                    .ubicacion(s.getUbicacion())
                    .stockSistema(s.getCantidad())
                    .build());
        }
        return InventarioFisicoResponse.fromEntity(inventarioRepository.save(inventario));
    }

    @Transactional
    public InventarioFisicoResponse.Detalle registrarConteo(RegistrarConteoRequest req) {
        DetalleInventarioFisico detalle = detalleRepository.findById(req.getDetalleId())
                .orElseThrow(() -> ResourceNotFoundException.of("Detalle de inventario", req.getDetalleId()));

        if (detalle.getInventarioFisico().getEstado() == InventarioFisico.EstadoInventario.CERRADO) {
            throw new BusinessException("Este inventario ya esta cerrado y no se puede modificar.");
        }

        detalle.setStockFisico(req.getStockFisico());
        detalle.setDiferencia(req.getStockFisico().subtract(detalle.getStockSistema()));
        detalle.setObservaciones(req.getObservaciones());

        return InventarioFisicoResponse.Detalle.fromEntity(detalleRepository.save(detalle));
    }

    /** Cierra el inventario y AJUSTA el stock real segun lo contado (RF-12). */
    @Transactional
    public InventarioFisicoResponse cerrar(Long inventarioId) {
        InventarioFisico inventario = buscar(inventarioId);

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
        return InventarioFisicoResponse.fromEntity(inventarioRepository.save(inventario));
    }

    private InventarioFisico buscar(Long id) {
        return inventarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Inventario fisico", id));
    }

    private Usuario usuarioActual() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
    }
}
