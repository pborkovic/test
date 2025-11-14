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

        // Strategy 4: Try VERY small detours first (minimal offsets for tight time limits)
        for (int offset = 3; offset <= 10; offset += 1) {
            // Try tiny offsets in all directions
            int[][] micro = {
                {0, offset}, {0, -offset}, {offset, 0}, {-offset, 0},
                {offset, 0}, {-offset, 0}, {0, offset}, {0, -offset},
                {offset, offset}, {-offset, offset}, {offset, -offset}, {-offset, -offset}
            };
            for (int[] detour : micro) {
                String[] path = generateDetourPath(goalX, goalY, detour[0], detour[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 5: Try U-shaped detours with increasing offsets
        for (int offset = 3; offset <= 50; offset += 1) {
            // Try all four cardinal directions
            int[][] cardinals = {{0, offset}, {0, -offset}, {offset, 0}, {-offset, 0}};
            for (int[] detour : cardinals) {
                String[] path = generateUShapedPath(goalX, goalY, detour[0], detour[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 6: Try diagonal detours
        for (int offset = 3; offset <= 30; offset += 1) {
            int[][] diagonals = {
                {offset, offset}, {-offset, offset}, {offset, -offset}, {-offset, -offset}
            };
            for (int[] detour : diagonals) {
                String[] path = generateDetourPath(goalX, goalY, detour[0], detour[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 7: Try waypoints in a grid pattern around the asteroid
        int astToGoalDX = goalX - asteroidX;
        int astToGoalDY = goalY - asteroidY;

        // Try waypoints perpendicular to the asteroid-goal line
        for (int perp = 5; perp <= 25; perp += 2) {
            // Perpendicular offsets
            int[][] perps = {
                {asteroidX + perp, asteroidY},
                {asteroidX - perp, asteroidY},
                {asteroidX, asteroidY + perp},
                {asteroidX, asteroidY - perp},
                {asteroidX + perp, asteroidY + perp},
                {asteroidX - perp, asteroidY + perp},
                {asteroidX + perp, asteroidY - perp},
                {asteroidX - perp, asteroidY - perp}
            };
            for (int[] waypoint : perps) {
                String[] path = generateDetourPath(goalX, goalY, waypoint[0], waypoint[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 8: Try wide range of mixed detours
        for (int dx = -40; dx <= 40; dx += 4) {
            for (int dy = -40; dy <= 40; dy += 4) {
                if (dx == 0 && dy == 0) continue;
                // Skip if waypoint is too close to asteroid
                if (Math.abs(dx - asteroidX) <= 3 && Math.abs(dy - asteroidY) <= 3) continue;

                String[] path = generateDetourPath(goalX, goalY, dx, dy);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Fallback: Try emergency detours ignoring time limit
        // This is better than returning a colliding path
        for (int offset = 3; offset <= 80; offset += 2) {
            int[][] emergency = {
                {0, offset}, {0, -offset}, {offset, 0}, {-offset, 0},
                {offset, offset}, {-offset, offset}, {offset, -offset}, {-offset, -offset}
            };
            for (int[] detour : emergency) {
                String[] path = generateDetourPath(goalX, goalY, detour[0], detour[1]);
                // Check without time limit constraint
                if (isSafeWithoutTimeLimit(path[0], path[1], asteroidX, asteroidY, goalX, goalY)) {
                    return path;
                }
            }
        }

        // Last resort: return direct path
        return direct;
    }

    private static boolean isSafeWithoutTimeLimit(String xSeq, String ySeq, int asteroidX, int asteroidY,
                                                    int goalX, int goalY) {
        List<Integer> xPaces = parseSeq(xSeq);
        List<Integer> yPaces = parseSeq(ySeq);

        // Pad sequences to same length
        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);

        // Expand paces
        List<Integer> expandedX = new ArrayList<>();
        List<Integer> expandedY = new ArrayList<>();

        for (int pace : xPaces) {
            int repeats = Math.max(1, Math.abs(pace));
            for (int i = 0; i < repeats; i++) {
                expandedX.add(pace);
            }
        }

        for (int pace : yPaces) {
            int repeats = Math.max(1, Math.abs(pace));
            for (int i = 0; i < repeats; i++) {
                expandedY.add(pace);
            }
        }

        // Pad expanded sequences
        while (expandedX.size() < expandedY.size()) expandedX.add(0);
        while (expandedY.size() < expandedX.size()) expandedY.add(0);

        // Simulate
        int x = 0, y = 0;
        int ticksX = 0, ticksY = 0;

        // Check initial position
        if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
            return false;
        }

        for (int step = 0; step < expandedX.size(); step++) {
            int vx = expandedX.get(step);
            int vy = expandedY.get(step);

            // Process X axis
            if (Math.abs(vx) > 0 && Math.abs(vx) <= 5) {
                ticksX++;
                if (ticksX >= Math.abs(vx)) {
                    ticksX = 0;
                    x += (vx > 0) ? 1 : -1;
                }
            }

            // Process Y axis
            if (Math.abs(vy) > 0 && Math.abs(vy) <= 5) {
                ticksY++;
                if (ticksY >= Math.abs(vy)) {
                    ticksY = 0;
                    y += (vy > 0) ? 1 : -1;
                }
            }

            // Check collision
            if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
                return false;
            }
        }

        // Check if we reached the goal
        return x == goalX && y == goalY;
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
        return generatePaceSequence(distance, false);
    }

    private static List<Integer> generatePaceSequence(int distance, boolean fast) {
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

        if (fast) {
            // Fast mode: minimize time by using pace 1 (fastest) as much as possible
            // Only use minimal acceleration/deceleration
            paces.add(5 * direction);
            paces.add(4 * direction);
            paces.add(3 * direction);
            paces.add(2 * direction);

            // Use pace 1 for remaining distance
            for (int i = 0; i < absDistance - 8; i++) {
                paces.add(1 * direction);
            }

            paces.add(2 * direction);
            paces.add(3 * direction);
            paces.add(4 * direction);
            paces.add(5 * direction);
            paces.add(0);
            return paces;
        }

        // Calculate optimal min pace for this distance
        int minPace = (int) Math.ceil((11.0 - absDistance) / 2.0);
        if (minPace < 1) minPace = 1;
        if (minPace > 5) minPace = 5;

        int accelMoves = 5 - minPace + 1;
        int decelMoves = 5 - minPace;
        int cruiseMoves = absDistance - accelMoves - decelMoves;

        // Acceleration phase
        for (int pace = 5; pace >= minPace; pace--) {
            paces.add(pace * direction);
        }

        // Cruise phase
        for (int i = 0; i < cruiseMoves; i++) {
            paces.add(minPace * direction);
        }

        // Deceleration phase
        for (int pace = minPace + 1; pace <= 5; pace++) {
            paces.add(pace * direction);
        }

        paces.add(0);
        return paces;
    }

    private static void addSegment(List<Integer> xPaces, List<Integer> yPaces,
                                    List<Integer> xSeg, List<Integer> ySeg) {
        for (int i = 1; i < xSeg.size(); i++) xPaces.add(xSeg.get(i));
        for (int i = 1; i < ySeg.size(); i++) yPaces.add(ySeg.get(i));

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

        // Expand paces - each pace of value V is repeated abs(V) times (or 1 if V=0)
        List<Integer> expandedX = new ArrayList<>();
        List<Integer> expandedY = new ArrayList<>();

        for (int pace : xPaces) {
            int repeats = Math.max(1, Math.abs(pace));
            for (int i = 0; i < repeats; i++) {
                expandedX.add(pace);
            }
        }

        for (int pace : yPaces) {
            int repeats = Math.max(1, Math.abs(pace));
            for (int i = 0; i < repeats; i++) {
                expandedY.add(pace);
            }
        }

        // Pad expanded sequences
        while (expandedX.size() < expandedY.size()) expandedX.add(0);
        while (expandedY.size() < expandedX.size()) expandedY.add(0);

        // Simulate exactly as visualizer does: set velocity, then call move()
        int x = 0, y = 0;
        int ticksX = 0, ticksY = 0;

        // Check initial position
        if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
            return false;
        }

        // For each step in the expanded sequence
        for (int step = 0; step < expandedX.size(); step++) {
            int vx = expandedX.get(step);
            int vy = expandedY.get(step);

            // Simulate move() function from visualizer
            // Process X axis
            if (Math.abs(vx) > 0 && Math.abs(vx) <= 5) {
                ticksX++;
                if (ticksX >= Math.abs(vx)) {
                    ticksX = 0;
                    x += (vx > 0) ? 1 : -1;
                }
            }

            // Process Y axis
            if (Math.abs(vy) > 0 && Math.abs(vy) <= 5) {
                ticksY++;
                if (ticksY >= Math.abs(vy)) {
                    ticksY = 0;
                    y += (vy > 0) ? 1 : -1;
                }
            }

            // Check collision after move
            if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
                return false;
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
