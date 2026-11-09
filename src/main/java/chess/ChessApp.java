package chess;

import chess.ui.StartMenu;
import chess.ui.Theme;
import javax.swing.SwingUtilities;

/** Entry point. Opens the start menu on the Swing event thread. */
public final class ChessApp {

    private ChessApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new StartMenu().show();
        });
    }
}
