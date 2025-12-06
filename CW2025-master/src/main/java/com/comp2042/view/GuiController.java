package com.comp2042.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;
import com.comp2042.GameConfig;
import com.comp2042.controller.InputHandler;
import com.comp2042.controller.MoveEvent;
import com.comp2042.model.DownData;
import com.comp2042.model.GameSettings;
import com.comp2042.model.ViewData;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.Optional;
import java.util.List;

public class GuiController implements Initializable {

    @FXML
    private GridPane gamePanel;
    @FXML
    private Group groupNotification;
    @FXML
    private GridPane brickPanel;
    @FXML
    private Group boardGroup;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label highScoreLabel;
    @FXML 
    private Label linesLabel;
    @FXML
    private GridPane nextBrickPanel;
    @FXML
    private GridPane nextBrickPanel2;
    @FXML
    private GridPane nextBrickPanel3;
    @FXML
    private GridPane nextBrickPanel4;
    @FXML
    private GridPane ghostBrickPanel;
    @FXML
    private GridPane holdBrickPanel;
    @FXML
    private Button pauseButton;
    @FXML
    private Button restartButton;
    @FXML
    private GameOverPanel gameOverPanel;
    @FXML
    private MainMenuController mainMenuController;

    // Icon views
    private ImageView pauseIconView;
    private ImageView resumeIconView;

    // Game state
    private BooleanProperty isPause = new SimpleBooleanProperty(false);
    private BooleanProperty isGameOver = new SimpleBooleanProperty(false);

    // Display matrices
    private Rectangle[][] displayMatrix;
    private Rectangle[][] rectangles;
    private Rectangle[][] ghostRectangles;
    private Rectangle[][] nextBrickRectangles;
    private Rectangle[][] nextBrickRectangles2;
    private Rectangle[][] nextBrickRectangles3;
    private Rectangle[][] nextBrickRectangles4;
    private Rectangle[][] holdBrickRectangle;

    // Sizing
    private double currentBrickSize = GameConfig.BRICK_SIZE;
    private double boardPixelWidth;
    private double boardPixelHeight;

    // Controllers and listeners
    private InputEventListener eventListener;
    private GameSettings settings;
    private Main mainApp;

    // Sound players
    private MediaPlayer clearRowSoundPlayer;
    private MediaPlayer speedUpSoundPlayer;

    @FXML
    private ImageView pauseOverlay;
    private MediaPlayer gameMusic;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Font.loadFont(getClass().getClassLoader().getResource("fonts/digital.ttf").toExternalForm(), 38);

        // Load assets
        try {
            Image pauseImg = new Image(getClass().getResourceAsStream("/icons/pauseButton.png"));
            Image resumeImg = new Image(getClass().getResourceAsStream("/icons/resumeButton.png"));
            Image restartImg = new Image(getClass().getResourceAsStream("/icons/restartButton.png"));
            pauseIconView = new ImageView(pauseImg);
            resumeIconView = new ImageView(resumeImg);
            ImageView restartIconView = new ImageView(restartImg);
            pauseIconView.setFitWidth(25);
            resumeIconView.setFitWidth(25);
            restartIconView.setFitWidth(25);
            pauseIconView.setPreserveRatio(true);
            resumeIconView.setPreserveRatio(true);
            restartIconView.setPreserveRatio(true);
            pauseButton.setGraphic(pauseIconView);
            restartButton.setGraphic(restartIconView);
        } catch (Exception e) {
            System.err.println("Failed to load icon img: " + e.getMessage());
            pauseButton.setText("Pause");
            restartButton.setText("Restart");
        }

        gamePanel.setFocusTraversable(true);
        gamePanel.requestFocus();

        // Setup the game over panel's "Main Menu" button action
        gameOverPanel.setMainMenu(() -> mainApp.showMainMenuScreen());
        gameOverPanel.setVisible(false);

        final Reflection reflection = new Reflection();
        reflection.setFraction(0.8);
        reflection.setTopOpacity(0.9);
        reflection.setTopOffset(-12);

