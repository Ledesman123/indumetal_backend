package com.indumetal.almacen.modules.stock;

import com.indumetal.almacen.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** RF-10 / RF-11: stock disponible en tiempo real de todos los materiales (consulta para todos los roles). */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockUbicacionRepository stockRepository;

    // GET /api/stock -> stock actual (> 0) por material y ubicacion
    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<StockResponse>> listar() {
        var filas = stockRepository.findByCantidadGreaterThan(BigDecimal.ZERO).stream()
                .map(StockResponse::fromEntity)
                .sorted(Comparator.comparing(StockResponse::getMaterialSku)
                        .thenComparing(StockResponse::getUbicacionCodigo))
                .toList();
        return ApiResponse.ok(filas);
    }
}
