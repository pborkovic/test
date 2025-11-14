import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Level5 {

    private static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class State {
        int x, y;
        int vx, vy;

        State(int x, int y, int vx, int vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    public static void main(String[] args) {
        try {
            String[] inputFiles = {
                "level5_0_example.in",
                "level5_1_small.in",
                "level5_2_large.in"
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
            int n = Integer.parseInt(firstLine.trim());

            for (int i = 0; i < n; i++) {
                String goalLine = reader.readLine();
                String asteroidLine = reader.readLine();

                String[] goalParts = goalLine.trim().split("\\s+");
                String[] goalCoords = goalParts[0].split(",");
                int goalX = Integer.parseInt(goalCoords[0]);
                int goalY = Integer.parseInt(goalCoords[1]);
                int timeLimit = Integer.parseInt(goalParts[1]);

                String[] asteroidCoords = asteroidLine.trim().split(",");
                int asteroidX = Integer.parseInt(asteroidCoords[0]);
                int asteroidY = Integer.parseInt(asteroidCoords[1]);

                Point goal = new Point(goalX, goalY);
                Point asteroid = new Point(asteroidX, asteroidY);

                String[] sequences = findSafePath(goal, asteroid, timeLimit);
                results.add(sequences[0]);
                results.add(sequences[1]);

                if (i < n - 1) {
                    results.add("");
                }
            }
        }

        return results;
    }

    private static String[] findSafePath(Point goal, Point asteroid, int timeLimit) {
        // Try direct path first
        String[] direct = generateDirectPath(goal, timeLimit);
        if (isPathSafe(direct, asteroid, goal)) {
            return direct;
        }

        // Try various detour strategies
        // Strategy 1: Vertical detours
        for (int detour : new int[]{3, 4, 5, 6, 7, -3, -4, -5, -6, -7, 8, 9, 10, -8, -9, -10}) {
            String[] path = generateVerticalDetour(goal, asteroid, timeLimit, detour);
            if (path != null && isPathSafe(path, asteroid, goal)) {
                return path;
            }
        }

        // Strategy 2: Horizontal detours
        for (int detour : new int[]{3, 4, 5, 6, 7, -3, -4, -5, -6, -7, 8, 9, 10, -8, -9, -10}) {
            String[] path = generateHorizontalDetour(goal, asteroid, timeLimit, detour);
            if (path != null && isPathSafe(path, asteroid, goal)) {
                return path;
            }
        }

        // Fallback: return direct path
        return direct;
    }

    private static String[] generateDirectPath(Point goal, int timeLimit) {
        String xSeq = generateSequence1D(goal.x, timeLimit);
        String ySeq = generateSequence1D(goal.y, timeLimit);
        return padSequences(xSeq, ySeq);
    }

    private static String[] generateVerticalDetour(Point goal, Point asteroid, int timeLimit, int detourY) {
        List<Integer> xSeq = new ArrayList<>();
        List<Integer> ySeq = new ArrayList<>();

        xSeq.add(0);
        ySeq.add(0);

        // Step 1: Move vertically to detour position
        List<Integer> vertSeq = parseSequence(generateSequence1D(detourY, timeLimit));
        addSequencePart(ySeq, vertSeq);
        padSequence(xSeq, vertSeq.size() - 1);

        // Step 2: Move horizontally to goal.x
        List<Integer> horizSeq = parseSequence(generateSequence1D(goal.x, timeLimit));
        addSequencePart(xSeq, horizSeq);
        padSequence(ySeq, horizSeq.size() - 1);

        // Step 3: Adjust Y to reach goal.y
        int remainingY = goal.y - detourY;
        List<Integer> finalYSeq = parseSequence(generateSequence1D(remainingY, timeLimit));
        addSequencePart(ySeq, finalYSeq);
        padSequence(xSeq, finalYSeq.size() - 1);

        return new String[]{sequenceToString(xSeq), sequenceToString(ySeq)};
    }

    private static String[] generateHorizontalDetour(Point goal, Point asteroid, int timeLimit, int detourX) {
        List<Integer> xSeq = new ArrayList<>();
        List<Integer> ySeq = new ArrayList<>();

        xSeq.add(0);
        ySeq.add(0);

        // Step 1: Move horizontally to detour position
        List<Integer> horizSeq = parseSequence(generateSequence1D(detourX, timeLimit));
        addSequencePart(xSeq, horizSeq);
        padSequence(ySeq, horizSeq.size() - 1);

        // Step 2: Move vertically to goal.y
        List<Integer> vertSeq = parseSequence(generateSequence1D(goal.y, timeLimit));
        addSequencePart(ySeq, vertSeq);
        padSequence(xSeq, vertSeq.size() - 1);

        // Step 3: Adjust X to reach goal.x
        int remainingX = goal.x - detourX;
        List<Integer> finalXSeq = parseSequence(generateSequence1D(remainingX, timeLimit));
        addSequencePart(xSeq, finalXSeq);
        padSequence(ySeq, finalXSeq.size() - 1);

        return new String[]{sequenceToString(xSeq), sequenceToString(ySeq)};
    }

    private static void addSequencePart(List<Integer> target, List<Integer> source) {
        // Skip the first 0 from source (it's the starting pace)
        for (int i = 1; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    private static void padSequence(List<Integer> seq, int count) {
        for (int i = 0; i < count; i++) {
            seq.add(0);
        }
    }

    private static String[] padSequences(String xSeq, String ySeq) {
        List<Integer> xPaces = parseSequence(xSeq);
        List<Integer> yPaces = parseSequence(ySeq);

        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);

        return new String[]{sequenceToString(xPaces), sequenceToString(yPaces)};
    }

    private static boolean isPathSafe(String[] sequences, Point asteroid, Point goal) {
        String xSeq = sequences[0];
        String ySeq = sequences[1];

        List<Integer> xPaces = parseSequence(xSeq);
        List<Integer> yPaces = parseSequence(ySeq);

        // Expand paces (each pace value repeats abs(pace) times, or 1 if pace is 0)
        List<Integer> xExpanded = expandPaces(xPaces);
        List<Integer> yExpanded = expandPaces(yPaces);

        // Pad to same length
        while (xExpanded.size() < yExpanded.size()) xExpanded.add(0);
        while (yExpanded.size() < xExpanded.size()) yExpanded.add(0);

        State state = new State(0, 0, 0, 0);
        int tickX = 0;
        int tickY = 0;

        // Simulate each step using tick-based movement
        for (int step = 0; step < xExpanded.size(); step++) {
            state.vx = xExpanded.get(step);
            state.vy = yExpanded.get(step);

            // X movement with tick system
            if (Math.abs(state.vx) > 0 && Math.abs(state.vx) <= 5) {
                tickX++;
                if (tickX >= Math.abs(state.vx)) {
                    tickX = 0;
                    state.x += (state.vx > 0) ? 1 : -1;
                }
            }

            // Y movement with tick system
            if (Math.abs(state.vy) > 0 && Math.abs(state.vy) <= 5) {
                tickY++;
                if (tickY >= Math.abs(state.vy)) {
                    tickY = 0;
                    state.y += (state.vy > 0) ? 1 : -1;
                }
            }

            // Check collision with asteroid (safety radius of 2)
            if (Math.abs(state.x - asteroid.x) <= 2 && Math.abs(state.y - asteroid.y) <= 2) {
                return false; // Collision!
            }
        }

        // Verify we reached the goal and stopped
        return state.x == goal.x && state.y == goal.y && state.vx == 0 && state.vy == 0;
    }

    private static List<Integer> expandPaces(List<Integer> paces) {
        List<Integer> expanded = new ArrayList<>();
        for (int pace : paces) {
            int count = Math.max(1, Math.abs(pace));
            for (int i = 0; i < count; i++) {
                expanded.add(pace);
            }
        }
        return expanded;
    }

    private static List<Integer> parseSequence(String seq) {
        List<Integer> result = new ArrayList<>();
        if (seq == null || seq.trim().isEmpty()) {
            return result;
        }
        String[] parts = seq.trim().split("\\s+");
        for (String part : parts) {
            result.add(Integer.parseInt(part));
        }
        return result;
    }

    private static String generateSequence1D(int target, int timeLimit) {
        if (target == 0) {
            return "0 0";
        }

        int absTarget = Math.abs(target);
        boolean forward = target > 0;

        // Try different maximum paces (1 is fastest, 5 is slowest)
        for (int maxPace = 1; maxPace <= 5; maxPace++) {
            List<Integer> sequence = buildSequence(absTarget, forward, maxPace);
            int time = calculateSequenceTime(sequenceToString(sequence));

            if (time <= timeLimit) {
                return sequenceToString(sequence);
            }
        }

        // Fallback: use slowest pace (5)
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

        if (accelSteps + decelSteps > absTarget) {
            return buildShortSequence(absTarget, forward);
        }

        int position = 0;

        // Acceleration phase
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

        // Cruise phase
        int remaining = absTarget - position;
        int cruiseSteps = remaining - decelSteps;

        if (cruiseSteps > 0) {
            int cruisePace = forward ? maxPace : -maxPace;
            for (int i = 0; i < cruiseSteps; i++) {
                sequence.add(cruisePace);
                position++;
            }
        }

        // Deceleration phase
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

    private static List<Integer> buildShortSequence(int absTarget, boolean forward) {
        List<Integer> sequence = new ArrayList<>();
        sequence.add(0);

        if (absTarget == 0) {
            sequence.add(0);
            return sequence;
        }

        if (absTarget == 1) {
            sequence.add(forward ? 5 : -5);
            sequence.add(0);
            return sequence;
        }

        if (absTarget == 2) {
            sequence.add(forward ? 5 : -5);
            sequence.add(forward ? 5 : -5);
            sequence.add(0);
            return sequence;
        }

        // For short distances, accelerate and decelerate
        int halfDist = absTarget / 2;
        int accelTo = Math.max(1, 6 - halfDist);

        int position = 0;

        if (forward) {
            for (int pace = 5; pace > accelTo && position < absTarget; pace--) {
                sequence.add(pace);
                position++;
            }
            for (int pace = accelTo + 1; pace <= 5 && position < absTarget; pace++) {
                sequence.add(pace);
                position++;
            }
        } else {
            for (int pace = -5; pace < -accelTo && position < absTarget; pace++) {
                sequence.add(pace);
                position++;
            }
            for (int pace = -accelTo - 1; pace >= -5 && position < absTarget; pace--) {
                sequence.add(pace);
                position++;
            }
        }

        sequence.add(0);
        return sequence;
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
