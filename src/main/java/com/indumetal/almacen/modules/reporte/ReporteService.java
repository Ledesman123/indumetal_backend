package com.indumetal.almacen.modules.reporte;

import com.indumetal.almacen.common.exception.ResourceNotFoundException;
import com.indumetal.almacen.modules.kardex.KardexItem;
import com.indumetal.almacen.modules.material.Material;
import com.indumetal.almacen.modules.material.MaterialRepository;
import com.indumetal.almacen.modules.movimiento.MovimientoAlmacen;
import com.indumetal.almacen.modules.movimiento.MovimientoAlmacenRepository;
import com.indumetal.almacen.modules.stock.StockUbicacion;
import com.indumetal.almacen.modules.stock.StockUbicacionRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RF-15: generacion de reportes exportables en PDF y Excel de movimientos,
 * stock y kardex. No persiste nada: arma el documento en memoria (byte[])
 * a partir de los repositorios ya existentes y lo devuelve al controller,
 * que lo entrega como descarga (Content-Disposition: attachment).
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final MovimientoAlmacenRepository movimientoRepository;
    private final MaterialRepository materialRepository;
    private final StockUbicacionRepository stockRepository;

    // =========================================================================
    // MOVIMIENTOS (ingresos, salidas, devoluciones, transferencias) por rango
    // =========================================================================

    public byte[] movimientosPdf(Instant desde, Instant hasta, Long materialId) {
        List<MovimientoAlmacen> movimientos = obtenerMovimientos(desde, hasta, materialId);
        String[] encabezados = {"Fecha", "Tipo", "Material", "Ubicacion", "Cantidad", "Saldo", "Usuario"};

        return generarPdf("Reporte de Movimientos de Almacen", rangoTexto(desde, hasta), encabezados,
                movimientos.stream().map(m -> new String[]{
                        FECHA_HORA.format(m.getCreadoEn()),
                        m.getTipo().name(),
                        m.getMaterial().getSku() + " - " + m.getMaterial().getNombre(),
                        m.getUbicacion().getCodigo(),
                        m.getCantidad().toPlainString(),
                        m.getSaldoResultante() != null ? m.getSaldoResultante().toPlainString() : "-",
                        m.getUsuario().getNombres() + " " + m.getUsuario().getApellidos()
                }).toList());
    }

    public byte[] movimientosExcel(Instant desde, Instant hasta, Long materialId) {
        List<MovimientoAlmacen> movimientos = obtenerMovimientos(desde, hasta, materialId);
        String[] encabezados = {"Fecha", "Tipo", "SKU", "Material", "Ubicacion", "Cantidad", "Saldo resultante",
                "Orden compra", "Proveedor", "Orden produccion", "Area solicitante", "Usuario"};

        return generarExcel("Movimientos", encabezados,
                movimientos.stream().map(m -> new Object[]{
                        FECHA_HORA.format(m.getCreadoEn()),
                        m.getTipo().name(),
                        m.getMaterial().getSku(),
                        m.getMaterial().getNombre(),
                        m.getUbicacion().getCodigo(),
                        m.getCantidad(),
                        m.getSaldoResultante(),
                        nullToBlank(m.getOrdenCompra()),
                        nullToBlank(m.getProveedor()),
                        nullToBlank(m.getOrdenProduccion()),
                        nullToBlank(m.getAreaSolicitante()),
                        m.getUsuario().getNombres() + " " + m.getUsuario().getApellidos()
                }).toList());
    }

    private List<MovimientoAlmacen> obtenerMovimientos(Instant desde, Instant hasta, Long materialId) {
        return materialId != null
                ? movimientoRepository.findByMaterialIdAndCreadoEnBetweenOrderByCreadoEnAsc(materialId, desde, hasta)
                : movimientoRepository.findByCreadoEnBetweenOrderByCreadoEnAsc(desde, hasta);
    }

    // =========================================================================
    // KARDEX de un material (RF-09 + RF-15)
    // =========================================================================

    public byte[] kardexPdf(Long materialId) {
        Material material = buscarMaterial(materialId);
        List<KardexItem> items = movimientoRepository.findByMaterialIdOrderByCreadoEnAsc(materialId).stream()
                .map(KardexItem::fromMovimiento).toList();

        String[] encabezados = {"Fecha", "Tipo", "Ubicacion", "Entrada", "Salida", "Saldo", "Referencia", "Usuario"};
        return generarPdf("Kardex - " + material.getSku() + " " + material.getNombre(), null, encabezados,
                items.stream().map(i -> new String[]{
                        FECHA_HORA.format(i.getFecha()),
                        i.getTipo(),
                        i.getUbicacion(),
                        i.getEntrada().compareTo(BigDecimal.ZERO) > 0 ? i.getEntrada().toPlainString() : "-",
                        i.getSalida().compareTo(BigDecimal.ZERO) > 0 ? i.getSalida().toPlainString() : "-",
                        i.getSaldo() != null ? i.getSaldo().toPlainString() : "-",
                        nullToBlank(i.getReferencia()),
                        i.getUsuario()
                }).toList());
    }

    public byte[] kardexExcel(Long materialId) {
        Material material = buscarMaterial(materialId);
        List<KardexItem> items = movimientoRepository.findByMaterialIdOrderByCreadoEnAsc(materialId).stream()
                .map(KardexItem::fromMovimiento).toList();

        String[] encabezados = {"Fecha", "Tipo", "Ubicacion", "Entrada", "Salida", "Saldo", "Referencia", "Usuario"};
        return generarExcel("Kardex " + material.getSku(), encabezados,
                items.stream().map(i -> new Object[]{
                        FECHA_HORA.format(i.getFecha()),
                        i.getTipo(),
                        i.getUbicacion(),
                        i.getEntrada(),
                        i.getSalida(),
                        i.getSaldo(),
                        nullToBlank(i.getReferencia()),
                        i.getUsuario()
                }).toList());
    }

    // =========================================================================
    // STOCK actual (por material y ubicacion)
    // =========================================================================

    public byte[] stockExcel() {
        List<StockUbicacion> stocks = stockRepository.findAll();
        String[] encabezados = {"SKU", "Material", "Categoria", "Almacen", "Ubicacion", "Cantidad", "Unidad", "Stock minimo", "Stock maximo"};

        return generarExcel("Stock actual", encabezados,
                stocks.stream().map(s -> new Object[]{
                        s.getMaterial().getSku(),
                        s.getMaterial().getNombre(),
                        s.getMaterial().getCategoria(),
                        s.getUbicacion().getAlmacen().getNombre(),
                        s.getUbicacion().getCodigo(),
                        s.getCantidad(),
                        s.getMaterial().getUnidadMedida(),
                        s.getMaterial().getStockMinimo(),
                        s.getMaterial().getStockMaximo()
                }).toList());
    }

    // =========================================================================
    // Generadores genericos de documento (PDF con OpenPDF / Excel con Apache POI)
    // =========================================================================

    private byte[] generarPdf(String titulo, String subtitulo, String[] encabezados, List<String[]> filas) {
        try {
            Document documento = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font fuenteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC);
            Font fuenteEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph paragraphTitulo = new Paragraph("INDUMETAL PERU S.A.C. - " + titulo, fuenteTitulo);
            paragraphTitulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(paragraphTitulo);

            if (subtitulo != null) {
                Paragraph paragraphSubtitulo = new Paragraph(subtitulo, fuenteSubtitulo);
                paragraphSubtitulo.setAlignment(Element.ALIGN_CENTER);
                documento.add(paragraphSubtitulo);
            }
            Paragraph generadoEl = new Paragraph(
                    "Generado el " + FECHA_HORA.format(Instant.now()), fuenteSubtitulo);
            generadoEl.setAlignment(Element.ALIGN_CENTER);
            generadoEl.setSpacingAfter(12f);
            documento.add(generadoEl);

            PdfPTable tabla = new PdfPTable(encabezados.length);
            tabla.setWidthPercentage(100);

            for (String encabezado : encabezados) {
                PdfPCell celda = new PdfPCell(new Phrase(encabezado, fuenteEncabezado));
                celda.setBackgroundColor(new Color(31, 58, 92)); // azul corporativo
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                celda.setPadding(5f);
                tabla.addCell(celda);
            }

            for (String[] fila : filas) {
                for (String valor : fila) {
                    PdfPCell celda = new PdfPCell(new Phrase(valor, fuenteCelda));
                    celda.setPadding(4f);
                    tabla.addCell(celda);
                }
            }
            if (filas.isEmpty()) {
                PdfPCell celdaVacia = new PdfPCell(new Phrase("No se encontraron registros para los filtros indicados.", fuenteCelda));
                celdaVacia.setColspan(encabezados.length);
                celdaVacia.setHorizontalAlignment(Element.ALIGN_CENTER);
                celdaVacia.setPadding(10f);
                tabla.addCell(celdaVacia);
            }

            documento.add(tabla);
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el reporte PDF: " + e.getMessage(), e);
        }
    }

    private byte[] generarExcel(String nombreHoja, String[] encabezados, List<Object[]> filas) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = workbook.createSheet(nombreHoja);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row filaEncabezado = hoja.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            int numeroFila = 1;
            for (Object[] fila : filas) {
                Row filaExcel = hoja.createRow(numeroFila++);
                for (int col = 0; col < fila.length; col++) {
                    Cell celda = filaExcel.createCell(col);
                    Object valor = fila[col];
                    if (valor == null) {
                        celda.setBlank();
                    } else if (valor instanceof BigDecimal bd) {
                        celda.setCellValue(bd.doubleValue());
                    } else if (valor instanceof Number n) {
                        celda.setCellValue(n.doubleValue());
                    } else {
                        celda.setCellValue(valor.toString());
                    }
                }
            }

            for (int i = 0; i < encabezados.length; i++) {
                hoja.autoSizeColumn(i);
            }

            workbook.write(salida);
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el reporte Excel: " + e.getMessage(), e);
        }
    }

    private Material buscarMaterial(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Material", id));
    }

    private String rangoTexto(Instant desde, Instant hasta) {
        return "Periodo: " + FECHA_HORA.format(desde) + "  a  " + FECHA_HORA.format(hasta);
    }

    private String nullToBlank(String valor) {
        return valor == null ? "-" : valor;
    }
}
