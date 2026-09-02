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
        return ApiResponse.ok(almacenRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Almacen", id)));
    }
}
