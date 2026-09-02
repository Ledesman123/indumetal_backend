package com.indumetal.almacen.modules.movimiento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface MovimientoAlmacenRepository extends JpaRepository<MovimientoAlmacen, Long> {

    // RF-09: kardex de un material, ordenado cronologicamente
    Page<MovimientoAlmacen> findByMaterialIdOrderByCreadoEnAsc(Long materialId, Pageable pageable);

    // RF-15: reportes por rango de fechas
    Page<MovimientoAlmacen> findByCreadoEnBetween(Instant desde, Instant hasta, Pageable pageable);

    Page<MovimientoAlmacen> findByMaterialIdAndCreadoEnBetween(Long materialId, Instant desde, Instant hasta, Pageable pageable);

    // RF-15: version sin paginar, usada por el generador de reportes PDF/Excel
    List<MovimientoAlmacen> findByCreadoEnBetweenOrderByCreadoEnAsc(Instant desde, Instant hasta);

    List<MovimientoAlmacen> findByMaterialIdAndCreadoEnBetweenOrderByCreadoEnAsc(Long materialId, Instant desde, Instant hasta);

    List<MovimientoAlmacen> findByMaterialIdOrderByCreadoEnAsc(Long materialId);

    // RF-14: materiales con mayor cantidad de movimientos en un rango de fechas.
    // Devuelve: [materialId, sku, nombre, cantidadMovimientos, sumaCantidad]
    @Query("""
        SELECT m.material.id, m.material.sku, m.material.nombre, COUNT(m), COALESCE(SUM(m.cantidad), 0)
        FROM MovimientoAlmacen m
        WHERE m.creadoEn BETWEEN :desde AND :hasta
        GROUP BY m.material.id, m.material.sku, m.material.nombre
        ORDER BY COUNT(m) DESC
        """)
    List<Object[]> materialesConMayorMovimiento(Instant desde, Instant hasta, Pageable pageable);

    // RF-14: total de unidades despachadas (SALIDA) por material en un rango, insumo para la rotacion de inventario.
    @Query("""
        SELECT m.material.id, COALESCE(SUM(m.cantidad), 0)
        FROM MovimientoAlmacen m
        WHERE m.tipo = com.indumetal.almacen.modules.movimiento.TipoMovimiento.SALIDA
          AND m.creadoEn BETWEEN :desde AND :hasta
        GROUP BY m.material.id
        """)
    List<Object[]> totalSalidasPorMaterial(Instant desde, Instant hasta);
}
