import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Level6 {

    public static void main(String[] args) {
        try {
            String[] inputFiles = {
                "level6_0_example.in",
                "level6_1_small.in",
                "level6_2_large.in"
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

                String[] sequences = findPath(goalX, goalY, asteroidX, asteroidY, timeLimit);
                results.add(sequences[0]);
                results.add(sequences[1]);

                if (i < n - 1) {
                    results.add("");
                }
            }
        }

        return results;
    }

    private static String[] findPath(int goalX, int goalY, int asteroidX, int asteroidY, int timeLimit) {
        // Strategy 1: Try direct diagonal path
        String[] direct = generateOptimalPath(goalX, goalY);
        if (isSafe(direct[0], direct[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
            return direct;
        }

        // Strategy 2: Try sequential X-first path
        String[] xFirst = generateSequentialPath(goalX, goalY, true);
        if (isSafe(xFirst[0], xFirst[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
            return xFirst;
        }

        // Strategy 3: Try sequential Y-first path
        String[] yFirst = generateSequentialPath(goalX, goalY, false);
        if (isSafe(yFirst[0], yFirst[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
            return yFirst;
        }

        // Strategy 4: Try small U-shaped detours (faster than large ones)
        for (int offset = 3; offset <= 15; offset++) {
            // Try all four cardinal directions
            String[] upPath = generateUShapedPath(goalX, goalY, 0, offset);
            if (isSafe(upPath[0], upPath[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                return upPath;
            }

            String[] downPath = generateUShapedPath(goalX, goalY, 0, -offset);
            if (isSafe(downPath[0], downPath[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                return downPath;
            }

            String[] rightPath = generateUShapedPath(goalX, goalY, offset, 0);
            if (isSafe(rightPath[0], rightPath[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                return rightPath;
            }

            String[] leftPath = generateUShapedPath(goalX, goalY, -offset, 0);
            if (isSafe(leftPath[0], leftPath[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                return leftPath;
            }
        }

        // Strategy 5: Try diagonal detours
        for (int offset = 3; offset <= 12; offset++) {
            int[][] detours = {
                {offset, offset}, {-offset, offset}, {offset, -offset}, {-offset, -offset}
            };

            for (int[] detour : detours) {
                String[] path = generateDetourPath(goalX, goalY, detour[0], detour[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 6: Try more complex mixed detours
        for (int dx = -10; dx <= 10; dx += 3) {
            for (int dy = -10; dy <= 10; dy += 3) {
                if (dx == 0 && dy == 0) continue;
                String[] path = generateDetourPath(goalX, goalY, dx, dy);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Fallback: return direct path even if not ideal
        return direct;
    }

    private static String[] generateOptimalPath(int goalX, int goalY) {
        List<Integer> xPaces = generatePaceSequence(goalX);
        List<Integer> yPaces = generatePaceSequence(goalY);
        return padAndFormat(xPaces, yPaces);
    }

    private static String[] generateSequentialPath(int goalX, int goalY, boolean xFirst) {
        List<Integer> xPaces = generatePaceSequence(goalX);
        List<Integer> yPaces = generatePaceSequence(goalY);

        if (xFirst) {
            // Do X movement first, then Y
            List<Integer> paddedY = new ArrayList<>();
            paddedY.add(0);
            for (int i = 1; i < xPaces.size(); i++) {
                paddedY.add(0);
            }
            for (int i = 1; i < yPaces.size(); i++) {
                paddedY.add(yPaces.get(i));
            }
            return new String[]{toSeqString(xPaces), toSeqString(paddedY)};
        } else {
            // Do Y movement first, then X
            List<Integer> paddedX = new ArrayList<>();
            paddedX.add(0);
            for (int i = 1; i < yPaces.size(); i++) {
                paddedX.add(0);
            }
            for (int i = 1; i < xPaces.size(); i++) {
                paddedX.add(xPaces.get(i));
            }
            return new String[]{toSeqString(paddedX), toSeqString(yPaces)};
        }
    }

    private static String[] generateUShapedPath(int goalX, int goalY, int detourX, int detourY) {
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // First leg: go to detour point
        List<Integer> dx = generatePaceSequence(detourX);
        List<Integer> dy = generatePaceSequence(detourY);
        addSegment(xPaces, yPaces, dx, dy);

        // Second leg: go from detour to goal
        List<Integer> gx = generatePaceSequence(goalX - detourX);
        List<Integer> gy = generatePaceSequence(goalY - detourY);
        addSegment(xPaces, yPaces, gx, gy);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
    }

    private static String[] generateDetourPath(int goalX, int goalY, int waypointX, int waypointY) {
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // First segment: origin to waypoint
        List<Integer> wx = generatePaceSequence(waypointX);
        List<Integer> wy = generatePaceSequence(waypointY);
        addSegment(xPaces, yPaces, wx, wy);

        // Second segment: waypoint to goal
        int remainingX = goalX - waypointX;
        int remainingY = goalY - waypointY;
        List<Integer> rx = generatePaceSequence(remainingX);
        List<Integer> ry = generatePaceSequence(remainingY);
        addSegment(xPaces, yPaces, rx, ry);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
    }

    private static List<Integer> generatePaceSequence(int distance) {
        List<Integer> paces = new ArrayList<>();
        paces.add(0);

        if (distance == 0) {
            paces.add(0);
            return paces;
        }

        int absDistance = Math.abs(distance);
        int direction = distance > 0 ? 1 : -1;

        // For very short distances
        if (absDistance <= 2) {
            for (int i = 0; i < absDistance; i++) {
                paces.add(5 * direction);
            }
            paces.add(0);
            return paces;
        }

        // Calculate optimal min pace for this distance
        // We want: accel_moves + cruise_moves + decel_moves = distance
        // accel_moves = (5 - minPace + 1), decel_moves = (5 - minPace)
        int minPace = (int) Math.ceil((11.0 - absDistance) / 2.0);
        if (minPace < 1) minPace = 1;
        if (minPace > 5) minPace = 5;

        int accelMoves = 5 - minPace + 1;  // How many moves during acceleration
        int decelMoves = 5 - minPace;       // How many moves during deceleration
        int cruiseMoves = absDistance - accelMoves - decelMoves;

        // Acceleration phase: 5, 4, 3, ... down to minPace
        for (int pace = 5; pace >= minPace; pace--) {
            paces.add(pace * direction);
        }

        // Cruise phase: maintain minPace
        for (int i = 0; i < cruiseMoves; i++) {
            paces.add(minPace * direction);
        }

        // Deceleration phase: minPace+1, minPace+2, ... up to 5
        for (int pace = minPace + 1; pace <= 5; pace++) {
            paces.add(pace * direction);
        }

        paces.add(0);
        return paces;
    }

    private static void addSegment(List<Integer> xPaces, List<Integer> yPaces,
                                    List<Integer> xSeg, List<Integer> ySeg) {
        // Skip the first 0 from segment (already have it)
        for (int i = 1; i < xSeg.size(); i++) xPaces.add(xSeg.get(i));
        for (int i = 1; i < ySeg.size(); i++) yPaces.add(ySeg.get(i));

        // Pad to equal length
        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);
    }

    private static String[] padAndFormat(List<Integer> xPaces, List<Integer> yPaces) {
        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);
        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
    }

    private static boolean isSafe(String xSeq, String ySeq, int asteroidX, int asteroidY,
                                   int goalX, int goalY, int timeLimit) {
        List<Integer> xPaces = parseSeq(xSeq);
        List<Integer> yPaces = parseSeq(ySeq);

        // Calculate total time
        int totalTime = calculateTotalTime(xPaces, yPaces);
        if (totalTime > timeLimit) {
            return false;
        }

        // Pad sequences to same length
        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);

        // Expand paces - each pace of value V lasts for abs(V) ticks (or 1 tick if V=0)
        List<Integer> expandedX = new ArrayList<>();
        List<Integer> expandedY = new ArrayList<>();

        for (int pace : xPaces) {
            int repeats = (pace == 0) ? 1 : Math.abs(pace);
            for (int i = 0; i < repeats; i++) {
                expandedX.add(pace);
            }
        }

        for (int pace : yPaces) {
            int repeats = (pace == 0) ? 1 : Math.abs(pace);
            for (int i = 0; i < repeats; i++) {
                expandedY.add(pace);
            }
        }

        // Pad expanded sequences
        while (expandedX.size() < expandedY.size()) expandedX.add(0);
        while (expandedY.size() < expandedX.size()) expandedY.add(0);

        // Simulate with tick-based movement
        int x = 0, y = 0;
        int tickX = 0, tickY = 0;

        // Check initial position
        if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
            return false;
        }

        for (int step = 0; step < expandedX.size(); step++) {
            int vx = expandedX.get(step);
            int vy = expandedY.get(step);

            // Process X movement
            if (vx != 0 && Math.abs(vx) >= 1 && Math.abs(vx) <= 5) {
                tickX++;
                if (tickX >= Math.abs(vx)) {
                    tickX = 0;
                    x += (vx > 0) ? 1 : -1;
                    // Check collision after movement
                    if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
                        return false;
                    }
                }
            } else {
                tickX = 0;
            }

            // Process Y movement
            if (vy != 0 && Math.abs(vy) >= 1 && Math.abs(vy) <= 5) {
                tickY++;
                if (tickY >= Math.abs(vy)) {
                    tickY = 0;
                    y += (vy > 0) ? 1 : -1;
                    // Check collision after movement
                    if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
                        return false;
                    }
                }
            } else {
                tickY = 0;
            }
        }

        // Check if we reached the goal
        return x == goalX && y == goalY;
    }

    private static int calculateTotalTime(List<Integer> xPaces, List<Integer> yPaces) {
        int maxSize = Math.max(xPaces.size(), yPaces.size());
        int totalTime = 0;

        for (int i = 0; i < maxSize; i++) {
            int vx = (i < xPaces.size()) ? xPaces.get(i) : 0;
            int vy = (i < yPaces.size()) ? yPaces.get(i) : 0;

            int xTime = (vx == 0) ? 1 : Math.abs(vx);
            int yTime = (vy == 0) ? 1 : Math.abs(vy);

            totalTime += Math.max(xTime, yTime);
        }

        return totalTime;
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
