package com.mongodb.mandate.generator;

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
import java.util.concurrent.ThreadLocalRandom;

public class MandateDataGenerator {

    private static final String DELIMITER = "|";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Realistic data pools
    private static final String[] CREDITOR_NAMES = {
            "British Gas Services Ltd", "Thames Water Utilities", "EDF Energy Customers Plc",
            "Sky UK Limited", "Virgin Media Payments", "O2 UK Limited", "Vodafone UK",
            "BT Payment Services Ltd", "Netflix International", "Spotify AB",
            "Amazon Prime UK", "Apple Services", "Google Payment Ltd", "Microsoft Ireland",
            "Sainsbury's Bank", "Tesco Personal Finance", "HSBC Insurance Services",
            "Aviva Insurance Ltd", "Direct Line Insurance", "Admiral Insurance",
            "AA Membership Services", "RAC Motoring Services", "Gymbox Ltd",
            "PureGym Limited", "David Lloyd Leisure", "National Trust Membership",
            "RSPCA Donations", "British Heart Foundation", "Cancer Research UK",
            "Oxfam GB", "Save the Children UK", "WWF UK", "Greenpeace UK",
            "London Borough Council", "Birmingham City Council", "Manchester City Council",
            "Scottish Power Energy", "Octopus Energy Ltd", "Bulb Energy Ltd",
            "Shell Energy Retail", "Ovo Energy Limited", "E.ON Next Energy",
            "Anglian Water Services", "United Utilities Water", "Severn Trent Water",
            "Yorkshire Water Services", "South West Water Ltd", "Northumbrian Water",
            "Now TV Limited", "Disney Plus UK", "HBO Max UK", "Paramount Plus UK",
            "Audible UK", "Kindle Unlimited", "Adobe Systems UK", "Dropbox UK Ltd"
    };

    private static final String[] FIRST_NAMES = {
            "Oliver", "George", "Arthur", "Noah", "Muhammad", "Leo", "Oscar", "Harry",
            "Olivia", "Amelia", "Isla", "Ava", "Mia", "Isabella", "Sophia", "Grace",
            "James", "William", "Thomas", "Henry", "Alexander", "Edward", "Charles",
            "Emily", "Poppy", "Ella", "Lily", "Freya", "Florence", "Willow", "Rosie",
            "Jack", "Jacob", "Freddie", "Alfie", "Theodore", "Archie", "Joshua", "Max",
            "Sophie", "Evie", "Ivy", "Elsie", "Daisy", "Sienna", "Charlotte", "Alice",
            "Mohammed", "Ethan", "Lucas", "Logan", "Daniel", "Sebastian", "Benjamin",
            "Chloe", "Matilda", "Maya", "Harper", "Phoebe", "Eva", "Ruby", "Esme",
            "David", "Michael", "Christopher", "Andrew", "Richard", "Paul", "Mark",
            "Sarah", "Emma", "Hannah", "Rachel", "Rebecca", "Laura", "Jennifer", "Lisa"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Jones", "Williams", "Taylor", "Brown", "Davies", "Evans", "Wilson",
            "Thomas", "Johnson", "Roberts", "Robinson", "Thompson", "Wright", "Walker",
            "White", "Edwards", "Hughes", "Green", "Hall", "Lewis", "Harris", "Clarke",
            "Patel", "Jackson", "Wood", "Turner", "Martin", "Cooper", "Hill", "Ward",
            "Morris", "Moore", "Clark", "Lee", "King", "Baker", "Harrison", "Morgan",
            "Allen", "James", "Scott", "Phillips", "Watson", "Davis", "Parker", "Price",
            "Bennett", "Young", "Griffiths", "Mitchell", "Kelly", "Cook", "Carter", "Shaw",
            "Khan", "Ahmed", "Ali", "Singh", "Kumar", "Sharma", "Hussain", "Begum",
            "Murphy", "O'Brien", "O'Connor", "Ryan", "Kelly", "Walsh", "McCarthy", "Byrne"
    };

