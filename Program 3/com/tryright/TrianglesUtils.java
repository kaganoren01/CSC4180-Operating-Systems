package com.tryright;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * TrianglesUtils - shared functions for counting right triangles
 *
 * Used by Triangles, ProcessTriangles, ThreadTriangles, and SingleProcessTriangleCounter.
 */
public class TrianglesUtils {
    /**
     * Create appropriate PointStore based on filename extension
     * @param filename Path to file (.dat for binary, otherwise text)
     * @return PointStore instance
     * @throws IOException if file cannot be read or is malformed
     */
    public static PointStore createPointStore(String filename) throws IOException {
        if (filename.endsWith(".dat")) {
            return new BinPointStore(filename);
        } else {
            return new TextPointStore(filename);
        }
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
     * Count right triangles using PointStore interface
     * @param store PointStore containing the points
     * @param startIdx starting index (inclusive)
     * @param endIdx ending index (exclusive)
     * @return count of right triangles
     */
    public static int countRightTriangles(PointStore store, int startIdx, int endIdx) {
        int n = store.numPoints();
        
        if (n < 3) {
            return 0;
        }
        
        // Make sure indices are valid
        if (startIdx < 0) startIdx = 0;
        if (endIdx > n) endIdx = n;
        if (startIdx >= endIdx) return 0;
        
        // OPTIMIZATION: Cache all coordinates to avoid repeated method calls
        // This reduces overhead in the O(n^2) inner loop
        int[] xCoords = new int[n];
        int[] yCoords = new int[n];
        for (int k = 0; k < n; k++) {
            xCoords[k] = store.getX(k);
            yCoords[k] = store.getY(k);
        }
        
        int totalCount = 0;
        
        // Check each point in the range as the right angle corner
        for (int i = startIdx; i < endIdx; i++) {
            int vertexX = xCoords[i];
            int vertexY = yCoords[i];
            
            // Count how many points are in each direction from this corner
            // Pre-size HashMap to avoid rehashing (estimate: n/4 unique directions)
            Map<Direction, Integer> directionCounts = new HashMap<>((n + 2) / 3);
            
            // Check all other points
            for (int j = 0; j < n; j++) {
                if (i == j) continue; // Skip itself
                
                int otherX = xCoords[j];
                int otherY = yCoords[j];
                
                // Find direction from corner to other point
                long deltaX = (long)otherX - vertexX;
                long deltaY = (long)otherY - vertexY;
                
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
