package com.indumetal.almacen.modules.material;

import com.indumetal.almacen.common.dto.PageResponse;
import com.indumetal.almacen.modules.material.dto.MaterialRequest;
import com.indumetal.almacen.modules.material.dto.MaterialResponse;
import org.springframework.data.domain.Pageable;

public interface MaterialService {
    MaterialResponse crear(MaterialRequest request);
    MaterialResponse actualizar(Long id, MaterialRequest request);
    void desactivar(Long id);
    MaterialResponse obtenerPorId(Long id);
    PageResponse<MaterialResponse> buscar(String texto, String categoria, Pageable pageable);
}
