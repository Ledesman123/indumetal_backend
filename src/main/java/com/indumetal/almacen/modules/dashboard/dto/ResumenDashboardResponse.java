package com.indumetal.almacen.modules.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** RF-14: indicadores generales mostrados en la cabecera del dashboard. */
@Getter
@Builder
public class ResumenDashboardResponse {
    private long materialesActivos;
    private long almacenesRegistrados;
    private long materialesEnAlertaStockMinimo;
    private BigDecimal valorizacionTotalInventario;
}
