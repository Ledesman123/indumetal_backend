package com.indumetal.almacen.modules.almacen;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.common.exception.BusinessException;
import com.indumetal.almacen.common.exception.ResourceNotFoundException;
import com.indumetal.almacen.modules.almacen.dto.AlmacenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RF-05: gestion de almacenes y ubicaciones.
 * Servicio simple embebido en el controller dado su bajo nivel de complejidad;
 * para modulos mas grandes se separa en Service/ServiceImpl (ver modulo Material).
 */
@RestController
@RequestMapping("/api/almacenes")
@RequiredArgsConstructor
public class AlmacenController {

    private final AlmacenRepository almacenRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR_ALMACEN','ADMINISTRADOR')")
    @Transactional
    public ApiResponse<Almacen> crear(@Valid @RequestBody AlmacenRequest request) {
        if (almacenRepository.existsByCodigoIgnoreCase(request.getCodigo())) {
            throw new BusinessException("Ya existe un almacen con el codigo " + request.getCodigo());
        }
        Almacen almacen = Almacen.builder()
                .codigo(request.getCodigo().toUpperCase())
                .nombre(request.getNombre())
                .direccion(request.getDireccion())
                .activo(true)
                .build();
        return ApiResponse.ok("Almacen creado", almacenRepository.save(almacen));
    }

    @GetMapping
    public ApiResponse<List<Almacen>> listar() {
        return ApiResponse.ok(almacenRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Almacen> obtener(@PathVariable Long id) {
        return ApiResponse.ok(buscar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR_ALMACEN','ADMINISTRADOR')")
    @Transactional
    public ApiResponse<Almacen> actualizar(@PathVariable Long id, @Valid @RequestBody AlmacenRequest request) {
        Almacen almacen = buscar(id);
        if (!almacen.getCodigo().equalsIgnoreCase(request.getCodigo())
                && almacenRepository.existsByCodigoIgnoreCase(request.getCodigo())) {
            throw new BusinessException("Ya existe un almacen con el codigo " + request.getCodigo());
        }
        almacen.setCodigo(request.getCodigo().toUpperCase());
        almacen.setNombre(request.getNombre());
        almacen.setDireccion(request.getDireccion());
        return ApiResponse.ok("Almacen actualizado", almacenRepository.save(almacen));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR_ALMACEN','ADMINISTRADOR')")
    @Transactional
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        Almacen almacen = buscar(id);
        almacen.setActivo(false);
        almacenRepository.save(almacen);
        return ApiResponse.ok("Almacen desactivado", null);
    }

    private Almacen buscar(Long id) {
        return almacenRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Almacen", id));
    }
}
