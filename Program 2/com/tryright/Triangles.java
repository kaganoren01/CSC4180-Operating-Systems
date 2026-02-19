package com.tryright;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Triangles - finds right triangles from a list of points
 *
 * Usage: java com.tryright.Triangles <input_file>
 */
public class Triangles {

    public static void main(String[] args) {
        // Need exactly 1 argument
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

        try {
            List<TrianglesUtils.Point> pts = TrianglesUtils.readPoints(filename);
            int count = TrianglesUtils.countRightTriangles(pts, 0, pts.size());
            System.out.println(count);

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                System.err.println("Error: Permission denied");
            } else {
                System.err.println("Error: " + e.getMessage());
            }
            System.exit(2);
        }
    }
}
