package com.indumetal.almacen.modules.movimiento;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.modules.movimiento.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/** RF-06, RF-08, RF-17, RF-18: registro de movimientos de almacen. */
@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ALMACENERO','SUPERVISOR_ALMACEN','ADMINISTRADOR')")
public class MovimientoController {

    private final MovimientoService movimientoService;
    private final MovimientoAlmacenRepository movimientoRepository;

    // GET /api/movimientos?limite=200 -> historial reciente (mas nuevo primero).
    // Lectura tambien disponible para el Jefe de Produccion (lo usa en Reportes).
    @GetMapping
    @PreAuthorize("hasAnyRole('ALMACENERO','SUPERVISOR_ALMACEN','JEFE_PRODUCCION','ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public ApiResponse<List<MovimientoResponse>> listar(@RequestParam(defaultValue = "200") int limite) {
        var movimientos = movimientoRepository
                .findAllByOrderByCreadoEnDesc(PageRequest.of(0, Math.min(Math.max(limite, 1), 1000)))
                .stream().map(MovimientoResponse::fromEntity).toList();
        return ApiResponse.ok(movimientos);
    }

    @PostMapping("/ingresos")
    public ApiResponse<MovimientoResponse> registrarIngreso(@Valid @RequestBody IngresoRequest request) {
        return ApiResponse.ok("Ingreso registrado correctamente",
                MovimientoResponse.fromEntity(movimientoService.registrarIngreso(request)));
    }

    @PostMapping("/salidas")
    public ApiResponse<MovimientoResponse> registrarSalida(@Valid @RequestBody SalidaRequest request) {
        return ApiResponse.ok("Salida registrada correctamente",
                MovimientoResponse.fromEntity(movimientoService.registrarSalida(request)));
    }

    @PostMapping("/devoluciones")
    public ApiResponse<MovimientoResponse> registrarDevolucion(@Valid @RequestBody DevolucionRequest request) {
        return ApiResponse.ok("Devolucion registrada correctamente",
                MovimientoResponse.fromEntity(movimientoService.registrarDevolucion(request)));
    }

    @PostMapping("/transferencias")
    public ApiResponse<List<MovimientoResponse>> registrarTransferencia(@Valid @RequestBody TransferenciaRequest request) {
        var movimientos = Arrays.stream(movimientoService.registrarTransferencia(request))
                .map(MovimientoResponse::fromEntity).toList();
        return ApiResponse.ok("Transferencia registrada correctamente", movimientos);
    }
}
