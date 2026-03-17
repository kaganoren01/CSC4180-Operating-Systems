package com.pizza;

import java.util.List;

/**
 * Test driver for pizza buffet implementations
 * 
 * @version 1.0
 */
public class TestDriver {
  public static void main(String[] args) throws InterruptedException {
    // Change the implementation being tested by swapping this one line:
    // final Buffet buffet = new BuffetSemaphore(20);
    // final Buffet buffet = new BuffetSynchronized(20);
    final Buffet buffet = new BuffetLock(20);
    System.out.println("Testing buffet implementation: " + buffet.type());
    // - Test basic add and take operations
    BasicAddTake(buffet);
    buffet.reset();
    // - Test edge cases (taking more than available, taking zero slices, etc.)
    EdgeCases(buffet);
    buffet.reset();
    // - Test concurrent access with multiple threads
    ConcurrentAccess(buffet);
    buffet.reset();
    // - Test vegetarian priority
    VegetarianPriority(buffet);
    buffet.reset();
    // - Test FIFO ordering
    FIFOOrdering(buffet);
    buffet.reset();
    // - Test blocking behavior at capacity
    BlockingAtCapacity(buffet);
    buffet.reset();
    // - Test buffet closure
    BuffetClosure(buffet);
    buffet.reset();
    // - Stress tests with multiple concurrent threads
    StressTest(buffet);
    // - Not sure if I should add more tests here? Maybe some long-running stress tests with more threads or more iterations?
    // - Not sure if I need to manually close the buffet at the end of main since all threads should have completed by then, but I can add that if desired.
  }

  // - Test basic add and take operations
  public static void BasicAddTake(Buffet buffet) throws InterruptedException {
    System.out.println("=== BasicAddTake ===");

    boolean addCheese = buffet.AddPizza(3, SliceType.Cheese);
    boolean addMeat = buffet.AddPizza(2, SliceType.Meat);
    boolean addVeggie = buffet.AddPizza(1, SliceType.Veggie);
    System.out.printf("AddPizza results: cheese=%s, meat=%s, veggie=%s%n", addCheese, addMeat, addVeggie);

    printSlices("TakeAny(2)", buffet.TakeAny(2)); // expected: [Cheese, Cheese]
    printSlices("TakeVeg(1)", buffet.TakeVeg(1)); // expected: [Cheese]
    printSlices("TakeAny(2)", buffet.TakeAny(2)); // expected: [Meat, Meat]
    System.out.println("=== End BasicAddTake ===");
  }

  // - Test edge cases (taking more than available, taking zero slices, etc.)
  public static void EdgeCases(Buffet buffet) throws InterruptedException {
    System.out.println("=== EdgeCases ===");

    // Zero-slice requests should return an empty list immediately.
    printSlices("TakeAny(0)", buffet.TakeAny(0));
    printSlices("TakeVeg(0)", buffet.TakeVeg(0));
    System.out.println("AddPizza(0, Cheese) -> " + buffet.AddPizza(0, SliceType.Cheese));

    // Invalid input checks.
    expectThrows("TakeAny(-1)", IllegalArgumentException.class, () -> buffet.TakeAny(-1));
    expectThrows("TakeVeg(-1)", IllegalArgumentException.class, () -> buffet.TakeVeg(-1));
    expectThrows("TakeAny(21)", IllegalArgumentException.class, () -> buffet.TakeAny(21));
    expectThrows("AddPizza(-1, Meat)", IllegalArgumentException.class, () -> buffet.AddPizza(-1, SliceType.Meat));
    expectThrows("AddPizza(1, null)", NullPointerException.class, () -> buffet.AddPizza(1, null));

    // Desired can be greater than currently available: caller should block until enough slices exist.
    System.out.println("AddPizza(2, Meat) -> " + buffet.AddPizza(2, SliceType.Meat));
    Thread blockedTake = new Thread(() -> printSlices("TakeAny(4) after block", buffet.TakeAny(4)), "edge-blocked-take");
    blockedTake.start();

    Thread.sleep(150);
    System.out.println("TakeAny(4) should still be waiting; adding 2 more slices now.");
    System.out.println("Should send after block: AddPizza(2, Meat) -> " + buffet.AddPizza(2, SliceType.Meat));

    blockedTake.join(3000);
    if (blockedTake.isAlive()) {
      System.out.println("WARNING: blocked taker did not complete in time; closing buffet.");
      buffet.close();
      blockedTake.join(1000);
    }

    System.out.println("=== End EdgeCases ===");
  }

