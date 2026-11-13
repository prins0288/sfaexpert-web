package in.opt.sfa.common.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal Excel (.xlsx) helpers for the "bulk upload = upsert" flow and for
 * downloadable import templates. Header row (row 0) supplies column keys;
 * every subsequent row becomes an ordered map of {header -> cell text}.
 */
public final class ExcelUtil {

    private ExcelUtil() { }

    /** Read the first sheet into a list of {header -> value} maps. */
    public static List<Map<String, String>> read(InputStream in) throws IOException {
        try (Workbook wb = WorkbookFactory.create(in)) {
            return wb.getNumberOfSheets() == 0 ? new ArrayList<>() : readSheet(wb.getSheetAt(0));
        }
    }

    /**
     * Read every sheet: {sheet name in lower case -> rows}. For multi-sheet
     * uploads (e.g. Items / Rates / Composition in one file).
     */
    public static Map<String, List<Map<String, String>>> readAll(InputStream in) throws IOException {
        Map<String, List<Map<String, String>>> res = new LinkedHashMap<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            for (Sheet sheet : wb) res.put(sheet.getSheetName().trim().toLowerCase(java.util.Locale.ROOT), readSheet(sheet));
        }
        return res;
    }

    private static List<Map<String, String>> readSheet(Sheet sheet) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (sheet == null) return rows;
        Row header = sheet.getRow(sheet.getFirstRowNum());
        if (header == null) return rows;

        List<String> keys = new ArrayList<>();
        for (int c = 0; c < header.getLastCellNum(); c++) keys.add(cellText(header.getCell(c)).trim());

        for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isBlank(row)) continue;
            Map<String, String> map = new LinkedHashMap<>();
            for (int c = 0; c < keys.size(); c++) {
                Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                map.put(keys.get(c), cell == null ? "" : cellText(cell).trim());
            }
            rows.add(map);
        }
        return rows;
    }

    /** One sheet of a multi-sheet template: name, header columns, sample rows (may be empty). */
    public record SheetSpec(String name, List<String> headers, List<List<String>> samples) { }

    /** Build a template workbook with several sheets. */
    public static byte[] template(List<SheetSpec> sheets) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CellStyle head = headStyle(wb);
            for (SheetSpec spec : sheets) {
                Sheet sheet = wb.createSheet(spec.name());
                Row h = sheet.createRow(0);
                for (int i = 0; i < spec.headers().size(); i++) {
                    Cell c = h.createCell(i);
                    c.setCellValue(spec.headers().get(i));
                    c.setCellStyle(head);
                    sheet.setColumnWidth(i, 5000);
                }
                int r = 1;
                for (List<String> sample : spec.samples()) {
                    Row s = sheet.createRow(r++);
                    for (int i = 0; i < sample.size(); i++) s.createCell(i).setCellValue(sample.get(i));
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private static CellStyle headStyle(Workbook wb) {
        CellStyle head = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        head.setFont(f);
        head.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return head;
    }

    /** Build a single-sheet template workbook with the given header columns. */
    public static byte[] template(String sheetName, List<String> headers, List<String> sampleRow)
            throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle head = headStyle(wb);

            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = h.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(head);
                sheet.setColumnWidth(i, 5000);
            }
            if (sampleRow != null && !sampleRow.isEmpty()) {
                Row s = sheet.createRow(1);
                for (int i = 0; i < sampleRow.size(); i++) {
                    s.createCell(i).setCellValue(sampleRow.get(i));
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /** Build the standard xlsx download response (attachment). */
    public static org.springframework.http.ResponseEntity<byte[]> xlsxResponse(String filename, byte[] body) {
        String cd = "attachment; filename=\"" + filename + "\"; filename*=UTF-8''"
                + java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, cd)
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    private static String cellText(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d) : String.valueOf(d);
            }
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private static boolean isBlank(Row row) {
        for (Cell c : row) {
            if (c != null && !cellText(c).trim().isEmpty()) return false;
        }
        return true;
    }
}
