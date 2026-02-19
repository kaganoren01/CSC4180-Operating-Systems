package com.tryright;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * ThreadTriangles - counts right triangles using multiple threads
 *
 * Usage: java com.tryright.ThreadTriangles <input_file> <num_threads>
 *
 * Unlike ProcessTriangles which uses separate processes and pipes for IPC,
 * this implementation uses threads that share the same memory space.
 * Communication is done through a shared results array; each thread
 * writes its count to its own slot, and the main thread sums them.
 */
public class ThreadTriangles {

    // Shared memory: array where each thread stores its result
    // Thread i writes to results[i], so no synchronization needed
    private static int[] results;

    // Shared memory: the list of points (read-only for worker threads)
    private static List<TrianglesUtils.Point> points;

    public static void main(String[] args) {
        // Check command line arguments
        if (args.length != 2) {
            System.err.println("Usage: java com.tryright.ThreadTriangles <input_file> <num_threads>");
            System.exit(1);
        }

        String filename = args[0];
        int numThreads;

        try {
            numThreads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Number of threads must be an integer");
            System.exit(1);
            return;
        }

        // Validate thread count
        if (numThreads <= 0) {
            System.err.println("Error: Number of threads must be positive");
            System.exit(1);
        }

        if (numThreads > 256) {
            System.err.println("Error: Number of threads cannot exceed 256");
            System.exit(1);
        }

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
            // Read all points from file into shared memory
            points = TrianglesUtils.readPoints(filename);

            // Handle small datasets with single thread
            if (points.size() < 3 || numThreads == 1) {
                int count = TrianglesUtils.countRightTriangles(points, 0, points.size());
                System.out.println(count);
                return;
            }

            // Limit threads to dataset size (no point having more threads than points)
            int actualThreads = Math.min(numThreads, points.size());

            // Allocate shared results array - each thread gets one slot
            results = new int[actualThreads];

            // Create and start worker threads
            Thread[] workers = new Thread[actualThreads];
            int pointsPerThread = (points.size() + actualThreads - 1) / actualThreads;

            for (int i = 0; i < actualThreads; i++) {
                final int threadIndex = i;
                final int startIdx = i * pointsPerThread;
                final int endIdx = Math.min((i + 1) * pointsPerThread, points.size());

                // Skip if no work for this thread
                if (startIdx >= points.size()) {
                    break;
                }

                // Create worker thread with explicit stack size for better performance
                // Each thread reads from shared 'points' list and writes to results[threadIndex]
                workers[i] = new Thread(null, () -> {
                    int count = TrianglesUtils.countRightTriangles(points, startIdx, endIdx);
                    results[threadIndex] = count;  // Write to shared memory
                }, "Worker-" + i, 512 * 1024); // 512KB stack
            }
            
            // Start all threads at once to reduce creation overhead
            for (int i = 0; i < actualThreads; i++) {
                if (workers[i] != null) {
                    workers[i].start();
                }
            }

            // Wait for all threads to complete
            for (Thread worker : workers) {
                if (worker != null) {
                    try {
                        worker.join();
                    } catch (InterruptedException e) {
                        System.err.println("Error: Thread interrupted");
                        System.exit(1);
                    }
                }
            }

            // Sum results from shared memory
            int totalCount = 0;
            for (int count : results) {
                totalCount += count;
            }

            System.out.println(totalCount);

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
