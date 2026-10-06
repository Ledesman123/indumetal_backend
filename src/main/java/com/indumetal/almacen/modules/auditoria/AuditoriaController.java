package com.indumetal.almacen.modules.auditoria;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RF-20: consulta de la bitacora de auditoria (solo Administrador). */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    @GetMapping
    public ApiResponse<PageResponse<Auditoria>> listar(
            @RequestParam(required = false) String entidad, Pageable pageable) {

        // Ignoramos el sort que envía el cliente (Swagger envía "[]" y rompe Hibernate).
        // El repositorio ya ordena por fecha DESC en el nombre del método.
        Pageable pageableLimpio = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        var page = (entidad == null || entidad.isBlank())
                ? auditoriaRepository.findAllByOrderByFechaDesc(pageableLimpio)
                : auditoriaRepository.findByEntidadOrderByFechaDesc(entidad, pageableLimpio);
        return ApiResponse.ok(PageResponse.from(page));
    }
}