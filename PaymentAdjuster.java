package com.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PaymentAdjuster {

    public static void main(String[] args) throws Exception {
        String inputFilePath = "src/main/resources/cca_extract.xlsx";
        String outputFilePath = "src/main/resources/adjusted_cca_extract.xlsx";

        // Define expected PAYMENT_AMOUNT by PRODUCT_TYPE
        Map<String, BigDecimal> expectedAmountMap = new HashMap<>();
        expectedAmountMap.put("A1", new BigDecimal("5000.00"));
        expectedAmountMap.put("B2", new BigDecimal("7000.00"));

        processExcel(inputFilePath, outputFilePath, expectedAmountMap);
    }

    public static void processExcel(String inputFilePath, String outputFilePath, Map<String, BigDecimal> expectedAmountMap) throws Exception {
        FileInputStream fis = new FileInputStream(inputFilePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        // Read all rows into a list, while preserving header
        List<Row> rows = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            rows.add(sheet.getRow(i));
        }

        // Remove duplicates based on TRANSACTION_ID
        Map<String, Row> uniqueRows = new HashMap<>();
        int transactionIdIndex = findColumnIndex(headerRow, "TRANSACTION_ID");
        for (Row row : rows) {
            String transactionId = getCellValueAsString(row.getCell(transactionIdIndex));
            if (!transactionId.isEmpty()) {
                uniqueRows.putIfAbsent(transactionId, row);
            }
        }

        // Process unique rows
        List<Row> filteredRows = new ArrayList<>(uniqueRows.values());
        adjustPaymentAmount(filteredRows, headerRow, expectedAmountMap);

        // Write the results to a new XLSX file
        writeToExcel(headerRow, filteredRows, outputFilePath);
        workbook.close();
    }

    private static void adjustPaymentAmount(List<Row> rows, Row headerRow, Map<String, BigDecimal> expectedAmountMap) {
        int productTypeIndex = findColumnIndex(headerRow, "PRODUCT_TYPE");
        int paymentAmountIndex = findColumnIndex(headerRow, "PAYMENT_AMOUNT");

        // Map to track total amounts per PRODUCT_TYPE
        Map<String, BigDecimal> currentAmountMap = new HashMap<>();
        for (Row row : rows) {
            String productType = getCellValueAsString(row.getCell(productTypeIndex));
            BigDecimal paymentAmount = getCellValueAsBigDecimal(row.getCell(paymentAmountIndex));

            if (!productType.isEmpty() && paymentAmount != null) {
                currentAmountMap.put(productType, currentAmountMap.getOrDefault(productType, BigDecimal.ZERO).add(paymentAmount));
            }
        }

        // Adjust payment amounts to match expected values
        for (Map.Entry<String, BigDecimal> entry : expectedAmountMap.entrySet()) {
            String productType = entry.getKey();
            BigDecimal expectedAmount = entry.getValue();
            BigDecimal currentAmount = currentAmountMap.getOrDefault(productType, BigDecimal.ZERO);
            BigDecimal difference = expectedAmount.subtract(currentAmount);

            if (difference.compareTo(BigDecimal.ZERO) != 0) {
                distributeAdjustment(rows, productType, paymentAmountIndex, difference);
            }
        }
    }

    private static void distributeAdjustment(List<Row> rows, String productType, int paymentAmountIndex, BigDecimal difference) {
        List<Row> matchingRows = rows.stream()
                .filter(row -> productType.equals(getCellValueAsString(row.getCell(findColumnIndex(row.getSheet().getRow(0), "PRODUCT_TYPE")))))
                .filter(row -> getCellValueAsBigDecimal(row.getCell(paymentAmountIndex)).compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (matchingRows.isEmpty()) {
            return;
        }

        BigDecimal adjustmentPerRow = difference.divide(new BigDecimal(matchingRows.size()), BigDecimal.ROUND_HALF_EVEN);
        for (Row row : matchingRows) {
            Cell paymentCell = row.getCell(paymentAmountIndex);
            BigDecimal originalAmount = getCellValueAsBigDecimal(paymentCell);
            BigDecimal newAmount = originalAmount.add(adjustmentPerRow);
            if (newAmount.compareTo(BigDecimal.ZERO) >= 0) {
                paymentCell.setCellValue(newAmount.doubleValue());
            }
        }
    }

    private static void writeToExcel(Row headerRow, List<Row> rows, String outputFilePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // Write header
        Row newHeaderRow = sheet.createRow(0);
        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            Cell newCell = newHeaderRow.createCell(i);
            newCell.setCellValue(headerRow.getCell(i).getStringCellValue());
        }

        // Write rows
        int rowNum = 1;
        for (Row row : rows) {
            Row newRow = sheet.createRow(rowNum++);
            for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
                Cell oldCell = row.getCell(i);
                Cell newCell = newRow.createCell(i);

                if (oldCell != null) {
                    switch (oldCell.getCellType()) {
                        case STRING:
                            newCell.setCellValue(oldCell.getStringCellValue());
                            break;
                        case NUMERIC:
                            newCell.setCellValue(oldCell.getNumericCellValue());
                            break;
                        default:
                            newCell.setCellValue("");
                    }
                }
            }
        }

        // Write output to file
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        workbook.write(fos);
        fos.close();
        workbook.close();
    }

    private static int findColumnIndex(Row headerRow, String columnName) {
        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            if (headerRow.getCell(i).getStringCellValue().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Column " + columnName + " not found");
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return "";
    }

    private static BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return BigDecimal.ZERO;
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return BigDecimal.ZERO;
    }
}
