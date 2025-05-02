package nl.vu.kai.contrastive.experiments;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CombineStatsCSVsFromList {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java CombineStatsCSVsFromList <file_list.txt> <csv_folder_path>");
            return;
        }

        String fileListPath = args[0];
        String folderPath = args[1];

        List<String> fileNames = Files.readAllLines(Paths.get(fileListPath));
        String currentDate = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
        String outputFilePath = "combined_stats_"+ currentDate+".csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            for (String fileName : fileNames) {
                File csvFile = Paths.get(folderPath, fileName).toFile();
                if (!csvFile.exists()) {
                    System.out.println("Missing file: " + csvFile.getAbsolutePath());
                    continue;
                }

                List<String> lines = Files.readAllLines(csvFile.toPath());

                // Add the filename only once
                writer.write("Log_file: " + fileName + "\n");

                boolean inSummarySection = false;

                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;

                    if (line.startsWith("Summary Statistics:")) {
                        inSummarySection = true;
                        writer.write("\nSummary Statistics:\n");
                        continue;
                    }

                    if (inSummarySection) {
                        writer.write(line + "\n");
                    } else {
                        // If it's a header or data row, write it as-is
                        writer.write(line + "\n");
                    }
                }

                // Add one blank row after each file
                writer.write("\n");
            }
        }

        System.out.println("Combined stats written to: " + outputFilePath);
    }
}