    private static final String[] MANDATE_TYPES = {"RECURRING", "ONE_OFF"};
    private static final String[] FREQUENCIES = {"WEEKLY", "FORTNIGHTLY", "MONTHLY", "QUARTERLY", "ANNUALLY"};
    private static final String[] STATUSES = {"ACTIVE", "ACTIVE", "ACTIVE", "ACTIVE", "SUSPENDED", "PENDING"}; // Weighted towards ACTIVE
    private static final String[] SCHEME_TYPES = {"BACS", "BACS", "BACS", "SEPA_CORE", "SEPA_B2B"}; // Weighted towards BACS
    private static final String[] CURRENCIES = {"GBP", "GBP", "GBP", "GBP", "EUR"}; // Weighted towards GBP

    private static final String[] EMAIL_DOMAINS = {
            "gmail.com", "yahoo.co.uk", "hotmail.com", "outlook.com", "icloud.com",
            "btinternet.com", "sky.com", "virginmedia.com", "talktalk.net", "aol.com"
    };

    private static final String[] DESCRIPTIONS = {
            "Monthly subscription", "Utility bill payment", "Insurance premium",
            "Membership fee", "Loan repayment", "Mortgage payment", "Rent payment",
            "Charity donation", "Service subscription", "Regular savings",
            "Council tax", "Water rates", "Energy bill", "Phone contract",
            "Broadband service", "TV subscription", "Gym membership", "Club dues"
    };

    // UK Bank Sort Codes (realistic patterns)
    private static final String[] SORT_CODE_PREFIXES = {
            "01", "04", "05", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16",
            "17", "18", "19", "20", "23", "30", "31", "32", "33", "34", "35", "36", "37",
            "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50",
            "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "70", "71", "72",
            "73", "74", "77", "80", "82", "83", "87", "89", "90", "91", "93"
    };

    private final Random random = new Random();
    private final int totalRecords;
    private final String outputDir;

    public MandateDataGenerator(int totalRecords, String outputDir) {
        this.totalRecords = totalRecords;
        this.outputDir = outputDir;
    }

    public static void main(String[] args) {
        int numRecords = 10000; // Default
        String outputDir = ".";

        if (args.length >= 1) {
            numRecords = parseNumberOfRecords(args[0]);
        }
        if (args.length >= 2) {
            outputDir = args[1];
        }

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         Direct Debit Mandate Data Generator                ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Records to generate: %-37s ║%n", formatNumber(numRecords));
        System.out.printf("║  Output directory:    %-37s ║%n", truncate(outputDir, 37));
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        MandateDataGenerator generator = new MandateDataGenerator(numRecords, outputDir);

        try {
            String outputFile = generator.generate();
            System.out.println("\n✓ File generated successfully: " + outputFile);
        } catch (IOException e) {
            System.err.println("\n✗ Error generating file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static int parseNumberOfRecords(String input) {
        String normalized = input.toUpperCase().trim();

        try {
            if (normalized.endsWith("K")) {
                return (int) (Double.parseDouble(normalized.substring(0, normalized.length() - 1)) * 1_000);
            } else if (normalized.endsWith("M")) {
                return (int) (Double.parseDouble(normalized.substring(0, normalized.length() - 1)) * 1_000_000);
            } else {
                return Integer.parseInt(normalized);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format: " + input);
            System.err.println("Use formats like: 1000, 500k, 1.5M");
            System.exit(1);
            return 0;
        }
    }

    private static String formatNumber(int num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }

    private static String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return "..." + str.substring(str.length() - maxLength + 3);
    }

    public String generate() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String formattedCount = formatNumber(totalRecords);
        String fileName = String.format("mandates_%s_%s.txt",
                now.format(FILE_DATE_FORMAT),
                formattedCount);

        Path outputPath = Paths.get(outputDir, fileName);
        Files.createDirectories(outputPath.getParent());

        long startTime = System.currentTimeMillis();
        int progressUpdateInterval = Math.max(1, totalRecords / 100);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // Write header
            writer.write(getHeader());
            writer.newLine();

            // Generate records with progress bar
            for (int i = 0; i < totalRecords; i++) {
                writer.write(generateRecord(i + 1));
                writer.newLine();

                // Update progress bar
                if (i % progressUpdateInterval == 0 || i == totalRecords - 1) {
                    printProgress(i + 1, totalRecords, startTime);
                }
            }
        }

        System.out.println(); // New line after progress bar
        long duration = System.currentTimeMillis() - startTime;
        printSummary(outputPath, duration);

        return outputPath.toString();
    }

