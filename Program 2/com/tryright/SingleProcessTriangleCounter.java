package com.tryright;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * SingleProcessTriangleCounter - child process that counts triangles
 *
 * Started by ProcessTriangles. Reads points and work assignment from stdin,
 * counts right triangles, and outputs the count to stdout.
 */
public class SingleProcessTriangleCounter {

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            // Read how many points to expect
            String line = reader.readLine();
            if (line == null) {
                System.err.println("Error: Empty input stream");
                System.exit(1);
            }

            int numPoints;
            try {
                numPoints = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.err.println("Error: First line must be number of points");
                System.exit(1);
                return;
            }

            // Read all the points
            List<TrianglesUtils.Point> points = new ArrayList<>();
            for (int i = 0; i < numPoints; i++) {
                line = reader.readLine();
                if (line == null) {
                    System.err.println("Error: Unexpected end of input while reading points");
                    System.exit(1);
                }

                line = line.trim();
                if (line.isEmpty()) {
                    i--; // Retry this point
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 2) {
                    System.err.println("Error: Invalid point format: " + line);
                    System.exit(1);
                }

                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    points.add(new TrianglesUtils.Point(x, y));
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid coordinate values: " + line);
                    System.exit(1);
                }
            }

            // Read which range to process
            line = reader.readLine();
            if (line == null) {
                System.err.println("Error: Missing start index");
                System.exit(1);
            }
            int startIdx;
            try {
                startIdx = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.err.println("Error: Start index must be an integer");
                System.exit(1);
                return;
            }

            line = reader.readLine();
            if (line == null) {
                System.err.println("Error: Missing end index");
                System.exit(1);
            }
            int endIdx;
            try {
                endIdx = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.err.println("Error: End index must be an integer");
                System.exit(1);
                return;
            }

            if (startIdx < 0 || endIdx < startIdx) {
                System.err.println("Error: Invalid indices: startIdx=" + startIdx + ", endIdx=" + endIdx);
                System.exit(1);
            }

            // Count triangles in this range
            int count = TrianglesUtils.countRightTriangles(points, startIdx, endIdx);

            // Send result back to parent
            System.out.println(count);
            System.out.flush();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
