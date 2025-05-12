package anonymized.contrastive.experiments;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExtractSummaryStatsToCSV {

    private static final List<String> TARGET_STATS = Arrays.asList(
            "stats-count",
            "common-size(avg)",
            "common-size(min)",
            "common-size(max)",
            "common-size(var)",
            "difference-size(avg)",
            "difference-size(min)",
            "difference-size(max)",
            "difference-size(var)",
            "conflict-size(avg)",
            "conflict-size(min)",
            "conflict-size(max)",
            "conflict-size(var)",
            "fresh-individuals(avg)",
            "fresh-individuals(min)",
            "fresh-individuals(max)",
            "fresh-individuals(var)",
            "duration(avg)",
            "duration(min)",
            "duration(max)",
            "duration(var)"
    );

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java ExtractSummaryStatsToCSV <file_list.txt> <csv_folder_path>");
            return;
        }

        String fileListPath = args[0];
        String folderPath = args[1];
        // Generate file name with current date
        String currentDate = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
        String outputFilePath = "extracted_summary_stats_"+ currentDate+".csv";
        List<String> fileNames = Files.readAllLines(Paths.get(fileListPath));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            // Write CSV header (excluding stats-counts)
            writer.write("No.,Ontology");
            for (String stat : TARGET_STATS) {
                writer.write("," + stat);
            }
            writer.newLine();

            int counter = 1;
            for (String fileName : fileNames) {
                File csvFile = Paths.get(folderPath, fileName).toFile();
                if (!csvFile.exists()) {
                    System.out.println("[Missing file]: " + csvFile.getAbsolutePath());
                    continue;
                }

                Map<String, String> statValues = new HashMap<>();
                List<String> lines = Files.readAllLines(csvFile.toPath());

                boolean inSummarySection = false;

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.startsWith("Summary Statistics:")) {
                        inSummarySection = true;
                        continue;
                    }

                    if (inSummarySection) {
                        String[] parts = line.split(",", 2);
                        if (parts.length == 2) {
                            String statName = parts[0].trim();
                            String statValue = parts[1].trim();

                            if (TARGET_STATS.contains(statName)) {
                                statValues.put(statName, statValue.isEmpty() ? "0" : statValue);
                            }
                        }
                    }
                }

                // Write row without stats-counts
                writer.write(counter + "," + fileName);
                for (String stat : TARGET_STATS) {
                    writer.write("," + statValues.getOrDefault(stat, ""));
                }
                writer.newLine();
                counter++;
            }
        }

        System.out.println("Extracted summary stats written to: " + outputFilePath);
    }
}
