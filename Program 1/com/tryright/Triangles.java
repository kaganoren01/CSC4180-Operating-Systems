package com.tryright;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Triangles - finds right triangles from a list of points
 *
 * Usage: java com.tryright.Triangles <input_file>
 */
public class Triangles {

    public static void main(String[] args) {
        // Need exactly 1 argument
        if (args.length == 0) {
            System.err.println("Usage: java com.tryright.Triangles <input_file>");
            System.exit(1);
        }

        if (args.length > 1) {
            System.err.println("Usage: java com.tryright.Triangles <input_file>");
            System.exit(1);
        }

        String filename = args[0];
        List<TrianglesUtils.Point> pts = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            line = reader.readLine();

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
                return;
            }

            // Read points
            int ptCount = 0;
            while ((line = reader.readLine()) != null && ptCount < n) {
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
                    pts.add(new TrianglesUtils.Point(x, y));
                    ptCount++;
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid coordinate values");
                    System.exit(3);
                }
            }

            // Check if we have enough pts
            if (ptCount < n) {
                System.err.println("Error: Expected " + n + " points but found only " + ptCount);
                System.exit(3);
            }

            // Count right triangles using shared utility
            int count = TrianglesUtils.countRightTriangles(pts, 0, pts.size());
            System.out.println(count);

        } catch (java.io.FileNotFoundException e) {
            System.err.println("Error: No such file or directory");
            System.exit(2);
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
