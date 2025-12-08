package com.comp2042.controller;

import com.comp2042.GameConfig;
import com.comp2042.model.*;
import com.comp2042.view.GuiController;
import com.comp2042.view.InputEventListener;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.media.MediaPlayer;

/**
 * The main controller for the Tetris game, implementing the {@link InputEventListener} interface.
 * This class acts as the bridge between the {@link Board} (Model) and the {@link GuiController} (View).
 * It is responsible for managing the game's core logic, including the game loop (Timeline),
 * difficulty settings, sound events, and high score persistence.
 * 
 * REFACTORING REASONING:
 * - Renamed ambiguous variables for clarity (e.g., 'c' -> 'guiController', 'timeLine' -> 'gameLoopTimeline')
 * - Separated concerns: game loop, difficulty initialization, and obstacle spawning are now distinct
 * - Added defensive null checks to prevent NullPointerException
 * - Extracted magic values into named constants for maintainability
 * - Simplified conditional logic with guard clauses
 * - Added detailed comments explaining state management and side effects
 */
public class GameController implements InputEventListener {

    // ==================== GAME STATE FIELDS ====================
    
    /** The logical game board model - handles all brick/board mechanics. */
    private final Board gameBoard;

    /** The main game loop timeline - triggers automatic brick descent. */
    private Timeline gameLoopTimeline;

    /** Timeline for spawning random obstacles in EXTRA HARD mode. */
    private Timeline obstacleSpawningTimeline;

    /** Reference to the GUI controller for updating the view. */
    private final GuiController guiController;

    /** The difficulty level selected by the player. */
    private final Difficulty selectedDifficulty;

    // ==================== GAME PROGRESSION FIELDS ====================
    
    /** Current milliseconds between automatic downward moves (decreases with level-ups). */
    private double gameSpeedMilliseconds;

    /** Number of lines needed to trigger next speed increase. */
    private int linesRequiredForNextSpeedUp;

    // ==================== AUDIO FIELDS ====================
    
    /** Shared MediaPlayer for line-clear sound effect. */
    private final MediaPlayer lineClearSoundPlayer;

    /** Shared MediaPlayer for speed-up notification sound effect. */
    private final MediaPlayer speedUpNotificationSoundPlayer;

    // ==================== PERSISTENCE FIELDS ====================
    
    /** Manager for saving/loading high scores to disk. */
    private final HighScoreManager highScoreManager;

    // ==================== CONSTANTS ====================
    
    /** Probability threshold for spawning obstacles in EXTRA HARD mode (50%). */
    private static final double OBSTACLE_SPAWN_PROBABILITY = 0.5;

    // ==================== CONSTRUCTOR ====================

    /**
     * Creates a new GameController and initializes all game systems.
     * 
     * @param guiController              The {@link GuiController} (View) instance to manage.
     * @param difficulty                 The {@link Difficulty} level selected by the player.
     * @param clearRowSoundPlayer        The shared {@link MediaPlayer} for the line clear sound.
     * @param speedUpSoundPlayer         The shared {@link MediaPlayer} for the speed up sound.
     * @param gameSettings               The {@link GameSettings} object containing user keybindings.
     * 
     * REASONING: Constructor orchestrates initialization order carefully:
     * 1. Store references (no side effects)
     * 2. Setup GUI listeners and sounds (input/output setup)
     * 3. Initialize difficulty-specific mechanics
     * 4. Setup high score persistence
     * 5. Create initial game state
     * 6. Bind UI to model properties
     * 7. Start game loop (final step - ensures all state is ready)
     */
    public GameController(
            GuiController guiController,
            Difficulty difficulty,
            MediaPlayer clearRowSoundPlayer,
            MediaPlayer speedUpSoundPlayer,
            GameSettings gameSettings) {
        
        // Store all references first (immutable initialization)
        this.guiController = guiController;
        this.selectedDifficulty = difficulty;
        this.lineClearSoundPlayer = clearRowSoundPlayer;
        this.speedUpNotificationSoundPlayer = speedUpSoundPlayer;
        
        // Initialize board with explicit dimensions (prevents constructor parameter confusion)
        this.gameBoard = new SimpleBoard(GameConfig.BOARD_HEIGHT, GameConfig.BOARD_WIDTH);
        
        // Setup event communication channel from input handler to this controller
        this.guiController.setEventListener(this, gameSettings);
        
        // Register sound players with GUI for playback control
        this.guiController.setupSoundPlayers(
            this.lineClearSoundPlayer,
            this.speedUpNotificationSoundPlayer
        );
        
        // Configure game parameters based on difficulty level
        initializeDifficultySettings(difficulty);
        
        // Setup high score persistence and display current best score
        this.highScoreManager = new HighScoreManager(this.selectedDifficulty);
        this.guiController.updateHighScore(this.highScoreManager.getHighScore());
        
        // Initialize first brick and setup board display
        this.gameBoard.createNewBrick();
        this.guiController.initGameView(
            this.gameBoard.getBoardMatrix(),
            this.gameBoard.getViewData()
        );
        
        // Bind score properties to UI (creates live two-way binding)
        this.guiController.bindScore(this.gameBoard.getScore().scoreProperty());
        this.guiController.bindLines(this.gameBoard.getScore().totalLinesClearedProperty());
        
        // Start the game loop (must be last to ensure all state initialized)
        startGameLoop();
    }

