import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Level5 {

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

                String[] asteroidCoords = asteroidLine.trim().split(",");
                int asteroidX = Integer.parseInt(asteroidCoords[0]);
                int asteroidY = Integer.parseInt(asteroidCoords[1]);

                String[] sequences = findPath(goalX, goalY, asteroidX, asteroidY);
                results.add(sequences[0]);
                results.add(sequences[1]);

                if (i < n - 1) {
                    results.add("");
                }
            }
        }

        return results;
    }

    private static String[] findPath(int goalX, int goalY, int asteroidX, int asteroidY) {
        // Try direct path first
        String[] direct = generateDirectPath(goalX, goalY);
        if (isSafe(direct[0], direct[1], asteroidX, asteroidY, goalX, goalY)) {
            return direct;
        }

        // Need to detour around asteroid
        // Try going around in different directions
        int[] detourOffsets = {3, 4, 5, 6, 7, 8, 10, 15, 20, 30, 50, 100};

        for (int offset : detourOffsets) {
            // Try going UP first
            String[] path = generateDetourPath(goalX, goalY, 0, offset, asteroidX, asteroidY);
            if (path != null && isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY)) {
                return path;
            }

            // Try going DOWN first
            path = generateDetourPath(goalX, goalY, 0, -offset, asteroidX, asteroidY);
            if (path != null && isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY)) {
                return path;
            }

            // Try going RIGHT first
            path = generateDetourPath(goalX, goalY, offset, 0, asteroidX, asteroidY);
            if (path != null && isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY)) {
                return path;
            }

            // Try going LEFT first
            path = generateDetourPath(goalX, goalY, -offset, 0, asteroidX, asteroidY);
            if (path != null && isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY)) {
                return path;
            }
        }

        // Fallback - return direct path (shouldn't happen with generous time)
        return direct;
    }

    private static String[] generateDirectPath(int goalX, int goalY) {
        String xSeq = generateSequence(goalX);
        String ySeq = generateSequence(goalY);
        return padSequences(xSeq, ySeq);
    }

    private static String[] generateDetourPath(int goalX, int goalY, int waypointX, int waypointY,
                                              int asteroidX, int asteroidY) {
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // Segment 1: Go to waypoint
        List<Integer> wx = parseSeq(generateSequence(waypointX));
        List<Integer> wy = parseSeq(generateSequence(waypointY));
        addSegment(xPaces, yPaces, wx, wy);

        // Segment 2: From waypoint to goal
        int remainingX = goalX - waypointX;
        int remainingY = goalY - waypointY;
        List<Integer> rx = parseSeq(generateSequence(remainingX));
        List<Integer> ry = parseSeq(generateSequence(remainingY));
        addSegment(xPaces, yPaces, rx, ry);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
    }

    private static void addSegment(List<Integer> xPaces, List<Integer> yPaces,
                                    List<Integer> xSeg, List<Integer> ySeg) {
        // Skip initial 0 from segments
        for (int i = 1; i < xSeg.size(); i++) xPaces.add(xSeg.get(i));
        for (int i = 1; i < ySeg.size(); i++) yPaces.add(ySeg.get(i));

        // Pad to same length
        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);
    }

    private static String generateSequence(int distance) {
        if (distance == 0) {
            return "0 0";
        }

        StringBuilder sb = new StringBuilder("0");
        int absDistance = Math.abs(distance);
        int pace = distance > 0 ? 5 : -5;

        for (int i = 0; i < absDistance; i++) {
            sb.append(" ").append(pace);
        }
        sb.append(" 0");

        return sb.toString();
    }

    private static String[] padSequences(String xSeq, String ySeq) {
        List<Integer> xPaces = parseSeq(xSeq);
        List<Integer> yPaces = parseSeq(ySeq);

        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
    }

    private static boolean isSafe(String xSeq, String ySeq, int asteroidX, int asteroidY,
                                   int goalX, int goalY) {
        List<Integer> xPaces = parseSeq(xSeq);
        List<Integer> yPaces = parseSeq(ySeq);

        int x = 0, y = 0;
        int vx = 0, vy = 0;
        int tickX = 0, tickY = 0;

        // Check initial position
        if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
            return false;
        }

        int maxSteps = Math.max(xPaces.size(), yPaces.size());

        for (int step = 0; step < maxSteps; step++) {
            // Update velocities
            if (step < xPaces.size()) vx = xPaces.get(step);
            else vx = 0;

            if (step < yPaces.size()) vy = yPaces.get(step);
            else vy = 0;

            // Simulate movement for each pace occurrence
            int xRepeat = Math.max(1, Math.abs(vx));
            int yRepeat = Math.max(1, Math.abs(vy));
            int maxRepeat = Math.max(xRepeat, yRepeat);

            for (int tick = 0; tick < maxRepeat; tick++) {
                // X movement
                if (vx != 0) {
                    tickX++;
                    if (tickX >= Math.abs(vx)) {
                        tickX = 0;
                        x += (vx > 0) ? 1 : -1;
                    }
                } else {
                    tickX = 0;
                }

                // Y movement
                if (vy != 0) {
                    tickY++;
                    if (tickY >= Math.abs(vy)) {
                        tickY = 0;
                        y += (vy > 0) ? 1 : -1;
                    }
                } else {
                    tickY = 0;
                }

                // Check collision
                if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
                    return false;
                }
            }
        }

        // Verify we reached the goal and stopped
        return x == goalX && y == goalY && vx == 0 && vy == 0;
    }

    private static List<Integer> parseSeq(String seq) {
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

    private static String toSeqString(List<Integer> seq) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seq.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(seq.get(i));
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
