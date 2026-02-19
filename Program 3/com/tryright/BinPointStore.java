package com.tryright;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * BinPointStore - reads points from binary-encoded files using memory-mapped I/O
 * Format: Zero or more pairs of 4-byte big-endian integers (x, y)
 */
public class BinPointStore implements PointStore {
    
    private static final int INTEGER_SIZE = 4; // Java int is 4 bytes
    private static final int POINT_SIZE = 2 * INTEGER_SIZE; // x and y = 8 bytes per point
    
    private final MappedByteBuffer buffer;
    private final RandomAccessFile file;
    private final FileChannel channel;
    private final int numPoints;
    
    /**
     * Constructor - maps binary file to memory
     * @param filename path to binary-encoded file
     */
    public BinPointStore(String filename) throws IOException {
        file = new RandomAccessFile(filename, "r");
        channel = file.getChannel();
        
        long fileSize = channel.size();
        
        // Validate file size
        if (fileSize % POINT_SIZE != 0) {
            close();
            throw new IOException("Invalid binary file format: file size (" + fileSize + 
                                " bytes) is not a multiple of " + POINT_SIZE + " bytes");
        }
        
        numPoints = (int)(fileSize / POINT_SIZE);
        
        // Map entire file to memory for efficient random access
        if (fileSize > 0) {
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
        } else {
            buffer = null; // Empty file
        }
    }
    
    @Override
    public int getX(int idx) {
        if (idx < 0 || idx >= numPoints) {
            throw new IndexOutOfBoundsException("Index " + idx + " out of bounds for " + numPoints + " points");
        }
        
        // X is at position: idx * POINT_SIZE
        int position = idx * POINT_SIZE;
        return buffer.getInt(position);
    }
    
    @Override
    public int getY(int idx) {
        if (idx < 0 || idx >= numPoints) {
            throw new IndexOutOfBoundsException("Index " + idx + " out of bounds for " + numPoints + " points");
        }
        
        // Y is at position: idx * POINT_SIZE + INTEGER_SIZE
        int position = idx * POINT_SIZE + INTEGER_SIZE;
        return buffer.getInt(position);
    }
    
    @Override
    public int numPoints() {
        return numPoints;
    }
    
    @Override
    public void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            System.err.println("Warning: Error closing file: " + e.getMessage());
        }
    }
}
