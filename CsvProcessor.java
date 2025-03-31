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

            List<Map<String, String>> updatedRecords = adjustPaymentAmounts(groupedRecords, productTypeTargetMap, records);
            writeCsv(records, updatedRecords, OUTPUT_FILE);

            System.out.println("File processed successfully. Check adjusted_cca_extract.csv.");
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
                                                                  Map<String, Double> productTypeTargetMap,
                                                                  List<CSVRecord> allRecords) {
        List<Map<String, String>> updatedRecords = new ArrayList<>();

        for (CSVRecord record : allRecords) {
            Map<String, String> updatedRecord = new HashMap<>(record.toMap());
            String productType = record.get(PRODUCT_TYPE);
            String paymentAmountStr = record.get(PAYMENT_AMOUNT);

            if (productTypeTargetMap.containsKey(productType) && !paymentAmountStr.isEmpty()) {
                double currentPayment = Double.parseDouble(paymentAmountStr);
                double targetSum = productTypeTargetMap.get(productType);

                // Get all records with the same product type to calculate total amount
                List<CSVRecord> productTypeRecords = groupedRecords.get(productType);
                double currentSum = productTypeRecords.stream()
                        .filter(r -> !r.get(PAYMENT_AMOUNT).isEmpty())
                        .mapToDouble(r -> Double.parseDouble(r.get(PAYMENT_AMOUNT)))
                        .sum();

                // Calculate the difference to be adjusted
                double difference = targetSum - currentSum;
                if (difference != 0 && currentPayment != 0) {
                    double adjustmentPerRecord = difference / productTypeRecords.size();
                    double newAmount = currentPayment + adjustmentPerRecord;
                    updatedRecord.put(PAYMENT_AMOUNT, String.format("%.2f", newAmount));
                }
            }

            // Add the updated record to the list
            updatedRecords.add(updatedRecord);
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
