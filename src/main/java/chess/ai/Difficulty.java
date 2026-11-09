package chess.ai;

/** How far ahead the computer looks, in half-moves (plies). */
public enum Difficulty {
    EASY("Easy", 1),
    MEDIUM("Medium", 2),
    HARD("Hard", 3),
    EXPERT("Expert", 4),
    MASTER("Master", 5);

    private final String displayName;
    private final int searchDepth;

    Difficulty(String displayName, int searchDepth) {
        this.displayName = displayName;
        this.searchDepth = searchDepth;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSearchDepth() {
        return searchDepth;
    }

    @Override
    public String toString() {
        return displayName + " (depth " + searchDepth + ")";
    }
}
