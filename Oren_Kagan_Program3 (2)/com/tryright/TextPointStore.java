package com.tryright;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TextPointStore - reads points from text-encoded files
 * Format: First line contains count, followed by "x y" pairs
 */
public class TextPointStore implements PointStore {
    
    private final int[] xCoords;
    private final int[] yCoords;
    private final int numPoints;
    
    /**
     * Constructor - reads and parses text file
     * @param filename path to text-encoded file
     */
    public TextPointStore(String filename) throws IOException {
        List<Integer> xList = new ArrayList<>();
        List<Integer> yList = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine();
            
            if (line == null) {
                throw new IOException("Empty file");
            }
            
            int expectedCount;
            try {
                expectedCount = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                throw new IOException("First line must be an integer");
            }
            
            int pointCount = 0;
            while ((line = reader.readLine()) != null && pointCount < expectedCount) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                String[] parts = line.split("\\s+");
                if (parts.length != 2) {
                    throw new IOException("Invalid point format: expected 'x y'");
                }
                
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    xList.add(x);
                    yList.add(y);
                    pointCount++;
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid coordinate values");
                }
            }
            
            if (pointCount < expectedCount) {
                throw new IOException("Expected " + expectedCount + " points but found only " + pointCount);
            }
        }
        
        // Convert to arrays for fast access
        this.numPoints = xList.size();
        this.xCoords = new int[numPoints];
        this.yCoords = new int[numPoints];
        
        for (int i = 0; i < numPoints; i++) {
            xCoords[i] = xList.get(i);
            yCoords[i] = yList.get(i);
        }
    }
    
    @Override
    public int getX(int idx) {
        if (idx < 0 || idx >= numPoints) {
            throw new IndexOutOfBoundsException("Index " + idx + " out of bounds for " + numPoints + " points");
        }
        return xCoords[idx];
    }
    
    @Override
    public int getY(int idx) {
        if (idx < 0 || idx >= numPoints) {
            throw new IndexOutOfBoundsException("Index " + idx + " out of bounds for " + numPoints + " points");
        }
        return yCoords[idx];
    }
    
    @Override
    public int numPoints() {
        return numPoints;
    }
    
    @Override
    public void close() {
        // No resources to close for text files (BufferedReader closed in constructor)
    }
}
