package com.tryright;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TrianglesUtils - shared functions for counting right triangles
 * 
 * Used by ProcessTriangles and SingleProcessTriangleCounter.
 * Triangles.java has its own copy of the algorithm.
 */
public class TrianglesUtils {
    
    public static class Point {
        public int x, y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
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
        
        Direction[] getPerpendiculars() {
            return new Direction[] {
                new Direction(-dy, dx),      // 90° left
                new Direction(dy, -dx)       // 90° right
            };
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Direction)) return false;
            Direction other = (Direction) obj;
            return dx == other.dx && dy == other.dy;
        }
        
        @Override
        public int hashCode() {
            return (int)(31 * dx + dy);
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
            Map<Direction, Integer> directionCounts = new HashMap<>();
            
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
            // For each direction, check both perpendicular directions
            java.util.Set<Direction> processedDirs = new java.util.HashSet<>();
            for (Map.Entry<Direction, Integer> entry : directionCounts.entrySet()) {
                Direction dir = entry.getKey();
                int countInThisDir = entry.getValue();

                // Skip if we already counted this direction
                if (processedDirs.contains(dir)) {
                    continue;
                }

                // Check both perpendicular directions
                Direction[] perpDirs = dir.getPerpendiculars();
                for (Direction perpDir : perpDirs) {
                    if (directionCounts.containsKey(perpDir) && !processedDirs.contains(perpDir)) {
                        int countInPerpDir = directionCounts.get(perpDir);
                        totalCount += countInThisDir * countInPerpDir;
                    }
                }

                // Mark this direction as done
                processedDirs.add(dir);
            }
        }
        
        return totalCount;
    }
}
