package chess.ui;

import chess.ai.Difficulty;
import chess.model.PieceColor;

/**
 * Everything chosen on the start menu.
 *
 * @param playerName   name shown for the person at the bottom of the board
 * @param opponentName name shown for the other side
 * @param vsComputer   true to play the AI, false for two people on one computer
 * @param playerColor  the color the first player controls
 * @param difficulty   AI strength (ignored for two-player games)
 * @param timeMillis   starting time per side, or 0 for an untimed game
 */
public record GameSettings(String playerName, String opponentName, boolean vsComputer,
                           PieceColor playerColor, Difficulty difficulty, long timeMillis) {

    public boolean isTimed() {
        return timeMillis > 0;
    }

    public String nameFor(PieceColor color) {
        return color == playerColor ? playerName : opponentName;
    }
}
