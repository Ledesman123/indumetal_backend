package com.indumetal.almacen.modules.kardex;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.common.dto.PageResponse;
import com.indumetal.almacen.modules.movimiento.MovimientoAlmacenRepository;
import com.indumetal.almacen.modules.stock.StockUbicacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** RF-09: kardex por material. RF-10: stock disponible en tiempo real. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KardexController {

    private final MovimientoAlmacenRepository movimientoRepository;
    private final StockUbicacionRepository stockRepository;

    // RF-09: GET /api/materiales/5/kardex?page=0&size=20
    @GetMapping("/materiales/{materialId}/kardex")
    public ApiResponse<PageResponse<KardexItem>> kardex(@PathVariable Long materialId, Pageable pageable) {
        var page = movimientoRepository
                .findByMaterialIdOrderByCreadoEnAsc(materialId, pageable)
                .map(KardexItem::fromMovimiento);
        return ApiResponse.ok(PageResponse.from(page));
    }

    // RF-10: GET /api/materiales/5/stock  -> stock total en todas las ubicaciones
    @GetMapping("/materiales/{materialId}/stock")
    public ApiResponse<BigDecimal> stockTotal(@PathVariable Long materialId) {
        return ApiResponse.ok(stockRepository.sumarStockPorMaterial(materialId));
    }

    // RF-10: GET /api/materiales/5/stock-por-ubicacion -> detalle por ubicacion
    @GetMapping("/materiales/{materialId}/stock-por-ubicacion")
    public ApiResponse<?> stockPorUbicacion(@PathVariable Long materialId) {
        return ApiResponse.ok(stockRepository.findByMaterialId(materialId));
    }
}
