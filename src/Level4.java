import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Level4 {

    public static void main(String[] args) {
        try {
            String[] inputFiles = {
                    "level4_1_small.in",
                    "level4_2_large.in"
            };

            int filesProcessed = 0;
            for (String inputFile : inputFiles) {
                if (Files.exists(Paths.get(inputFile))) {
                    processInputFile(inputFile);
                    filesProcessed++;
                } else {
                    System.out.println("Warning: Input file not found: " + inputFile);
                }
            }

            if (filesProcessed == 0) {
                System.out.println("\n=== No Level 4 files found to process! ===");
            } else {
                System.out.println("\n=== All Level 4 files processed successfully! ===");
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

                // Parse: "X,Y timeLimit"
                String[] parts = line.trim().split("\\s+");
                String[] coords = parts[0].split(",");
                int targetX = Integer.parseInt(coords[0]);
                int targetY = Integer.parseInt(coords[1]);
                int timeLimit = Integer.parseInt(parts[1]);

                TwoDSequence sequences = generateOptimalSequences(targetX, targetY, timeLimit);

                results.add(sequences.xSequence);
                results.add(sequences.ySequence);

                if (i < n - 1) {
                    results.add("");
                }
            }
        }

        return results;
    }

    private static class TwoDSequence {
        String xSequence;
        String ySequence;
        int maxTime;

        TwoDSequence(String xSequence, String ySequence, int maxTime) {
            this.xSequence = xSequence;
            this.ySequence = ySequence;
            this.maxTime = maxTime;
        }
    }

    private static TwoDSequence generateOptimalSequences(int targetX, int targetY, int timeLimit) {
        String xSeq = generateOptimalSequence1D(targetX, timeLimit);
        String ySeq = generateOptimalSequence1D(targetY, timeLimit);
        int xTime = calculateSequenceTime(xSeq);
        int yTime = calculateSequenceTime(ySeq);

        return new TwoDSequence(xSeq, ySeq, Math.max(xTime, yTime));
    }

    private static String generateOptimalSequence1D(int target, int timeLimit) {
        if (target == 0) {
            return "0 0";
        }

        int absTarget = Math.abs(target);
        boolean forward = target > 0;

        for (int maxPace = 1; maxPace <= 5; maxPace++) {
            List<Integer> sequence = buildSequence(absTarget, forward, maxPace);
            int time = calculateSequenceTime(sequenceToString(sequence));

            if (time <= timeLimit) {
                return sequenceToString(sequence);
            }
        }

        List<Integer> fallback = new ArrayList<>();
        fallback.add(0);
        int pace = forward ? 5 : -5;

        for (int i = 0; i < absTarget; i++) {
            fallback.add(pace);
        }
        fallback.add(0);

        return sequenceToString(fallback);
    }

    private static List<Integer> buildSequence(int absTarget, boolean forward, int maxPace) {
        List<Integer> sequence = new ArrayList<>();
        sequence.add(0);

        if (absTarget == 0) {
            sequence.add(0);
            return sequence;
        }

        int accelSteps = 5 - maxPace;
        int decelSteps = accelSteps;
        int accelDistance = accelSteps;
        int decelDistance = decelSteps;

        if (accelDistance + decelDistance > absTarget) {
            return buildSimpleSequence(absTarget, forward);
        }

        int position = 0;

        if (forward) {
            for (int pace = 5; pace > maxPace; pace--) {
                sequence.add(pace);
                position++;
            }
        } else {
            for (int pace = -5; pace < -maxPace; pace++) {
                sequence.add(pace);
                position++;
            }
        }

        int remaining = absTarget - position;
        int cruiseSteps = remaining - decelDistance;

        if (cruiseSteps > 0) {
            int cruisePace = forward ? maxPace : -maxPace;
            for (int i = 0; i < cruiseSteps; i++) {
                sequence.add(cruisePace);
                position++;
            }
        }

        if (forward) {
            for (int pace = maxPace + 1; pace <= 5; pace++) {
                if (position < absTarget) {
                    sequence.add(pace);
                    position++;
                }
            }
        } else {
            for (int pace = -maxPace - 1; pace >= -5; pace--) {
                if (position < absTarget) {
                    sequence.add(pace);
                    position++;
                }
            }
        }

        sequence.add(0);
        return sequence;
    }

    private static List<Integer> buildSimpleSequence(int absTarget, boolean forward) {
        List<Integer> sequence = new ArrayList<>();
        sequence.add(0);

        if (absTarget == 0) {
            sequence.add(0);
            return sequence;
        }

        List<Integer> bestSequence = null;
        int bestTime = Integer.MAX_VALUE;

        for (int maxSpeed = 1; maxSpeed <= 5; maxSpeed++) {
            List<Integer> candidate = buildWithMaxSpeed(absTarget, forward, maxSpeed);
            int time = calculateSequenceTime(sequenceToString(candidate));

            int pos = calculatePosition(candidate);

            if (Math.abs(pos) == absTarget && time < bestTime) {
                bestTime = time;
                bestSequence = candidate;
            }
        }

        if (bestSequence != null) {
            return bestSequence;
        }

        int pace = forward ? 5 : -5;
        for (int i = 0; i < absTarget; i++) {
            sequence.add(pace);
        }
        sequence.add(0);

        return sequence;
    }

    private static List<Integer> buildWithMaxSpeed(int absTarget, boolean forward, int maxSpeed) {
        List<Integer> sequence = new ArrayList<>();
        sequence.add(0);

        int accelSteps = 5 - maxSpeed;
        int decelSteps = accelSteps;
        int minDistance = accelSteps + decelSteps;

        if (absTarget < minDistance) {
            return buildShortPattern(absTarget, forward);
        }

        int position = 0;

        if (forward) {
            for (int pace = 5; pace > maxSpeed; pace--) {
                sequence.add(pace);
                position++;
            }
        } else {
            for (int pace = -5; pace < -maxSpeed; pace++) {
                sequence.add(pace);
                position++;
            }
        }

        int remaining = absTarget - position;
        int cruiseSteps = remaining - decelSteps;

        if (cruiseSteps > 0) {
            int pace = forward ? maxSpeed : -maxSpeed;
            for (int i = 0; i < cruiseSteps; i++) {
                sequence.add(pace);
                position++;
            }
        }

        if (forward) {
            for (int pace = maxSpeed + 1; pace <= 5; pace++) {
                if (position < absTarget) {
                    sequence.add(pace);
                    position++;
                }
            }
        } else {
            for (int pace = -maxSpeed - 1; pace >= -5; pace--) {
                if (position < absTarget) {
                    sequence.add(pace);
                    position++;
                }
            }
        }

        sequence.add(0);
        return sequence;
    }

    private static List<Integer> buildShortPattern(int absTarget, boolean forward) {
        List<Integer> sequence = new ArrayList<>();
        sequence.add(0);

        if (absTarget == 1) {
            sequence.add(forward ? 5 : -5);
            sequence.add(0);
            return sequence;
        }

        if (absTarget == 2) {
            if (forward) {
                sequence.add(5);
                sequence.add(5);
            } else {
                sequence.add(-5);
                sequence.add(-5);
            }
            sequence.add(0);
            return sequence;
        }

        int halfDist = absTarget / 2;
        int accelTo = Math.max(1, 6 - halfDist);

        if (forward) {
            for (int pace = 5; pace > accelTo && sequence.size() - 1 < absTarget; pace--) {
                sequence.add(pace);
            }
            for (int pace = accelTo + 1; pace <= 5 && sequence.size() - 1 < absTarget; pace++) {
                sequence.add(pace);
            }
        } else {
            for (int pace = -5; pace < -accelTo && sequence.size() - 1 < absTarget; pace++) {
                sequence.add(pace);
            }
            for (int pace = -accelTo - 1; pace >= -5 && sequence.size() - 1 < absTarget; pace--) {
                sequence.add(pace);
            }
        }

        sequence.add(0);

        return sequence;
    }

    private static int calculatePosition(List<Integer> sequence) {
        int pos = 0;
        for (int pace : sequence) {
            if (pace > 0) pos++;
            else if (pace < 0) pos--;
        }

        return pos;
    }

    private static int calculateSequenceTime(String sequence) {
        if (sequence.isEmpty()) return 0;

        String[] paces = sequence.trim().split("\\s+");
        int time = 0;

        for (String paceStr : paces) {
            int pace = Integer.parseInt(paceStr);
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