    private String getHeader() {
        return String.join(DELIMITER,
                "mandateId", "lastUpdateDate", "creditorId", "creditorName",
                "creditorAccountNumber", "creditorSortCode", "creditorIban", "creditorBic",
                "debtorName", "debtorAccountNumber", "debtorSortCode", "debtorIban", "debtorBic",
                "debtorEmail", "debtorPhone", "mandateReference", "mandateType", "frequency",
                "status", "signatureDate", "effectiveDate", "expiryDate",
                "maxAmountPerTransaction", "maxAmountPerMonth", "maxTransactionsPerMonth",
                "currency", "description", "schemeType"
        );
    }

    private String generateRecord(int index) {
        String mandateId = String.format("MND-%010d", index);
        LocalDateTime lastUpdateDate = randomDateTime(2023, 2026);

        // Creditor info
        String creditorId = String.format("CRED%06d", random.nextInt(1000) + 1);
        String creditorName = randomElement(CREDITOR_NAMES);
        String creditorAccountNumber = randomDigits(8);
        String creditorSortCode = randomSortCode();
        String creditorIban = generateIban("GB", creditorSortCode, creditorAccountNumber);
        String creditorBic = generateBic();

        // Debtor info
        String debtorFirstName = randomElement(FIRST_NAMES);
        String debtorLastName = randomElement(LAST_NAMES);
        String debtorName = debtorFirstName + " " + debtorLastName;
        String debtorAccountNumber = randomDigits(8);
        String debtorSortCode = randomSortCode();
        String debtorIban = generateIban("GB", debtorSortCode, debtorAccountNumber);
        String debtorBic = generateBic();
        String debtorEmail = generateEmail(debtorFirstName, debtorLastName);
        String debtorPhone = generateUkPhone();

        // Mandate details
        String mandateReference = String.format("REF-%s-%06d",
                creditorName.substring(0, Math.min(4, creditorName.length())).toUpperCase().replaceAll("[^A-Z]", ""),
                random.nextInt(999999));
        String mandateType = randomElement(MANDATE_TYPES);
        String frequency = mandateType.equals("ONE_OFF") ? "ONE_OFF" : randomElement(FREQUENCIES);
        String status = randomElement(STATUSES);

        LocalDate signatureDate = randomDate(2020, 2026);
        LocalDate effectiveDate = signatureDate.plusDays(random.nextInt(30) + 1);
        LocalDate expiryDate = effectiveDate.plusYears(random.nextInt(5) + 1);

        // Financial limits
        BigDecimal maxPerTransaction = randomAmount(10, 5000);
        BigDecimal maxPerMonth = maxPerTransaction.multiply(BigDecimal.valueOf(random.nextInt(5) + 1));
        int maxTransactionsPerMonth = random.nextInt(10) + 1;

        String currency = randomElement(CURRENCIES);
        String description = randomElement(DESCRIPTIONS);
        String schemeType = currency.equals("EUR") ? "SEPA_CORE" : randomElement(SCHEME_TYPES);

        return String.join(DELIMITER,
                mandateId,
                lastUpdateDate.format(DATE_TIME_FORMAT),
                creditorId,
                creditorName,
                creditorAccountNumber,
                creditorSortCode,
                creditorIban,
                creditorBic,
                debtorName,
                debtorAccountNumber,
                debtorSortCode,
                debtorIban,
                debtorBic,
                debtorEmail,
                debtorPhone,
                mandateReference,
                mandateType,
                frequency,
                status,
                signatureDate.format(DATE_FORMAT),
                effectiveDate.format(DATE_FORMAT),
                expiryDate.format(DATE_FORMAT),
                maxPerTransaction.toString(),
                maxPerMonth.toString(),
                String.valueOf(maxTransactionsPerMonth),
                currency,
                description,
                schemeType
        );
    }

