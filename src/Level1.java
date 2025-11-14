import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Level1 {

    public static void main(String[] args) {
        try {
            // Determine which input file to process
            String inputFile = determineInputFile(args);
            String outputFile = inputFile.replace(".in", ".out");

            // Process the file
            List<String> results = processFile(inputFile);

            // Write results
            writeResults(outputFile, results);

            System.out.println("Processing complete!");
            System.out.println("Input: " + inputFile);
            System.out.println("Output: " + outputFile);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Determines which input file to process based on command line args or default
     */
    private static String determineInputFile(String[] args) {
        if (args.length > 0) {
            return args[0];
        }

        // Default: look for input files in current directory or uploads
        String[] possiblePaths = {
                "level1_0_example.in",
                "level1_1_small.in",
                "level1_2_large.in",
                "/mnt/user-data/uploads/level1_0_example.in",
                "/mnt/user-data/uploads/level1_1_small.in",
                "/mnt/user-data/uploads/level1_2_large.in"
        };

        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        throw new RuntimeException("No input file found. Please provide a file path as argument.");
    }

    /**
     * Processes the input file and calculates results
     */
    private static List<String> processFile(String inputFile) throws IOException {
        List<String> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            // Read number of sequences
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.trim().isEmpty()) {
                throw new IOException("Invalid input: empty file");
            }

            int n = Integer.parseInt(firstLine.trim());

            // Process each sequence
            for (int i = 0; i < n; i++) {
                String line = reader.readLine();

                if (line == null) {
                    throw new IOException("Invalid input: expected " + n + " sequences, found only " + i);
                }

                long totalTime = calculateSequenceTime(line.trim());
                results.add(String.valueOf(totalTime));
            }
        }

        return results;
    }

    /**
     * Calculates the total time for a sequence by summing all pace values
     */
    private static long calculateSequenceTime(String sequence) {
        if (sequence.isEmpty()) {
            return 0;
        }

        String[] paces = sequence.split("\\s+");
        long totalTime = 0;

        for (String paceStr : paces) {
            if (!paceStr.isEmpty()) {
                try {
                    int pace = Integer.parseInt(paceStr);
                    if (pace <= 0) {
                        throw new IllegalArgumentException("Invalid pace value: " + pace + " (must be > 0)");
                    }
                    totalTime += pace;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid pace format: " + paceStr);
                }
            }
        }

        return totalTime;
    }

    /**
     * Writes results to output file
     */
    private static void writeResults(String outputFile, List<String> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String result : results) {
                writer.write(result);
                writer.newLine();
            }
        }

        System.out.println("Results written to: " + outputFile);
    }
}