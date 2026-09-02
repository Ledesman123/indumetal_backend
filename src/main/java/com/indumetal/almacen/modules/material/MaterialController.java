package com.indumetal.almacen.modules.material;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.common.dto.PageResponse;
import com.indumetal.almacen.modules.material.dto.MaterialRequest;
import com.indumetal.almacen.modules.material.dto.MaterialResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** RF-04, RF-16, RF-19. Consulta abierta a todos los roles autenticados; escritura solo Almacenero/Supervisor/Admin. */
@RestController
@RequestMapping("/api/materiales")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ALMACENERO','SUPERVISOR_ALMACEN','ADMINISTRADOR')")
    public ApiResponse<MaterialResponse> crear(@Valid @RequestBody MaterialRequest request) {
        return ApiResponse.ok("Material creado correctamente", materialService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ALMACENERO','SUPERVISOR_ALMACEN','ADMINISTRADOR')")
    public ApiResponse<MaterialResponse> actualizar(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return ApiResponse.ok("Material actualizado", materialService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR_ALMACEN','ADMINISTRADOR')")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        materialService.desactivar(id);
        return ApiResponse.ok("Material desactivado", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<MaterialResponse> obtener(@PathVariable Long id) {
        return ApiResponse.ok(materialService.obtenerPorId(id));
    }

    // RF-16: GET /api/materiales?texto=acero&categoria=Materia Prima&page=0&size=20
    @GetMapping
    public ApiResponse<PageResponse<MaterialResponse>> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String categoria,
            Pageable pageable) {
        return ApiResponse.ok(materialService.buscar(texto, categoria, pageable));
    }
}
