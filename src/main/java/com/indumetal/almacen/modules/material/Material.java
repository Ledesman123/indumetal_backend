package com.indumetal.almacen.modules.material;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // <--- NUEVO IMPORT
import com.indumetal.almacen.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** RF-04, RF-13, RF-19: material/insumo maestro del almacen. */
@Entity
@Table(name = "materiales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <--- NUEVA ANOTACIÓN
public class Material extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String sku;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida; // KG, UND, M, L, etc.

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(name = "stock_minimo", nullable = false)
    private BigDecimal stockMinimo;

    @Column(name = "stock_maximo", nullable = false)
    private BigDecimal stockMaximo;

    // RF-14: costo unitario usado para calcular la valorizacion del inventario en el dashboard.
    @Column(name = "costo_unitario", nullable = false)
    @Builder.Default
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(name = "requiere_lote", nullable = false)
    @Builder.Default
    private Boolean requiereLote = false; // RF-13: trazabilidad por lote/vencimiento

    @Column(name = "imagen_url")
    private String imagenUrl; // RF-19: imagen de referencia (Supabase Storage)

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}