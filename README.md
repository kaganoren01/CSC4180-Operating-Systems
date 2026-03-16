# CSC4180 Operating Systems

Coursework repository for Java programs exploring concurrency, multi-processing, and synchronization in an OS context.

## Workspace Layout

- Program 1/ - single-process right triangle counter baseline
- Program 2/ - multi-thread and multi-process versions
- Program 3/ - PointStore abstraction with text and binary input support
- Program 4/ - JUnit-tested PointStore + pizza buffet concurrency problem (three synchronization implementations)

Each program folder contains:
- com/tryright/ - Java source files for right triangle counting
- com/pizza/ - Java source files for the pizza buffet problem (Programs 3–4)
- test/ - input datasets, TestPlan.txt, and PDFs
- lib/ - third-party JARs (JUnit, Programs 3–4)

## Build

From any program folder:

```
javac com/tryright/*.java
```

To compile Program 4 pizza sources:

```
javac com/pizza/*.java
```

## Run

### Single-process

```
java com.tryright.Triangles test/<testfile>
```

### Multi-thread

```
java com.tryright.ThreadTriangles test/<testfile> <num_threads>
```

### Multi-process

```
java com.tryright.ProcessTriangles test/<testfile> <num_processes>
```

## Input Formats

### Text (.txt)

First line is the number of points. Each subsequent line is "x y".

### Binary (.dat)

Raw stream of 4-byte big-endian signed integers (two's complement). Each point is two ints: x then y. File size must be a multiple of 8 bytes.

Example for one point (1, 2):

```
00 00 00 01 00 00 00 02
```

## Program 3 Notes

- PointStore interface abstracts storage.
- TextPointStore loads text into int arrays.
- BinPointStore uses memory-mapped I/O for binary files.
- TrianglesUtils auto-detects .dat files.

## Program 4 Notes

### PointStore JUnit Tests

`PointStoreTest.java` uses JUnit Jupiter (via `lib/junit-platform-console-standalone-1.10.2.jar`) to validate both `TextPointStore` and `BinPointStore` against correct and malformed input files.

Run tests from the `Program 4/` folder:

```
java -jar lib/junit-platform-console-standalone-1.10.2.jar --class-path . --select-class com.tryright.PointStoreTest
```

### Pizza Buffet Concurrency Problem

The `com/pizza/` package implements a thread-safe pizza buffet (`Buffet` interface) in three ways:

| Class | Mechanism |
|---|---|
| `BuffetSynchronized` | Java `synchronized` methods + `notifyAll()` |
| `BuffetLock` | `ReentrantLock` + `Condition` variables |
| `BuffetSemaphore` | `Semaphore` primitives |

Key semantics:
- `TakeAny(n)` — blocks a non-vegetarian patron until *n* slices are available; prioritizes non-veg slices when vegetarian patrons are waiting.
- `TakeVeg(n)` — blocks a vegetarian patron until *n* vegetarian slices (cheese or veggie) are available.
- `AddPizza(n, type)` — adds slices one at a time, blocking when the buffet is at capacity.
- `close()` — signals all blocked threads to unblock and return `null`/`false`.

## Tests

Test inputs and expected outputs are documented in each program's test/TestPlan.txt.

## License

Student coursework for CSC4180. All rights reserved.
