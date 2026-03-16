package com.pizza;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

/**
 * Pizza buffet controller implementation using Java synchronized methods
 * 
 * @version 1.0
 */
public class BuffetSynchronized implements Buffet {
  private final int maxSlices;
  private final Queue<SliceType> slices;
  private boolean isClosed;
  private int vegWaiters;
  private int amountOfVegSlices;
  
  /**
   * Creates a buffet with specified maximum slice capacity
   * 
   * @param maxSlices maximum number of slices allowed on buffet
   */
  public BuffetSynchronized(int maxSlices) {
    this.maxSlices = maxSlices;
    slices = new LinkedList<>();
    isClosed = false;
    vegWaiters = 0;
    amountOfVegSlices = 0;
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
  public synchronized List<SliceType> TakeAny(int desired) {
    validateDesired(desired);
    if (desired == 0) {
      return new ArrayList<>();
    }

    while (true) {
      if (isClosed) {
        return null;
      }

      int available = vegWaiters > 0 ? countNonVegSlices() : slices.size();
      if (available < desired) {
        waitUninterruptibly();
        continue;
      }

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
      notifyAll();
      return takenSlices;
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
  public synchronized List<SliceType> TakeVeg(int desired) {
    validateDesired(desired);
    if (desired == 0) {
      return new ArrayList<>();
    }

    vegWaiters++;
    try {
      while (true) {
        if (isClosed) {
          return null;
        }

        if (amountOfVegSlices < desired) {
          waitUninterruptibly();
          continue;
        }

        List<SliceType> takenSlices = new ArrayList<>(desired);
        for (int i = 0; i < desired; i++) {
          SliceType next = removeOldestMatching(SliceType::isVeg);
          takenSlices.add(next);
          amountOfVegSlices--;
        }
        notifyAll();
        return takenSlices;
      }
    } finally {
      vegWaiters--;
      notifyAll();
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
  public synchronized boolean AddPizza(int count, SliceType stype) {
    if (count < 0) {
      throw new IllegalArgumentException("Count must be non-negative");
    }
    if (stype == null) {
      throw new NullPointerException("Slice type cannot be null");
    }
    if (isClosed) {
      return false;
    }

    while (count > 0) {
      if (isClosed) {
        return false;
      }
      if (slices.size() >= maxSlices) {
        waitUninterruptibly();
        continue;
      }

      slices.offer(stype);
      if (stype.isVeg()) {
        amountOfVegSlices++;
      }
      count--;
      notifyAll();
    }
    return true;
  }

  /**
   * Closes the buffet, waking all blocked threads
   */
  @Override
  public synchronized void close() {
    isClosed = true;
    notifyAll();
  }

  /**
   * Returns the type of buffet implementation.
   */
  @Override
  public String type() {
    return "Synchronized";
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

  private void waitUninterruptibly() {
    while (true) {
      try {
        wait();
        return;
      } catch (InterruptedException e) {
        // Ignore interrupts and continue waiting to preserve method semantics.
      }
    }
  }

}
