package com.mongodb.mandate;

import com.mongodb.mandate.repository.MandateRepository;
import com.mongodb.mandate.service.MandateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class MandateProcessorApplication {

    private static final Logger logger = LoggerFactory.getLogger(MandateProcessorApplication.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar mandate-processor.jar <input-file> [batch-size]");
            System.err.println("Example: java -jar mandate-processor.jar mandates.txt 200");
            System.exit(1);
        }

        String inputFile = args[0];
        int batchSize = args.length > 1 ? Integer.parseInt(args[1]) : 200;

        // Load configuration
        Properties props = loadProperties();
        String connectionString = props.getProperty("mongodb.uri", "mongodb://localhost:27017");
        String databaseName = props.getProperty("mongodb.database", "mandate_db");

        logger.info("Starting Mandate Processor");
        logger.info("Input file: {}", inputFile);
        logger.info("Batch size: {}", batchSize);
        logger.info("Database: {}", databaseName);

        try (MandateRepository repository = new MandateRepository(connectionString, databaseName)) {
            MandateProcessor processor = new MandateProcessor(repository, batchSize);

            Path filePath = Paths.get(inputFile);
            processor.processFile(filePath);

            logger.info("Processing completed successfully");

        } catch (Exception e) {
            logger.error("Error processing file: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream input = MandateProcessorApplication.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            logger.warn("Could not load application.properties, using defaults");
        }

        // Allow environment variable overrides
        String mongoUri = System.getenv("MONGODB_URI");
        if (mongoUri != null) {
            props.setProperty("mongodb.uri", mongoUri);
        }

        String mongoDb = System.getenv("MONGODB_DATABASE");
        if (mongoDb != null) {
            props.setProperty("mongodb.database", mongoDb);
        }

        return props;
    }
}
