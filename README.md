# ++Tetris

++Tetris is a roguelike version of Tetris, made in Java Swing by Alex and Noah for the CBL
project of Programming (2IP90).

A run starts as normal Tetris. Every 5 cleared lines the game pauses and lets you choose
one of up to three upgrades. Some upgrades stay for the rest of the run, others are single-
use tools that can get you out of a bad position. After 30 cleared lines the first boss,
**Bedrock**, appears and pushes unbreakable rows into the board. To survive, you have to use 
the upgrades you collected and score enough points before the boss timer runs out. Beat it
and it comes back stronger 30 lines later.

---

## Requirements

* **Java JDK 17 or newer.** The game was developed with JDK 21.
* Windows, macOS or Linux.
* No external libraries. The game only uses the standard Java library (Swing and AWT),
* so nothing else has to be installed or downloaded.
* The tests use JUnit 5, which is included in the `lib` folder.

To check if Java is installed, open a terminal (Command Prompt or PowerShell on Windows)
and run:

```
javac -version
```

If this prints a version of 17 or higher, you are ready. If the command is not found, install a JDK,
for example [Eclipse Temurin](https://adoptium.net/), and open a new terminal.

---

## How to build and run

1. Unzip the project.
2. Open a terminal **inside the project folder**, which is the folder that contains `src` and
this `README.md`.
3. Compile the game:

   ```
   javac -d bin -sourcepath src src/game/Main.java
   ```

   This compiles every class into a new folder called `bin`.

4. Start the game:

   ```
   java -cp bin game.Main
   ```

A 1280×720 window opens and the first run starts immediately. These two commands are
the same on Windows, macOS and Linux.

**Shortcut for macOS and Linux:** `run.sh` does both steps in one go.

```
./run.sh
```

If this says "permission denied", run `chmod +x run.sh` once and try again.

**From an IDE:** open the project folder in IntelliJ IDEA, Eclipse or VS Code, mark `src` as
the source folder and run the `main` method in `game.Main`.

### Running the tests

On macOS and Linux, `test.sh` compiles the game and the tests and runs them with JUnit:

```
./test.sh
```

On Windows, compile the test files (the game classes they use are compiled automatically)
and run them:

```
javac -cp lib/junit-platform-console-standalone-1.11.4.jar -sourcepath src -d bin-test tests/game/ScoreTrackerTest.java tests/boss/BedrockBossTest.java
java -jar lib/junit-platform-console-standalone-1.11.4.jar execute --class-path bin-test --scan-class-path
```

When you add a new test file, add it to the end of the `javac` command.

---

## Controls

### During play

| Key | Action |
|---|---|
| ← / → or A / D | Move the piece left or right |
| ↑ or W | Rotate the piece clockwise |
| ↓ or S | Move the piece down one row |
| Space | Hard drop: the piece falls straight down and locks |
| P | Pause or continue |
| R | Start a new run (also works after game over) |

### Upgrade keys (fix later)

These only work after you have picked the upgrade. The keys of the upgrades you own are
shown next to the board.

| Key | Upgrade | Action |
|---|---|---|
| C or Shift | Hold | Put the falling piece in hold slot 1, or swap it back |
| V / B | Hold (level 2 / 3) | Use hold slot 2 / 3 |
| H | Hammer | Choose a row and smash it |
| G | Patch | Fill up to 3 adjacent empty cells |
| F | Flush | Wash away the bottom 3 rows |

### Menus

| Screen | Keys |
|---|---|
| Choosing an upgrade | ← / → to highlight, Enter or Space to take it, or press 1, 2 or 3 directly |
| Aiming the Hammer or Patch | Arrow keys or WASD to move the cursor, Space to mark a cell (Patch), Enter to confirm, Esc to cancel without using the upgrade |
| Boss intro and result | Enter or Space to continue |

---

## Gameplay

### Scoring and levels

Clearing 1, 2, 3 or 4 lines at once gives 100, 300, 500 or 1000 points, times the current
level. Every 10 cleared lines the level goes up and the pieces fall faster. A clear that
takes you to a new level is still scored at the old level. Pieces are dealt from a shuffled "bag" of all seven shapes, so you
never wait too long for a specific piece. An outline on the board (the ghost piece) shows
where the falling piece will land.

### Upgrades

| Upgrade | Kind | What it does |
|---|---|---|
| Hold | Permanent, up to level 3 | Lets you keep a piece aside for later. Every level adds one more slot. You can hold once per piece. |
| See the Future | Permanent, up to level 3 | Shows one more upcoming piece per level, up to four in total. |
| Hammer | Single use | Destroys one row of your choice and the rows above fall down. Gives points per destroyed block, but doesn't count as a cleared line. |
| Patch | Single use | Places up to 3 connected blocks into gaps in your stack. Lines it completes count as normal line clears. |
| Flush | Single use | Removes the bottom 3 rows. Gives no points, it is an emergency button. |

An upgrade that is already at its maximum level, or a single-use upgrade you haven't used
yet, is not offered again.

### The boss: Bedrock

At 30 cleared lines Bedrock appears and an intro screen explains the fight. When you press
Enter, 4 rows of grey unbreakable stone rise from the bottom of the board and push your
stack up. You have **2 minutes to score 2500 × your level in points** (10,000 points if it
appears at level 4). The target is set when Bedrock appears, so it grows along with the
points you get per clear. The boss timer keeps running in the upgrade menu and while
aiming, so only pausing with P stops it.

* **Win:** the bedrock crumbles away and the run continues. Bedrock returns 30 cleared
lines later, with a target based on your level at that time.
* **Lose:** the run is over.

The run also ends when the stack reaches the top of the board. Press R to start again.

---

## Project structure

```
tetris/
├── README.md
├── run.sh                 Compiles and starts the game (macOS / Linux)
├── test.sh                Compiles and runs the tests (macOS / Linux)
├── lib/                   The JUnit jar, only used by the tests
├── tests/                 JUnit tests, in the same packages as the classes they test
│   ├── game/              ScoreTrackerTest
│   └── boss/              BedrockBossTest
└── src/
    ├── game/              Rules of the game, no drawing
    │   ├── Main.java          Opens the window and starts the game
    │   ├── PlayManager.java   Runs one game: pieces, locking, clears, which screen comes next
    │   ├── Board.java         The 10×20 grid, line clears and unbreakable rows
    │   ├── PiecePicker.java   Deals pieces from a shuffled bag of seven
    │   ├── ScoreTracker.java  Score, lines and level
    │   ├── Timer.java         Falling speed and lock delay, counted in frames
    │   ├── GameState.java     The screens the game can be in (PLAY, SELECT_UPGRADE, BOSS_INTRO, ...)
    │   └── ClearEvent.java, ClearHistory.java, ClearSource.java
    │                          A record of every clear during a run
    ├── tetromino/         The pieces
    │   ├── Shape.java         The seven shapes, their colours and blocks
    │   └── Tetromino.java     The falling piece: moving, rotating and wall kicks
    ├── upgrades/          One class per upgrade (Hold, SeeTheFuture, Hammer, Patch, Flush),
    │                      Upgrade (name, level, description), Upgrader (when and which
    │                      upgrades are offered) and UpgradeSelection
    ├── boss/              BedrockBoss and BossOutcome
    └── gui/               Everything that uses Swing
        ├── GamePanel.java     The window content and the game loop (60 frames per second)
        ├── KeyHandler.java    Keyboard input
        ├── Renderer.java      Draws the board, pieces and side panels
        ├── UpgradeMenu.java   Draws the upgrade choice screen
        └── BossPanel.java     Draws the boss timer, score and intro/result screens
```

The game logic works with board cells, not pixels. `gui.Renderer` is the only class that converts cells into pixels, so the rules of the game are kept separate from how it is drawn.

---

## Troubleshooting

* **`javac` is not recognised or not found:** a JDK is not installed or not on your PATH. Install JDK 17 or newer and open a new terminal.
* **"switch rules are not supported" when compiling:** your JDK is older than 17. Install a newer one.
* **The game doesn't react to the keyboard:** click once inside the game window so it has focus.

---

## Credits

* Our first version of the game loop and class layout was based on RyiSnow's Tetris
in Java tutorial on YouTube (<https://www.youtube.com/@RyiSnow>). Most of that code was 
later rewritten.
* The Tetris rules we followed come from the [Tetris Guideline](https://tetris.wiki/Tetris_Guideline) on TetrisWiki.
* Source code and history: <https://github.com/xoes0teric/tetris>