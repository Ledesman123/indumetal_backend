package com.indumetal.almacen.modules.material;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {

    boolean existsBySkuIgnoreCase(String sku);

    // RF-16: usado junto a MaterialSpecification para busqueda por codigo/nombre/categoria/ubicacion
    Page<Material> findByActivoTrue(Pageable pageable);

    // RF-14: total de materiales activos, usado en el resumen del dashboard
    long countByActivoTrue();

    List<Material> findByActivoTrue();
}
