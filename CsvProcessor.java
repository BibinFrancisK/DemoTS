package csvprocessor;

import org.apache.commons.csv.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CsvProcessor {

    private static final String INPUT_FILE = "src/main/resources/cca_extract.csv";
    private static final String OUTPUT_FILE = "src/main/resources/adjusted_cca_extract.csv";
    private static final String PAYMENT_AMOUNT = "PAYMENT_AMOUNT";
    private static final String PRODUCT_TYPE = "PRODUCT_TYPE";

    public static void main(String[] args) {
        try {
            // Define the expected payment amounts for each product type
            Map<String, Double> productTypeTargetMap = new HashMap<>();
            productTypeTargetMap.put("101", 1500.00);
            productTypeTargetMap.put("202", 3000.00);

            List<CSVRecord> records = readCsv(INPUT_FILE);
            Map<String, List<CSVRecord>> groupedRecords = records.stream()
                    .collect(Collectors.groupingBy(record -> record.get(PRODUCT_TYPE)));

            List<Map<String, String>> updatedRecords = adjustPaymentAmounts(groupedRecords, productTypeTargetMap);
            writeCsv(records, updatedRecords, OUTPUT_FILE);

            System.out.println("✅ File processed successfully. Check adjusted_cca_extract.csv.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<CSVRecord> readCsv(String filePath) throws IOException {
        try (Reader reader = Files.newBufferedReader(Paths.get(filePath))) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            return csvParser.getRecords();
        }
    }

    private static List<Map<String, String>> adjustPaymentAmounts(Map<String, List<CSVRecord>> groupedRecords,
                                                                  Map<String, Double> productTypeTargetMap) {
        List<Map<String, String>> updatedRecords = new ArrayList<>();

        for (Map.Entry<String, List<CSVRecord>> entry : groupedRecords.entrySet()) {
            String productType = entry.getKey();
            List<CSVRecord> productTypeRecords = entry.getValue();

            if (productTypeTargetMap.containsKey(productType)) {
                double targetSum = productTypeTargetMap.get(productType);

                // Calculate current sum of PAYMENT_AMOUNT
                double currentSum = productTypeRecords.stream()
                        .filter(record -> !record.get(PAYMENT_AMOUNT).isEmpty())
                        .mapToDouble(record -> Double.parseDouble(record.get(PAYMENT_AMOUNT)))
                        .sum();

                // Calculate the difference to be adjusted
                double difference = targetSum - currentSum;

                // Get non-zero entries for adjustment
                List<CSVRecord> nonZeroRecords = productTypeRecords.stream()
                        .filter(record -> !record.get(PAYMENT_AMOUNT).isEmpty() && Double.parseDouble(record.get(PAYMENT_AMOUNT)) != 0)
                        .collect(Collectors.toList());

                if (!nonZeroRecords.isEmpty() && difference != 0) {
                    double adjustmentPerRecord = difference / nonZeroRecords.size();
                    double cumulativeAdjustment = 0.0;
                    int lastIndex = nonZeroRecords.size() - 1;

                    for (int i = 0; i < nonZeroRecords.size(); i++) {
                        CSVRecord record = nonZeroRecords.get(i);
                        Map<String, String> updatedRecord = new HashMap<>(record.toMap());

                        double currentAmount = Double.parseDouble(record.get(PAYMENT_AMOUNT));
                        double adjustedAmount;

                        // Apply adjustment to all but the last record to ensure total sum matches exactly
                        if (i == lastIndex) {
                            adjustedAmount = currentAmount + (difference - cumulativeAdjustment);
                        } else {
                            adjustedAmount = currentAmount + adjustmentPerRecord;
                            cumulativeAdjustment += adjustmentPerRecord;
                        }

                        updatedRecord.put(PAYMENT_AMOUNT, String.format("%.2f", adjustedAmount));
                        updatedRecords.add(updatedRecord);
                    }
                }
            }

            // Add unchanged records (including zero PAYMENT_AMOUNT and non-matching records)
            for (CSVRecord record : productTypeRecords) {
                if (!updatedRecords.contains(record.toMap())) {
                    updatedRecords.add(record.toMap());
                }
            }
        }

        return updatedRecords;
    }

    private static void writeCsv(List<CSVRecord> originalRecords, List<Map<String, String>> updatedRecords, String outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(originalRecords.get(0).toMap().keySet().toArray(new String[0])))) {

            for (Map<String, String> updatedRecord : updatedRecords) {
                csvPrinter.printRecord(updatedRecord.values());
            }
        }
    }
}