  // - Test concurrent access with multiple threads
  public static void ConcurrentAccess(Buffet buffet) throws InterruptedException {
    System.out.println("=== ConcurrentAccess ===");

    java.util.List<Thread> threads = new java.util.ArrayList<>();

    // Spawn multiple baker threads adding pizza concurrently
    Thread baker1 = new Thread(() -> {
      for (int i = 0; i < 10; i++) {
        System.out.println("Adding 1 Cheese slice.");
        if (!buffet.AddPizza(1, SliceType.Cheese)) {
          System.out.println("Baker1 stopped: buffet closed.");
          break;
        }
        try { Thread.sleep(2); } catch (InterruptedException e) {}
      }
      System.out.println("Baker1 finished adding 10 Cheese slices");
    }, "baker1");

    Thread baker2 = new Thread(() -> {
      for (int i = 0; i < 10; i++) {
        System.out.println("Adding 1 Meat slice.");
        if (!buffet.AddPizza(1, SliceType.Meat)) {
          System.out.println("Baker2 stopped: buffet closed.");
          break;
        }
        try { Thread.sleep(2); } catch (InterruptedException e) {}
      }
      System.out.println("Baker2 finished adding 10 Meat slices");
    }, "baker2");

    Thread baker3 = new Thread(() -> {
      for (int i = 0; i < 10; i++) {
        System.out.println("Adding 1 Veggie slice.");
        if (!buffet.AddPizza(1, SliceType.Veggie)) {
          System.out.println("Baker3 stopped: buffet closed.");
          break;
        }
        try { Thread.sleep(2); } catch (InterruptedException e) {}
      }
      System.out.println("Baker3 finished adding 10 Veggie slices");
    }, "baker3");

    // Spawn multiple eater threads taking slices concurrently
    Thread eater1 = new Thread(() -> {
      List<SliceType> slices = buffet.TakeAny(3);
      System.out.println("Eater1 (non-veg) took: " + slices);
    }, "eater1");

    Thread eater2 = new Thread(() -> {
      List<SliceType> slices = buffet.TakeVeg(3);
      System.out.println("Eater2 (veg) took: " + slices);
    }, "eater2");

    Thread eater3 = new Thread(() -> {
      List<SliceType> slices = buffet.TakeAny(3);
      System.out.println("Eater3 (non-veg) took: " + slices);
    }, "eater3");

    Thread eater4 = new Thread(() -> {
      List<SliceType> slices = buffet.TakeVeg(3);
      System.out.println("Eater4 (veg) took: " + slices);
    }, "eater4");

    // Add all threads to list
    threads.add(baker1);
    threads.add(baker2);
    threads.add(baker3);
    threads.add(eater1);
    threads.add(eater2);
    threads.add(eater3);
    threads.add(eater4);

    // Start all threads (interleaved order to maximize concurrency)
    
    eater1.start();
    eater2.start();
    eater3.start();
    eater4.start();

    baker1.start();
    baker2.start();
    baker3.start();


    // Wait for all to complete (timeout of 5 seconds prevents hanging on deadlock)
    for (Thread t : threads) {
      t.join(5000);
    }

    // Check if any threads are still blocked
    boolean hasBlocked = false;
    for (Thread t : threads) {
      if (t.isAlive()) {
        hasBlocked = true;
        break;
      }
    }

    if (hasBlocked) {
      System.out.println("WARNING: Some threads did not complete; closing buffet.");
      buffet.close();
      // Give threads time to exit cleanly after close
      for (Thread t : threads) {
        t.join(1000);
      }
    } else {
      System.out.println("All threads completed successfully!");
    }

    System.out.println("=== End ConcurrentAccess ===");
  }

