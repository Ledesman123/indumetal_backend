package com.indumetal.almacen.modules.ubicacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {
    List<Ubicacion> findByAlmacenIdAndActivoTrue(Long almacenId);

    // RF-07: fallback para asignacion automatica cuando no existe una ubicacion preferente.
    Optional<Ubicacion> findFirstByActivoTrueOrderByIdAsc();
}
