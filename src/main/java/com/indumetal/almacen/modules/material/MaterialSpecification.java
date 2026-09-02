package com.indumetal.almacen.modules.material;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** RF-16: buscador y filtros de materiales por codigo, descripcion y categoria. */
public class MaterialSpecification {

    public static Specification<Material> conFiltros(String texto, String categoria) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            predicate = cb.and(predicate, cb.isTrue(root.get("activo")));

            if (StringUtils.hasText(texto)) {
                String like = "%" + texto.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(root.get("nombre")), like),
                        cb.like(cb.lower(root.get("descripcion")), like)
                ));
            }
            if (StringUtils.hasText(categoria)) {
                predicate = cb.and(predicate, cb.equal(root.get("categoria"), categoria));
            }
            return predicate;
        };
    }
}
