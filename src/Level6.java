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
        String[] direct = generateOptimalPath(goalX, goalY, false);
        if (isSafe(direct[0], direct[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
            return direct;
        }

        // Strategy 2: Try direct path with FAST mode
        String[] directFast = generateOptimalPath(goalX, goalY, true);
        if (isSafe(directFast[0], directFast[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
            return directFast;
        }

        // Strategy 3: Try sequential X-first and Y-first paths
        String[] xFirst = generateSequentialPath(goalX, goalY, true, true);
        if (isSafe(xFirst[0], xFirst[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
            return xFirst;
        }

        String[] yFirst = generateSequentialPath(goalX, goalY, false, true);
        if (isSafe(yFirst[0], yFirst[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
            return yFirst;
        }

        // Strategy 3.5: Try specific calculated waypoints for edge cases
        // When goal is very close to collision zone, try going past in one dimension then returning
        int[][] calculatedWaypoints = {
            // Go past goal in X, stay at goal Y
            {goalX + 1, goalY}, {goalX + 2, goalY}, {goalX + 3, goalY}, {goalX + 5, goalY},
            {goalX - 1, goalY}, {goalX - 2, goalY}, {goalX - 3, goalY}, {goalX - 5, goalY},
            // Go past goal in Y, stay at goal X
            {goalX, goalY + 1}, {goalX, goalY + 2}, {goalX, goalY + 3}, {goalX, goalY + 5},
            {goalX, goalY - 1}, {goalX, goalY - 2}, {goalX, goalY - 3}, {goalX, goalY - 5},
            // Go past collision zone then to goal
            {asteroidX - 6, goalY}, {asteroidX + 6, goalY}, {goalX, asteroidY - 6}, {goalX, asteroidY + 6},
            {asteroidX - 8, asteroidY}, {asteroidX + 8, asteroidY}, {asteroidX, asteroidY - 8}, {asteroidX, asteroidY + 8}
        };

        for (int[] waypoint : calculatedWaypoints) {
            int wx = waypoint[0];
            int wy = waypoint[1];
            // Allow all waypoints, let isSafe determine collision

            String[] path = generateFastDetourPath(goalX, goalY, wx, wy);
            if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                return path;
            }
        }

        // Strategy 3.6: Try geometrically optimal detours around the asteroid
        // Calculate perpendicular offsets to the direct path
        double goalDist = Math.sqrt(goalX * goalX + goalY * goalY);
        if (goalDist > 0) {
            // Perpendicular direction to direct path
            double perpX = -goalY / goalDist;
            double perpY = goalX / goalDist;

            // Try small perpendicular offsets from the direct path
            for (int offset = 3; offset <= 20; offset++) {
                int[][] perpOffsets = {
                    {(int)(perpX * offset), (int)(perpY * offset)},
                    {(int)(-perpX * offset), (int)(-perpY * offset)},
                    {(int)(perpX * offset * 1.5), (int)(perpY * offset * 1.5)},
                    {(int)(-perpX * offset * 1.5), (int)(-perpY * offset * 1.5)},
                    {(int)(perpX * offset * 2), (int)(perpY * offset * 2)},
                    {(int)(-perpX * offset * 2), (int)(-perpY * offset * 2)}
                };

                for (int[] perp : perpOffsets) {
                    // Try waypoint at midpoint of path
                    int midX = goalX / 2 + perp[0];
                    int midY = goalY / 2 + perp[1];
                    String[] path = generateFastDetourPath(goalX, goalY, midX, midY);
                    if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                        return path;
                    }

                    // Try waypoint at asteroid position + offset
                    int astX = asteroidX + perp[0];
                    int astY = asteroidY + perp[1];
                    path = generateFastDetourPath(goalX, goalY, astX, astY);
                    if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                        return path;
                    }

                    // Try waypoint at goal position + offset
                    int gx = goalX + perp[0];
                    int gy = goalY + perp[1];
                    path = generateFastDetourPath(goalX, goalY, gx, gy);
                    if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                        return path;
                    }
                }
            }
        }

        // Strategy 4: Ultra-fine search around asteroid bypass points
        // Try waypoints just before/after asteroid in both axes
        for (int xOffset = -10; xOffset <= 10; xOffset++) {
            for (int yOffset = -10; yOffset <= 10; yOffset++) {
                // Waypoints relative to asteroid
                int[] waypoints = {
                    asteroidX - 5 + xOffset, asteroidY + yOffset,
                    asteroidX + 5 + xOffset, asteroidY + yOffset,
                    asteroidX + xOffset, asteroidY - 5 + yOffset,
                    asteroidX + xOffset, asteroidY + 5 + yOffset
                };

                for (int i = 0; i < waypoints.length; i += 2) {
                    int wx = waypoints[i];
                    int wy = waypoints[i + 1];
                    // Allow all waypoints, let isSafe determine collision

                    String[] path = generateFastDetourPath(goalX, goalY, wx, wy);
                    if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                        return path;
                    }
                }
            }
        }

        // Strategy 4b: Try three-waypoint paths for smoother detours
        // This can be more efficient than sharp turns, especially for tight cases
        for (int off1 = 1; off1 <= 30; off1 += 2) {
            for (int off2 = 1; off2 <= 30; off2 += 2) {
                // Try many three-point combinations around asteroid and goal
                int[][] threePointRoutes = {
                    // Around asteroid horizontally
                    {asteroidX - off1, asteroidY, asteroidX + off2, asteroidY},
                    {asteroidX + off1, asteroidY, asteroidX - off2, asteroidY},
                    {asteroidX - off1, asteroidY - off2, goalX, goalY},
                    {asteroidX + off1, asteroidY + off2, goalX, goalY},
                    // Around asteroid vertically
                    {asteroidX, asteroidY - off1, asteroidX, asteroidY + off2},
                    {asteroidX, asteroidY + off1, asteroidX, asteroidY - off2},
                    {asteroidX - off2, asteroidY - off1, goalX, goalY},
                    {asteroidX + off2, asteroidY + off1, goalX, goalY},
                    // Via goal with offset
                    {goalX + off1, goalY, goalX, goalY + off2},
                    {goalX - off1, goalY, goalX, goalY - off2},
                    {goalX, goalY + off1, goalX + off2, goalY},
                    {goalX, goalY - off1, goalX - off2, goalY},
                    // General offsets
                    {off1, 0, goalX - off1, 0},
                    {-off1, 0, goalX + off1, 0},
                    {0, off1, 0, goalY - off1},
                    {0, -off1, 0, goalY + off1}
                };

                for (int i = 0; i < threePointRoutes.length; i += 2) {
                    int wp1X = threePointRoutes[i][0];
                    int wp1Y = threePointRoutes[i][1];
                    int wp2X = threePointRoutes[i + 1][0];
                    int wp2Y = threePointRoutes[i + 1][1];

                    if (Math.abs(wp1X - asteroidX) <= 3 && Math.abs(wp1Y - asteroidY) <= 3) continue;
                    if (Math.abs(wp2X - asteroidX) <= 3 && Math.abs(wp2Y - asteroidY) <= 3) continue;

                    String[] path = generateThreeWaypointPath(goalX, goalY, wp1X, wp1Y, wp2X, wp2Y);
                    if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                        return path;
                    }
                }
            }
        }

        // Strategy 5: Try minimal offsets systematically with FAST mode
        // This is critical for tight time limits - try every single offset from 1 to 50
        for (int offset = 1; offset <= 50; offset++) {
            // Try all 8 directions with this offset
            int[][] directions = {
                {offset, 0}, {-offset, 0}, {0, offset}, {0, -offset},
                {offset, offset}, {-offset, offset}, {offset, -offset}, {-offset, -offset},
                {offset*2, offset}, {-offset*2, offset}, {offset*2, -offset}, {-offset*2, -offset},
                {offset, offset*2}, {-offset, offset*2}, {offset, -offset*2}, {-offset, -offset*2}
            };

            for (int[] detour : directions) {
                String[] path = generateFastDetourPath(goalX, goalY, detour[0], detour[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 5: Try waypoints around the asteroid systematically
        // Place waypoints in a grid pattern around the asteroid
        for (int offset = 5; offset <= 40; offset++) {
            // Try waypoints at various positions relative to asteroid
            int[][] waypoints = {
                // Cardinal directions from asteroid
                {asteroidX + offset, asteroidY},
                {asteroidX - offset, asteroidY},
                {asteroidX, asteroidY + offset},
                {asteroidX, asteroidY - offset},
                // Diagonal directions from asteroid
                {asteroidX + offset, asteroidY + offset},
                {asteroidX - offset, asteroidY + offset},
                {asteroidX + offset, asteroidY - offset},
                {asteroidX - offset, asteroidY - offset},
                // Extended positions
                {asteroidX + offset, asteroidY + offset/2},
                {asteroidX - offset, asteroidY + offset/2},
                {asteroidX + offset, asteroidY - offset/2},
                {asteroidX - offset, asteroidY - offset/2},
                {asteroidX + offset/2, asteroidY + offset},
                {asteroidX - offset/2, asteroidY + offset},
                {asteroidX + offset/2, asteroidY - offset},
                {asteroidX - offset/2, asteroidY - offset}
            };

            for (int[] waypoint : waypoints) {
                String[] path = generateFastDetourPath(goalX, goalY, waypoint[0], waypoint[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 6: Try U-shaped paths with fast mode
        for (int offset = 1; offset <= 60; offset++) {
            int[][] detours = {
                {0, offset}, {0, -offset}, {offset, 0}, {-offset, 0},
                {offset, offset}, {-offset, offset}, {offset, -offset}, {-offset, -offset}
            };
            for (int[] detour : detours) {
                String[] path = generateFastUShapedPath(goalX, goalY, detour[0], detour[1]);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 7: ULTRA FINE-GRAINED grid search with step 1
        // Critical for cases where optimal waypoint is at odd coordinates
        // Expand search range for better coverage
        int minWx = Math.min(0, Math.min(goalX, asteroidX)) - 50;
        int maxWx = Math.max(0, Math.max(goalX, asteroidX)) + 50;
        int minWy = Math.min(0, Math.min(goalY, asteroidY)) - 50;
        int maxWy = Math.max(0, Math.max(goalY, asteroidY)) + 50;

        for (int wx = minWx; wx <= maxWx; wx++) {
            for (int wy = minWy; wy <= maxWy; wy++) {
                // Skip if waypoint is origin or goal
                if ((wx == 0 && wy == 0) || (wx == goalX && wy == goalY)) continue;

                // Skip if waypoint is too close to asteroid
                // Allow all waypoints, let isSafe determine collision

                String[] path = generateFastDetourPath(goalX, goalY, wx, wy);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 7.5: Try with non-fast mode for comparison (might be better for certain distances)
        for (int wx = minWx; wx <= maxWx; wx += 2) {
            for (int wy = minWy; wy <= maxWy; wy += 2) {
                if ((wx == 0 && wy == 0) || (wx == goalX && wy == goalY)) continue;
                // Allow all waypoints, let isSafe determine collision

                String[] path = generateDetourPathMixed(goalX, goalY, wx, wy);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 7b: Try comprehensive grid search with fast mode (coarser)
        // This is more expensive but tries many combinations
        for (int wx = -60; wx <= Math.max(60, goalX + 20); wx += 2) {
            for (int wy = -60; wy <= Math.max(60, goalY + 20); wy += 2) {
                // Skip if waypoint is origin or goal
                if ((wx == 0 && wy == 0) || (wx == goalX && wy == goalY)) continue;

                // Skip if waypoint is too close to asteroid
                // Allow all waypoints, let isSafe determine collision

                String[] path = generateFastDetourPath(goalX, goalY, wx, wy);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Strategy 8: Try wider grid search with fast mode
        for (int wx = -100; wx <= Math.max(100, goalX + 40); wx += 5) {
            for (int wy = -100; wy <= Math.max(100, goalY + 40); wy += 5) {
                if ((wx == 0 && wy == 0) || (wx == goalX && wy == goalY)) continue;
                // Allow all waypoints, let isSafe determine collision

                String[] path = generateFastDetourPath(goalX, goalY, wx, wy);
                if (isSafe(path[0], path[1], asteroidX, asteroidY, goalX, goalY, timeLimit)) {
                    return path;
                }
            }
        }

        // Fallback: Try without time limit constraint
        for (int offset = 1; offset <= 100; offset += 2) {
            int[][] emergency = {
                {offset, 0}, {-offset, 0}, {0, offset}, {0, -offset},
                {offset, offset}, {-offset, offset}, {offset, -offset}, {-offset, -offset}
            };
            for (int[] detour : emergency) {
                String[] path = generateFastDetourPath(goalX, goalY, detour[0], detour[1]);
                if (isSafeWithoutTimeLimit(path[0], path[1], asteroidX, asteroidY, goalX, goalY)) {
                    return path;
                }
            }
        }

        // Last resort: return direct fast path
        return directFast;
    }

    private static boolean isSafeWithoutTimeLimit(String xSeq, String ySeq, int asteroidX, int asteroidY,
                                                    int goalX, int goalY) {
        List<Integer> xPaces = parseSeq(xSeq);
        List<Integer> yPaces = parseSeq(ySeq);

        // Pad to same length
        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);

        // Expand paces exactly as visualizer does
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

        // Simulate movement with tick-based system
        int x = 0, y = 0;
        int ticksX = 0, ticksY = 0;

        // Check initial collision
        if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
            return false;
        }

        for (int step = 0; step < expandedX.size(); step++) {
            int vx = expandedX.get(step);
            int vy = expandedY.get(step);

            // Process X axis movement
            if (Math.abs(vx) > 0 && Math.abs(vx) <= 5) {
                ticksX++;
                if (ticksX >= Math.abs(vx)) {
                    ticksX = 0;
                    x += (vx > 0) ? 1 : -1;
                }
            }

            // Process Y axis movement
            if (Math.abs(vy) > 0 && Math.abs(vy) <= 5) {
                ticksY++;
                if (ticksY >= Math.abs(vy)) {
                    ticksY = 0;
                    y += (vy > 0) ? 1 : -1;
                }
            }

            // Check collision after movement
            if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
                return false;
            }
        }

        // Check if we reached the goal
        return x == goalX && y == goalY;
    }

    private static boolean isSafe(String xSeq, String ySeq, int asteroidX, int asteroidY,
                                   int goalX, int goalY, int timeLimit) {
        List<Integer> xPaces = parseSeq(xSeq);
        List<Integer> yPaces = parseSeq(ySeq);

        // Pad to same length
        while (xPaces.size() < yPaces.size()) xPaces.add(0);
        while (yPaces.size() < xPaces.size()) yPaces.add(0);

        // Calculate total time
        int totalTime = 0;
        for (int i = 0; i < xPaces.size(); i++) {
            int vx = xPaces.get(i);
            int vy = yPaces.get(i);
            int xTime = (vx == 0) ? 1 : Math.abs(vx);
            int yTime = (vy == 0) ? 1 : Math.abs(vy);
            totalTime += Math.max(xTime, yTime);
        }

        if (totalTime > timeLimit) {
            return false;
        }

        // Expand paces exactly as visualizer does
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

        // Simulate movement with tick-based system
        int x = 0, y = 0;
        int ticksX = 0, ticksY = 0;

        // Check initial collision
        if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
            return false;
        }

        for (int step = 0; step < expandedX.size(); step++) {
            int vx = expandedX.get(step);
            int vy = expandedY.get(step);

            // Process X axis movement
            if (Math.abs(vx) > 0 && Math.abs(vx) <= 5) {
                ticksX++;
                if (ticksX >= Math.abs(vx)) {
                    ticksX = 0;
                    x += (vx > 0) ? 1 : -1;
                }
            }

            // Process Y axis movement
            if (Math.abs(vy) > 0 && Math.abs(vy) <= 5) {
                ticksY++;
                if (ticksY >= Math.abs(vy)) {
                    ticksY = 0;
                    y += (vy > 0) ? 1 : -1;
                }
            }

            // Check collision after movement
            if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
                return false;
            }
        }

        // Check if we reached the goal
        return x == goalX && y == goalY;
    }

    private static String[] generateOptimalPath(int goalX, int goalY, boolean fast) {
        List<Integer> xPaces = generatePaceSequence(goalX, fast);
        List<Integer> yPaces = generatePaceSequence(goalY, fast);
        return padAndFormat(xPaces, yPaces);
    }

    private static String[] generateSequentialPath(int goalX, int goalY, boolean xFirst, boolean fast) {
        List<Integer> xPaces = generatePaceSequence(goalX, fast);
        List<Integer> yPaces = generatePaceSequence(goalY, fast);

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

    private static String[] generateFastUShapedPath(int goalX, int goalY, int detourX, int detourY) {
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // First leg: go to detour point with FAST mode
        List<Integer> dx = generatePaceSequence(detourX, true);
        List<Integer> dy = generatePaceSequence(detourY, true);
        addSegment(xPaces, yPaces, dx, dy);

        // Second leg: go from detour to goal with FAST mode
        List<Integer> gx = generatePaceSequence(goalX - detourX, true);
        List<Integer> gy = generatePaceSequence(goalY - detourY, true);
        addSegment(xPaces, yPaces, gx, gy);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
    }

    private static String[] generateFastDetourPath(int goalX, int goalY, int waypointX, int waypointY) {
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // First segment: origin to waypoint with FAST mode
        List<Integer> wx = generatePaceSequence(waypointX, true);
        List<Integer> wy = generatePaceSequence(waypointY, true);
        addSegment(xPaces, yPaces, wx, wy);

        // Second segment: waypoint to goal with FAST mode
        int remainingX = goalX - waypointX;
        int remainingY = goalY - waypointY;
        List<Integer> rx = generatePaceSequence(remainingX, true);
        List<Integer> ry = generatePaceSequence(remainingY, true);
        addSegment(xPaces, yPaces, rx, ry);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
    }

    private static String[] generateDetourPathMixed(int goalX, int goalY, int waypointX, int waypointY) {
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // First segment: origin to waypoint with NON-FAST mode (might be more efficient for certain distances)
        List<Integer> wx = generatePaceSequence(waypointX, false);
        List<Integer> wy = generatePaceSequence(waypointY, false);
        addSegment(xPaces, yPaces, wx, wy);

        // Second segment: waypoint to goal with NON-FAST mode
        int remainingX = goalX - waypointX;
        int remainingY = goalY - waypointY;
        List<Integer> rx = generatePaceSequence(remainingX, false);
        List<Integer> ry = generatePaceSequence(remainingY, false);
        addSegment(xPaces, yPaces, rx, ry);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
    }

    private static String[] generateThreeWaypointPath(int goalX, int goalY, int wp1X, int wp1Y, int wp2X, int wp2Y) {
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // First segment: origin to waypoint1 with FAST mode
        List<Integer> w1x = generatePaceSequence(wp1X, true);
        List<Integer> w1y = generatePaceSequence(wp1Y, true);
        addSegment(xPaces, yPaces, w1x, w1y);

        // Second segment: waypoint1 to waypoint2 with FAST mode
        int dx2 = wp2X - wp1X;
        int dy2 = wp2Y - wp1Y;
        List<Integer> w2x = generatePaceSequence(dx2, true);
        List<Integer> w2y = generatePaceSequence(dy2, true);
        addSegment(xPaces, yPaces, w2x, w2y);

        // Third segment: waypoint2 to goal with FAST mode
        int remainingX = goalX - wp2X;
        int remainingY = goalY - wp2Y;
        List<Integer> rx = generatePaceSequence(remainingX, true);
        List<Integer> ry = generatePaceSequence(remainingY, true);
        addSegment(xPaces, yPaces, rx, ry);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
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
            // For distances 3-8, need special handling
            if (absDistance <= 8) {
                // Use a simpler acceleration pattern
                if (absDistance == 3) {
                    paces.add(5 * direction);
                    paces.add(4 * direction);
                    paces.add(5 * direction);
                } else if (absDistance == 4) {
                    paces.add(5 * direction);
                    paces.add(4 * direction);
                    paces.add(4 * direction);
                    paces.add(5 * direction);
                } else if (absDistance == 5) {
                    paces.add(5 * direction);
                    paces.add(4 * direction);
                    paces.add(3 * direction);
                    paces.add(4 * direction);
                    paces.add(5 * direction);
                } else if (absDistance == 6) {
                    paces.add(5 * direction);
                    paces.add(4 * direction);
                    paces.add(3 * direction);
                    paces.add(3 * direction);
                    paces.add(4 * direction);
                    paces.add(5 * direction);
                } else if (absDistance == 7) {
                    paces.add(5 * direction);
                    paces.add(4 * direction);
                    paces.add(3 * direction);
                    paces.add(2 * direction);
                    paces.add(3 * direction);
                    paces.add(4 * direction);
                    paces.add(5 * direction);
                } else { // absDistance == 8
                    paces.add(5 * direction);
                    paces.add(4 * direction);
                    paces.add(3 * direction);
                    paces.add(2 * direction);
                    paces.add(2 * direction);
                    paces.add(3 * direction);
                    paces.add(4 * direction);
                    paces.add(5 * direction);
                }
                paces.add(0);
                return paces;
            }

            // For distances > 8: use full acceleration/deceleration with pace 1 cruise
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

    private static String toSeqString(List<Integer> paces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paces.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(paces.get(i));
        }
        return sb.toString();
    }

    private static List<Integer> parseSeq(String seq) {
        List<Integer> paces = new ArrayList<>();
        for (String s : seq.trim().split("\\s+")) {
            paces.add(Integer.parseInt(s));
        }
        return paces;
    }

    private static void writeResults(String outputFile, List<String> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String line : results) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