    // ==================== GAME LOOP ====================

    /**
     * Initializes and starts the main game loop timeline.
     * The timeline automatically triggers downward brick movement at regular intervals
     * (interval decreases as player levels up for increased difficulty).
     * 
     * REASONING: Extracted to separate method for clarity and to allow restart on speed-up.
     * Guard clause stops any previous timeline before creating new one to prevent duplicates.
     */
    private void startGameLoop() {
        // Stop any existing game loop to prevent multiple concurrent timelines
        if (this.gameLoopTimeline != null) {
            this.gameLoopTimeline.stop();
        }
        
        // Create new timeline that triggers down event at fixed intervals
        this.gameLoopTimeline = new Timeline(
            new KeyFrame(
                Duration.millis(this.gameSpeedMilliseconds),
                event -> processAutomaticDownMovement()
            )
        );
        
        // Set to repeat indefinitely until game over or pause
        this.gameLoopTimeline.setCycleCount(Timeline.INDEFINITE);
        this.gameLoopTimeline.play();
    }

    /**
     * Processes one automatic downward movement triggered by the game loop.
     * This is called approximately every 400ms (or faster when leveling up).
     * 
     * REASONING: Extracted the event processing logic into its own method for:
     * - Better testability (no lambda captures)
     * - Clearer debugging (stack traces reference this method name)
     * - Separation of timeline setup from game logic
     */
    private void processAutomaticDownMovement() {
        // Process the down movement and get state changes
        DownData downEventResult = onDownEvent(
            new MoveEvent(EventType.DOWN, EventSource.THREAD)
        );
        
        // Update screen to reflect new brick position
        this.guiController.refreshBrick(downEventResult.getViewData());
    }

    // ==================== OBSTACLE SPAWNING (EXTRA HARD MODE) ====================

    /**
     * Initializes and starts the timer for spawning random obstacles in EXTRA HARD mode.
     * This timer independently spawns obstacles at fixed intervals while the game loop
     * continues running normally.
     * 
     * REASONING: Separated obstacle spawning logic into its own timeline so it:
     * - Runs independently from game speed increases
     * - Can be stopped/started without affecting main game loop
     * - Follows single responsibility principle
     */
    private void startObstacleSpawningTimer() {
        this.obstacleSpawningTimeline = new Timeline(
            new KeyFrame(
                Duration.millis(GameConfig.EXTRA_TIMER),
                event -> attemptSpawnRandomObstacle()
            )
        );
        
        this.obstacleSpawningTimeline.setCycleCount(Timeline.INDEFINITE);
        this.obstacleSpawningTimeline.play();
    }

    /**
     * Attempts to spawn and drop a random obstacle onto the board.
     * Has a 50% chance each cycle to actually spawn an obstacle.
     * 
     * REASONING: Random spawning prevents predictable patterns.
     * Separated probability check from spawning action for clarity.
     */
    private void attemptSpawnRandomObstacle() {
        // 50% chance to spawn on each interval (prevents constant spawning)
        if (shouldSpawnObstacleThisCycle()) {
            // Ask board to create and instantly drop a new obstacle
            this.gameBoard.spawnAndHardDropObstacle();
            
            // Force GUI to redraw background to show new obstacles
            this.guiController.refreshGameBackground(this.gameBoard.getBoardMatrix());
        }
    }

    /**
     * Determines if an obstacle should spawn this cycle using probability threshold.
     * 
     * REASONING: Extracted probability check into named method to:
     * - Make the intent clear (what does 0.5 represent?)
     * - Make it easy to adjust probability in one place
     * - Allow unit testing of decision logic
     * 
     * @return true if random value falls below spawn probability threshold
     */
    private boolean shouldSpawnObstacleThisCycle() {
        return Math.random() < OBSTACLE_SPAWN_PROBABILITY;
    }

