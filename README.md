# THE LAST ALIBI
### A Murder Mystery – Advanced Algorithms Project

**Three worlds. Multiple suspects. One murderer. Can you solve the case?**

---

## About the Project

**The Last Alibi** is an interactive murder-mystery grid logic game in which players investigate crime scenes, analyse suspect statements, interpret clues and identify the murderer.

Set across three themed worlds — Hollywood, Jaipur and Arabian Nights — the game challenges players to position suspects correctly on a mansion floor plan using logical deductions.

The project combines an interactive browser-based gaming experience with a Java implementation of advanced algorithms and data structures.

Developed as part of the **Advanced Algorithms** subject, the project demonstrates how computational techniques can be applied to practical problem-solving scenarios.

## Game Features

- **Three Themed Worlds:** Hollywood, Jaipur and Arabian Nights.
- **Multiple Cases and Difficulty Levels:** Progress through different murder mysteries.
- **Interactive Grid:** Place suspects on the floor plan using furniture, room and positional clues.
- **Visual Clues:** Examine rooms, furniture and suspect statements to eliminate impossible positions.
- **X-Mark System:** Mark and eliminate squares while solving a puzzle.
- **Clue Search:** Search suspect clues using string-matching and autocomplete techniques.
- **Hint System:** Purchase hints to receive assistance.
- **Solvability Checking:** Analyse whether the current arrangement can lead to a valid solution.
- **Detective Route:** Generate an approximate route through selected locations.
- **Notes:** Record observations about individual suspects.
- **Scoring and Progress:** Earn points and stars based on performance.

## Technologies Used

| Area | Technology |
|---|---|
| Core algorithms | Java |
| Java version | JDK 17 |
| Build automation | Apache Maven |
| Browser interface | HTML5, CSS3, JavaScript |
| Visual rendering | HTML Canvas |
| Version control | Git and GitHub |
| Web deployment | Render |

## Advanced Algorithms Implemented

The Java implementation incorporates a collection of algorithms and data structures.

| Algorithm / Data Structure | Application |
|---|---|
| Backtracking | Puzzle solving |
| Bipartite Matching | Constraint pruning |
| Counting Sort | Solver search ordering |
| Greedy Set Cover | Clue selection |
| Las Vegas Randomized Algorithm | Puzzle generation |
| Edmonds-Karp | Maximum flow and solvability analysis |
| Ford-Fulkerson | Maximum flow |
| Fibonacci Heap | Hint management |
| AVL Tree | Ranking and leaderboard operations |
| Red-Black Tree | Suspect notes |
| Splay Tree | Self-adjusting search tree demonstration |
| Binary Heap | Priority queue operations |
| Segment Tree | Range queries and tightest-row analysis |
| Sparse Table | Range query demonstrations |
| Disjoint Set Union | Room connectivity |
| Bloom Filter | Repeated seed detection |
| Trie | Clue autocomplete |
| Radix Tree | String indexing |
| Suffix Array | String processing |
| Suffix Tree | String processing |
| KMP | Clue searching |
| TSP Approximation | Detective route planning |
| Knuth's Algorithm | Monte Carlo tree-size estimation |

### Algorithm Lab

The project includes an Algorithm Lab that demonstrates advanced algorithmic concepts through executable examples.

It covers graph algorithms, sorting, searching, trees, heaps, range-query structures, string algorithms, randomized algorithms and computational complexity concepts.

## Project Architecture

```text
The-Last-Alibi/
│
├── pom.xml
├── README.md
├── .gitignore
│
└── src/
    └── main/
        ├── java/
        │   └── com/lastalibi/
        │       ├── Main.java
        │       ├── GuiLauncher.java
        │       │
        │       ├── algo/
        │       │   ├── Alg.java
        │       │   ├── AVL.java
        │       │   ├── RBT.java
        │       │   ├── FibHeap.java
        │       │   ├── BinaryHeap.java
        │       │   ├── Flow.java
        │       │   ├── DSU.java
        │       │   ├── SegTree.java
        │       │   ├── Sparse.java
        │       │   ├── Trie.java
        │       │   ├── RadixTree.java
        │       │   ├── SuffixArray.java
        │       │   ├── SuffixTree.java
        │       │   ├── Splay.java
        │       │   └── Bloom.java
        │       │
        │       └── game/
        │           ├── World.java
        │           ├── Cases.java
        │           ├── Clue.java
        │           ├── Solver.java
        │           ├── Generator.java
        │           ├── Analysis.java
        │           ├── Game.java
        │           ├── Progress.java
        │           └── Lab.java
        │
        └── resources/
            └── web/
                ├── index.html
                └── assets/
                    └── beginner-mode-visual-reference.png
```

## Getting Started

### Prerequisites

Install the following:

- Java Development Kit (JDK 17 or newer)
- Apache Maven 3.8 or newer
- Git

### Clone the Repository

```bash
git clone https://github.com/TasmiyaKhan696/The-Last-Alibi.git
cd The-Last-Alibi
```

### Build the Project

```bash
mvn clean package
```

### Run the Application

```bash
java -jar target/the-last-alibi-1.0.0.jar
```


**The Last Alibi**  
*Every suspect has a story. Every clue has a purpose. Every alibi can be questioned.*
