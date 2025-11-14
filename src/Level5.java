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

        // Try X first, then Y
        String[] xFirst = generateSequentialPath(goalX, goalY, true);
        if (isSafe(xFirst[0], xFirst[1], asteroidX, asteroidY, goalX, goalY)) {
            return xFirst;
        }

        // Try Y first, then X
        String[] yFirst = generateSequentialPath(goalX, goalY, false);
        if (isSafe(yFirst[0], yFirst[1], asteroidX, asteroidY, goalX, goalY)) {
            return yFirst;
        }

        // Try U-shaped paths: perpendicular detour, then parallel, then back
        for (int offset = 3; offset <= 20; offset++) {
            // Try going UP, then X, then back to Y
            String[] upPath = generateUShapedPath(goalX, goalY, 0, offset);
            if (isSafe(upPath[0], upPath[1], asteroidX, asteroidY, goalX, goalY)) {
                return upPath;
            }

            // Try going DOWN, then X, then back to Y
            String[] downPath = generateUShapedPath(goalX, goalY, 0, -offset);
            if (isSafe(downPath[0], downPath[1], asteroidX, asteroidY, goalX, goalY)) {
                return downPath;
            }

            // Try going RIGHT, then Y, then back to X
            String[] rightPath = generateUShapedPath(goalX, goalY, offset, 0);
            if (isSafe(rightPath[0], rightPath[1], asteroidX, asteroidY, goalX, goalY)) {
                return rightPath;
            }

            // Try going LEFT, then Y, then back to X
            String[] leftPath = generateUShapedPath(goalX, goalY, -offset, 0);
            if (isSafe(leftPath[0], leftPath[1], asteroidX, asteroidY, goalX, goalY)) {
                return leftPath;
            }
        }

        // Try general waypoint detours
        for (int offset = 3; offset <= 100; offset += 5) {
            int[][] detours = {
                {0, offset}, {0, -offset}, {offset, 0}, {-offset, 0},
                {offset, offset}, {-offset, offset}, {offset, -offset}, {-offset, -offset}
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

    private static String[] generateUShapedPath(int goalX, int goalY, int detourX, int detourY) {
        // Generate U-shaped path: (0,0) -> (detourX, detourY) -> (goalX, goalY)
        List<Integer> xPaces = new ArrayList<>();
        List<Integer> yPaces = new ArrayList<>();

        xPaces.add(0);
        yPaces.add(0);

        // Segment 1: Go to detour point
        List<Integer> dx = generatePaceSequence(detourX);
        List<Integer> dy = generatePaceSequence(detourY);
        addSegment(xPaces, yPaces, dx, dy);

        // Segment 2: Go to goal
        List<Integer> gx = generatePaceSequence(goalX - detourX);
        List<Integer> gy = generatePaceSequence(goalY - detourY);
        addSegment(xPaces, yPaces, gx, gy);

        return new String[]{toSeqString(xPaces), toSeqString(yPaces)};
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
            // Pad Y with zeros at the beginning
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
            // Pad X with zeros at the beginning
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

    private static List<Integer> generatePaceSequence(int distance) {
        List<Integer> paces = new ArrayList<>();
        paces.add(0);

        if (distance == 0) {
            paces.add(0);
            return paces;
        }

        int absDistance = Math.abs(distance);
        int direction = distance > 0 ? 1 : -1;

        // minPace >= (11 - absDistance) / 2
        int minPace = (int) Math.ceil((11.0 - absDistance) / 2.0);
        if (minPace < 1) minPace = 1;
        if (minPace > 5) minPace = 5;

        int accelMoves = 5 - minPace + 1;
        int decelMoves = 5 - minPace;
        int cruiseMoves = absDistance - accelMoves - decelMoves;

        // Accelerate: 5, 4, 3, 2, 1, ..., minPace
        for (int pace = 5; pace >= minPace; pace--) {
            paces.add(pace * direction);
        }

        // Cruise at minPace
        for (int i = 0; i < cruiseMoves; i++) {
            paces.add(minPace * direction);
        }

        // Decelerate: minPace+1, ..., 4, 5
        for (int pace = minPace + 1; pace <= 5; pace++) {
            paces.add(pace * direction);
        }

        paces.add(0);
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

        if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
            return false;
        }

        int maxSteps = Math.max(xPaces.size(), yPaces.size());

        for (int step = 0; step < maxSteps; step++) {
            if (step < xPaces.size()) vx = xPaces.get(step);
            else vx = 0;

            if (step < yPaces.size()) vy = yPaces.get(step);
            else vy = 0;

            int xSteps = Math.abs(vx) > 0 ? Math.abs(vx) : 1;
            int ySteps = Math.abs(vy) > 0 ? Math.abs(vy) : 1;
            int maxTicks = Math.max(xSteps, ySteps);

            for (int tick = 0; tick < maxTicks; tick++) {
                if (vx != 0) {
                    tickX++;
                    if (tickX >= Math.abs(vx)) {
                        tickX = 0;
                        x += (vx > 0) ? 1 : -1;
                    }
                } else {
                    tickX = 0;
                }

                if (vy != 0) {
                    tickY++;
                    if (tickY >= Math.abs(vy)) {
                        tickY = 0;
                        y += (vy > 0) ? 1 : -1;
                    }
                } else {
                    tickY = 0;
                }

                if (Math.abs(x - asteroidX) <= 2 && Math.abs(y - asteroidY) <= 2) {
                    return false;
                }
            }
        }

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