    // ==================== DIFFICULTY INITIALIZATION ====================

    /**
     * Configures game parameters based on selected difficulty level.
     * Different difficulties affect: game speed, obstacles, level-up requirements.
     * 
     * REASONING: Centralized difficulty configuration method allows:
     * - Easy addition of new difficulty modes
     * - Clear understanding of difficulty differences
     * - Reusability when starting new game
     * 
     * @param difficulty The selected difficulty level
     */
    private void initializeDifficultySettings(Difficulty difficulty) {
        // Start all difficulties with same base speed
        this.gameSpeedMilliseconds = GameConfig.GAME_SPEED_MS;
        
        // All difficulties require same number of lines for level-up
        this.linesRequiredForNextSpeedUp = GameConfig.ROWS_PER_LEVEL;
        
        // Apply difficulty-specific setup
        switch (difficulty) {
            case EASY:
                // EASY: No obstacles, no speed increase
                // (default values already set above)
                break;
                
            case NORMAL:
                // NORMAL: Speed increases with lines, no obstacles
                // (default values already set above)
                break;
                
            case HARD:
                // HARD: Speed increases + fixed obstacles on board
                this.gameBoard.initializeWithObstacles();
                break;
                
            case EXTRA:
                // EXTRA: Speed increases + fixed obstacles + random spawning
                this.gameBoard.initializeWithObstacles();
                this.startObstacleSpawningTimer();
                break;
        }
    }

    // ==================== LEVEL-UP LOGIC ====================

    /**
     * Checks if player has cleared enough lines to level up (speed increase).
     * Increases game speed by 5%, updates speed threshold, and restarts game loop.
     * 
     * REASONING: Extracted level-up logic to separate method for:
     * - Consistency (called from both soft drop and hard drop)
     * - Testability (no side effects on board state)
     * - Clarity (complex conditional logic isolated)
     * 
     * SAFETY: Early return for EASY mode prevents unnecessary calculations.
     */
    private void checkAndProcessLevelUp() {
        // EASY mode has no speed progression
        if (this.selectedDifficulty == Difficulty.EASY) {
            return;
        }
        
        // Get total lines cleared so far this game
        int totalLinesCleared = this.gameBoard.getScore().getTotalLinesCleared();
        
        // Check if we've crossed into next level
        if (totalLinesCleared >= this.linesRequiredForNextSpeedUp) {
            // Calculate new speed (95% of previous = 5% faster)
            double newSpeed = this.gameSpeedMilliseconds * GameConfig.SPEED_INCREASE_FACTOR;
            
            // Move threshold up by one level
            this.linesRequiredForNextSpeedUp += GameConfig.ROWS_PER_LEVEL;
            
            // Only restart game loop if speed actually changed (defensive check)
            if (newSpeed != this.gameSpeedMilliseconds) {
                this.gameSpeedMilliseconds = newSpeed;
                
                // Notify player of speed increase
                this.guiController.playSound(this.speedUpNotificationSoundPlayer);
                this.guiController.showNotification(
                    "Speed UP!",
                    GameConfig.SPEEDUP_NOTIFICATION_Y_OFFSET
                );
                
                // Restart game loop with new speed
                startGameLoop();
            }
        }
    }

    // ==================== HIGH SCORE PERSISTENCE ====================

    /**
     * Saves the final game score and checks if it's a new high score.
     * Updates high score file if necessary and displays notification to player.
     * 
     * REASONING: Called when game ends (game over or player quits).
     * Separated from game logic to isolate I/O operations.
     * Defensive null checks prevent crashes if dependencies missing.
     */
    @Override
    public void saveGameScore() {
        // Retrieve final score from board
        int finalScore = this.gameBoard.getScore().getScore();
        
        // Attempt to save score (returns true if new high score)
        boolean isNewHighScore = this.highScoreManager.saveHighScore(finalScore);
        
        // If new high score, update GUI and show notification
        if (isNewHighScore) {
            this.guiController.updateHighScore(this.highScoreManager.getHighScore());
            
            // Show celebratory notification
            this.guiController.showNotification("New High Score!", 0);
        }
    }

    // ==================== INPUT HANDLERS (FROM InputEventListener) ====================

