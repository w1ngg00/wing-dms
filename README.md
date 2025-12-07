# COMP2042 Coursework - Tetris Maintenance and Extension

---

## GitHub Repository
[https://github.com/w1ngg00/wing-dms](https://github.com/w1ngg00/wing-dms)

---

## Compilation Instructions

### Prerequisites
- **Java Development Kit (JDK)**: Version 11 or higher
- **JavaFX SDK**: Version 21 or compatible with your JDK
- **Maven**: Version 3.6 or higher (or use the included `mvnw`/`mvnw.cmd`)

### Step-by-Step Build Instructions

1. **Open the project** in IntelliJ IDEA or Visual Studio Code
   - Navigate to the project folder: `c:\Users\wingg\Downloads\COMP2042ThamWingLok\wing-dms-master\CW2025-master`

2. **Configure Maven**
   - Right-click on `pom.xml` in the project root
   - Select **"Add as Maven Project"** (or **"Reload All Maven Projects"** if already configured)
   - Wait for Maven to download dependencies

3. **Set Up Run Configuration**
   - Open **Run → Edit Configurations**
   - Create a new **Application** configuration
   - Set **Main class** to: `com.comp2042.view.Main`
   - Set **Working directory** to: `$PROJECT_DIR$`

4. **Configure JavaFX Module Options**
   - Click **Modify options** in the run configuration
   - Select **"Add VM options"**
   - Add the following to the **VM options** field:
   ```
   --module-path "PATH_TO_JAVAFX_SDK_LIB" --add-modules javafx.controls,javafx.fxml,javafx.media
   ```
   - Replace `PATH_TO_JAVAFX_SDK_LIB` with your JavaFX SDK lib path (e.g., `C:\javafx-sdk-21\lib`)

5. **Apply and Run**
   - Click **Apply** and **OK**
   - Click the **Run** button or press `Shift + F10` to start the application

### Building from Command Line
```bash
mvn clean compile
mvn javafx:run
```

---

## Implemented and Working Properly

### Core Game Features
- **Game Board Display**: proper brick rendering and collision detection
- **Brick Movement**: Left/Right arrow keys for horizontal movement with boundary checking
- **Brick Rotation**: Up arrow or down arrow key to rotate pieces with wall-kick collision detection
- **Soft Drop**: Press `Space` once for controlled downward movement
- **Hard Drop**: Double-tap `Space` for instant brick placement
- **Game Over Detection**: Detects when bricks spawn above the playfield boundary
- **Line Clearing**: Identifies and removes complete rows with proper grid updates and score increment

### Advanced Features
- **Hold Feature**: Press `V` to hold the current brick and swap with the stored piece (once per turn)
- **Multiple Next Bricks**: Displays 4 upcoming pieces in the preview panel on the right side
- **Ghost Piece (Drop Forecast)**: Semi-transparent preview showing where the current brick will land
- **Custom Keybindings**: Players can rebind all controls in the Settings menu; bindings persist in `settings.txt`

### Difficulty System
- **Easy Mode**: Standard Tetris rules without modifications
- **Normal Mode**: Speed increases as rows are cleared (acceleration configured in `GameConfig`)
- **Hard Mode**: Pre-placed obstacles at game start to increase challenge
- **Extra Hard Mode**: Hard mode + random obstacle generation during gameplay; unlocked after achieving 5000+ score in Easy, Normal, and Hard modes

### User Interface
- **Main Menu**: Navigate between Start Game, difficulties,  Settings, and Exit options
- **Settings Screen**: 
  - Volume control sliders for sound effects
  - Visual keybinding customization interface
  - Settings persist between sessions in `settings.txt`
- **Pause/Resume**: Press `Escape` to pause mid-game; press again to resume without losing progress
- **Game Over Screen**: Displays final score with "Back to Menu" button
- **High Score Tracking**: Persistent high scores per difficulty mode stored in `highscore_easy.txt` and `highscore_hard.txt`

### Audio System
- **Sound Effects**: Audio cues for line clears and level up events
- **Volume Control**: Adjustable sound effect volume via Settings menu
- **Resource Management**: All audio files preloaded at application startup

---

## Implemented but Not Working Properly

**Game Fullscreen** - because of how the bricks and game board are anchored bricks are slightly off and the game board moves around a little when bricks fall down.
**settings music** - the background music from the main menu will replay after going into settings and clicking main menu again, the music will overlap twice
**game over screen** - game over screen font doesnt show properly only the main menu button can be seen clearly


---

## Features Not Implemented

**AI Opponent / Smart Hint System** -An AI that could intelligently suggest moves or compete against the player was not included. Implementing a robust AI would require algorithms capable of evaluating multiple board states and predicting future piece placements, which exceeded the current project timeline.
**Puzzle / Challenge Mode** - A mode where players solve pre-defined puzzles with limited pieces was planned but not implemented. Designing multiple challenging puzzles and integrating them with the existing game mechanics would have extended development time significantly.
**Custom Themes and Skins** - Although the idea of customizable Tetromino textures and dynamic backgrounds was considered, this feature was left out due to the focus on core gameplay mechanics. Adding this would have required additional resources for artwork and UI management.


---

## New Java Classes

### Controller Layer
- **`com.comp2042.controller.InputHandler`**
  - **Purpose**: Centralized keyboard input processing with custom keybinding support
  - **Responsibility**: Maps raw key events to game actions (move, rotate, drop, hold); implements double-tap detection for hard drop; loads custom bindings from `GameSettings`
  - **Reason**: Extracted from `GuiController` to follow Single Responsibility Principle; enables custom controls and decouples input logic from UI rendering

- **`com.comp2042.controller.EventSource`** (Enum)
  - **Purpose**: Identifies the origin of a game event
  - **Values**: `USER` (player input), `THREAD` (automated/game loop events)
  - **Reason**: Distinguishes player-initiated actions from timer-driven events for proper event sequencing

- **`com.comp2042.controller.EventType`** (Enum)
  - **Purpose**: Defines all possible game event types
  - **Values**: `MOVE_LEFT`, `MOVE_RIGHT`, `ROTATE`, `DROP`, `HOLD`, `HARD_DROP`, `PAUSE`, `RESUME`
  - **Reason**: Provides type-safe event classification and enables compile-time checking

- **`com.comp2042.controller.MoveEvent`**
  - **Purpose**: Encapsulates event information (type, source) in MVC architecture
  - **Fields**: `eventType`, `eventSource`
  - **Reason**: Decouples `InputHandler` from `GameController` via event objects; enables loose coupling

### Model Layer
- **`com.comp2042.GameConfig`**
  - **Purpose**: Centralized configuration class for all magic numbers and game constants
  - **Key Constants**: `BOARD_WIDTH`, `BOARD_HEIGHT`, `DROP_INTERVAL_MS`, `DIFFICULTY_SPEED_MULTIPLIER`, etc.
  - **Reason**: Improves readability and maintainability; allows easy parameter adjustment without code recompilation

- **`com.comp2042.model.HighScoreManager`**
  - **Purpose**: Manages persistent high score data for each difficulty level
  - **Responsibility**: Reads/writes high scores to `highscore_easy.txt`, `highscore_normal.txt`, `highscore_hard.txt`, `highscore_extra.txt`
  - **Methods**: `getHighScore(Difficulty)`, `setHighScore(Difficulty, int)`, `checkAllModesUnlocked()`, `loadScores()`, `saveScores()`
  - **Reason**: Follows SRP by isolating file I/O logic from `GameController`; enables high score tracking per difficulty

- **`com.comp2042.model.GameSettings`**
  - **Purpose**: Manages persistent user settings (keybindings, volume preferences)
  - **Responsibility**: Loads/saves settings from `settings.txt` using key=value format
  - **Methods**: `loadSettings()`, `saveSettings()`, `getVolume()`, `setVolume(double)`, `getKeyBinding(String)`
  - **Reason**: Separates settings persistence from UI controllers; maintains state across application restarts

- **`com.comp2042.model.Difficulty`** (Enum)
  - **Purpose**: Defines game difficulty levels with associated parameters
  - **Values**: `EASY`, `NORMAL`, `HARD`, `EXTRA`
  - **Properties**: `baseSpeed`, `speedMultiplier`, `hasObstacles`, `requiresUnlock`
  - **Reason**: Provides type-safe difficulty selection; enables mode-specific behavior and unlock conditions

- **`com.comp2042.model.Score`**
  - **Purpose**: Manages game score state with JavaFX observable properties
  - **Responsibility**: Tracks score, level, lines cleared; fires property change events for UI binding
  - **Properties**: `scoreProperty()`, `levelProperty()`, `linesClearedProperty()`
  - **Reason**: Enables automatic UI updates via property binding; decouples score logic from view rendering

- **`com.comp2042.model.MatrixOperations`**
  - **Purpose**: Utility class for matrix/grid operations on the game board
  - **Methods**: `rotateMatrix()`, `isBoundaryValid()`, `checkCollision()`, `clearRow()`, `compressRows()`
  - **Reason**: Centralizes grid manipulation logic; improves code reusability and testability

- **`com.comp2042.model.ViewData`** (DTO)
  - **Purpose**: Data Transfer Object for transferring current brick state to the view
  - **Fields**: `shape` (2D array), `positionX`, `positionY`, `rotationState`, `brickColor`
  - **Reason**: Decouples model representation from view rendering; prevents direct access to internal game state

- **`com.comp2042.model.DownData`** (DTO)
  - **Purpose**: Encapsulates results of downward movement operations
  - **Fields**: `canMoveDown`, `isPlaced`, `clearedLines`, `scoreGained`
  - **Reason**: Returns multiple values from movement operations without exposing mutable state

- **`com.comp2042.model.NextShapeInfo`** (DTO)
  - **Purpose**: Transfers upcoming brick information to preview panels
  - **Fields**: `shape`, `color`, `index` (position in queue)
  - **Reason**: Enables multiple preview displays without exposing the random generator queue

### View Layer
- **`com.comp2042.view.MainMenuController`**
  - **Purpose**: Controls main menu navigation and game mode selection
  - **FXML**: `src/main/resources/fxml/main_menu.fxml`
  - **Responsibility**: Handles Start Game, Settings, Exit buttons; displays difficulty selector; checks high scores for Extra Hard unlock; displays high score labels
  - **Methods**: `onStartGame()`, `onSettings()`, `onExit()`, `refreshHighScoreDisplay()`
  - **Reason**: Separates menu logic from main game controller; centralizes entry point navigation

- **`com.comp2042.view.SettingController`**
  - **Purpose**: Controls the Settings screen UI
  - **FXML**: `src/main/resources/fxml/settings_screen.fxml`
  - **Responsibility**: Volume adjustment sliders, keybinding UI, settings persistence via `GameSettings`
  - **Methods**: `onVolumeChange()`, `onKeyBindingChange()`, `saveSettings()`
  - **Reason**: Isolates settings UI logic from game controller; enables live settings preview

- **`com.comp2042.view.GameOverPanel`**
  - **Purpose**: Displays game over screen with final score and navigation
  - **Responsibility**: Shows final score, difficulty achieved, high score comparison, "Back to Menu" button
  - **Methods**: `setGameOverData(int score, Difficulty)`, `onBackToMenu()`
  - **Reason**: Separates game over UI from main game controller; provides clear game state closure

- **`com.comp2042.view.NotificationPanel`**
  - **Purpose**: Shows transient notifications during gameplay (line clears, level up)
  - **Responsibility**: Displays "Lines Cleared: X", "Level Up!", fade animations
  - **Methods**: `showLinesClearedNotification(int)`, `showLevelUpNotification()`
  - **Reason**: Provides user feedback without interrupting gameplay; improves visual communication

- **`com.comp2042.view.InputEventListener`** (Interface)
  - **Purpose**: Defines contract between `InputHandler` and `GameController`
  - **Methods**: `onMoveEvent(MoveEvent)`, `onDropEvent()`, `onRotateEvent()`
  - **Reason**: Enables loose coupling through dependency inversion; allows `InputHandler` to operate independently

- **`com.comp2042.view.Main`**
  - **Purpose**: Application entry point and global resource manager
  - **Responsibility**: Initializes JavaFX Stage, loads FXML files, preloads audio files, manages scene transitions
  - **Methods**: `start(Stage)`, `loadAudioResources()`, `switchScene(String)`
  - **Reason**: Centralizes resource management and application lifecycle; provides global access to audio files

---

## Modified Java Classes

### Controller Layer
- **`com.comp2042.controller.GameController`**
  - **Changes Made**:
    - Added `Difficulty` parameter to constructor for mode-specific behavior
    - Integrated `HighScoreManager` for high score persistence and unlock checking
    - Integrated `GameSettings` for loading custom keybindings
    - Added `obstacleTimeline` (AnimationTimer) for Extra Hard mode random obstacle spawning
    - Implemented `pause()` and `resume()` methods for pause/resume functionality
    - Added speed acceleration logic based on lines cleared (Normal, Hard, Extra modes)
    - Integrated sound effect triggering for line clears, level ups, and game over
    - Added `heldBrickProperty` observable for UI binding
    - Modified `dropBrick()` to implement hard drop mechanics
  - **Why Necessary**: 
    - Difficulty modes require different gameplay mechanics and win conditions
    - High score tracking requires persistent storage per difficulty
    - Custom keybindings must be loaded from settings file
    - Pause/resume prevents game state corruption mid-gameplay
    - Audio feedback improves user experience
    - Observable properties enable real-time UI synchronization

- **`com.comp2042.controller.InputHandler`**
  - **Changes Made**:
    - Loads custom keybindings from `GameSettings` on initialization
    - Implements double-tap detection (200ms threshold) for hard drop vs soft drop
    - Communicates with `GameController` via `InputEventListener` interface
    - Validates key bindings before execution
    - Tracks `lastSpaceKeyPressTime` for hard drop detection
  - **Why Necessary**: 
    - Custom controls must be loaded from persistent settings
    - Hard drop requires distinguishing single vs double space presses
    - Event-driven architecture enables loose coupling
    - Input validation prevents invalid game states

### Model Layer
- **`com.comp2042.model.SimpleBoard`** (implements `Board`)
  - **Changes Made**:
    - Added `heldBrick` field for hold feature implementation
    - Added `holdUsedThisTurn` flag to restrict hold to once per piece
    - Implemented `swapHoldBrick(IBrick currentBrick)` method with validation
    - Added `spawnObstacle(int x, int y)` method for Hard/Extra mode obstacles
    - Modified `placeBrick(IBrick brick)` to check hold availability and trigger line clear checks
    - Added `ghostBrickY` calculation in `getViewData()` for drop position preview
    - Implemented `getHoldBrickData()` method for hold brick panel display
    - Added `getGhostBrickY(int dropSpeed)` method to calculate ghost piece position
    - Modified constructor to accept `Difficulty` parameter for obstacle initialization
  - **Why Necessary**: 
    - Hold feature requires state tracking and validation
    - Difficulty-based obstacles need storage and collision detection
    - Ghost piece requires calculating final drop position without modifying game state
    - Observable properties enable UI binding for held brick display

- **`com.comp2042.model.bricks.RandomBrickGenerator`**
  - **Changes Made**:
    - Added internal `Queue<IBrick>` for managing 4+ upcoming bricks
    - Extended `newBrick()` to pre-generate next pieces when queue size drops below 3
    - Implemented `getNextBricks(int count)` method to retrieve upcoming pieces
    - Implemented `peekNextBrick(int index)` to preview specific bricks without consuming queue
    - Modified constructor to accept `Random` seed for deterministic testing
  - **Why Necessary**: 
    - Multiple next brick display requires queue-based approach
    - Pre-generation ensures bricks are available when needed
    - Peek functionality enables preview without affecting generation sequence

- **`com.comp2042.model.BrickRotator`**
  - **Changes Made**:
    - Enhanced rotation logic with wall-kick detection (moves brick left/right if rotation blocked)
    - Added `rotationOffsetX` and `rotationOffsetY` for SRS (Super Rotation System)
    - Implemented `isRotationValid(IBrick brick, int x, int y)` boundary checking
    - Modified `rotate(IBrick brick, Board board)` to return rotation success status
  - **Why Necessary**: 
    - Wall-kick prevents bricks from getting stuck in wall-adjacent positions
    - SRS implementation matches modern Tetris rotation expectations
    - Validation prevents bricks from rotating into invalid positions

- **`com.comp2042.model.Board`** (Interface)
  - **Changes Made**:
    - Added `swapHoldBrick(IBrick currentBrick)` method signature
    - Added `getHoldBrickData()` method for UI binding
    - Added `getGhostBrickY(IBrick brick, int dropSpeed)` for drop forecast
    - Added `spawnObstacle(int x, int y)` for difficulty-specific obstacles
    - Added `isPaused()` status check method
  - **Why Necessary**: 
    - Interface contracts define new feature requirements
    - Ensures `SimpleBoard` implements all necessary methods
    - Enables future alternative board implementations

- **`com.comp2042.model.ClearRow`**
  - **Changes Made**:
    - Updated line clear detection to work with obstacle blocks
    - Modified to trigger `GameController` line clear sound effects
    - Added line animation effects before actual clearing
  - **Why Necessary**: 
    - Obstacles change clearing logic (must check all cells)
    - Audio feedback improves user experience
    - Animation provides visual clarity

- **`com.comp2042.model.Score`**
  - **Changes Made**:
    - Added `levelProperty` for level tracking
    - Added `linesClearedProperty` for cumulative line counter
    - Modified `addScore(int points)` to automatically calculate level based on lines
    - Added `resetScore()` for new game initialization
    - All properties now support JavaFX property binding for automatic UI updates
  - **Why Necessary**: 
    - Level tracking enables difficulty progression
    - Observable properties enable real-time UI synchronization without manual updates
    - Centralized score logic prevents inconsistencies

### View Layer
- **`com.comp2042.view.GuiController`**
  - **Changes Made**:
    - Integrated `InputHandler` for keyboard input processing
    - Added UI bindings for hold brick panel, 4 next brick panels, and ghost brick panel
    - Implemented pause button with visual state indicator
    - Added game over panel integration with `GameOverPanel`
    - Modified to load `GameSettings` for volume control
    - Added property bindings for score, level, and lines cleared labels
    - Implemented notification panel for line clear/level up feedback
    - Modified board rendering to include ghost brick semi-transparency
  - **FXML**: `src/main/resources/fxml/game_layout.fxml`
  - **CSS**: `src/main/resources/css/game_layout.css`, `src/main/resources/css/global.css`
  - **Why Necessary**: 
    - New UI elements require layout updates
    - Input handler integration centralizes control handling
    - Property bindings eliminate manual UI refresh code
    - Game state visualization requires updated rendering pipeline

- **`com.comp2042.view.Main`**
  - **Changes Made**:
    - Global audio resource loading in `start()` method
    - Preloads all sound files before game initialization
    - Passes sound references to `GameController` and UI controllers
    - Implements scene switching logic between Menu, Game, and Settings screens
    - Added resource cleanup on application exit
  - **Why Necessary**: 
    - Audio preloading prevents runtime loading delays
    - Centralized resource management prevents memory leaks
    - Scene switching requires coordinated lifecycle management

---

## Unexpected Problems & Resolutions

### Problem 1: Ghost Piece Rendering Behind Main Brick
**Issue**: The ghost piece (drop forecast) was rendering behind the main falling brick, making it completely invisible to players.

**Root Cause**: FXML node order—the main brick canvas was declared after the ghost brick canvas, causing it to render on top.

**Resolution**: Reordered FXML elements in `game_layout.fxml` so the ghost brick panel is declared last in the VBox, placing it on top in the Z-order. Adjusted opacity to 0.3 for semi-transparency.

---

### Problem 2: Hold Brick Swap Logic Edge Cases
**Issue**: Players could spam the hold button to cycle through bricks infinitely, allowing sequence breaking.

**Root Cause**: No validation preventing multiple holds per piece.

**Resolution**: Implemented `holdUsedThisTurn` flag in `SimpleBoard`. Set to `true` when piece is swapped, reset to `false` only after the new piece is placed, restricting hold to once per piece cycle.

---

### Problem 3: Custom Keybinding Persistence Lost on Restart
**Issue**: After implementing custom keybindings in Settings menu, values reverted to defaults on application restart.

**Root Cause**: Settings were loaded but not saved; no `settings.txt` file I/O implementation.

**Resolution**: Implemented `GameSettings.saveSettings()` method that writes keybindings and volume to `settings.txt` in `key=value` format. Added auto-save on every Settings menu change via `SettingController.onSettingChanged()` callback.

---

### Problem 4: Extra Hard Mode Unlock Detection Inefficient
**Issue**: Checking high scores across multiple files every time the menu loaded caused noticeable lag (reading 4 files per frame).

**Root Cause**: `MainMenuController` was calling high score retrieval without caching.

**Resolution**: Implemented `HighScoreManager.checkAllModesUnlocked()` centralized method that reads all high score files once and returns unlock status. Added result caching with 5-minute TTL.

---

### Problem 5: Double-Tap Hard Drop Detection Unreliable
**Issue**: Distinguishing between rapid single keypresses and intentional double-taps was inconsistent; sometimes treating single press as hard drop.

**Root Cause**: Initial implementation used simple counter without timestamp validation; system latency varied.

**Resolution**: Implemented timestamp-based detection in `InputHandler`. Each `Space` keypress records `System.currentTimeMillis()`. Consecutive presses within 200ms window trigger hard drop; presses >200ms apart trigger soft drop. Threshold tuned via `GameConfig.HARD_DROP_DOUBLE_TAP_MS`.

---

### Problem 6: Random Obstacles Spawning Inside Falling Brick
**Issue**: In Extra Hard mode, random obstacles occasionally spawned at the same position as the falling brick, causing instant collision and immediate loss.

**Root Cause**: `GameController.spawnAndHardDropObstacle()` didn't validate grid positions before placement.

**Resolution**: Added collision checking before spawning. `SimpleBoard.spawnObstacle(x, y)` now checks if target cell is empty. If occupied, tries offset positions (+1, -1 horizontally). If all positions occupied, skips spawn rather than creating impossible state. Validates spawn positions are within bounds (0-9 width, 0-19 height).

---

### Problem 7: Score Not Updating in Real-Time
**Issue**: Score label in UI remained static even after lines were cleared; only updated visually after next brick placement.

**Root Cause**: Manual UI updates in `GuiController` were deferred; no property binding.

**Resolution**: Converted `Score` class to use JavaFX `IntegerProperty` for score, level, and lines cleared. Updated `GuiController` to bind labels directly: `scoreLabel.textProperty().bind(gameScore.scoreProperty().asString())`. Now updates automatically whenever property changes.

---

### Problem 8: Obstacle Spawning Exceeded Game Dimensions
**Issue**: Extra Hard mode obstacles sometimes appeared outside the 10×20 board boundary.

**Root Cause**: Random obstacle position generation wasn't constrained.

**Resolution**: Modified `GameController.spawnObstacle()` to use bounded random: `random.nextInt(BOARD_WIDTH)` for X (0-9) and `random.nextInt(BOARD_HEIGHT)` for Y (0-19). Added bounds validation assertions.

---

### Problem 9: Hold Feature Didn't Prevent Initial Null Brick
**Issue**: On first hold call, swapping with `null` held brick caused NullPointerException.

**Root Cause**: `heldBrick` field uninitialized; no null check in `swapHoldBrick()`.

**Resolution**: Added null-safety check in `swapHoldBrick()`: if `heldBrick == null`, simply store current brick without swap. Added initializer: `private IBrick heldBrick = null;` with explicit checks before operations.

---

### Problem 10: Settings File Corruption on Concurrent Writes
**Issue**: If game paused and Settings menu opened simultaneously, `settings.txt` could become corrupted.

**Root Cause**: No file locking; multiple threads writing without synchronization.

**Resolution**: Added `synchronized` keyword to `GameSettings.saveSettings()` method. Implemented atomic write pattern using temporary file + rename (write to `settings.txt.tmp`, then rename to `settings.txt`). Prevents partial file corruption on interruption.

---


## Author & License

**Coursework**: COMP2042 - Software Maintenance & Evolution  
**Student**: Tham Wing Lok  
**Academic Year**: 2024-2025  
**Repository**: [https://github.com/w1ngg00/wing-dms](https://github.com/w1ngg00/wing-dms)

---
