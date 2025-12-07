package com.comp2042.view;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.util.List;

/**
 * A custom JavaFX component that displays the "GAME OVER" message
 * and a "Main Menu" button.
 * This panel extends {@link VBox} to vertically align the label and the button.
 * It provides a method ({@link #setMainMenu(Runnable)}) to inject the action
 * that should be performed when the "Main Menu" button is clicked.
 */
// change to VBox to align label and button vertically
public class GameOverPanel extends VBox {

    private MediaPlayer gameMusic;
    private MediaPlayer gameOverMusicPlayer;
    private Runnable mainMenuCallback;

    /**
     * Constructs the GameOverPanel.
     * Initializes the "GAME OVER" label and "Main Menu" button,
     * sets their styles and layout (centered, with spacing), and
     * configures the button's click event.
     */
    public GameOverPanel() {
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-border-color: red; -fx-border-width: 5;");
        setAlignment(Pos.CENTER);
        setSpacing(30);
        setPrefWidth(600);
        setPrefHeight(400);

        // Game Over text
        Text gameOverText = new Text("GAME OVER");
        gameOverText.setStyle("-fx-font-family: 'Let\\'s go Digital'; -fx-font-size: 60px; -fx-fill: red;");

        // Main Menu button
        Button mainMenuButton = new Button("MAIN MENU");
        mainMenuButton.setStyle("-fx-font-family: 'Let\\'s go Digital'; -fx-font-size: 24px; -fx-padding: 15px 40px;");
        mainMenuButton.setOnAction(e -> {
            stopGameOverMusic();
            if (mainMenuCallback != null) {
                mainMenuCallback.run();
            }
        });

        getChildren().addAll(gameOverText, mainMenuButton);
    }

    /**
     * Sets the action to be executed when the "Main Menu" button is pressed.
     * This method is used to pass the navigation logic (e.g., {@code mainApp.showMainMenuScreen()})
     * from the {@link GuiController} into this panel.
     *
     * @param callback The {@link Runnable} to execute on button click.
     */
    public void setMainMenu(Runnable callback) {
        this.mainMenuCallback = callback;
    }


    /**
     * Displays this panel with a fade-in animation.
     * This method performs the following sequence:
     * Sets the panel's visibility to {@code true}.
     * Brings the panel to the front of the scene graph to ensure it overlays other elements.
     * Plays a {@link FadeTransition} that changes the opacity from 0.0 (transparent) to 1.0 (opaque) over 1 second.
     */
    public void showWithAnimation() {
        setVisible(true);
        
        // Stop game music first
        stopGameMusic();
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        // Play game over music only
        startGameOverMusic();
    }

    // Stop in-game music
    public void stopGameMusic() {
        if (gameMusic != null) {
            gameMusic.stop();
            gameMusic.dispose();
            gameMusic = null;
        }
    }

    // Start in-game music
    public void startGameMusic() {
        try {
            String musicUrl = getClass().getResource("/sounds/bg_game.mp3").toExternalForm();
            Media media = new Media(musicUrl);
            gameMusic = new MediaPlayer(media);
            gameMusic.setCycleCount(MediaPlayer.INDEFINITE);
            gameMusic.setVolume(0.3);
            gameMusic.play();
        } catch (Exception e) {
            System.err.println("Game music not started: " + e.getMessage());
        }
    }

    // Start game over music (plays once, no loop)
    private void startGameOverMusic() {
        try {
            String musicUrl = getClass().getResource("/sounds/bg_game_over.mp3").toExternalForm();
            Media media = new Media(musicUrl);
            gameOverMusicPlayer = new MediaPlayer(media);
            gameOverMusicPlayer.setCycleCount(1); // Play once
            gameOverMusicPlayer.setVolume(0.3);
            gameOverMusicPlayer.play();
        } catch (Exception e) {
            System.err.println("Game over music not started: " + e.getMessage());
        }
    }

    // Stop game over music
    private void stopGameOverMusic() {
        if (gameOverMusicPlayer != null) {
            gameOverMusicPlayer.stop();
            gameOverMusicPlayer.dispose();
            gameOverMusicPlayer = null;
        }
    }

    public MediaPlayer getGameMusic() {
        return gameMusic;
    }
}
