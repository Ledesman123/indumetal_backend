package com.indumetal.almacen.modules.reporte;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * RF-15: reportes exportables en PDF y Excel de movimientos, stock y kardex,
 * filtrables por rango de fechas. Disponible para todos los roles operativos;
 * la escritura de datos sigue protegida en sus respectivos modulos.
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ALMACENERO','SUPERVISOR_ALMACEN','JEFE_PRODUCCION','ADMINISTRADOR')")
public class ReporteController {

    private static final DateTimeFormatter NOMBRE_ARCHIVO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService reporteService;

    // GET /api/reportes/movimientos/pdf?desde=2026-09-01&hasta=2026-09-30[&materialId=5]
    @GetMapping("/movimientos/pdf")
    public ResponseEntity<byte[]> movimientosPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long materialId) {
        byte[] pdf = reporteService.movimientosPdf(inicioDelDia(desde), finDelDia(hasta), materialId);
        return archivo(pdf, MediaType.APPLICATION_PDF, "movimientos_" + rango(desde, hasta) + ".pdf");
    }

    @GetMapping("/movimientos/excel")
    public ResponseEntity<byte[]> movimientosExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long materialId) {
        byte[] excel = reporteService.movimientosExcel(inicioDelDia(desde), finDelDia(hasta), materialId);
        return archivo(excel, XLSX, "movimientos_" + rango(desde, hasta) + ".xlsx");
    }

    // GET /api/reportes/kardex/5/pdf
    @GetMapping("/kardex/{materialId}/pdf")
    public ResponseEntity<byte[]> kardexPdf(@PathVariable Long materialId) {
        byte[] pdf = reporteService.kardexPdf(materialId);
        return archivo(pdf, MediaType.APPLICATION_PDF, "kardex_" + materialId + ".pdf");
    }

    @GetMapping("/kardex/{materialId}/excel")
    public ResponseEntity<byte[]> kardexExcel(@PathVariable Long materialId) {
        byte[] excel = reporteService.kardexExcel(materialId);
        return archivo(excel, XLSX, "kardex_" + materialId + ".xlsx");
    }

    // GET /api/reportes/stock/excel -> foto del stock actual de todos los materiales/ubicaciones
    @GetMapping("/stock/excel")
    public ResponseEntity<byte[]> stockExcel() {
        byte[] excel = reporteService.stockExcel();
        return archivo(excel, XLSX, "stock_actual_" + NOMBRE_ARCHIVO_FECHA.format(Instant.now().atZone(ZoneOffset.UTC)) + ".xlsx");
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, MediaType tipo, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .body(contenido);
    }

    private Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant finDelDia(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
    }

    private String rango(LocalDate desde, LocalDate hasta) {
        return NOMBRE_ARCHIVO_FECHA.format(desde) + "_" + NOMBRE_ARCHIVO_FECHA.format(hasta);
    }
}
