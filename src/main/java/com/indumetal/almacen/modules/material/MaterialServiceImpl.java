package com.indumetal.almacen.modules.material;

import com.indumetal.almacen.common.dto.PageResponse;
import com.indumetal.almacen.common.exception.BusinessException;
import com.indumetal.almacen.common.exception.ResourceNotFoundException;
import com.indumetal.almacen.modules.auditoria.AuditoriaService;
import com.indumetal.almacen.modules.material.dto.MaterialRequest;
import com.indumetal.almacen.modules.material.dto.MaterialResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional
    public MaterialResponse crear(MaterialRequest request) {
        if (materialRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new BusinessException("Ya existe un material registrado con el SKU " + request.getSku());
        }
        validarStock(request);

        Material material = Material.builder()
                .sku(request.getSku().toUpperCase())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .unidadMedida(request.getUnidadMedida())
                .categoria(request.getCategoria())
                .stockMinimo(request.getStockMinimo())
                .stockMaximo(request.getStockMaximo())
                .costoUnitario(request.getCostoUnitario() != null ? request.getCostoUnitario() : java.math.BigDecimal.ZERO)
                .requiereLote(Boolean.TRUE.equals(request.getRequiereLote()))
                .imagenUrl(request.getImagenUrl())
                .activo(true)
                .build();

        Material guardado = materialRepository.save(material);
        auditoriaService.registrar("Material", guardado.getId(), "CREAR", "SKU: " + guardado.getSku());
        return MaterialResponse.fromEntity(guardado);
    }

    @Override
    @Transactional
    public MaterialResponse actualizar(Long id, MaterialRequest request) {
        Material material = buscar(id);
        validarStock(request);

        material.setNombre(request.getNombre());
        material.setDescripcion(request.getDescripcion());
        material.setUnidadMedida(request.getUnidadMedida());
        material.setCategoria(request.getCategoria());
        material.setStockMinimo(request.getStockMinimo());
        material.setStockMaximo(request.getStockMaximo());
        if (request.getCostoUnitario() != null) {
            material.setCostoUnitario(request.getCostoUnitario());
        }
        material.setRequiereLote(Boolean.TRUE.equals(request.getRequiereLote()));
        material.setImagenUrl(request.getImagenUrl());

        return MaterialResponse.fromEntity(materialRepository.save(material));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Material material = buscar(id);
        material.setActivo(false);
        materialRepository.save(material);
    }

    @Override
    public MaterialResponse obtenerPorId(Long id) {
        return MaterialResponse.fromEntity(buscar(id));
    }

    @Override
    public PageResponse<MaterialResponse> buscar(String texto, String categoria, Pageable pageable) {
        var page = materialRepository
                .findAll(MaterialSpecification.conFiltros(texto, categoria), pageable)
                .map(MaterialResponse::fromEntity);
        return PageResponse.from(page);
    }

    private void validarStock(MaterialRequest request) {
        if (request.getStockMinimo().compareTo(request.getStockMaximo()) > 0) {
            throw new BusinessException("El stock minimo no puede ser mayor al stock maximo");
        }
    }

    private Material buscar(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Material", id));
    }
}
