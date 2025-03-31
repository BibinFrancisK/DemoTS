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

            List<CSVRecord> updatedRecords = adjustPaymentAmounts(groupedRecords, productTypeTargetMap, records);
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

    private static List<CSVRecord> adjustPaymentAmounts(Map<String, List<CSVRecord>> groupedRecords, 
                                                        Map<String, Double> productTypeTargetMap, 
                                                        List<CSVRecord> allRecords) {
        List<CSVRecord> updatedRecords = new ArrayList<>();
        
        for (String productType : groupedRecords.keySet()) {
            List<CSVRecord> records = groupedRecords.get(productType);
            double currentSum = records.stream()
                    .filter(record -> !record.get(PAYMENT_AMOUNT).isEmpty())
                    .mapToDouble(record -> Double.parseDouble(record.get(PAYMENT_AMOUNT)))
                    .sum();

            Double targetSum = productTypeTargetMap.get(productType);
            if (targetSum != null && currentSum != targetSum) {
                double difference = targetSum - currentSum;
                List<CSVRecord> nonZeroRecords = records.stream()
                        .filter(record -> !record.get(PAYMENT_AMOUNT).isEmpty() && Double.parseDouble(record.get(PAYMENT_AMOUNT)) != 0)
                        .collect(Collectors.toList());

                if (!nonZeroRecords.isEmpty()) {
                    double adjustmentPerRecord = difference / nonZeroRecords.size();
                    for (CSVRecord record : nonZeroRecords) {
                        Map<String, String> updatedRecord = new HashMap<>(record.toMap());
                        double originalAmount = Double.parseDouble(record.get(PAYMENT_AMOUNT));
                        updatedRecord.put(PAYMENT_AMOUNT, String.format("%.2f", originalAmount + adjustmentPerRecord));
                        updatedRecords.add(new CSVRecord(updatedRecord, record.getParser()));
                    }
                }
            }
        }
        return updatedRecords;
    }

    private static void writeCsv(List<CSVRecord> originalRecords, List<CSVRecord> updatedRecords, String outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(originalRecords.get(0).toMap().keySet().toArray(new String[0])))) {

            for (CSVRecord record : originalRecords) {
                Map<String, String> updatedRecord = updatedRecords.stream()
                        .filter(r -> r.get("CONTRACT_ID").equals(record.get("CONTRACT_ID")))
                        .findFirst()
                        .map(CSVRecord::toMap)
                        .orElse(record.toMap());

                csvPrinter.printRecord(updatedRecord.values());
            }
        }
    }
}
