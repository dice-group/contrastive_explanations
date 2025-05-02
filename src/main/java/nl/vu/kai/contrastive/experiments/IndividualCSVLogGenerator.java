package nl.vu.kai.contrastive.experiments;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class IndividualCSVLogGenerator {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java LogToCsvConverter <file_list.txt> <log_folder_path>");
            return;
        }

        String fileListPath = args[0];
        String logFolderPath = args[1];

        List<String> fileNames = Files.readAllLines(Paths.get(fileListPath));

        for (String logFileName : fileNames) {
            String logFilePath = Paths.get(logFolderPath, logFileName).toString();
            File logFile = new File(logFilePath);

            // Remove .log extension and append _stats.csv
            String baseName = logFileName.replaceAll("\\.log$", "");
            String outputCsv = baseName + "_stats.csv";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputCsv))) {
                writer.write("common_part_size,difference_part_size,conflict_set_size,fresh_individuals,duration\n");

                if (!logFile.exists()) {
                    System.out.println("Log file not found: " + logFilePath);
                    writer.write(",,,,,Log file not found\n");
                    continue;
                }

                List<String> lines = Files.readAllLines(logFile.toPath());
                boolean foundStats = false;

                List<Integer> commonSizes = new ArrayList<>();
                List<Integer> differenceSizes = new ArrayList<>();
                List<Integer> conflictSizes = new ArrayList<>();
                List<Integer> freshIndividuals = new ArrayList<>();
                List<Long> durations = new ArrayList<>();
                int statsCount = 0; // Counter for STATS entries

                for (String line : lines) {
                    if (line.startsWith("STATS:")) {
                        foundStats = true;
                        statsCount++;
                        String[] parts = line.replace("STATS:", "").trim().split("\\s+");
                        if (parts.length == 5) {
                            writer.write(String.join(",", parts) + "\n");

                            // Parse values
                            commonSizes.add(Integer.parseInt(parts[0]));
                            differenceSizes.add(Integer.parseInt(parts[1]));
                            conflictSizes.add(Integer.parseInt(parts[2]));
                            freshIndividuals.add(Integer.parseInt(parts[3]));
                            durations.add(Long.parseLong(parts[4]));
                        }
                    }
                }

                if (!foundStats) {
                    boolean issueWritten = false;
                    for (String line : lines) {
                        if (line.contains("NO CONTRASTIVE EXPLANATION PROBLEMS")) {
                            writer.write(",,,,,NO CONTRASTIVE EXPLANATION PROBLEMS!\n");
                            issueWritten = true;
                            break;
                        }
                        if (line.contains("Ontology has more than 10000 axioms")) {
                            writer.write(",,,,,Ontology has more than 10000 axioms!\n");
                            issueWritten = true;
                            break;
                        }
                    }

                    if (!issueWritten) {
                        writer.write(",,,,,Unknown error or timeout\n");
                    }
                }

                // Summary stats
                if (!commonSizes.isEmpty()) {
                    writer.write("\nSummary Statistics:\n");

                    writer.write(String.format("stats-count,%d\n", statsCount));

                    writer.write(String.format("common-size(avg),%.2f\n", average(commonSizes)));
                    writer.write(String.format("common-size(min),%d\n", Collections.min(commonSizes)));
                    writer.write(String.format("common-size(max),%d\n", Collections.max(commonSizes)));
                    writer.write(String.format("common-size(var),%.2f\n", variance(commonSizes)));

                    writer.write(String.format("difference-size(avg),%.2f\n", average(differenceSizes)));
                    writer.write(String.format("difference-size(min),%d\n", Collections.min(differenceSizes)));
                    writer.write(String.format("difference-size(max),%d\n", Collections.max(differenceSizes)));
                    writer.write(String.format("difference-size(var),%.2f\n", variance(differenceSizes)));

                    writer.write(String.format("conflict-size(avg),%.2f\n", average(conflictSizes)));
                    writer.write(String.format("conflict-size(min),%d\n", Collections.min(conflictSizes)));
                    writer.write(String.format("conflict-size(max),%d\n", Collections.max(conflictSizes)));
                    writer.write(String.format("conflict-size(var),%.2f\n", variance(conflictSizes)));

                    writer.write(String.format("fresh-individuals(avg),%.2f\n", average(freshIndividuals)));
                    writer.write(String.format("fresh-individuals(min),%d\n", Collections.min(freshIndividuals)));
                    writer.write(String.format("fresh-individuals(max),%d\n", Collections.max(freshIndividuals)));
                    writer.write(String.format("fresh-individuals(var),%.2f\n", variance(freshIndividuals)));

                    writer.write(String.format("duration(avg),%.2f\n", averageLong(durations)));
                    writer.write(String.format("duration(min),%d\n", Collections.min(durations)));
                    writer.write(String.format("duration(max),%d\n", Collections.max(durations)));
                    writer.write(String.format("duration(var),%.2f\n", varianceLong(durations)));
                }

                System.out.println("CSV generated for: " + logFileName);
            }
        }
    }

    private static double average(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static double averageLong(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }


    private static double variance(List<Integer> values) {
        double avg = average(values);
        return values.stream()
                .mapToDouble(i -> Math.pow(i - avg, 2))
                .average()
                .orElse(0);
    }

    private static double varianceLong(List<Long> values) {
        double avg = averageLong(values);
        return values.stream()
                .mapToDouble(l -> Math.pow(l - avg, 2))
                .average()
                .orElse(0);
    }
}
