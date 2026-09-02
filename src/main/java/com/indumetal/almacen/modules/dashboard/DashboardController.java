package com.indumetal.almacen.modules.dashboard;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.modules.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * RF-14: panel de indicadores (dashboard) para la jefatura de almacen.
 * Solo lectura; disponible para Supervisor, Jefe de Produccion y Administrador
 * (el Almacenero opera el dia a dia y no necesita estos indicadores gerenciales).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERVISOR_ALMACEN','JEFE_PRODUCCION','ADMINISTRADOR')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumen")
    public ApiResponse<ResumenDashboardResponse> resumen() {
        return ApiResponse.ok(dashboardService.resumen());
    }

    // GET /api/dashboard/materiales-mas-movimiento?desde=2026-09-01&hasta=2026-09-30&limite=10
    @GetMapping("/materiales-mas-movimiento")
    public ApiResponse<List<MaterialMovimientoResponse>> materialesMasMovimiento(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return ApiResponse.ok(dashboardService.materialesConMayorMovimiento(
                inicioDelDia(desde), finDelDia(hasta), limite));
    }

    // GET /api/dashboard/rotacion-inventario?desde=2026-09-01&hasta=2026-09-30
    @GetMapping("/rotacion-inventario")
    public ApiResponse<List<RotacionInventarioResponse>> rotacionInventario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ApiResponse.ok(dashboardService.rotacionInventario(inicioDelDia(desde), finDelDia(hasta)));
    }

    @GetMapping("/valorizacion")
    public ApiResponse<ValorizacionResponse> valorizacion() {
        return ApiResponse.ok(dashboardService.valorizacion());
    }

    @GetMapping("/cumplimiento-despacho")
    public ApiResponse<CumplimientoDespachoResponse> cumplimientoDespacho() {
        return ApiResponse.ok(dashboardService.cumplimientoDespacho());
    }

    private Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant finDelDia(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
    }
}
