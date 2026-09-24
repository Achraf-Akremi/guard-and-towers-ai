# 🏰 Guard & Towers – Game-Playing AI

A strong game-playing AI for the two-player strategy board game **Guard & Towers**, written in **Java** with classic symbolic AI techniques: Alpha-Beta search, Zobrist hashing, transposition tables, null-move pruning and move-ordering heuristics.

> Developed in the project course **"Symbolische Künstliche Intelligenz"** (SoSe 2025) at **Technische Universität Berlin**, Fachgebiet AOT.

---

## 📋 Table of Contents

- [About the Game](#-about-the-game)
- [Features](#-features)
- [Repository Structure](#-repository-structure)
- [Getting Started](#-getting-started)
- [Architecture](#-architecture)
- [Evaluation Function](#-evaluation-function)
- [Development Milestones](#-development-milestones)
- [Benchmarks](#-benchmarks)
- [Future Work](#-future-work)
- [Team](#-team)
- [References](#-references)

---

## 🎲 About the Game

Guard & Towers is played on a **7×7 board** by two players (red and blue). Each player starts with **7 soldiers and 1 guard**.

| Rule | Description |
|---|---|
| **Setup** | Guards start on their guard houses: **D1** and **D7**. |
| **Towers** | Two or more of a player's soldiers on the same square form a tower. Its height = number of stacked soldiers. |
| **Movement** | Orthogonal only. A tower moves **exactly** as many squares as its height, and the path must be empty. |
| **Splitting & merging** | A player may move part of a tower or merge towers on the same square. |
| **Capturing** | A tower may capture an enemy piece of **equal or lower height**. |

**Win conditions:** capture the opponent's guard, **or** move your own guard onto the opponent's guard house.

---

## ✨ Features

- ✅ Complete, JUnit-tested **move generator**
- 🔍 **Alpha-Beta search** with **iterative deepening**
- 🧮 **Zobrist hashing** (64-bit position keys)
- 🗃️ **Transposition table** for reusing evaluated positions
- ⚡ **Null-move pruning**
- 📊 **Move ordering**: captures, killer moves, history heuristic, TT moves
- 🧠 **Phase-aware evaluation** (opening / middlegame / endgame)
- ♟️ **Three opening formations**, chosen at random
- 🖥️ **Swing GUI** with four modes: Human vs Computer, Human vs Human, Computer vs Computer, Benchmarks
- 🌐 **Game-server client** for playing tournament matches

---

## 📁 Repository Structure

```
guard-and-towers-ai/
├── README.md
└── src/
    ├── main/        Main.java – entry point (GUI and command-line mode)
    ├── game/        GamePanel, Board, BoardParser, Mouse, BackgroundPanel
    ├── pieces/      Piece, Tower, Guard
    ├── ki/          AlphaBetaAI, TranspositionTable, Zobrist
    ├── benchmark/   Move, MoveGenerator, MoveFormatter
    ├── res/piece/   piece images and splash screen
    ├── tests/       JUnit tests and Minimax benchmark
    └── client/      client.py – Python client for the game server
```

---

## 🛠️ Getting Started

### Prerequisites

- **Java 24** or newer (the release JAR is compiled with JDK 24)
- For the game-server client: **Python 3**

### Option 1: Download the JAR

Download `game.jar` from the [Releases](../../releases) page, then run:

```bash
java -jar game.jar
```

### Option 2: Build from source

```bash
git clone https://github.com/Achraf-Akremi/guard-and-towers-ai.git
cd guard-and-towers-ai/src

javac main/*.java pieces/*.java benchmark/*.java game/*.java ki/*.java
java main.Main
```

> Run the program from inside `src/`, because the GUI loads its images from `res/piece/` relative to the working directory.

### Using the GUI

1. After the splash screen, choose a mode, for example **Human vs Computer**.
2. Choose which colour the AI plays.
3. Enter a FEN string to start from a specific position, or just press **OK** to use the default starting position.

### Command-line mode

When given a position as its only argument, the program prints the AI's best move (1-second search budget) and exits:

```bash
java -jar game.jar "7/2r1RG3/2r1r13/7/b12BG2b1/1b12b12/4b12 b"
# → e.g. A7-B7-1
```

This is the mode used by the game-server client.

### Playing on the game server

```bash
python src/client/client.py
# or with an explicit path to the JAR:
python src/client/client.py --jar path/to/game.jar
```

The client finds `game.jar` automatically if it is placed in `src/client/` or `src/`. You can also set the `GAME_JAR` environment variable. Other options: `--timeout` (default 5 s), `--java` and `--fallback-move`.

The client needs the course-provided `network.py` in the same folder as `client.py`.

### Running the tests

The tests in `src/tests/` use **JUnit 5**. Add JUnit 5 to your IDE (e.g. IntelliJ IDEA) and run them from there. They are not part of the `javac` command above, so the main program builds without JUnit.

---

## 🏗️ Architecture

| Package | Class | Responsibility |
|---|---|---|
| `main` | `Main` | Entry point: splash screen, mode selection, command-line move output |
| `game` | `GamePanel` | Game loop, applying moves, switching players |
| `game` | `Board` | Board rendering |
| `game` | `BoardParser` | Parses FEN strings into pieces |
| `game` | `Mouse` | Mouse input for human players |
| `pieces` | `Piece`, `Tower`, `Guard` | Piece model, movement checks, drawing |
| `ki` | `AlphaBetaAI` | Search, move ordering, evaluation, phase detection, openings |
| `ki` | `TranspositionTable` | Stores search results by position hash |
| `ki` | `Zobrist` | Computes and updates 64-bit position hashes |
| `benchmark` | `MoveGenerator` | Generates all legal moves |
| `benchmark` | `Move`, `MoveFormatter` | Move representation and notation (e.g. `A7-B7-1`) |

---

## 🧠 Evaluation Function

The game phase is detected from the number of towers on the board:

| Phase | Towers on board |
|---|---|
| Opening | ≥ 15 |
| Middlegame | 8 – 14 |
| Endgame | < 8 |

Each phase weights these heuristics differently:

| Criterion | Effect |
|---|---|
| **Win / loss detection** | ±∞ when a game-ending position is reached |
| **Material** | Own tower height × phase factor; opponent pieces −10 each |
| **Centre control** | Bonus for keeping the guard close to the centre |
| **Danger analysis** | Up to −2000 for threats against the own guard |
| **Threat potential** | Bonus for towers aligned with the enemy guard (stronger in endgame) |
| **Guard defence** | Bonus for towers within 2 squares of the guard; +50 for blocking positions |
| **Mobility** | Difference between own and opponent legal moves |

---

## 🚀 Development Milestones

| Milestone | Version | Highlights |
|---|---|---|
| **1** | Dummy AI | Simple board representation, random/predefined moves, GUI, JUnit-tested move generator |
| **2** | Base AI | Alpha-Beta search, iterative deepening, heuristic evaluation, full games via game server |
| **3** | Extended AI | Zobrist hashing, transposition table, move ordering, null-move pruning |
| **4** | Optimised AI | Opening formations, phase-dependent dynamic evaluation |

### Opening Formations

- **W-Tactic** – pieces form a "W" covering flanks and centre; early space control and flank attacks.
- **Square Formation** – compact 2×2 square in front of the guard; solid defence and central control.
- **Triangle Formation** – inverted triangle around the guard; maximum protection against early attacks.

---

## 📈 Benchmarks

Execution time in the **middlegame** at search depth 5 (identical positions):

| Version | Time at depth 5 |
|---|---|
| Minimax | ~4,900,000 ms |
| Alpha-Beta (Milestone 2) | ~331,000 ms |
| Alpha-Beta + optimisations (Milestone 3) | ~77,000 ms |

➡️ About **4× faster** than plain Alpha-Beta and about **60× faster** than Minimax.

In the endgame, the number of searched states at depth 5 dropped from about **7,000 to 1,500**.

In test games against earlier versions, the final AI showed more stable openings, much better detection of threats to its own guard, and more effective use of tactical chances in the endgame.

---

## 🔭 Future Work

- Monte Carlo Tree Search (MCTS) or machine-learning based evaluation
- Multithreaded search to use multi-core CPUs
- Visualisation of search trees and position evaluations

---

## 👥 Team

**Gruppe V** – TU Berlin, SoSe 2025

- [Achraf Akremi](https://github.com/Achraf-Akremi)
- Mohamed Hedi Ben Brahim
- Mohamed Rami Ben Moussa
- Rami Zayati

---

## 📚 References

- TU Berlin PJ KI lecture slides: *KI-Basistechniken für Schach* and *Fortschrittliche KI-Techniken für Schach*
- *Replacement Schemes for Transposition Tables*
- [Zobrist hashing – Wikipedia](https://en.wikipedia.org/wiki/Zobrist_hashing)
- [Transposition table – Wikipedia](https://en.wikipedia.org/wiki/Transposition_table)
- [Null Move Pruning – Chessprogramming Wiki](https://www.chessprogramming.org/Null_Move_Pruning)

---

## 📄 License

Created for educational purposes at TU Berlin.
