import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Level2 {

    public static void main(String[] args) {
        try {
            String[] inputFiles = {
                    "level2_1_small.in",
                    "level2_2_large.in"
            };

            for (String inputFile : inputFiles) {
                if (Files.exists(Paths.get(inputFile))) {
                    processInputFile(inputFile);
                }
            }

            System.out.println("\n=== All Level 2 files processed successfully! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());

            e.printStackTrace();
        }
    }

    private static void processInputFile(String inputFile) throws IOException {
        String fileName = Paths.get(inputFile).getFileName().toString();
        String outputFileName = fileName.replace(".in", ".out");

        Path outputDir = Paths.get("outputs");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        String outputFile = outputDir.resolve(outputFileName).toString();
        List<String> results = processFile(inputFile);

        writeResults(outputFile, results);

        System.out.println("Output written to: " + outputFile);
    }

    private static List<String> processFile(String inputFile) throws IOException {
        List<String> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.trim().isEmpty()) {
                throw new IOException("Invalid input: empty file");
            }

            int n = Integer.parseInt(firstLine.trim());

            for (int i = 0; i < n; i++) {
                String line = reader.readLine();

                if (line == null) {
                    throw new IOException("Invalid input: expected " + n + " sequences, found only " + i);
                }

                SequenceResult result = calculateSequenceResult(line.trim());
                results.add(result.position + " " + result.time);
            }
        }

        return results;
    }

    private static class SequenceResult {
        long position;
        long time;

        SequenceResult(long position, long time) {
            this.position = position;
            this.time = time;
        }
    }

    private static SequenceResult calculateSequenceResult(String sequence) {
        if (sequence.isEmpty()) {
            return new SequenceResult(0, 0);
        }

        String[] paces = sequence.split("\\s+");
        long position = 0;
        long totalTime = 0;

        for (String paceStr : paces) {
            if (!paceStr.isEmpty()) {
                try {
                    int pace = Integer.parseInt(paceStr);

                    if (pace > 0) {
                        position += 1;
                        totalTime += pace;
                    } else if (pace < 0) {
                        position -= 1;
                        totalTime += Math.abs(pace);
                    } else {
                        totalTime += 1;
                    }

                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid pace format: " + paceStr);
                }
            }
        }

        return new SequenceResult(position, totalTime);
    }

    private static void writeResults(String outputFile, List<String> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String result : results) {
                writer.write(result);
                writer.newLine();
            }
        }
    }
}