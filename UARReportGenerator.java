import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class UARReportGenerator {

    public static void main(String[] args) throws IOException {
        String alipFilePath = "src/main/resources/alip.xlsx";
        String db2FilePath = "src/main/resources/db2.xlsx";
        String outputFilePath = "uar.xlsx";
        generateUARReport(alipFilePath, db2FilePath, outputFilePath);
    }

    public static void generateUARReport(String alipPath, String db2Path, String outputPath) throws IOException {
        Workbook alipWorkbook = new XSSFWorkbook(new FileInputStream(new File(alipPath)));
        Workbook db2Workbook = new XSSFWorkbook(new FileInputStream(new File(db2Path)));
        Workbook outputWorkbook = new XSSFWorkbook();
        Sheet outputSheet = outputWorkbook.createSheet("UAR Report");

        int rowIndex = 0;
        CellStyle boldStyle = createBoldStyle(outputWorkbook);
        
        // Extract CCA Client IDs from db2.xlsx
        Set<String> ccaClientIds = extractCCAClientIds(db2Workbook);

        // Section 1: CCA to ALIP
        rowIndex = writeSection("CCA to ALIP", alipWorkbook, "P_CLNT_ID", "CNTR_STAT_TXT", ccaClientIds, outputSheet, rowIndex, boldStyle);
        
        // Section 2: CCA Clients for Above Contracts
        rowIndex = writeSectionFromDB2("CCA Clients for Above Contracts", db2Workbook, outputSheet, rowIndex, boldStyle);

        // Section 3: ALIP to ALIP
        rowIndex = writeSectionWithDuplicates("ALIP to ALIP", alipWorkbook, "P_CLNT_ID", outputSheet, rowIndex, boldStyle);
        
        // Write to file
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            outputWorkbook.write(fos);
        }

        alipWorkbook.close();
        db2Workbook.close();
        outputWorkbook.close();
    }

    private static Set<String> extractCCAClientIds(Workbook db2Workbook) {
        Set<String> ccaClientIds = new HashSet<>();
        Sheet sheet = db2Workbook.getSheetAt(0);
        for (Row row : sheet) {
            Cell clientIdCell = row.getCell(0);
            Cell typeCell = row.getCell(1);
            if (clientIdCell != null && typeCell != null && typeCell.getNumericCellValue() == 24) {
                ccaClientIds.add(clientIdCell.getStringCellValue().trim());
            }
        }
        return ccaClientIds;
    }

    private static int writeSection(String sectionName, Workbook alipWorkbook, String clientColumn, String statusColumn,
                                    Set<String> validClientIds, Sheet outputSheet, int rowIndex, CellStyle boldStyle) {
        Sheet alipSheet = alipWorkbook.getSheetAt(0);
        rowIndex = writeHeader(outputSheet, alipSheet, rowIndex, sectionName, boldStyle);
        for (Row row : alipSheet) {
            Cell statusCell = row.getCell(1);
            Cell clientCell = row.getCell(0);
            if (statusCell != null && clientCell != null && statusCell.getStringCellValue().trim().equalsIgnoreCase("ACTIVE") &&
                    validClientIds.contains(clientCell.getStringCellValue().trim())) {
                rowIndex = copyRow(outputSheet, row, rowIndex);
            }
        }
        return rowIndex + 1;
    }

    private static int writeSectionFromDB2(String sectionName, Workbook db2Workbook, Sheet outputSheet, int rowIndex, CellStyle boldStyle) {
        Sheet db2Sheet = db2Workbook.getSheetAt(0);
        rowIndex = writeHeader(outputSheet, db2Sheet, rowIndex, sectionName, boldStyle);
        for (Row row : db2Sheet) {
            rowIndex = copyRow(outputSheet, row, rowIndex);
        }
        return rowIndex + 1;
    }

    private static int writeSectionWithDuplicates(String sectionName, Workbook alipWorkbook, String clientColumn, 
                                                  Sheet outputSheet, int rowIndex, CellStyle boldStyle) {
        Sheet alipSheet = alipWorkbook.getSheetAt(0);
        Map<String, List<Row>> groupedByClient = new HashMap<>();
        for (Row row : alipSheet) {
            Cell clientCell = row.getCell(0);
            if (clientCell != null) {
                String clientId = clientCell.getStringCellValue().trim();
                if (clientId.length() > 8) {
                    groupedByClient.computeIfAbsent(clientId, k -> new ArrayList<>()).add(row);
                }
            }
        }
        rowIndex = writeHeader(outputSheet, alipSheet, rowIndex, sectionName, boldStyle);
        for (List<Row> rows : groupedByClient.values()) {
            if (rows.size() > 1) {
                for (Row row : rows) {
                    rowIndex = copyRow(outputSheet, row, rowIndex);
                }
            }
        }
        return rowIndex + 1;
    }

    private static int writeHeader(Sheet outputSheet, Sheet sourceSheet, int rowIndex, String sectionTitle, CellStyle boldStyle) {
        Row titleRow = outputSheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(sectionTitle);
        titleCell.setCellStyle(boldStyle);
        
        Row headerRow = outputSheet.createRow(rowIndex++);
        for (int i = 0; i < sourceSheet.getRow(0).getLastCellNum(); i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(sourceSheet.getRow(0).getCell(i).getStringCellValue());
            headerCell.setCellStyle(boldStyle);
        }
        return rowIndex;
    }

    private static int copyRow(Sheet outputSheet, Row sourceRow, int rowIndex) {
        Row newRow = outputSheet.createRow(rowIndex++);
        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            Cell sourceCell = sourceRow.getCell(i);
            if (sourceCell != null) {
                Cell newCell = newRow.createCell(i);
                newCell.setCellValue(sourceCell.toString());
            }
        }
        return rowIndex;
    }

    private static CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
