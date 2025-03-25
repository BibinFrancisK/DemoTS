package com.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;

public class ExcelProcessor {

    private static final String EXTERNAL_REPORT_PATH = "src/main/resources/External_Report.xlsx";
    private static final String INTERNAL_REPORT_PATH = "src/main/resources/Internal_Report.xlsx";
    private static final String CONSOLIDATED_REPORT_PATH = "src/main/resources/Consolidated_Report.xlsx";

    public static void main(String[] args) {
        try {
            Map<String, Row> externalReportData = loadExternalReport();
            Map<String, Double> internalReportData = loadInternalReport();

            Workbook consolidatedWorkbook = new XSSFWorkbook();
            Sheet consolidatedSheet = consolidatedWorkbook.createSheet("Consolidated Report");

            // Create header row
            String[] headers = {"CONTRACT_ID", "TRANSACTION_ID", "ISSUE_DATE", "PRODUCT_TYPE",
                    "PRODUCT_NAME", "ACCOUNT_VALUE", "POSTAL_CODE", "PAYMENT_AMOUNT",
                    "SURRENDER_INDICATOR", "SURRENDER_AMOUNT", "SALES_CODE_ID"};

            Row headerRow = consolidatedSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;

            // Process external report
            for (String contractId : externalReportData.keySet()) {
                Row externalRow = externalReportData.get(contractId);
                double paymentAmount = externalRow.getCell(7).getNumericCellValue();

                if (internalReportData.containsKey(contractId)) {
                    // Update payment amount from internal report
                    externalRow.getCell(7).setCellValue(internalReportData.get(contractId));
                    writeRowToSheet(externalRow, consolidatedSheet, rowNum++);
                } else {
                    // Keep only if payment amount is zero
                    if (paymentAmount == 0) {
                        writeRowToSheet(externalRow, consolidatedSheet, rowNum++);
                    }
                }
            }

            // Process internal report and add missing CONTRACT_IDs
            for (String contractId : internalReportData.keySet()) {
                if (!externalReportData.containsKey(contractId)) {
                    Row newRow = consolidatedSheet.createRow(rowNum++);
                    newRow.createCell(0).setCellValue(contractId);
                    newRow.createCell(7).setCellValue(internalReportData.get(contractId));
                }
            }

            // Save consolidated report
            try (FileOutputStream fileOut = new FileOutputStream(CONSOLIDATED_REPORT_PATH)) {
                consolidatedWorkbook.write(fileOut);
            }

            System.out.println("✅ Consolidated report generated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Row> loadExternalReport() throws Exception {
        Map<String, Row> dataMap = new LinkedHashMap<>();
        try (InputStream fis = new FileInputStream(EXTERNAL_REPORT_PATH);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String contractId = row.getCell(0).getStringCellValue();
                dataMap.put(contractId, row);
            }
        }
        return dataMap;
    }

    private static Map<String, Double> loadInternalReport() throws Exception {
        Map<String, Double> dataMap = new LinkedHashMap<>();
        try (InputStream fis = new FileInputStream(INTERNAL_REPORT_PATH);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String contractId = row.getCell(0).getStringCellValue();
                double sales = row.getCell(1).getNumericCellValue();
                dataMap.put(contractId, sales);
            }
        }
        return dataMap;
    }

    private static void writeRowToSheet(Row sourceRow, Sheet targetSheet, int rowNum) {
        Row newRow = targetSheet.createRow(rowNum);
        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            Cell sourceCell = sourceRow.getCell(i);
            Cell targetCell = newRow.createCell(i);
            if (sourceCell != null) {
                switch (sourceCell.getCellType()) {
                    case STRING:
                        targetCell.setCellValue(sourceCell.getStringCellValue());
                        break;
                    case NUMERIC:
                        targetCell.setCellValue(sourceCell.getNumericCellValue());
                        break;
                    case BOOLEAN:
                        targetCell.setCellValue(sourceCell.getBooleanCellValue());
                        break;
                    default:
                        targetCell.setCellValue("");
                }
            }
        }
    }
}
