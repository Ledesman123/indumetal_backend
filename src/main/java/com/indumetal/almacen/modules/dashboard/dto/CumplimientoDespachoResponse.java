package com.indumetal.almacen.modules.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * RF-14: nivel de cumplimiento de despacho.
 *
 * NOTA METODOLOGICA (documentada tambien en el README): el sistema no registra
 * "ordenes de despacho" solicitadas por produccion como una entidad separada de
 * los movimientos ya ejecutados (eso esta fuera del alcance definido en el
 * Product Backlog). Por eso este indicador se calcula como una metrica proxy:
 * el porcentaje de materiales ACTIVOS cuyo stock total actual se encuentra
 * en o por encima de su stock minimo configurado, es decir, materiales que el
 * almacen esta en condiciones de despachar sin quiebre en este momento.
 * Si en el futuro se agrega un modulo de "solicitudes de despacho", este
 * indicador deberia recalcularse como (solicitudes atendidas completas / total
 * de solicitudes) en el periodo.
 */
@Getter
@Builder
public class CumplimientoDespachoResponse {
    private long materialesActivos;
    private long materialesEnCondicionDeDespacho; // stock actual >= stock minimo
    private double porcentajeCumplimiento; // 0-100
}
