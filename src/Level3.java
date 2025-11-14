import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Level3 {

    public static void main(String[] args) {
        try {
            String[] inputFiles = {
                    "level3_1_small.in",
                    "level3_2_large.in"
            };

            for (String inputFile : inputFiles) {
                if (Files.exists(Paths.get(inputFile))) {
                    processInputFile(inputFile);
                }
            }
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
                    throw new IOException("Invalid input: expected " + n + " cases, found only " + i);
                }

                String[] parts = line.trim().split("\\s+");
                int targetPosition = Integer.parseInt(parts[0]);
                int timeLimit = Integer.parseInt(parts[1]);

                String sequence = generateSequence(targetPosition, timeLimit);
                results.add(sequence);
            }
        }

        return results;
    }

    private static String generateSequence(int targetPosition, int timeLimit) {
        List<Integer> sequence = new ArrayList<>();
        sequence.add(0);

        if (targetPosition == 0) {
            sequence.add(0);
            return sequenceToString(sequence);
        }

        int absTarget = Math.abs(targetPosition);
        boolean forward = targetPosition > 0;

        int maxPace = forward ? 1 : -1;
        int minPace = forward ? 5 : -5;

        for (int accelTo = Math.abs(minPace); accelTo >= Math.abs(maxPace); accelTo--) {
            List<Integer> testSeq = new ArrayList<>();
            testSeq.add(0);

            int position = 0;
            int currentPace = 0;

            if (forward) {
                for (int p = 5; p >= accelTo; p--) {
                    testSeq.add(p);
                    position += 1;
                }
                currentPace = accelTo;
            } else {
                for (int p = -5; p <= -accelTo; p++) {
                    testSeq.add(p);
                    position -= 1;
                }
                currentPace = -accelTo;
            }

            int remaining = absTarget - Math.abs(position);

            int decelDist = 0;
            if (forward) {
                for (int p = accelTo + 1; p <= 5; p++) {
                    decelDist += 1;
                }
            } else {
                for (int p = -accelTo - 1; p >= -5; p--) {
                    decelDist += 1;
                }
            }

            if (remaining >= decelDist) {
                int maintainSteps = remaining - decelDist;
                for (int i = 0; i < maintainSteps; i++) {
                    testSeq.add(currentPace);
                    if (forward) position += 1;
                    else position -= 1;
                }

                if (forward) {
                    for (int p = accelTo + 1; p <= 5; p++) {
                        testSeq.add(p);
                        position += 1;
                    }
                } else {
                    for (int p = -accelTo - 1; p >= -5; p--) {
                        testSeq.add(p);
                        position -= 1;
                    }
                }

                testSeq.add(0);

                if (position == targetPosition) {
                    int time = calculateTime(testSeq);
                    if (time <= timeLimit) {
                        return sequenceToString(testSeq);
                    }
                }
            }
        }

        sequence.clear();
        sequence.add(0);
        if (forward) {
            for (int i = 0; i < absTarget; i++) {
                sequence.add(5);
            }
        } else {
            for (int i = 0; i < absTarget; i++) {
                sequence.add(-5);
            }
        }
        sequence.add(0);

        return sequenceToString(sequence);
    }

    private static int calculateTime(List<Integer> sequence) {
        int time = 0;
        for (int pace : sequence) {
            if (pace == 0) {
                time += 1;
            } else {
                time += Math.abs(pace);
            }
        }
        return time;
    }

    private static String sequenceToString(List<Integer> sequence) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sequence.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(sequence.get(i));
        }
        return sb.toString();
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