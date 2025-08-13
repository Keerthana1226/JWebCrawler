package com.jwebcrawler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DataWriter {

    private final PrintWriter writer;

    public DataWriter(String filePath) throws IOException {
        // Open the FileWriter in "append" mode (the 'true' flag).
        FileWriter fileWriter = new FileWriter(filePath, true);
        this.writer = new PrintWriter(fileWriter);

        // Write the CSV header row if the file is new/empty.
        if (new java.io.File(filePath).length() == 0) {
            writer.println("\"URL\",\"Title\",\"Description\"");
        }
    }

    /**
     * Writes a single row of data to the CSV file.
     * This method is 'synchronized' to make it thread-safe. Only one thread
     * can execute this method at a time, preventing file corruption.
     * @param data The CrawlData object to write.
     */
    public synchronized void writeData(CrawlData data) {
        writer.println(
                escapeCsvField(data.url()) + "," +
                        escapeCsvField(data.title()) + "," +
                        escapeCsvField(data.description())
        );
        // Flush the writer to ensure data is written to the file immediately.
        writer.flush();
    }

    /**
     * Wraps a string in double quotes and escapes any existing double quotes inside it.
     * This is necessary to create a valid CSV file.
     * @param field The string to escape.
     * @return The properly escaped and quoted string.
     */
    private String escapeCsvField(String field) {
        if (field == null || field.isEmpty()) {
            return "\"\"";
        }
        // Replace any double quotes with two double quotes, then wrap the whole thing in double quotes.
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    /**
     * Closes the file writer. This must be called when the crawl is finished.
     */
    public void close() {
        writer.close();
    }
}