        // when scene becomes available, ensure we adjust sizes and center on resize
        gamePanel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // adjust immediately and on resize
                newScene.widthProperty().addListener((o, oldW, newW) -> adjustBoardToScene(newScene));
                newScene.heightProperty().addListener((o, oldH, newH) -> adjustBoardToScene(newScene));
                Platform.runLater(() -> adjustBoardToScene(newScene));
            }
        });

        // Start game music when game scene loads
        if (gameOverPanel != null) {
            gameOverPanel.startGameMusic();
        }

        // Initialize pause overlay (hidden by default)
        if (pauseOverlay != null) {
            pauseOverlay.setVisible(false);
        }
    }

    @FXML
    public void onExitToMenu() {
        // Stop game music first
        if (gameOverPanel != null) {
            gameOverPanel.stopGameMusic();
        }
        // Then start menu music
        if (mainMenuController != null) {
            mainMenuController.startMenuMusic();
        }
        // Keep fullscreen when showing menu
        mainApp.showMainMenuScreen();
    }

    public void setupSoundPlayers(MediaPlayer clearRowSoundPlayer, MediaPlayer speedUpSoundPlayer) {
        this.clearRowSoundPlayer = clearRowSoundPlayer;
        this.speedUpSoundPlayer = speedUpSoundPlayer;
    }

    public void playSound(MediaPlayer player) {
        player.stop();
        player.play();
    }

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    public void initGameView(int[][] boardMatrix, ViewData brick) {
        displayMatrix = new Rectangle[boardMatrix.length][boardMatrix[0].length];
        for (int i = 2; i < boardMatrix.length; i++) {
            for (int j = 0; j < boardMatrix[i].length; j++) {
                Rectangle rectangle = new Rectangle(currentBrickSize, currentBrickSize);
                setRectangleData(boardMatrix[i][j], rectangle);
                displayMatrix[i][j] = rectangle;
                gamePanel.add(rectangle, j, i - 2);
            }
        }

        int cols = boardMatrix[0].length;
        int visibleRows = boardMatrix.length - 2;
        boardPixelWidth = cols * currentBrickSize;
        boardPixelHeight = visibleRows * currentBrickSize;

        ghostRectangles = initializeNextBrickPanel(ghostBrickPanel, currentBrickSize);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                ghostRectangles[i][j].setFill(Color.TRANSPARENT);
            }
        }

        nextBrickRectangles = initializeNextBrickPanel(nextBrickPanel, GameConfig.NEXT_BRICK_SIZE_LARGE);
        nextBrickRectangles2 = initializeNextBrickPanel(nextBrickPanel2, GameConfig.NEXT_BRICK_SIZE_SMALL);
        nextBrickRectangles3 = initializeNextBrickPanel(nextBrickPanel3, GameConfig.NEXT_BRICK_SIZE_SMALL);
        nextBrickRectangles4 = initializeNextBrickPanel(nextBrickPanel4, GameConfig.NEXT_BRICK_SIZE_SMALL);
        holdBrickRectangle = initializeNextBrickPanel(holdBrickPanel, GameConfig.NEXT_BRICK_SIZE_LARGE);

        rectangles = new Rectangle[brick.getBrickData().length][brick.getBrickData()[0].length];
        for (int i = 0; i < brick.getBrickData().length; i++) {
            for (int j = 0; j < brick.getBrickData()[i].length; j++) {
                Rectangle rectangle = new Rectangle(currentBrickSize, currentBrickSize);
                rectangle.setFill(getFillColor(brick.getBrickData()[i][j]));
                rectangles[i][j] = rectangle;
                brickPanel.add(rectangle, j, i);
            }
        }

        updateMovingPanelsPosition(brick);

        Scene s = gamePanel.getScene();
        if (s != null) Platform.runLater(() -> adjustBoardToScene(s));
    }

    private void adjustBoardToScene(Scene scene) {
        if (displayMatrix == null) return;

        int cols = displayMatrix[0].length;
        int visibleRows = displayMatrix.length - 2;

        double availW = Math.max(200, scene.getWidth() * 0.46);
        double availH = Math.max(200, scene.getHeight() * 0.72);

        double newCell = Math.floor(Math.min((availW - 4) / cols, (availH - 4) / visibleRows));
        if (newCell < 4) newCell = 4;

        if (Math.abs(newCell - currentBrickSize) < 0.1) {
            centerBoard();
            return;
        }

        currentBrickSize = newCell;

        for (int i = 2; i < displayMatrix.length; i++) {
            for (int j = 0; j < displayMatrix[i].length; j++) {
                Rectangle r = displayMatrix[i][j];
                if (r != null) {
                    r.setWidth(currentBrickSize);
                    r.setHeight(currentBrickSize);
                }
            }
        }

        if (rectangles != null) {
            for (int i = 0; i < rectangles.length; i++) {
                for (int j = 0; j < rectangles[i].length; j++) {
                    Rectangle r = rectangles[i][j];
                    if (r != null) { r.setWidth(currentBrickSize); r.setHeight(currentBrickSize); }
                }
            }
        }

        if (ghostRectangles != null) {
            for (int i = 0; i < ghostRectangles.length; i++)
                for (int j = 0; j < ghostRectangles[i].length; j++) {
                    Rectangle r = ghostRectangles[i][j];
                    if (r != null) { r.setWidth(currentBrickSize); r.setHeight(currentBrickSize); }
                }
        }
        if (nextBrickRectangles != null) updateNextRectanglesSize(nextBrickRectangles, GameConfig.NEXT_BRICK_SIZE_LARGE);
        if (nextBrickRectangles2 != null) updateNextRectanglesSize(nextBrickRectangles2, GameConfig.NEXT_BRICK_SIZE_SMALL);
        if (nextBrickRectangles3 != null) updateNextRectanglesSize(nextBrickRectangles3, GameConfig.NEXT_BRICK_SIZE_SMALL);
        if (nextBrickRectangles4 != null) updateNextRectanglesSize(nextBrickRectangles4, GameConfig.NEXT_BRICK_SIZE_SMALL);
        if (holdBrickRectangle != null) updateNextRectanglesSize(holdBrickRectangle, GameConfig.NEXT_BRICK_SIZE_LARGE);

        boardPixelWidth = cols * currentBrickSize;
        boardPixelHeight = visibleRows * currentBrickSize;

        centerBoard();
    }

    private void updateNextRectanglesSize(Rectangle[][] rects, double baseSize) {
        for (int i = 0; i < rects.length; i++)
            for (int j = 0; j < rects[i].length; j++) {
                Rectangle r = rects[i][j];
                if (r != null) { r.setWidth(baseSize); r.setHeight(baseSize); }
            }
    }

    private void updateMovingPanelsPosition(ViewData brick) {
        double scaleFactor = currentBrickSize / GameConfig.BRICK_SIZE;
        brickPanel.setLayoutX(gamePanel.getLayoutX() + brick.getxPosition() * brickPanel.getVgap() + brick.getxPosition() * currentBrickSize);
        brickPanel.setLayoutY(GameConfig.BRICK_PANEL_Y_OFFSET * scaleFactor + gamePanel.getLayoutY() + brick.getyPosition() * brickPanel.getHgap() + brick.getyPosition() * currentBrickSize);
    }

    private void displayNextBricks(List<int[][]> nextBricks) {
        displayNextBrick(nextBricks.get(0), nextBrickRectangles);
        displayNextBrick(nextBricks.get(1), nextBrickRectangles2);
        displayNextBrick(nextBricks.get(2), nextBrickRectangles3);
        displayNextBrick(nextBricks.get(3), nextBrickRectangles4);
    }

    private void displayNextBrick(int[][] nextBrick, Rectangle[][] rects) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                rects[i][j].setFill(Color.TRANSPARENT);
            }
        }
        for (int i = 0; i < nextBrick.length; i++) {
            for (int j = 0; j < nextBrick[i].length; j++) {
                if (nextBrick[i][j] != 0) setRectangleData(nextBrick[i][j], rects[i][j]);
            }
        }
    }

    private void displayHoldBrick(int[][] holdingBrick) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                holdBrickRectangle[i][j].setFill(Color.TRANSPARENT);
            }
        }

        if (holdingBrick != null) {
            for (int i = 0; i < holdingBrick.length; i++) {
                for (int j = 0; j < holdingBrick[i].length; j++) {
                    if (holdingBrick[i][j] != 0) {
                        setRectangleData(holdingBrick[i][j], holdBrickRectangle[i][j]);
                    }
                }
            }
        }
    }

    private Rectangle[][] initializeNextBrickPanel(GridPane panel, double size) {
        Rectangle[][] rectangles = new Rectangle[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Rectangle rectangle = new Rectangle(size, size);
                rectangles[i][j] = rectangle;
                panel.add(rectangle, j, i);
            }
        }
        return rectangles;
    }

    private Paint getFillColor(int i) {
        return switch (i) {
            case 0 -> Color.TRANSPARENT;
            case 1 -> Color.AQUA;
            case 2 -> Color.BLUEVIOLET;
            case 3 -> Color.DARKGREEN;
            case 4 -> Color.YELLOW;
            case 5 -> Color.RED;
            case 6 -> Color.BEIGE;
            case 7 -> Color.BURLYWOOD;
            case 8 -> Color.GRAY;
            default -> Color.WHITE;
        };
    }

    public void refreshBrick(ViewData brick) {
        if (isPause.getValue() == Boolean.FALSE) {
            double scaleFactor = currentBrickSize / GameConfig.BRICK_SIZE;

            brickPanel.setLayoutX(gamePanel.getLayoutX() + brick.getxPosition() * brickPanel.getVgap() + brick.getxPosition() * currentBrickSize);
            brickPanel.setLayoutY(GameConfig.BRICK_PANEL_Y_OFFSET * scaleFactor + gamePanel.getLayoutY() + brick.getyPosition() * brickPanel.getHgap() + brick.getyPosition() * currentBrickSize);
            for (int i = 0; i < brick.getBrickData().length; i++) {
                for (int j = 0; j < brick.getBrickData()[i].length; j++) {
                    setRectangleData(brick.getBrickData()[i][j], rectangles[i][j]);
                }
            }

            int[][] ghostBrick = brick.getBrickData();

            ghostBrickPanel.setLayoutX(gamePanel.getLayoutX() + brick.getxPosition() * brickPanel.getVgap() + brick.getxPosition() * currentBrickSize);
            ghostBrickPanel.setLayoutY(GameConfig.BRICK_PANEL_Y_OFFSET * scaleFactor + gamePanel.getLayoutY() + brick.getGhostYPosition() * brickPanel.getHgap() + brick.getGhostYPosition() * currentBrickSize);

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (i < ghostBrick.length && j < ghostBrick[i].length && ghostBrick[i][j] != 0) setRectangleData(ghostBrick[i][j], ghostRectangles[i][j]);
                    else { setRectangleData(0, ghostRectangles[i][j]); }
                }
            }

            displayHoldBrick(brick.getHoldBrickData());
            displayNextBricks(brick.getNextBrickData());
        }
    }

    public void refreshGameBackground(int[][] board) {
        for (int i = 2; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                setRectangleData(board[i][j], displayMatrix[i][j]);
            }
        }
    }

    private void setRectangleData(int color, Rectangle rectangle) {
        rectangle.setFill(getFillColor(color));
        rectangle.setArcHeight(9);
        rectangle.setArcWidth(9);
    }

    public void moveDown(MoveEvent event) {
        if (isPause.getValue() == Boolean.FALSE) {
            DownData downData = eventListener.onDownEvent(event);
            if (downData.getClearRow() != null && downData.getClearRow().getLinesRemoved() > 0) {
                showNotification("+" + downData.getClearRow().getScoreBonus(), 0);
                playSound(clearRowSoundPlayer);
            }
            refreshBrick(downData.getViewData());
        }
        gamePanel.requestFocus();
    }

    public void handleHardDrop() {
        DownData downData = eventListener.onHardDropEvent();
        if (downData.getClearRow() != null && downData.getClearRow().getLinesRemoved() > 0) {
            showNotification("+" + downData.getClearRow().getScoreBonus(), 0);
            playSound(clearRowSoundPlayer);
        }
        refreshBrick(downData.getViewData());
        gamePanel.requestFocus();
    }

    public void showNotification(String text, double yOffset) {
        NotificationPanel speedUpPanel = new NotificationPanel(text);
        speedUpPanel.setLayoutY(yOffset);
        groupNotification.getChildren().add(speedUpPanel);
        speedUpPanel.showScore(groupNotification.getChildren());
    }

    public void setEventListener(InputEventListener eventListener, GameSettings settings) {
        this.eventListener = eventListener;
        this.settings = settings;
        InputHandler inputHandler = new InputHandler(this, this.eventListener, settings);
        gamePanel.setOnKeyPressed(inputHandler);
    }

    public void bindScore(IntegerProperty integerProperty) {
        scoreLabel.textProperty().bind(integerProperty.asString());
    }

    public void bindLines(IntegerProperty integerProperty) {
        linesLabel.textProperty().bind(integerProperty.asString());
    }

    public void updateHighScore(int score) {
        highScoreLabel.setText(String.valueOf(score));
    }

    public void gameOver() {
        eventListener.saveGameScore();
        gameOverPanel.showWithAnimation();
        isGameOver.setValue(Boolean.TRUE);
    }

    public void newGame(ActionEvent actionEvent) {
        gameOverPanel.setVisible(false);
        eventListener.createNewGame();
        gamePanel.requestFocus();
        isPause.setValue(Boolean.FALSE);
        isGameOver.setValue(Boolean.FALSE);
        pauseButton.setGraphic(pauseIconView);
    }

    public void pauseGame(ActionEvent actionEvent) {
        if (isPause.getValue() == Boolean.FALSE) {
            // Pause the game
            eventListener.stopGame();
            isPause.setValue(Boolean.TRUE);
            pauseButton.setGraphic(resumeIconView);
            
            // Stop game music
            if (gameOverPanel != null && gameOverPanel.getGameMusic() != null) {
                gameOverPanel.getGameMusic().pause();
            }
            
            // Show pause overlay image
            if (pauseOverlay != null) {
                pauseOverlay.setVisible(true);
            }
        } else {
            // Resume the game
            eventListener.resumeGame();
            isPause.setValue(Boolean.FALSE);
            pauseButton.setGraphic(pauseIconView);
            
            // Resume game music
            if (gameOverPanel != null && gameOverPanel.getGameMusic() != null) {
                gameOverPanel.getGameMusic().play();
            }
            
            // Hide pause overlay image
            if (pauseOverlay != null) {
                pauseOverlay.setVisible(false);
            }
        }
        gamePanel.requestFocus();
    }

    public boolean isPause() { return isPause.get(); }
    public boolean isGameOver() { return isGameOver.get(); }
    public InputEventListener getEventListener() { return this.eventListener; }

    @FXML
    private void goBackToMenu(ActionEvent actionEvent) {
        eventListener.stopGame();
        isPause.setValue(Boolean.TRUE);
        
        // Stop game music
        if (gameOverPanel != null) {
            gameOverPanel.stopGameMusic();
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Are you sure you want to go back to menu?");
        Optional<ButtonType> response = alert.showAndWait();

        if (response.isPresent() && response.get() == ButtonType.OK) {
            // Start menu music when going back
            if (mainMenuController != null) {
                mainMenuController.startMenuMusic();
            }
            // Keep fullscreen when showing menu
            mainApp.showMainMenuScreen();
        } else {
            eventListener.resumeGame();
            isPause.setValue(Boolean.FALSE);
            gamePanel.requestFocus();
        }
    }

    @FXML
    private void onShowKeybindings() {
        eventListener.stopGame();
        isPause.setValue(Boolean.TRUE);
        String keybindings =
                "Move Left:\t\t" + settings.getKeyCode("MOVE_LEFT").name() + "\n" +
                "Move Right:\t\t" + settings.getKeyCode("MOVE_RIGHT").name() + "\n" +
                "Rotate Left:\t\t" + settings.getKeyCode("ROTATE_LEFT").name() + "\n" +
                "Rotate Right:\t\t" + settings.getKeyCode("ROTATE_RIGHT").name() + "\n" +
                "Soft Drop:\t\t" + settings.getKeyCode("SOFT_DROP").name() + "\n" +
                "Hard Drop:\t\t" + settings.getKeyCode("HARD_DROP").name() + "\n" +
                "Hold:\t\t\t" + settings.getKeyCode("HOLD").name() + "\n" +
                "Move Left Most: \t" + settings.getKeyCode("MOVE_LEFT_MOST").name() + "\n" +
                "Move Right Most:\t" + settings.getKeyCode("MOVE_RIGHT_MOST").name();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keybindings");
        alert.setHeaderText("Current Keybindings");
        alert.setContentText(keybindings);
        alert.showAndWait();

        isPause.setValue(Boolean.FALSE);
        eventListener.resumeGame();
        gamePanel.requestFocus();
    }

    private void centerBoard() {
        Scene s = gamePanel.getScene();
        if (s == null) return;
        if (boardPixelWidth <= 0 || boardPixelHeight <= 0) return;

        double sceneW = s.getWidth();
        double sceneH = s.getHeight();
        double x = (sceneW - boardPixelWidth) / 2.0;
        double y = (sceneH - boardPixelHeight) / 2.0;

        if (Double.isNaN(x) || Double.isNaN(y)) return;
        gamePanel.setLayoutX(x < 0 ? 0 : x);
        gamePanel.setLayoutY(y < 0 ? 0 : y);

        brickPanel.setLayoutX(gamePanel.getLayoutX());
        brickPanel.setLayoutY(gamePanel.getLayoutY());
        ghostBrickPanel.setLayoutX(gamePanel.getLayoutX());
        ghostBrickPanel.setLayoutY(gamePanel.getLayoutY());
        groupNotification.setLayoutX(gamePanel.getLayoutX());
        groupNotification.setLayoutY(gamePanel.getLayoutY());
    }
}
 // test test