package com.indumetal.almacen.modules.ubicacion;

import com.indumetal.almacen.common.dto.ApiResponse;
import com.indumetal.almacen.common.exception.ResourceNotFoundException;
import com.indumetal.almacen.modules.almacen.Almacen;
import com.indumetal.almacen.modules.almacen.AlmacenRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-05, RF-07: administracion de ubicaciones fisicas dentro de cada almacen. */
@RestController
@RequestMapping("/api/ubicaciones")
@RequiredArgsConstructor
public class UbicacionController {

    private final UbicacionRepository ubicacionRepository;
    private final AlmacenRepository almacenRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR_ALMACEN','ADMINISTRADOR')")
    @Transactional
    public ApiResponse<Ubicacion> crear(@Valid @RequestBody UbicacionRequest req) {
        Almacen almacen = almacenRepository.findById(req.getAlmacenId())
                .orElseThrow(() -> ResourceNotFoundException.of("Almacen", req.getAlmacenId()));

        Ubicacion ubicacion = Ubicacion.builder()
                .almacen(almacen)
                .codigo(req.getCodigo())
                .zona(req.getZona())
                .pasillo(req.getPasillo())
                .estante(req.getEstante())
                .nivel(req.getNivel())
                .capacidadMaxima(req.getCapacidadMaxima())
                .activo(true)
                .build();
        return ApiResponse.ok("Ubicacion creada", ubicacionRepository.save(ubicacion));
    }

    @GetMapping("/por-almacen/{almacenId}")
    public ApiResponse<List<Ubicacion>> listarPorAlmacen(@PathVariable Long almacenId) {
        return ApiResponse.ok(ubicacionRepository.findByAlmacenIdAndActivoTrue(almacenId));
    }

    @Getter
    @Setter
    public static class UbicacionRequest {
        @NotNull
        private Long almacenId;
        @NotBlank
        private String codigo;
        private String zona;
        private String pasillo;
        private String estante;
        private String nivel;
        private Integer capacidadMaxima;
    }
}