  // - Test vegetarian priority
  public static void VegetarianPriority(Buffet buffet) throws InterruptedException {
    System.out.println("=== VegetarianPriority ===");

    // Put only vegetarian slices first.
    buffet.AddPizza(3, SliceType.Cheese);

    final List<SliceType>[] vegResult = new List[1];
    final List<SliceType>[] anyResult = new List[1];

    Thread vegEater = new Thread(() -> {
      vegResult[0] = buffet.TakeVeg(4); // Will block waiting for one more veg slice.
      System.out.println("VegEater took: " + vegResult[0]);
    }, "veg-priority-eater");

    Thread anyEater = new Thread(() -> {
      anyResult[0] = buffet.TakeAny(2); // Should avoid veg while vegEater is waiting.
      System.out.println("AnyEater took: " + anyResult[0]);
    }, "any-while-veg-waiting");

    vegEater.start();
    Thread.sleep(100);
    anyEater.start();
    Thread.sleep(100);

    // Add meat so non-veg requester can proceed without taking vegetarian slices.
    buffet.AddPizza(2, SliceType.Meat);
    anyEater.join(3000);

    if (anyResult[0] != null) {
      boolean hasVeg = false;
      for (SliceType s : anyResult[0]) {
        if (s.isVeg()) {
          hasVeg = true;
          break;
        }
      }
      System.out.println("AnyEater respected vegetarian priority: " + (!hasVeg));
    }

    // Add one more veg slice so vegetarian requester can complete 4 slices.
    buffet.AddPizza(1, SliceType.Veggie);
    vegEater.join(3000);

    if (vegEater.isAlive() || anyEater.isAlive()) {
      System.out.println("WARNING: VegetarianPriority threads still blocked; closing buffet.");
      buffet.close();
      vegEater.join(1000);
      anyEater.join(1000);
    }

    System.out.println("=== End VegetarianPriority ===");
  }

  // - Test FIFO ordering
  public static void FIFOOrdering(Buffet buffet) throws InterruptedException {
    System.out.println("=== FIFOOrdering ===");

    // Case 1: TakeAny should return slices in the exact insertion order.
    buffet.AddPizza(1, SliceType.Cheese);
    buffet.AddPizza(1, SliceType.Meat);
    buffet.AddPizza(1, SliceType.Veggie);
    buffet.AddPizza(1, SliceType.Works);
    buffet.AddPizza(1, SliceType.Meat);
    List<SliceType> anyOrder = buffet.TakeAny(5);
    printSlices("FIFO TakeAny(5)", anyOrder);
    boolean anyFifoOk = anyOrder.equals(java.util.Arrays.asList(
        SliceType.Cheese,
        SliceType.Meat,
        SliceType.Veggie,
        SliceType.Works,
        SliceType.Meat));
    System.out.println("TakeAny FIFO correct: " + anyFifoOk);

    // Case 2: TakeVeg should return vegetarian slices in oldest-veg-first order.
    buffet.AddPizza(1, SliceType.Meat);
    buffet.AddPizza(1, SliceType.Veggie);
    buffet.AddPizza(1, SliceType.Works);
    buffet.AddPizza(1, SliceType.Veggie);
    buffet.AddPizza(1, SliceType.Cheese);
    List<SliceType> vegOrder = buffet.TakeVeg(3);
    printSlices("FIFO TakeVeg(3)", vegOrder);
    boolean vegFifoOk = vegOrder.equals(java.util.Arrays.asList(
        SliceType.Veggie,
        SliceType.Veggie,
        SliceType.Cheese));
    System.out.println("TakeVeg FIFO correct: " + vegFifoOk);

    // Drain remaining non-veg slices from case 2 so next tests start clean after reset.
    printSlices("Drain leftovers TakeAny(2)", buffet.TakeAny(2));

    System.out.println("=== End FIFOOrdering ===");
  }

  // - Test blocking behavior at capacity
  public static void BlockingAtCapacity(Buffet buffet) throws InterruptedException {
    System.out.println("=== BlockingAtCapacity ===");

    // Fill buffet to max capacity (20) so next add must block.
    System.out.println("Fill to capacity: AddPizza(20, Meat) -> " + buffet.AddPizza(20, SliceType.Meat));

    final boolean[] addResult = new boolean[1];
    Thread baker = new Thread(() -> {
      addResult[0] = buffet.AddPizza(3, SliceType.Cheese);
      System.out.println("Blocked baker AddPizza(3, Cheese) returned: " + addResult[0]);
    }, "baker");

    baker.start();
    Thread.sleep(150);
    System.out.println("Baker blocked while full: " + baker.isAlive());

    Thread eater = new Thread(() -> printSlices("Eater frees space with TakeAny(5)", buffet.TakeAny(5)), "capacity-eater");
    eater.start();

    eater.join(3000);
    baker.join(3000);

    if (baker.isAlive() || eater.isAlive()) {
      System.out.println("WARNING: BlockingAtCapacity threads still blocked; closing buffet.");
      buffet.close();
      baker.join(1000);
      eater.join(1000);
    }

    // Confirm baker eventually added 3 cheese slices after space opened.
    List<SliceType> remaining = buffet.TakeAny(18);
    int cheeseCount = 0;
    for (SliceType s : remaining) {
      if (s == SliceType.Cheese) {
        cheeseCount++;
      }
    }
    System.out.println("Cheese added after unblock (expected 3): " + cheeseCount);

    System.out.println("=== End BlockingAtCapacity ===");
  }