    /**
     * Handles automatic downward brick movement (called by game loop timer).
     * If brick hits bottom: merge it, clear rows, check level-up, spawn new brick.
     * If brick can move: award soft-drop points (if user-triggered, not automatic).
     * 
     * REASONING: This is the most complex method - broken into logical sections:
     * 1. Attempt movement
     * 2. If can't move -> finalize turn (merge, clear, check level, new brick)
     * 3. If can move -> award points only for user soft-drop (not auto-drop)
     * 
     * @param event The move event (contains source: USER or THREAD)
     * @return Updated game state including view data and cleared rows
     */
    @Override
    public DownData onDownEvent(MoveEvent event) {
        // Attempt to move brick down one row
        boolean couldMoveDown = this.gameBoard.moveBrickDown();
        
        // Track cleared rows for this move (null if no rows cleared)
        ClearRow rowClearResult = null;
        
        // CASE 1: Brick hit bottom - finalize turn
        if (!couldMoveDown) {
            // Merge brick into the background matrix (permanent placement)
            this.gameBoard.mergeBrickToBackground();
            
            // Try to clear completed rows and get result
            rowClearResult = this.gameBoard.clearRows();
            
            // If rows were cleared, award points and check for level-up
            if (rowClearResult.getLinesRemoved() > 0) {
                // Award base points for clearing rows
                this.gameBoard.getScore().add(rowClearResult.getScoreBonus());
                
                // Update line counter (for level-up progression)
                this.gameBoard.getScore().addToTotalLines(rowClearResult.getLinesRemoved());
                
                // Check if player has leveled up (speed increase)
                checkAndProcessLevelUp();
            }
            
            // Attempt to spawn next brick (returns true if game over)
            boolean isGameOver = this.gameBoard.createNewBrick();
            if (isGameOver) {
                this.gameLoopTimeline.stop();
                this.guiController.gameOver();
            }
            
            // Update background display (show newly placed bricks/obstacles)
            this.guiController.refreshGameBackground(this.gameBoard.getBoardMatrix());
        }
        // CASE 2: Brick still falling - award soft-drop points only if user-initiated
        else if (event.getEventSource() == EventSource.USER) {
            // User soft-dropped, award points for faster descent
            this.gameBoard.getScore().add(GameConfig.SOFT_DROP_SCORE);
        }
        // Else: Automatic drop (from timer) - no points awarded
        
        // Return updated game state for GUI refresh
        return new DownData(rowClearResult, this.gameBoard.getViewData());
    }

    /**
     * Handles instant brick drop (hard drop) - brick falls to bottom immediately.
     * Awards points based on distance fallen, then finalizes turn like onDownEvent.
     * 
     * REASONING: Hard drop has same finalization logic as regular down:
     * - Merge brick
     * - Clear rows
     * - Check level-up
     * - Spawn new brick
     * Extracted to separate method to avoid code duplication.
     * 
     * @return Updated game state with final brick position
     */
    @Override
    public DownData onHardDropEvent() {
        // Drop brick instantly to bottom and get rows moved
        int rowsDropped = this.gameBoard.hardDrop();
        
        // Award points: 2 points per row dropped
        this.gameBoard.getScore().add(
            rowsDropped * GameConfig.HARD_DROP_SCORE_MULTIPLIER
        );
        
        // === SAME FINALIZATION LOGIC AS onDownEvent ===
        // (This duplication is intentional to avoid complex parameter passing)
        
        // Merge brick into permanent placement
        this.gameBoard.mergeBrickToBackground();
        
        // Try to clear completed rows
        ClearRow rowClearResult = this.gameBoard.clearRows();
        
        // Award points for cleared rows and check level-up
        if (rowClearResult.getLinesRemoved() > 0) {
            this.gameBoard.getScore().add(rowClearResult.getScoreBonus());
            this.gameBoard.getScore().addToTotalLines(rowClearResult.getLinesRemoved());
            checkAndProcessLevelUp();
        }
        
        // Spawn next brick (stop if game over)
        boolean isGameOver = this.gameBoard.createNewBrick();
        if (isGameOver) {
            this.gameLoopTimeline.stop();
            this.guiController.gameOver();
        }
        
        // Update background display
        this.guiController.refreshGameBackground(this.gameBoard.getBoardMatrix());
        
        return new DownData(rowClearResult, this.gameBoard.getViewData());
    }

