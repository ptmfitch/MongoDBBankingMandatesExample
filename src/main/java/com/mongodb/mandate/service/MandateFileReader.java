package com.mongodb.mandate.service;

import com.mongodb.mandate.model.DirectDebitMandate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class MandateFileReader implements Iterable<DirectDebitMandate>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MandateFileReader.class);
    private static final String DELIMITER = "\\|";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BufferedReader reader;
    private final String fileName;
    private String[] headers;
    private long lineNumber = 0;

    public MandateFileReader(Path filePath) throws IOException {
        this.fileName = filePath.getFileName().toString();
        this.reader = Files.newBufferedReader(filePath);
        readHeaders();
    }

    private void readHeaders() throws IOException {
        String headerLine = reader.readLine();
        lineNumber++;
        if (headerLine != null) {
            headers = headerLine.split(DELIMITER);
            logger.info("Read {} columns from header", headers.length);
        }
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Read mandates in batches for memory efficiency
     */
    public List<DirectDebitMandate> readBatch(int batchSize) throws IOException {
        List<DirectDebitMandate> batch = new ArrayList<>(batchSize);
        String line;

        while (batch.size() < batchSize && (line = reader.readLine()) != null) {
            lineNumber++;
            try {
                DirectDebitMandate mandate = parseLine(line);
                if (mandate != null) {
                    batch.add(mandate);
                }
            } catch (Exception e) {
                logger.error("Error parsing line {}: {}", lineNumber, e.getMessage());
            }
        }

        return batch;
    }

    private DirectDebitMandate parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] values = line.split(DELIMITER, -1); // -1 to keep trailing empty strings

        try {
            return DirectDebitMandate.builder()
                    .mandateId(getStringValue(values, 0))
                    .lastUpdateDate(getDateTimeValue(values, 1))
                    .creditorId(getStringValue(values, 2))
                    .creditorName(getStringValue(values, 3))
                    .creditorAccountNumber(getStringValue(values, 4))
                    .creditorSortCode(getStringValue(values, 5))
                    .creditorIban(getStringValue(values, 6))
                    .creditorBic(getStringValue(values, 7))
                    .debtorName(getStringValue(values, 8))
                    .debtorAccountNumber(getStringValue(values, 9))
                    .debtorSortCode(getStringValue(values, 10))
                    .debtorIban(getStringValue(values, 11))
                    .debtorBic(getStringValue(values, 12))
                    .debtorEmail(getStringValue(values, 13))
                    .debtorPhone(getStringValue(values, 14))
                    .mandateReference(getStringValue(values, 15))
                    .mandateType(getStringValue(values, 16))
                    .frequency(getStringValue(values, 17))
                    .status(getStringValue(values, 18))
                    .signatureDate(getDateValue(values, 19))
                    .effectiveDate(getDateValue(values, 20))
                    .expiryDate(getDateValue(values, 21))
                    .maxAmountPerTransaction(getBigDecimalValue(values, 22))
                    .maxAmountPerMonth(getBigDecimalValue(values, 23))
                    .maxTransactionsPerMonth(getIntegerValue(values, 24))
                    .currency(getStringValue(values, 25))
                    .description(getStringValue(values, 26))
                    .schemeType(getStringValue(values, 27))
                    .build();
        } catch (Exception e) {
            logger.error("Error parsing mandate at line {}: {}", lineNumber, e.getMessage());
            return null;
        }
    }

    private String getStringValue(String[] values, int index) {
        if (index >= values.length) return null;
        String value = values[index].trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDateTime getDateTimeValue(String[] values, int index) {
        String value = getStringValue(values, index);
        if (value == null) return null;
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private LocalDate getDateValue(String[] values, int index) {
        String value = getStringValue(values, index);
        if (value == null) return null;
        return LocalDate.parse(value, DATE_FORMATTER);
    }

    private BigDecimal getBigDecimalValue(String[] values, int index) {
        String value = getStringValue(values, index);
        if (value == null) return null;
        return new BigDecimal(value);
    }

    private Integer getIntegerValue(String[] values, int index) {
        String value = getStringValue(values, index);
        if (value == null) return null;
        return Integer.parseInt(value);
    }

    @Override
    public Iterator<DirectDebitMandate> iterator() {
        return new MandateIterator();
    }

    private class MandateIterator implements Iterator<DirectDebitMandate> {
        private String nextLine;

        @Override
        public boolean hasNext() {
            if (nextLine != null) {
                return true;
            }
            try {
                nextLine = reader.readLine();
                return nextLine != null;
            } catch (IOException e) {
                logger.error("Error reading file: {}", e.getMessage());
                return false;
            }
        }

        @Override
        public DirectDebitMandate next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String line = nextLine;
            nextLine = null;
            lineNumber++;
            return parseLine(line);
        }
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
