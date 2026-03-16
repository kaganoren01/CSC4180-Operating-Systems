package com.pizza;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;

/**
 * Pizza buffet controller implementation using Java Semaphores
 * 
 * @version 1.0
 */
public class BuffetSemaphore implements Buffet {
  private final int maxSlices;
  private final Semaphore mutex;
  private final Semaphore stateChanged;
  private final Queue<SliceType> slices;

  private boolean isClosed;
  private int vegWaiters;
  private int amountOfVegSlices;
  private int blockedThreads;
  private int stateVersion;
  
  /**
   * Creates a buffet with specified maximum slice capacity
   * 
   * @param maxSlices maximum number of slices allowed on buffet
   */
  public BuffetSemaphore(int maxSlices) {
    this.maxSlices = maxSlices;
    mutex = new Semaphore(1, true);
    stateChanged = new Semaphore(0, true);
    slices = new LinkedList<>();
    isClosed = false;
    vegWaiters = 0;
    amountOfVegSlices = 0;
    blockedThreads = 0;
    stateVersion = 0;
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
    validateDesired(desired);
    if (desired == 0) {
      return new ArrayList<>();
    }

    while (true) {
      mutex.acquireUninterruptibly();
      if (isClosed) {
        mutex.release();
        return null;
      }

      int available = vegWaiters > 0 ? countNonVegSlices() : slices.size();
      if (available >= desired) {
        List<SliceType> takenSlices = new ArrayList<>(desired);
        for (int i = 0; i < desired; i++) {
          SliceType next = vegWaiters > 0
              ? removeOldestMatching(slice -> !slice.isVeg())
              : removeOldestMatching(slice -> true);
          takenSlices.add(next);
          if (next.isVeg()) {
            amountOfVegSlices--;
          }
        }
        signalStateChangeLocked();
        mutex.release();
        return takenSlices;
      }

      int observedVersion = stateVersion;
      blockedThreads++;
      mutex.release();
      waitForStateChange(observedVersion);
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
    validateDesired(desired);
    if (desired == 0) {
      return new ArrayList<>();
    }

    mutex.acquireUninterruptibly();
    vegWaiters++;
    signalStateChangeLocked();
    mutex.release();

    try {
      while (true) {
        mutex.acquireUninterruptibly();
        if (isClosed) {
          mutex.release();
          return null;
        }

        if (amountOfVegSlices >= desired) {
          List<SliceType> takenSlices = new ArrayList<>(desired);
          for (int i = 0; i < desired; i++) {
            SliceType next = removeOldestMatching(SliceType::isVeg);
            takenSlices.add(next);
            amountOfVegSlices--;
          }
          signalStateChangeLocked();
          mutex.release();
          return takenSlices;
        }

        int observedVersion = stateVersion;
        blockedThreads++;
        mutex.release();
        waitForStateChange(observedVersion);
      }
    } finally {
      mutex.acquireUninterruptibly();
      vegWaiters--;
      signalStateChangeLocked();
      mutex.release();
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
    if (count < 0) {
      throw new IllegalArgumentException("Count must be non-negative");
    }
    if (stype == null) {
      throw new NullPointerException("Slice type cannot be null");
    }

    while (count > 0) {
      mutex.acquireUninterruptibly();
      if (isClosed) {
        mutex.release();
        return false;
      }

      if (slices.size() < maxSlices) {
        slices.offer(stype);
        if (stype.isVeg()) {
          amountOfVegSlices++;
        }
        count--;
        signalStateChangeLocked();
        mutex.release();
        continue;
      }

      int observedVersion = stateVersion;
      blockedThreads++;
      mutex.release();
      waitForStateChange(observedVersion);
    }

    return true;
  }

  /**
   * Closes the buffet, waking all blocked threads
   */
  @Override
  public void close() {
    mutex.acquireUninterruptibly();
    isClosed = true;
    signalStateChangeLocked();
    mutex.release();
  }

  /**
   * Returns the type of buffet implementation.
   */
  @Override
  public String type() {
    return "Semaphore";
  }

  private void validateDesired(int desired) {
    if (desired < 0 || desired > maxSlices) {
      throw new IllegalArgumentException("Desired slices must be between 0 and " + maxSlices);
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

  private SliceType removeOldestMatching(Predicate<SliceType> predicate) {
    Iterator<SliceType> iterator = slices.iterator();
    while (iterator.hasNext()) {
      SliceType next = iterator.next();
      if (predicate.test(next)) {
        iterator.remove();
        return next;
      }
    }
    throw new IllegalStateException("No matching slice available despite pre-check");
  }

  private void waitForStateChange(int observedVersion) {
    while (true) {
      stateChanged.acquireUninterruptibly();
      mutex.acquireUninterruptibly();
      blockedThreads--;
      if (isClosed || stateVersion != observedVersion) {
        mutex.release();
        return;
      }

      blockedThreads++;
      mutex.release();
    }
  }

  private void signalStateChangeLocked() {
    stateVersion++;
    if (blockedThreads > 0) {
      stateChanged.release(blockedThreads);
    }
  }
}
