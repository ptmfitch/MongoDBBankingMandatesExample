package com.mongodb.mandate.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class MandateDataModifier {

    private static final String DELIMITER = "\\|";
    private static final String OUTPUT_DELIMITER = "|";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Random random = new Random();
    private final double editPercentage;
    private final Path inputFile;
    private final String outputDir;

    // Counters
    private int totalRecords = 0;
    private int modifiedRecords = 0;
    private int unchangedRecords = 0;

    // Field indices
    private static final int IDX_MANDATE_ID = 0;
    private static final int IDX_LAST_UPDATE_DATE = 1;
    private static final int IDX_CREDITOR_NAME = 3;
    private static final int IDX_DEBTOR_NAME = 8;
    private static final int IDX_DEBTOR_EMAIL = 13;
    private static final int IDX_DEBTOR_PHONE = 14;
    private static final int IDX_FREQUENCY = 17;
    private static final int IDX_STATUS = 18;
    private static final int IDX_MAX_AMOUNT_PER_TRANSACTION = 22;
    private static final int IDX_MAX_AMOUNT_PER_MONTH = 23;
    private static final int IDX_MAX_TRANSACTIONS_PER_MONTH = 24;
    private static final int IDX_DESCRIPTION = 26;

    // Realistic modification values
    private static final String[] STATUSES = {"ACTIVE", "SUSPENDED", "PENDING", "CANCELLED"};
    private static final String[] FREQUENCIES = {"WEEKLY", "FORTNIGHTLY", "MONTHLY", "QUARTERLY", "ANNUALLY"};
    private static final String[] DESCRIPTIONS = {
            "Monthly subscription", "Utility bill payment", "Insurance premium",
            "Membership fee", "Loan repayment", "Service subscription",
            "Updated payment plan", "Revised direct debit", "Amended standing order"
    };

    public MandateDataModifier(Path inputFile, double editPercentage, String outputDir) {
        this.inputFile = inputFile;
        this.editPercentage = editPercentage;
        this.outputDir = outputDir;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: MandateDataModifier <input-file> <edit-percentage> [output-dir]");
            System.err.println("Example: MandateDataModifier ./data/mandates.txt 25 ./data");
            System.err.println("  This would randomly modify ~25% of records");
            System.exit(1);
        }

        Path inputFile = Paths.get(args[0]);
        double editPercentage = Double.parseDouble(args[1]);
        String outputDir = args.length > 2 ? args[2] : inputFile.getParent().toString();

        if (editPercentage < 0 || editPercentage > 100) {
            System.err.println("Error: Edit percentage must be between 0 and 100");
            System.exit(1);
        }

        if (!Files.exists(inputFile)) {
            System.err.println("Error: Input file not found: " + inputFile);
            System.exit(1);
        }

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           Direct Debit Mandate Data Modifier               ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Input file:      %-40s ║%n", truncate(inputFile.getFileName().toString(), 40));
        System.out.printf("║  Edit percentage: %-40s ║%n", editPercentage + "%");
        System.out.printf("║  Output dir:      %-40s ║%n", truncate(outputDir, 40));
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        MandateDataModifier modifier = new MandateDataModifier(inputFile, editPercentage, outputDir);

        try {
            String outputFile = modifier.modify();
            System.out.println("\n✓ File generated successfully: " + outputFile);
        } catch (IOException e) {
            System.err.println("\n✗ Error modifying file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return "..." + str.substring(str.length() - maxLength + 3);
    }

    public String modify() throws IOException {
        // Count total lines first for progress bar
        long totalLines = Files.lines(inputFile).count() - 1; // Subtract header

        // Generate output filename
        LocalDateTime now = LocalDateTime.now();
        String inputFileName = inputFile.getFileName().toString();
        String baseName = inputFileName.replace(".txt", "");
        String outputFileName = String.format("%s_modified_%s_%.0fpct.txt",
                baseName,
                now.format(FILE_DATE_FORMAT),
                editPercentage);

        Path outputPath = Paths.get(outputDir, outputFileName);
        Files.createDirectories(outputPath.getParent());

        long startTime = System.currentTimeMillis();
        int progressUpdateInterval = Math.max(1, (int) totalLines / 100);

        try (BufferedReader reader = Files.newBufferedReader(inputFile);
             BufferedWriter writer = Files.newBufferedWriter(outputPath)) {

            // Copy header
            String header = reader.readLine();
            writer.write(header);
            writer.newLine();

            String line;
            while ((line = reader.readLine()) != null) {
                totalRecords++;

                String outputLine;
                if (shouldModify()) {
                    outputLine = modifyRecord(line);
                    modifiedRecords++;
                } else {
                    unchangedRecords++;
                    outputLine = line;
                }

                writer.write(outputLine);
                writer.newLine();

                // Update progress bar
                if (totalRecords % progressUpdateInterval == 0 || totalRecords == totalLines) {
                    printProgress(totalRecords, (int) totalLines, startTime);
                }
            }
        }

        System.out.println(); // New line after progress bar
        long duration = System.currentTimeMillis() - startTime;
        printSummary(outputPath, duration);

        return outputPath.toString();
    }

    private boolean shouldModify() {
        return random.nextDouble() * 100 < editPercentage;
    }

    private String modifyRecord(String line) {
        String[] fields = line.split(DELIMITER, -1);

        // Always update the lastUpdateDate for modified records
        fields[IDX_LAST_UPDATE_DATE] = LocalDateTime.now().format(DATE_TIME_FORMAT);

        // Randomly choose which fields to modify (1-4 fields)
        int numFieldsToModify = random.nextInt(4) + 1;

        for (int i = 0; i < numFieldsToModify; i++) {
            int fieldChoice = random.nextInt(8);

            switch (fieldChoice) {
                case 0 -> fields[IDX_STATUS] = randomElement(STATUSES);
                case 1 -> fields[IDX_FREQUENCY] = randomElement(FREQUENCIES);
                case 2 -> fields[IDX_MAX_AMOUNT_PER_TRANSACTION] = randomAmount(10, 5000);
                case 3 -> fields[IDX_MAX_AMOUNT_PER_MONTH] = randomAmount(50, 10000);
                case 4 -> fields[IDX_MAX_TRANSACTIONS_PER_MONTH] = String.valueOf(random.nextInt(20) + 1);
                case 5 -> fields[IDX_DESCRIPTION] = randomElement(DESCRIPTIONS);
                case 6 -> fields[IDX_DEBTOR_EMAIL] = modifyEmail(fields[IDX_DEBTOR_EMAIL]);
                case 7 -> fields[IDX_DEBTOR_PHONE] = generateUkPhone();
            }
        }

        return String.join(OUTPUT_DELIMITER, fields);
    }

    private String modifyEmail(String currentEmail) {
        if (currentEmail == null || currentEmail.isEmpty()) {
            return "updated.user@email.com";
        }

        String[] parts = currentEmail.split("@");
        if (parts.length != 2) {
            return "updated.user@email.com";
        }

        // Add or change a number suffix
        String localPart = parts[0].replaceAll("\\d+$", "");
        return localPart + (random.nextInt(999) + 1) + "@" + parts[1];
    }

    private String generateUkPhone() {
        String[] prefixes = {"07700", "07701", "07702", "07800", "07801", "07900", "07901"};
        return "+44" + randomElement(prefixes).substring(1) + randomDigits(6);
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private <T> T randomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }

    private String randomAmount(int min, int max) {
        double amount = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toString();
    }

    private void printProgress(int current, int total, long startTime) {
        int barWidth = 50;
        double progress = (double) current / total;
        int filledWidth = (int) (barWidth * progress);

        long elapsed = System.currentTimeMillis() - startTime;
        long estimatedTotal = progress > 0 ? (long) (elapsed / progress) : 0;
        long remaining = Math.max(0, estimatedTotal - elapsed);

        StringBuilder bar = new StringBuilder("\r[");
        for (int i = 0; i < barWidth; i++) {
            if (i < filledWidth) {
                bar.append("█");
            } else if (i == filledWidth) {
                bar.append("▓");
            } else {
                bar.append("░");
            }
        }
        bar.append(String.format("] %6.2f%% | %s/%s | ETA: %s",
                progress * 100,
                formatNumber(current),
                formatNumber(total),
                formatDuration(remaining)));

        System.out.print(bar);
        System.out.flush();
    }

    private void printSummary(Path outputPath, long duration) throws IOException {
        long fileSize = Files.size(outputPath);
        double actualPercentage = (double) modifiedRecords / totalRecords * 100;

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                   Modification Summary                     ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Total records:     %-38s ║%n", formatNumber(totalRecords));
        System.out.printf("║  Modified:          %-38s ║%n",
                formatNumber(modifiedRecords) + String.format(" (%.1f%%)", actualPercentage));
        System.out.printf("║  Unchanged:         %-38s ║%n",
                formatNumber(unchangedRecords) + String.format(" (%.1f%%)", 100 - actualPercentage));
        System.out.printf("║  File size:         %-38s ║%n", formatFileSize(fileSize));
        System.out.printf("║  Duration:          %-38s ║%n", formatDuration(duration));
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    private String formatNumber(int num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }

    private String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else if (seconds > 0) {
            return String.format("%ds", seconds);
        } else {
            return String.format("%dms", millis);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes >= 1_073_741_824) {
            return String.format("%.2f GB", bytes / 1_073_741_824.0);
        } else if (bytes >= 1_048_576) {
            return String.format("%.2f MB", bytes / 1_048_576.0);
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        return bytes + " bytes";
    }
}
