# CSC4180 Operating Systems

Coursework repository with three Java programs that count right triangles in 2D point sets. Each program builds on the previous one with performance and storage improvements.

## Workspace Layout

- Program 1/ - single-process baseline
- Program 2/ - multi-thread and multi-process versions
- Program 3/ - PointStore abstraction with text and binary input support

Each program folder contains:
- com/tryright/ - Java source files
- test/ - input datasets, TestPlan.txt, and PDFs

## Build

From any program folder:

```
javac com/tryright/*.java
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

## Tests

Test inputs and expected outputs are documented in each program's test/TestPlan.txt.

## License

Student coursework for CSC4180. All rights reserved.
