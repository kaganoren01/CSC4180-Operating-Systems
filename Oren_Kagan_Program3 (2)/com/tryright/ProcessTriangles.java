package com.tryright;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ProcessTriangles - counts right triangles using multiple processes
 * Uses PointStore interface to support both text and binary formats
 *
 * Usage: java com.tryright.ProcessTriangles <input_file> <num_processes>
 *
 * Splits the work among multiple processes to use all CPU cores.
 * Each process handles a subset and reports its count back.
 */
public class ProcessTriangles {

    public static void main(String[] args) {
        // Check command line arguments
        if (args.length != 2) {
            System.err.println("Usage: java com.tryright.ProcessTriangles <input_file> <num_processes>");
            System.exit(1);
        }

        String filename = args[0];
        int numProcesses;

        try {
            numProcesses = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Number of processes must be an integer");
            System.exit(1);
            return;
        }

        // Check number of processes is valid
        if (numProcesses <= 0) {
            System.err.println("Error: Number of processes must be positive");
            System.exit(1);
        }

        if (numProcesses > 256) {
            System.err.println("Error: Number of processes cannot exceed 256");
            System.exit(1);
        }

        // Check if file exists and can be read
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
            // Create PointStore to get number of points
            store = TrianglesUtils.createPointStore(filename);
            int numPoints = store.numPoints();

            // Use single process if we have too few points
            if (numPoints < 3 || numProcesses == 1) {
                int count = TrianglesUtils.countRightTriangles(store, 0, numPoints);
                System.out.println(count);
                return;
            }

            // Limit processes to dataset size (no point having more processes than points)
            int actualProcesses = Math.min(numProcesses, numPoints);

            // Use single process if dataset is very small
            if (actualProcesses <= 1 || numPoints < 4) {
                int count = TrianglesUtils.countRightTriangles(store, 0, numPoints);
                System.out.println(count);
                return;
            }

            // Distribute work among processes
            int totalCount = countWithMultipleProcesses(filename, numPoints, actualProcesses);
            System.out.println(totalCount);

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

    /**
     * Count right triangles using multiple processes.
     * Each process checks a subset of the points as right-angle corners.
     * Uses pipes (stdin/stdout) for communication between processes.
     */
    private static int countWithMultipleProcesses(String filename, int numPoints, int numProcesses)
            throws IOException {

        int totalCount = 0;
        List<Process> processes = new ArrayList<>();
        List<BufferedReader> readers = new ArrayList<>();

        // Split work evenly among processes
        int pointsPerProcess = (numPoints + numProcesses - 1) / numProcesses;

        // Start child processes
        for (int processIndex = 0; processIndex < numProcesses; processIndex++) {
            int startIdx = processIndex * pointsPerProcess;
            int endIdx = Math.min((processIndex + 1) * pointsPerProcess, numPoints);

            // Skip if no work for this process
            if (startIdx >= numPoints) {
                break;
            }

            try {
                // Start child process
                ProcessBuilder pb = new ProcessBuilder(
                    "java", "-cp", ".", "com.tryright.SingleProcessTriangleCounter"
                );

                Process process = pb.start();
                processes.add(process);

                // Send parameters to child via stdin (filename, startIdx, endIdx)
                try (PrintWriter writer = new PrintWriter(process.getOutputStream())) {
                    writer.println(filename);
                    writer.println(startIdx);
                    writer.println(endIdx);
                    writer.flush();
                }

                // Read result from child via stdout
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );
                readers.add(reader);

            } catch (IOException e) {
                System.err.println("Error: Failed to start child process");
                for (Process p : processes) {
                    p.destroy();
                }
                throw e;
            }
        }

        // Collect results from all child processes
        for (int i = 0; i < processes.size(); i++) {
            try {
                BufferedReader reader = readers.get(i);
                String result = reader.readLine();

                if (result != null) {
                    try {
                        int count = Integer.parseInt(result.trim());
                        totalCount += count;
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid result from child process");
                        for (Process p : processes) {
                            p.destroy();
                        }
                        System.exit(1);
                    }
                }

                int exitCode = processes.get(i).waitFor();
                if (exitCode != 0) {
                    System.err.println("Warning: Child process exited with code " + exitCode);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Error: Interrupted waiting for child process");
                System.exit(1);
            }
        }

        return totalCount;
    }
}
