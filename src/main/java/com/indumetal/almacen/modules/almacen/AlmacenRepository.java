package com.indumetal.almacen.modules.almacen;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
}
