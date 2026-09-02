package com.indumetal.almacen.modules.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockUbicacionRepository extends JpaRepository<StockUbicacion, Long> {

    Optional<StockUbicacion> findByMaterialIdAndUbicacionId(Long materialId, Long ubicacionId);

    List<StockUbicacion> findByMaterialId(Long materialId);

    // RF-10: stock total disponible de un material (suma en todas sus ubicaciones)
    @Query("SELECT COALESCE(SUM(s.cantidad), 0) FROM StockUbicacion s WHERE s.material.id = :materialId")
    BigDecimal sumarStockPorMaterial(Long materialId);

    // RF-11: materiales cuyo stock total cayo por debajo del stock minimo
    @Query("""
        SELECT s.material.id FROM StockUbicacion s
        GROUP BY s.material.id, s.material.stockMinimo
        HAVING COALESCE(SUM(s.cantidad), 0) < s.material.stockMinimo
        """)
    List<Long> buscarMaterialesEnStockMinimo();

    // RF-14: valorizacion total del inventario = suma(cantidad * costoUnitario) de todas las ubicaciones.
    @Query("SELECT COALESCE(SUM(s.cantidad * s.material.costoUnitario), 0) FROM StockUbicacion s")
    BigDecimal calcularValorizacionTotal();

    // RF-14: valorizacion agrupada por categoria de material. Devuelve [categoria, valorizacion].
    @Query("""
        SELECT s.material.categoria AS categoria, COALESCE(SUM(s.cantidad * s.material.costoUnitario), 0) AS valor
        FROM StockUbicacion s
        GROUP BY s.material.categoria
        ORDER BY valor DESC
        """)
    List<Object[]> calcularValorizacionPorCategoria();
}
