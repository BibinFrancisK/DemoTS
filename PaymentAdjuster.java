package com.example;

import org.apache.commons.csv.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PaymentAdjuster {

    // File paths
    private static final String INPUT_CSV = "src/main/resources/cca_extract.csv";
    private static final String OUTPUT_CSV = "src/main/resources/adjusted_cca_extract.csv";
    private static final LocalDate DATE_THRESHOLD = LocalDate.of(2024, 7, 1);

    public static void main(String[] args) {
        try {
            // Define product type and expected total amount
            Map<String, Double> expectedAmounts = new HashMap<>();
            expectedAmounts.put("ProductA", 5000.0);
            expectedAmounts.put("ProductB", 7500.0);

            List<CSVRecord> records = readCsv(INPUT_CSV);
            List<Map<String, String>> adjustedRecords = adjustPayments(records, expectedAmounts);
            writeCsv(adjustedRecords, OUTPUT_CSV);

            System.out.println("Payment adjustment completed. New file created: " + OUTPUT_CSV);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read the CSV file
    public static List<CSVRecord> readCsv(String filePath) throws IOException {
        try (Reader reader = Files.newBufferedReader(Paths.get(filePath))) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            return csvParser.getRecords();
        }
    }

    // Adjust payments
    public static List<Map<String, String>> adjustPayments(List<CSVRecord> records, Map<String, Double> expectedAmounts) {
        // Group records by PRODUCT_TYPE
        Map<String, List<CSVRecord>> recordsByProductType = records.stream()
                .collect(Collectors.groupingBy(record -> record.get("PRODUCT_TYPE")));

        List<Map<String, String>> adjustedRecords = new ArrayList<>();

        for (String productType : recordsByProductType.keySet()) {
            List<CSVRecord> productRecords = recordsByProductType.get(productType);
            double currentTotal = productRecords.stream()
                    .filter(record -> isValidRecord(record))
                    .mapToDouble(record -> Double.parseDouble(record.get("PAYMENT_AMOUNT")))
                    .sum();

            double targetAmount = expectedAmounts.getOrDefault(productType, currentTotal);
            double amountDifference = targetAmount - currentTotal;

            if (amountDifference != 0) {
                distributeAdjustment(productRecords, amountDifference);
            }

            // Prepare adjusted records
            for (CSVRecord record : productRecords) {
                Map<String, String> recordMap = new HashMap<>();
                for (String header : record.toMap().keySet()) {
                    recordMap.put(header, record.get(header));
                }
                adjustedRecords.add(recordMap);
            }
        }
        return adjustedRecords;
    }

    // Check if record is valid for adjustment
    private static boolean isValidRecord(CSVRecord record) {
        LocalDate issueDate = LocalDate.parse(record.get("ISSUE DATE"));
        double paymentAmount = Double.parseDouble(record.get("PAYMENT_AMOUNT"));
        return issueDate.isBefore(DATE_THRESHOLD) && paymentAmount != 0;
    }

    // Distribute the adjustment across multiple records
    // Distribute the adjustment across multiple records
private static void distributeAdjustment(List<CSVRecord> productRecords, double amountDifference) {
    int numRecords = (int) productRecords.stream().filter(PaymentAdjuster::isValidRecord).count();
    double adjustmentPerRecord = amountDifference / Math.max(numRecords, 1);

    for (CSVRecord record : productRecords) {
        if (isValidRecord(record)) {
            double currentAmount = Double.parseDouble(record.get("PAYMENT_AMOUNT"));
            double newAmount = currentAmount + adjustmentPerRecord;

            // Create a map to modify record values and update PAYMENT_AMOUNT
            Map<String, String> updatedRecord = new HashMap<>(record.toMap());
            updatedRecord.put("PAYMENT_AMOUNT", String.format("%.2f", newAmount));
            adjustedRecords.add(updatedRecord);
        } else {
            // Add unmodified records to the final list
            adjustedRecords.add(new HashMap<>(record.toMap()));
        }
    }
}


    // Write the adjusted CSV
    public static void writeCsv(List<Map<String, String>> records, String outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(records.get(0).keySet().toArray(new String[0])))) {
            for (Map<String, String> record : records) {
                csvPrinter.printRecord(record.values());
            }
        }
    }
}
