package chess.model;

/**
 * The two sides in a game of chess.
 *
 * <p>Row 0 of the board is rank 8 (Black's back rank) and row 7 is rank 1
 * (White's back rank), so White pawns move toward lower row numbers.
 */
public enum PieceColor {
    WHITE("White"),
    BLACK("Black");

    private final String displayName;

    PieceColor(String displayName) {
        this.displayName = displayName;
    }

    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** Row delta a pawn of this color moves by. */
    public int forwardDirection() {
        return this == WHITE ? -1 : 1;
    }

    public int pawnStartRow() {
        return this == WHITE ? 6 : 1;
    }

    public int promotionRow() {
        return this == WHITE ? 0 : 7;
    }

    public int backRank() {
        return this == WHITE ? 7 : 0;
    }

    public String getDisplayName() {
        return displayName;
    }
}
