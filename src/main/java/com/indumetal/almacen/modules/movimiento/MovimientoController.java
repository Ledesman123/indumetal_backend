package com.indumetal.almacen.modules.movimiento;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.modules.movimiento.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** RF-06, RF-08, RF-17, RF-18: registro de movimientos de almacen. */
@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ALMACENERO','SUPERVISOR_ALMACEN','ADMINISTRADOR')")
public class MovimientoController {

    private final MovimientoService movimientoService;

    @PostMapping("/ingresos")
    public ApiResponse<MovimientoAlmacen> registrarIngreso(@Valid @RequestBody IngresoRequest request) {
        return ApiResponse.ok("Ingreso registrado correctamente", movimientoService.registrarIngreso(request));
    }

    @PostMapping("/salidas")
    public ApiResponse<MovimientoAlmacen> registrarSalida(@Valid @RequestBody SalidaRequest request) {
        return ApiResponse.ok("Salida registrada correctamente", movimientoService.registrarSalida(request));
    }

    @PostMapping("/devoluciones")
    public ApiResponse<MovimientoAlmacen> registrarDevolucion(@Valid @RequestBody DevolucionRequest request) {
        return ApiResponse.ok("Devolucion registrada correctamente", movimientoService.registrarDevolucion(request));
    }

    @PostMapping("/transferencias")
    public ApiResponse<MovimientoAlmacen[]> registrarTransferencia(@Valid @RequestBody TransferenciaRequest request) {
        return ApiResponse.ok("Transferencia registrada correctamente", movimientoService.registrarTransferencia(request));
    }
}