  // - Test buffet closure
  public static void BuffetClosure(Buffet buffet) throws InterruptedException {
    System.out.println("=== BuffetClosure ===");

    Thread blockedTaker = new Thread(() -> {
      List<SliceType> result = buffet.TakeAny(5);
      System.out.println("Blocked TakeAny after close: " + result);
    }, "taker");

    blockedTaker.start();
    Thread.sleep(100);
    buffet.close();
    blockedTaker.join(2000);

    // Subsequent calls should return null/false immediately.
    System.out.println("TakeAny after close: " + buffet.TakeAny(1));
    System.out.println("TakeVeg after close: " + buffet.TakeVeg(1));
    System.out.println("AddPizza after close: " + buffet.AddPizza(1, SliceType.Meat));

    System.out.println("=== End BuffetClosure ===");
  }

  // - Stress tests with multiple concurrent threads
  public static void StressTest(Buffet buffet) throws InterruptedException {
    System.out.println("=== StressTest ===");

    java.util.List<Thread> threads = new java.util.ArrayList<>();
    final int NUM_BAKERS = 10;
    final int NUM_EATERS = 10;

    // Create multiple baker threads
    for (int i = 0; i < NUM_BAKERS; i++) {
      final int bakerId = i;
      Thread baker = new Thread(() -> {
        for (int j = 0; j < 20; j++) {
          SliceType type = (bakerId % 3 == 0) ? SliceType.Cheese :
                          (bakerId % 3 == 1) ? SliceType.Meat : SliceType.Veggie;
          System.out.println("Baker" + bakerId + " adding 1 " + type + " slice...");
          if (!buffet.AddPizza(1, type)) {
            System.out.println("Baker" + bakerId + " stopped: buffet closed.");
            break;
          }
          System.out.println("Baker" + bakerId + " done adding 1 " + type + " slice.");
        }
      }, "baker" + i);
      threads.add(baker);
    }

    // Create multiple eater threads
    for (int i = 0; i < NUM_EATERS; i++) {
      final int eaterId = i;
      Thread eater = new Thread(() -> {
        for (int j = 0; j < 10; j++) {
          if (eaterId % 2 == 0) {
            System.out.println("Eater" + eaterId + " requesting 2 slices with TakeAny...");
            List<SliceType> got = buffet.TakeAny(2);
            System.out.println("Eater" + eaterId + " got: " + got);
          } else {
            System.out.println("Eater" + eaterId + " requesting 2 slices with TakeVeg...");
            List<SliceType> got = buffet.TakeVeg(2);
            System.out.println("Eater" + eaterId + " got: " + got);
          }
        }
      }, "eater" + i);
      threads.add(eater);
    }

    // Start all threads
    for (Thread t : threads) {
      t.start();
    }

    // Wait for completion
    for (Thread t : threads) {
      t.join(10000);
    }

    // Check for hangs
    boolean hasBlocked = false;
    for (Thread t : threads) {
      if (t.isAlive()) {
        hasBlocked = true;
        break;
      }
    }

    if (hasBlocked) {
      System.out.println("WARNING: Stress test threads still blocked; closing buffet.");
      buffet.close();
      for (Thread t : threads) {
        t.join(1000);
      }
    } else {
      System.out.println("Stress test completed: 20 threads, 400 operations total!");
    }

    System.out.println("=== End StressTest ===");
  }

  private static void expectThrows(String label, Class<? extends Throwable> expected, Runnable action) {
    try {
      action.run();
      System.out.println(label + " -> FAIL (no exception)");
    } catch (Throwable t) {
      if (expected.isInstance(t)) {
        System.out.println(label + " -> PASS (" + t.getClass().getSimpleName() + ")");
      } else {
        System.out.println(label + " -> FAIL (" + t.getClass().getSimpleName() + ")");
      }
    }
  }

  private static void printSlices(String label, List<SliceType> slices) {
    System.out.println(label + " -> " + slices);
  }
}
