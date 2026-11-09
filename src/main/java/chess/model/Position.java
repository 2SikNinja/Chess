package chess.model;

/**
 * An immutable board coordinate. Row 0 is rank 8 and column 0 is file a,
 * so {@code new Position(7, 4)} is e1.
 */
public record Position(int row, int col) {

    public static boolean isOnBoard(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    public boolean isOnBoard() {
        return isOnBoard(row, col);
    }

    public Position offset(int rowDelta, int colDelta) {
        return new Position(row + rowDelta, col + colDelta);
    }

    /** True for dark squares (a1, c1, ...). */
    public boolean isDarkSquare() {
        return (row + col) % 2 == 1;
    }

    public char fileChar() {
        return (char) ('a' + col);
    }

    public char rankChar() {
        return (char) ('8' - row);
    }

    public String toAlgebraic() {
        return "" + fileChar() + rankChar();
    }

    /** Parses a square name such as "e4". */
    public static Position fromAlgebraic(String square) {
        if (square == null || square.length() != 2) {
            throw new IllegalArgumentException("Not a square: " + square);
        }
        int col = square.charAt(0) - 'a';
        int row = '8' - square.charAt(1);
        if (!isOnBoard(row, col)) {
            throw new IllegalArgumentException("Not a square: " + square);
        }
        return new Position(row, col);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
