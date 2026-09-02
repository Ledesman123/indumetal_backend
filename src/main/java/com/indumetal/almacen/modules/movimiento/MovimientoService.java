package com.indumetal.almacen.modules.movimiento;

import com.indumetal.almacen.common.exception.BusinessException;
import com.indumetal.almacen.common.exception.ResourceNotFoundException;
import com.indumetal.almacen.modules.auditoria.AuditoriaService;
import com.indumetal.almacen.modules.material.Material;
import com.indumetal.almacen.modules.material.MaterialRepository;
import com.indumetal.almacen.modules.movimiento.dto.*;
import com.indumetal.almacen.modules.stock.StockUbicacion;
import com.indumetal.almacen.modules.stock.StockUbicacionRepository;
import com.indumetal.almacen.modules.ubicacion.Ubicacion;
import com.indumetal.almacen.modules.ubicacion.UbicacionRepository;
import com.indumetal.almacen.modules.usuario.Usuario;
import com.indumetal.almacen.modules.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Orquesta el registro de movimientos y la actualizacion atomica del stock
 * (tabla stock_ubicacion). Cada metodo publico es una transaccion: si algo
 * falla (ej. stock insuficiente), no se persiste ni el movimiento ni el stock.
 */
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoAlmacenRepository movimientoRepository;
    private final MaterialRepository materialRepository;
    private final UbicacionRepository ubicacionRepository;
    private final StockUbicacionRepository stockRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    // ---------------------------------------------------------------
    // RF-06 + RF-07: Ingreso de materiales (con asignacion de ubicacion)
    // ---------------------------------------------------------------
    @Transactional
    public MovimientoAlmacen registrarIngreso(IngresoRequest req) {
        Material material = buscarMaterial(req.getMaterialId());
        Ubicacion ubicacion = req.getUbicacionId() != null
                ? buscarUbicacion(req.getUbicacionId())
                : asignarUbicacionAutomatica(material); // RF-07

        BigDecimal nuevoSaldo = sumarStock(material.getId(), ubicacion.getId(), req.getCantidad());

        MovimientoAlmacen mov = MovimientoAlmacen.builder()
                .tipo(TipoMovimiento.INGRESO)
                .material(material)
                .ubicacion(ubicacion)
                .cantidad(req.getCantidad())
                .saldoResultante(nuevoSaldo)
                .lote(req.getLote())
                .fechaVencimiento(req.getFechaVencimiento())
                .ordenCompra(req.getOrdenCompra())
                .proveedor(req.getProveedor())
                .observaciones(req.getObservaciones())
                .usuario(usuarioActual())
                .build();

        MovimientoAlmacen guardado = movimientoRepository.save(mov);
        auditoriaService.registrar("MovimientoAlmacen", guardado.getId(), "CREAR",
                "INGRESO " + req.getCantidad() + " de " + material.getSku());
        return guardado;
    }

    // ---------------------------------------------------------------
    // RF-08: Salida hacia produccion
    // ---------------------------------------------------------------
    @Transactional
    public MovimientoAlmacen registrarSalida(SalidaRequest req) {
        Material material = buscarMaterial(req.getMaterialId());
        Ubicacion ubicacion = buscarUbicacion(req.getUbicacionId());

        BigDecimal nuevoSaldo = restarStock(material.getId(), ubicacion.getId(), req.getCantidad());

        MovimientoAlmacen mov = MovimientoAlmacen.builder()
                .tipo(TipoMovimiento.SALIDA)
                .material(material)
                .ubicacion(ubicacion)
                .cantidad(req.getCantidad())
                .saldoResultante(nuevoSaldo)
                .ordenProduccion(req.getOrdenProduccion())
                .areaSolicitante(req.getAreaSolicitante())
                .observaciones(req.getObservaciones())
                .usuario(usuarioActual())
                .build();

        MovimientoAlmacen guardado = movimientoRepository.save(mov);
        auditoriaService.registrar("MovimientoAlmacen", guardado.getId(), "CREAR",
                "SALIDA " + req.getCantidad() + " de " + material.getSku() + " -> OP " + req.getOrdenProduccion());
        return guardado;
    }

    // ---------------------------------------------------------------
    // RF-17: Devolucion de produccion hacia almacen
    // ---------------------------------------------------------------
    @Transactional
    public MovimientoAlmacen registrarDevolucion(DevolucionRequest req) {
        Material material = buscarMaterial(req.getMaterialId());
        Ubicacion ubicacion = buscarUbicacion(req.getUbicacionId());

        BigDecimal nuevoSaldo = sumarStock(material.getId(), ubicacion.getId(), req.getCantidad());

        MovimientoAlmacen mov = MovimientoAlmacen.builder()
                .tipo(TipoMovimiento.DEVOLUCION)
                .material(material)
                .ubicacion(ubicacion)
                .cantidad(req.getCantidad())
                .saldoResultante(nuevoSaldo)
                .ordenProduccion(req.getOrdenProduccion())
                .observaciones(req.getObservaciones())
                .usuario(usuarioActual())
                .build();

        return movimientoRepository.save(mov);
    }

    // ---------------------------------------------------------------
    // RF-18: Transferencia entre ubicaciones/almacenes
    // Genera DOS movimientos enlazados: salida del origen + entrada al destino
    // ---------------------------------------------------------------
    @Transactional
    public MovimientoAlmacen[] registrarTransferencia(TransferenciaRequest req) {
        if (req.getUbicacionOrigenId().equals(req.getUbicacionDestinoId())) {
            throw new BusinessException("La ubicacion de origen y destino no pueden ser la misma");
        }
        Material material = buscarMaterial(req.getMaterialId());
        Ubicacion origen = buscarUbicacion(req.getUbicacionOrigenId());
        Ubicacion destino = buscarUbicacion(req.getUbicacionDestinoId());
        Usuario usuario = usuarioActual();

        BigDecimal saldoOrigen = restarStock(material.getId(), origen.getId(), req.getCantidad());
        BigDecimal saldoDestino = sumarStock(material.getId(), destino.getId(), req.getCantidad());

        MovimientoAlmacen salida = movimientoRepository.save(MovimientoAlmacen.builder()
                .tipo(TipoMovimiento.TRANSFERENCIA_SALIDA)
                .material(material).ubicacion(origen)
                .cantidad(req.getCantidad()).saldoResultante(saldoOrigen)
                .observaciones(req.getObservaciones()).usuario(usuario)
                .build());

        MovimientoAlmacen entrada = movimientoRepository.save(MovimientoAlmacen.builder()
                .tipo(TipoMovimiento.TRANSFERENCIA_ENTRADA)
                .material(material).ubicacion(destino)
                .cantidad(req.getCantidad()).saldoResultante(saldoDestino)
                .observaciones(req.getObservaciones()).usuario(usuario)
                .movimientoRelacionadoId(salida.getId())
                .build());

        salida.setMovimientoRelacionadoId(entrada.getId());
        movimientoRepository.save(salida);

        return new MovimientoAlmacen[]{salida, entrada};
    }

    // ---------------------------------------------------------------
    // Helpers de stock (con bloqueo optimista via @Version en StockUbicacion)
    // ---------------------------------------------------------------
    private BigDecimal sumarStock(Long materialId, Long ubicacionId, BigDecimal cantidad) {
        StockUbicacion stock = obtenerOCrearStock(materialId, ubicacionId);
        stock.setCantidad(stock.getCantidad().add(cantidad));
        stockRepository.save(stock);
        return stock.getCantidad();
    }

    private BigDecimal restarStock(Long materialId, Long ubicacionId, BigDecimal cantidad) {
        StockUbicacion stock = obtenerOCrearStock(materialId, ubicacionId);
        if (stock.getCantidad().compareTo(cantidad) < 0) {
            throw new BusinessException("Stock insuficiente en la ubicacion. Disponible: " + stock.getCantidad());
        }
        stock.setCantidad(stock.getCantidad().subtract(cantidad));
        stockRepository.save(stock);
        return stock.getCantidad();
    }

    private StockUbicacion obtenerOCrearStock(Long materialId, Long ubicacionId) {
        return stockRepository.findByMaterialIdAndUbicacionId(materialId, ubicacionId)
                .orElseGet(() -> StockUbicacion.builder()
                        .material(materialRepository.getReferenceById(materialId))
                        .ubicacion(ubicacionRepository.getReferenceById(ubicacionId))
                        .cantidad(BigDecimal.ZERO)
                        .build());
    }

    /**
     * RF-07: asignacion automatica de ubicacion cuando el ingreso no especifica una.
     * Estrategia (documentada en README, seccion "Reglas de negocio"):
     *  1) Si el material ya tiene stock en alguna ubicacion, se reutiliza esa misma
     *     ubicacion (evita fragmentar el mismo material en muchos sitios).
     *  2) Si es la primera vez que ingresa el material, se asigna la primera
     *     ubicacion activa registrada en el sistema (orden ascendente por id).
     */
    private Ubicacion asignarUbicacionAutomatica(Material material) {
        return stockRepository.findByMaterialId(material.getId()).stream()
                .findFirst()
                .map(StockUbicacion::getUbicacion)
                .orElseGet(() -> ubicacionRepository.findFirstByActivoTrueOrderByIdAsc()
                        .orElseThrow(() -> new BusinessException(
                                "No hay ubicaciones activas configuradas para la asignacion automatica. "
                                        + "Registre al menos una ubicacion o indique 'ubicacionId' manualmente.")));
    }

    private Material buscarMaterial(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Material", id));
    }

    private Ubicacion buscarUbicacion(Long id) {
        return ubicacionRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Ubicacion", id));
    }

    /** Obtiene el usuario autenticado a partir del contexto de seguridad (correo del JWT). */
    private Usuario usuarioActual() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no encontrado"));
    }
}
