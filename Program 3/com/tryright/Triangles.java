package com.tryright;

import java.io.File;
import java.io.IOException;

/**
 * Triangles - finds right triangles from a list of points
 * Uses PointStore interface to support both text and binary formats
 *
 * Usage: java com.tryright.Triangles <input_file>
 */
public class Triangles {

    public static void main(String[] args){
        // Need exactly 1 argumentY
        if (args.length != 1) {
            System.err.println("Usage: java com.tryright.Triangles <input_file>");
            System.exit(1);
        }

        String filename = args[0];

        // Check if file exists and is readable
        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            System.err.println("Error: No such file or directory");
            System.exit(2);
        }

        if (!inputFile.canRead()) {
            System.err.println("Error: Permission denied");
            System.exit(2);
        }

        PointStore store = null;
        try {
            // Create appropriate PointStore based on file extension
            store = TrianglesUtils.createPointStore(filename);
            
            int count = TrianglesUtils.countRightTriangles(store, 0, store.numPoints());
            System.out.println(count);

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                System.err.println("Error: Permission denied");
            } else {
                System.err.println("Error: " + e.getMessage());
            }
            System.exit(2);
        } finally {
            if (store != null) {
                store.close();
            }
        }
    }
}
