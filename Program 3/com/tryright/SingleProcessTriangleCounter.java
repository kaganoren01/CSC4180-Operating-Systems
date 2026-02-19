package com.tryright;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * SingleProcessTriangleCounter - child process that counts triangles
 * Uses PointStore interface to support both text and binary formats
 *
 * Started by ProcessTriangles. Reads filename and work assignment from stdin,
 * counts right triangles using PointStore, and outputs the count to stdout.
 */
public class SingleProcessTriangleCounter {

    public static void main(String[] args) {
        PointStore store = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            // Read filename
            String filename = reader.readLine();
            if (filename == null) {
                System.err.println("Error: Missing filename");
                System.exit(1);
            }
            filename = filename.trim();

            // Read start index
            String line = reader.readLine();
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

            // Read end index
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

            // Create PointStore from filename
            store = TrianglesUtils.createPointStore(filename);

            // Count triangles in this range
            int count = TrianglesUtils.countRightTriangles(store, startIdx, endIdx);

            // Send result back to parent
            System.out.println(count);
            System.out.flush();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } finally {
            if (store != null) {
                store.close();
            }
        }
    }
}
