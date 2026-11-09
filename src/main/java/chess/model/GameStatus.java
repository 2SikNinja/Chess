package chess.model;

/** Where a game stands after the most recent move. */
public enum GameStatus {
    IN_PROGRESS("In progress", false),
    CHECKMATE("Checkmate", true),
    STALEMATE("Stalemate", true),
    FIFTY_MOVE_RULE("Draw by the fifty-move rule", true),
    THREEFOLD_REPETITION("Draw by threefold repetition", true),
    INSUFFICIENT_MATERIAL("Draw by insufficient material", true);

    private final String description;
    private final boolean gameOver;

    GameStatus(String description, boolean gameOver) {
        this.description = description;
        this.gameOver = gameOver;
    }

    public String getDescription() {
        return description;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isDraw() {
        return gameOver && this != CHECKMATE;
    }
}
