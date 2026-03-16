package com.pizza;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pizza buffet controller implementation using Java ReentrantLock
 * 
 * @version 1.0
 */
public class BuffetLock implements Buffet {
    private final int maxSlices;
    private final ReentrantLock lock = new ReentrantLock();
    private Condition notFull = lock.newCondition();
    private Condition notEmpty = lock.newCondition();
    private Queue<SliceType> slices = new LinkedList<>();
    private boolean isClosed = false;
    private int vegWaiters = 0;
    private int amountOfVegSlices = 0;

    /**
     * Creates a buffet with specified maximum slice capacity
     * 
     * @param maxSlices maximum number of slices allowed on buffet
     */
    public BuffetLock(int maxSlices) {
        this.maxSlices = maxSlices;
    }

    /**
     * Takes any slices for non-vegetarian patrons
     * Blocks until desired slices are available
     * Respects vegetarian priority
     * 
     * @param desired number of slices requested
     * @return list of slices (oldest first) or null if closed
     */
    @Override
    public List<SliceType> TakeAny(int desired) {
        lock.lock();
        try {
            if (desired < 0 || desired > maxSlices) {
                throw new IllegalArgumentException("Desired slices must be between 0 and " + maxSlices);
            }
            if (desired == 0) {
                return new ArrayList<>();
            }
            while (true) {
                if (isClosed) {
                    return null;
                }

                int available = (vegWaiters > 0) ? countNonVegSlices() : slices.size();
                if (available < desired) {
                    // System.out.println("Not enough slices available for TakeAny(" + desired + "), waiting...");
                    notEmpty.awaitUninterruptibly();
                    continue;
                }

                List<SliceType> takenSlices = new ArrayList<>(desired);
                for (int i = 0; i < desired; i++) {
                    SliceType next = (vegWaiters > 0)
                            ? removeOldestMatching(slice -> !slice.isVeg())
                            : removeOldestMatching(slice -> true);
                    takenSlices.add(next);
                    if (next.isVeg()) {
                        amountOfVegSlices--;
                    }
                }
                notFull.signalAll();
                return takenSlices;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Takes vegetarian slices for vegetarian patrons
     * Blocks until desired vegetarian slices are available
     * 
     * @param desired number of vegetarian slices requested
     * @return list of vegetarian slices (oldest first) or null if closed
     */
    @Override
    public List<SliceType> TakeVeg(int desired) {
        lock.lock();
        vegWaiters++;
        try {
            if (desired < 0 || desired > maxSlices) {
                throw new IllegalArgumentException("Desired slices must be between 0 and " + maxSlices);
            }
            if (desired == 0) {
                return new ArrayList<>();
            }
            while (true) {
                if (isClosed) {
                    return null;
                }

                if (amountOfVegSlices < desired) {
                    // System.out.println("Not enough slices available for TakeVeg(" + desired + "), waiting...");
                    notEmpty.awaitUninterruptibly();
                    continue;
                }

                List<SliceType> takenSlices = new ArrayList<>(desired);
                for (int i = 0; i < desired; i++) {
                    SliceType next = removeOldestMatching(SliceType::isVeg);
                    takenSlices.add(next);
                    amountOfVegSlices--;
                }
                notFull.signalAll();
                return takenSlices;
            }
        } finally {
            vegWaiters--;
            // Vegetarian waiter count changed; wake non-veg takers so they can re-evaluate
            // constraints.
            notEmpty.signalAll();
            lock.unlock();
        }
    }

    /**
     * Adds slices to the buffet
     * Blocks if buffet reaches capacity
     * 
     * @param count number of slices to add
     * @param stype type of slices to add
     * @return true if slices added, false if closed
     */
    @Override
    public boolean AddPizza(int count, SliceType stype) {
        lock.lock();
        try {
            if (count < 0) {
                throw new IllegalArgumentException("Count must be non-negative");
            }
            if (stype == null) {
                throw new NullPointerException("Slice type cannot be null");
            }
            if (isClosed) {
                return false; // Buffet is closed, cannot add slices
            }
            while (count > 0) {
                if (isClosed) {
                    return false;
                }
                if (slices.size() >= maxSlices) {
                    // System.out.println("Buffet full, waiting to add more slices...");
                    notFull.awaitUninterruptibly();
                    continue;
                }
                slices.offer(stype);
                if (stype.isVeg()) {
                    amountOfVegSlices++;
                }
                count--;
                notEmpty.signalAll();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    private int countNonVegSlices() {
        int count = 0;
        for (SliceType slice : slices) {
            if (!slice.isVeg()) {
                count++;
            }
        }
        return count;
    }

    private SliceType removeOldestMatching(java.util.function.Predicate<SliceType> predicate) {
        Iterator<SliceType> it = slices.iterator();
        while (it.hasNext()) {
            SliceType next = it.next();
            if (predicate.test(next)) {
                it.remove();
                return next;
            }
        }
        throw new IllegalStateException("No matching slice available despite pre-check");
    }

    /**
     * Closes the buffet, waking all blocked threads
     */
    @Override
    public void close() {
        lock.lock();
        try {
            isClosed = true;
            notEmpty.signalAll();
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }
    /**
     * Returns the type of buffet implementation
     */
    @Override
    public String type() {
        return "Lock";
    }
}