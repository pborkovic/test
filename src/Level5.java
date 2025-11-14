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
        String[] direct = generateOptimalPath(goalX, goalY);
        if (isSafe(direct[0], direct[1], asteroidX, asteroidY, goalX, goalY)) {
            return direct;
        }

        // Try detours with increasing offsets
        for (int offset = 3; offset <= 100; offset += 2) {
            // Try all 8 directions
            int[][] detours = {
                {0, offset},       // Up
                {0, -offset},      // Down
                {offset, 0},       // Right
                {-offset, 0},      // Left
                {offset, offset},  // Diagonal
                {-offset, offset},
                {offset, -offset},
                {-offset, -offset}
            };

            for (int[] detour : detours) {
                String[] path = generateDetourPath(goalX, goalY, detour[0], detour[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY)) {
                    return path;
                }
            }
        }

        // Fallback
        return direct;
    }

    private static String[] generateOptimalPath(int goalX, int goalY) {
        List<Integer> xPaces = generatePaceSequence(goalX);
        List<Integer> yPaces = generatePaceSequence(goalY);
        return padAndFormat(xPaces, yPaces);
    }

    private static List<Integer> generatePaceSequence(int distance) {
        List<Integer> paces = new ArrayList<>();
        paces.add(0); // Start at rest

        if (distance == 0) {
            paces.add(0);
            return paces;
        }

        int absDistance = Math.abs(distance);
        int direction = distance > 0 ? 1 : -1;

        // Acceleration sequence: 5,4,3,2,1 (getting faster - covers 1+1+1+1+1 = 5 units)
        // Deceleration sequence: 1,2,3,4,5 (getting slower - covers 1+1+1+1+1 = 5 units)
        // Total = 10 units

        if (absDistance <= 10) {
            // Short distance - partial acceleration/deceleration
            int accelDist = absDistance / 2;
            int decelDist = absDistance - accelDist;

            // Accelerate
            for (int i = 0; i < accelDist; i++) {
                int pace = Math.max(1, 5 - i);
                paces.add(pace * direction);
            }

            // Decelerate
            int startPace = Math.max(1, 6 - accelDist);
            for (int i = 0; i < decelDist; i++) {
                int pace = Math.min(5, startPace + i);
                paces.add(pace * direction);
            }
        } else {
            // Long distance - full accel, cruise, full decel
            // Accelerate: 5,4,3,2,1
            for (int pace = 5; pace >= 1; pace--) {
                paces.add(pace * direction);
            }

            // Cruise at pace 1 (fastest)
            int cruiseDist = absDistance - 10;
            for (int i = 0; i < cruiseDist; i++) {
                paces.add(1 * direction);
            }

            // Decelerate: 1,2,3,4,5
            for (int pace = 1; pace <= 5; pace++) {
                paces.add(pace * direction);
            }
        }

        paces.add(0); // End at rest
        return paces;
    }

    private static String[] generateDetourPath(int goalX, int goalY, int waypointX, int waypointY) {
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // Segment 1: Go to waypoint
        List<Integer> wx = generatePaceSequence(waypointX);
        List<Integer> wy = generatePaceSequence(waypointY);
        addSegment(xPaces, yPaces, wx, wy);

        // Segment 2: From waypoint to goal
        int remainingX = goalX - waypointX;
        int remainingY = goalY - waypointY;
        List<Integer> rx = generatePaceSequence(remainingX);
        List<Integer> ry = generatePaceSequence(remainingY);
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

    private static String[] padAndFormat(List<Integer> xPaces, List<Integer> yPaces) {
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
            // Update velocities (paces)
            if (step < xPaces.size()) vx = xPaces.get(step);
            else vx = 0;

            if (step < yPaces.size()) vy = yPaces.get(step);
            else vy = 0;

            // Simulate this pace step
            // Each pace value determines how many ticks before moving 1 unit
            int xSteps = Math.abs(vx) > 0 ? Math.abs(vx) : 1;
            int ySteps = Math.abs(vy) > 0 ? Math.abs(vy) : 1;
            int maxTicks = Math.max(xSteps, ySteps);

            for (int tick = 0; tick < maxTicks; tick++) {
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

                // Check collision after each tick
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
