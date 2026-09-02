package com.indumetal.almacen.modules.dashboard;

import com.indumetal.almacen.modules.almacen.AlmacenRepository;
import com.indumetal.almacen.modules.dashboard.dto.*;
import com.indumetal.almacen.modules.material.Material;
import com.indumetal.almacen.modules.material.MaterialRepository;
import com.indumetal.almacen.modules.movimiento.MovimientoAlmacenRepository;
import com.indumetal.almacen.modules.stock.StockUbicacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RF-14: panel de indicadores. Todas las consultas son de solo lectura y se
 * apoyan en las tablas ya existentes (materiales, stock_ubicacion,
 * movimientos_almacen); no se crean tablas nuevas para el dashboard.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MaterialRepository materialRepository;
    private final AlmacenRepository almacenRepository;
    private final StockUbicacionRepository stockRepository;
    private final MovimientoAlmacenRepository movimientoRepository;

    public ResumenDashboardResponse resumen() {
        return ResumenDashboardResponse.builder()
                .materialesActivos(materialRepository.countByActivoTrue())
                .almacenesRegistrados(almacenRepository.count())
                .materialesEnAlertaStockMinimo(stockRepository.buscarMaterialesEnStockMinimo().size())
                .valorizacionTotalInventario(stockRepository.calcularValorizacionTotal())
                .build();
    }

    public List<MaterialMovimientoResponse> materialesConMayorMovimiento(Instant desde, Instant hasta, int limite) {
        return movimientoRepository.materialesConMayorMovimiento(desde, hasta, PageRequest.of(0, limite)).stream()
                .map(fila -> MaterialMovimientoResponse.builder()
                        .materialId((Long) fila[0])
                        .sku((String) fila[1])
                        .nombre((String) fila[2])
                        .cantidadMovimientos((Long) fila[3])
                        .unidadesMovidas((BigDecimal) fila[4])
                        .build())
                .toList();
    }

    public List<RotacionInventarioResponse> rotacionInventario(Instant desde, Instant hasta) {
        Map<Long, BigDecimal> salidasPorMaterial = movimientoRepository.totalSalidasPorMaterial(desde, hasta).stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (BigDecimal) fila[1]));

        List<Material> materialesActivos = materialRepository.findByActivoTrue();

        return materialesActivos.stream()
                .map(material -> {
                    BigDecimal despachado = salidasPorMaterial.getOrDefault(material.getId(), BigDecimal.ZERO);
                    BigDecimal stockActual = stockRepository.sumarStockPorMaterial(material.getId());
                    BigDecimal indice = (stockActual != null && stockActual.compareTo(BigDecimal.ZERO) > 0)
                            ? despachado.divide(stockActual, new MathContext(4))
                            : null;
                    return RotacionInventarioResponse.builder()
                            .materialId(material.getId())
                            .sku(material.getSku())
                            .nombre(material.getNombre())
                            .unidadesDespachadas(despachado)
                            .stockActual(stockActual)
                            .indiceRotacion(indice)
                            .build();
                })
                // materiales con mayor rotacion primero; sin rotacion (null) al final
                .sorted((a, b) -> {
                    if (a.getIndiceRotacion() == null && b.getIndiceRotacion() == null) return 0;
                    if (a.getIndiceRotacion() == null) return 1;
                    if (b.getIndiceRotacion() == null) return -1;
                    return b.getIndiceRotacion().compareTo(a.getIndiceRotacion());
                })
                .toList();
    }

    public ValorizacionResponse valorizacion() {
        List<ValorizacionResponse.ValorizacionCategoria> porCategoria =
                stockRepository.calcularValorizacionPorCategoria().stream()
                        .map(fila -> ValorizacionResponse.ValorizacionCategoria.builder()
                                .categoria((String) fila[0])
                                .valorizacion((BigDecimal) fila[1])
                                .build())
                        .toList();

        return ValorizacionResponse.builder()
                .valorizacionTotal(stockRepository.calcularValorizacionTotal())
                .porCategoria(porCategoria)
                .build();
    }

    /** Ver la nota metodologica en {@link CumplimientoDespachoResponse}. */
    public CumplimientoDespachoResponse cumplimientoDespacho() {
        List<Material> activos = materialRepository.findByActivoTrue();
        long total = activos.size();

        long enCondicion = activos.stream()
                .filter(m -> {
                    BigDecimal stockActual = stockRepository.sumarStockPorMaterial(m.getId());
                    return stockActual != null && stockActual.compareTo(m.getStockMinimo()) >= 0;
                })
                .count();

        double porcentaje = total == 0 ? 100.0
                : BigDecimal.valueOf(enCondicion)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        return CumplimientoDespachoResponse.builder()
                .materialesActivos(total)
                .materialesEnCondicionDeDespacho(enCondicion)
                .porcentajeCumplimiento(porcentaje)
                .build();
    }
}
