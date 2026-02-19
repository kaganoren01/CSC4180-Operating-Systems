package com.tryright;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TrianglesUtils - shared functions for counting right triangles
 *
 * Used by Triangles, ProcessTriangles, ThreadTriangles, and SingleProcessTriangleCounter.
 */
public class TrianglesUtils {
    
    public static class Point {
        public int x, y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Read points from an input file.
     * First line is the count, followed by x y coordinates.
     */
    public static List<Point> readPoints(String filename) throws IOException {
        List<Point> points = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine();

            if (line == null) {
                System.err.println("Error: Empty file");
                System.exit(3);
            }

            int n;
            try {
                n = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.err.println("Error: First line must be an integer");
                System.exit(3);
                return points;
            }

            int pointCount = 0;
            while ((line = reader.readLine()) != null && pointCount < n) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 2) {
                    System.err.println("Error: Invalid point format");
                    System.exit(3);
                }

                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    points.add(new Point(x, y));
                    pointCount++;
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid coordinate values");
                    System.exit(3);
                }
            }

            if (pointCount < n) {
                System.err.println("Error: Expected " + n + " points but found only " + pointCount);
                System.exit(3);
            }
        }

        return points;
    }

    // Direction vector between two points
    static class Direction {
        long dx, dy;
        
        Direction(long dx, long dy) {
            // Skip if both are zero (same point)
            if (dx == 0 && dy == 0) {
                this.dx = 0;
                this.dy = 0;
                return;
            }
            
            // Simplify the direction so (2,4) and (1,2) are treated as the same
            long divisor = gcd(Math.abs(dx), Math.abs(dy));
            if (divisor > 0) {
                this.dx = dx / divisor;
                this.dy = dy / divisor;
            } else {
                this.dx = dx;
                this.dy = dy;
            }
        }
        
        private static long gcd(long a, long b) {
            if (a == 0) return b;
            if (b == 0) return a;
            while (b != 0) {
                long temp = b;
                b = a % b;
                a = temp;
            }
            return a;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Direction)) return false;
            Direction other = (Direction) obj;
            return dx == other.dx && dy == other.dy;
        }
        
        @Override
        public int hashCode() {
            long hash = dx * 31L + dy;
            return (int)(hash ^ (hash >>> 32));
        }
    }
    
    /**
     * Count right triangles where the right angle corner is in the range [startIdx, endIdx).
     * Only checks corners at indices >= startIdx to avoid counting triangles twice.
     * 
     * For each point, check if it can be the corner with the right angle.
     * Count how many points are in each direction from this point.
     * For perpendicular directions, multiply the counts to get triangles.
     * 
     * Runtime: O(n^2) - for each point, check all other points
     * Memory: O(n) - HashMap for each point
     */
    public static int countRightTriangles(List<Point> points, int startIdx, int endIdx) {
        int n = points.size();
        
        if (n < 3) {
            return 0;
        }
        
        // Make sure indices are valid
        if (startIdx < 0) startIdx = 0;
        if (endIdx > n) endIdx = n;
        if (startIdx >= endIdx) return 0;
        
        int totalCount = 0;
        
        // Check each point in the range as the right angle corner
        for (int i = startIdx; i < endIdx; i++) {
            Point vertex = points.get(i);
            
            // Count how many points are in each direction from this corner
            // Pre-size HashMap to avoid rehashing (estimate: n/4 unique directions)
            Map<Direction, Integer> directionCounts = new HashMap<>((n + 2) / 3);
            
            // Check all other points
            for (int j = 0; j < n; j++) {
                if (i == j) continue; // Skip itself
                
                Point other = points.get(j);
                
                // Find direction from corner to other point
                long deltaX = (long)other.x - vertex.x;
                long deltaY = (long)other.y - vertex.y;
                
                Direction dir = new Direction(deltaX, deltaY);
                
                // Add one to the count for this direction
                directionCounts.put(dir, directionCounts.getOrDefault(dir, 0) + 1);
            }
            
            // Count triangles with right angle at this corner
            // For each direction, check only the left perpendicular to avoid double-counting
            for (Map.Entry<Direction, Integer> entry : directionCounts.entrySet()) {
                Direction dir = entry.getKey();
                int countInThisDir = entry.getValue();

                // Check only left perpendicular (90° counterclockwise)
                Direction perpLeft = new Direction(-dir.dy, dir.dx);
                Integer countInPerpDir = directionCounts.get(perpLeft);
                if (countInPerpDir != null) {
                    totalCount += countInThisDir * countInPerpDir;
                }
            }
        }
        
        return totalCount;
    }
}