    private void printProgress(int current, int total, long startTime) {
        int barWidth = 50;
        double progress = (double) current / total;
        int filledWidth = (int) (barWidth * progress);

        long elapsed = System.currentTimeMillis() - startTime;
        long estimatedTotal = (long) (elapsed / progress);
        long remaining = estimatedTotal - elapsed;

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

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    Generation Summary                      ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Records generated: %-38s ║%n", formatNumber(totalRecords));
        System.out.printf("║  File size:         %-38s ║%n", formatFileSize(fileSize));
        System.out.printf("║  Duration:          %-38s ║%n", formatDuration(duration));
        System.out.printf("║  Throughput:        %-38s ║%n",
                String.format("%s records/sec", formatNumber((int) (totalRecords * 1000L / Math.max(1, duration)))));
        System.out.println("╚════════════════════════════════════════════════════════════╝");
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

    // Helper methods for generating realistic data

    private <T> T randomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String randomSortCode() {
        String prefix = randomElement(SORT_CODE_PREFIXES);
        return prefix + "-" + String.format("%02d", random.nextInt(100)) + "-" + String.format("%02d", random.nextInt(100));
    }

    private String generateIban(String countryCode, String sortCode, String accountNumber) {
        String cleanSortCode = sortCode.replace("-", "");
        // Simplified IBAN generation (not fully valid but realistic looking)
        String checkDigits = String.format("%02d", random.nextInt(90) + 10);
        String bankCode = "NWBK"; // Example bank code
        return countryCode + checkDigits + bankCode + cleanSortCode + accountNumber;
    }

    private String generateBic() {
        String[] bankCodes = {"NWBK", "BARC", "HSBC", "LOYD", "MIDL", "NATW", "RBOS", "SWIN", "SANT", "TSBS"};
        String[] countryCodes = {"GB", "GB", "GB", "GB", "IE"};
        String[] locationCodes = {"2L", "22", "21", "2S", "MM"};

        return randomElement(bankCodes) + randomElement(countryCodes) + randomElement(locationCodes);
    }

    private String generateEmail(String firstName, String lastName) {
        String[] formats = {
                "%s.%s@%s",
                "%s%s@%s",
                "%s_%s@%s",
                "%s.%s%d@%s",
                "%s%d@%s"
        };

        String format = randomElement(formats);
        String domain = randomElement(EMAIL_DOMAINS);
        String first = firstName.toLowerCase();
        String last = lastName.toLowerCase();
        int num = random.nextInt(99) + 1;

        return switch (formats.length - java.util.Arrays.asList(formats).indexOf(format)) {
            case 5 -> String.format(format, first, last, domain);
            case 4 -> String.format(format, first, last, domain);
            case 3 -> String.format(format, first, last, domain);
            case 2 -> String.format(format, first, last, num, domain);
            default -> String.format(format, first, num, domain);
        };
    }

    private String generateUkPhone() {
        String[] prefixes = {"07700", "07701", "07702", "07703", "07704", "07705", "07706", "07707", "07708", "07709",
                "07800", "07801", "07802", "07803", "07804", "07805", "07806", "07807", "07808", "07809",
                "07900", "07901", "07902", "07903", "07904", "07905", "07906", "07907", "07908", "07909"};
        return "+" + "44" + randomElement(prefixes).substring(1) + randomDigits(6);
    }

    private LocalDateTime randomDateTime(int startYear, int endYear) {
        long startEpoch = LocalDateTime.of(startYear, 1, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC);
        long endEpoch = LocalDateTime.of(endYear, 12, 31, 23, 59).toEpochSecond(java.time.ZoneOffset.UTC);
        long randomEpoch = ThreadLocalRandom.current().nextLong(startEpoch, endEpoch);
        return LocalDateTime.ofEpochSecond(randomEpoch, 0, java.time.ZoneOffset.UTC);
    }

    private LocalDate randomDate(int startYear, int endYear) {
        long startEpoch = LocalDate.of(startYear, 1, 1).toEpochDay();
        long endEpoch = LocalDate.of(endYear, 12, 31).toEpochDay();
        long randomEpoch = ThreadLocalRandom.current().nextLong(startEpoch, endEpoch);
        return LocalDate.ofEpochDay(randomEpoch);
    }

    private BigDecimal randomAmount(int min, int max) {
        double amount = min + (max - min) * random.nextDouble();
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
