package com.indumetal.almacen.modules.inventariofisico;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.modules.inventariofisico.dto.IniciarInventarioRequest;
import com.indumetal.almacen.modules.inventariofisico.dto.RegistrarConteoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** RF-12: inventario fisico / ciclico. */
@RestController
@RequestMapping("/api/inventarios-fisicos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ALMACENERO','SUPERVISOR_ALMACEN','ADMINISTRADOR')")
public class InventarioFisicoController {

    private final InventarioFisicoService inventarioFisicoService;

    @PostMapping
    public ApiResponse<InventarioFisico> iniciar(@Valid @RequestBody IniciarInventarioRequest request) {
        return ApiResponse.ok("Inventario fisico iniciado", inventarioFisicoService.iniciar(request));
    }

    @PostMapping("/conteos")
    public ApiResponse<DetalleInventarioFisico> registrarConteo(@Valid @RequestBody RegistrarConteoRequest request) {
        return ApiResponse.ok(inventarioFisicoService.registrarConteo(request));
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('SUPERVISOR_ALMACEN','ADMINISTRADOR')")
    public ApiResponse<InventarioFisico> cerrar(@PathVariable Long id) {
        return ApiResponse.ok("Inventario cerrado y stock ajustado", inventarioFisicoService.cerrar(id));
    }
}
