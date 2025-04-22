import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import models.Record;

/**
 * CsvWriter is used to write API request logs into a CSV file.
 */
public class CsvWriter {

    /**
     * Path to the output CSV file
     */
    private static final String CSV_FILE_PATH = "output/api_request_log.csv";

    /**
     * Constructor
     */
    public CsvWriter() {
        verifyOrCreateFilePath();
    }

    /**
     * Verifies that the directory and CSV file exist. If not, they are created.
     */
    private void verifyOrCreateFilePath() {
        File file = new File(CSV_FILE_PATH);
        File parentDir = file.getParentFile();

        // If the directory does not exist, create it
        if (!parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                System.out.println("Directory created: " + parentDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        // If the file does not exist, create it with headers
        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE_PATH, true))) {
                writer.write("startTime,requestType,latency,statusCode");
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Error creating CSV file: " + e.getMessage());
            }
        }
    }

    /**
     * Writes the list of records to the CSV file. The file is overwritten every time this method is called.
     *
     * @param recordList List of records to write to the CSV file
     * @throws IOException If an I/O error occurs while writing to the file
     */
    public void writeRecordsToCsv(List<Record> recordList) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE_PATH, false))) {
            for (Record record : recordList) {
                String formattedTime = formatter.format(Instant.ofEpochMilli(record.getStartTime()));

                writer.write(formattedTime + "," + record.getRequestType() + "," +
                        record.getLatency() + "," + record.getStatusCode());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
        }
    }

    /**
     * Formatter to format the timestamp to readable format
     */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
}