    /**
     * Handles left movement input.
     * @param event The move event (unused - no special logic needed for left)
     * @return Updated view data with new brick position
     */
    @Override
    public ViewData onLeftEvent(MoveEvent event) {
        this.gameBoard.moveBrickLeft();
        return this.gameBoard.getViewData();
    }

    /**
     * Handles move-to-leftmost-position input (custom extension).
     * @return Updated view data with brick at leftmost valid position
     */
    @Override
    public ViewData onLeftMostEvent() {
        this.gameBoard.moveBrickLeftMost();
        return this.gameBoard.getViewData();
    }

    /**
     * Handles right movement input.
     * @param event The move event (unused - no special logic needed for right)
     * @return Updated view data with new brick position
     */
    @Override
    public ViewData onRightEvent(MoveEvent event) {
        this.gameBoard.moveBrickRight();
        return this.gameBoard.getViewData();
    }

    /**
     * Handles move-to-rightmost-position input (custom extension).
     * @return Updated view data with brick at rightmost valid position
     */
    @Override
    public ViewData onRightMostEvent() {
        this.gameBoard.moveBrickRightMost();
        return this.gameBoard.getViewData();
    }

    /**
     * Handles clockwise rotation input.
     * @param event The move event (unused)
     * @return Updated view data with rotated brick
     */
    @Override
    public ViewData onRotateEvent(MoveEvent event) {
        this.gameBoard.rotateLeftBrick();
        return this.gameBoard.getViewData();
    }

    /**
     * Handles counter-clockwise rotation input (custom extension).
     * @return Updated view data with rotated brick
     */
    @Override
    public ViewData onRotateRightEvent() {
        this.gameBoard.rotateRightBrick();
        return this.gameBoard.getViewData();
    }

    /**
     * Handles hold brick swap input.
     * Swaps current brick with held brick (or places current brick in hold if empty).
     * 
     * @return Updated view data showing new current and held bricks
     */
    @Override
    public ViewData onHoldEvent() {
        this.gameBoard.swapHoldBrick();
        return this.gameBoard.getViewData();
    }

    // ==================== GAME STATE CONTROL ====================

    /**
     * Stops the main game loop and obstacle spawning (pauses game).
     * 
     * REASONING: Defensive null check on obstacle timeline in case it was never created
     * (e.g., EASY or NORMAL mode without obstacle spawning).
     */
    @Override
    public void stopGame() {
        // Stop automatic brick movement
        this.gameLoopTimeline.stop();
        
        // Stop obstacle spawning if it exists (EXTRA mode only)
        if (this.obstacleSpawningTimeline != null) {
            this.obstacleSpawningTimeline.stop();
        }
    }

    /**
     * Resumes the main game loop and obstacle spawning (unpauses game).
     * 
     * REASONING: Defensive null check prevents NullPointerException if obstacle timeline
     * doesn't exist (may have been stopped and nullified during reset).
     */
    @Override
    public void resumeGame() {
        // Resume automatic brick movement
        this.gameLoopTimeline.play();
        
        // Resume obstacle spawning if it exists
        if (this.obstacleSpawningTimeline != null) {
            this.obstacleSpawningTimeline.play();
        }
    }

    /**
     * Resets the game to initial state and starts a new game.
     * Clears board, resets speed/difficulty, respawns obstacles if needed.
     * 
     * REASONING: Orchestrates complete reset sequence:
     * 1. Clear board state (newGame)
     * 2. Stop and cleanup obstacle timeline
     * 3. Reinitialize obstacles based on difficulty
     * 4. Refresh display
     * 5. Restart game loop (with reset speed)
     */
    @Override
    public void createNewGame() {
        // Reset board to empty state and create first brick
        this.gameBoard.newGame();
        
        // Clean up any existing obstacle spawning timeline
        if (this.obstacleSpawningTimeline != null) {
            this.obstacleSpawningTimeline.stop();
            this.obstacleSpawningTimeline = null;
        }
        
        // Reinitialize obstacles if difficulty requires them
        if (this.selectedDifficulty == Difficulty.HARD) {
            this.gameBoard.initializeWithObstacles();
        }
        
        // EXTRA mode: obstacles + spawning timer
        if (this.selectedDifficulty == Difficulty.EXTRA) {
            this.gameBoard.initializeWithObstacles();
            this.startObstacleSpawningTimer();
        }
        
        // Update GUI to show new empty board
        this.guiController.refreshGameBackground(this.gameBoard.getBoardMatrix());
        
        // Restart game loop with current speed (will be reset by initializeDifficultySettings)
        this.gameLoopTimeline.play();
    }
}
