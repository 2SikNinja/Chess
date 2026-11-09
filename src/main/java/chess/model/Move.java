package chess.model;

/**
 * A single move. Moves are created by the pieces during move generation, so
 * {@code piece} and {@code captured} are the actual objects on the board.
 *
 * @param from      starting square
 * @param to        destination square
 * @param piece     the piece being moved
 * @param captured  the captured piece, or null (for en passant this is the pawn beside {@code to})
 * @param promotion the piece a pawn turns into, or null
 * @param type      special handling the board must apply
 */
public record Move(Position from, Position to, Piece piece, Piece captured, Piece promotion, MoveType type) {

    public static Move normal(Position from, Position to, Piece piece, Piece captured) {
        return new Move(from, to, piece, captured, null, MoveType.NORMAL);
    }

    public boolean isCapture() {
        return captured != null;
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    public boolean isCastle() {
        return type == MoveType.CASTLE_KINGSIDE || type == MoveType.CASTLE_QUEENSIDE;
    }

    /** Long algebraic / UCI form, for example "e2e4" or "e7e8q". */
    public String toUci() {
        String text = from.toAlgebraic() + to.toAlgebraic();
        if (promotion != null) {
            text += Character.toLowerCase(promotion.getSymbol());
        }
        return text;
    }

    /**
     * Two moves are equal when they go between the same squares and promote to
     * the same kind of piece. Promotion pieces are created fresh during move
     * generation, so comparing them by identity would never match.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Move move)) {
            return false;
        }
        return from.equals(move.from) && to.equals(move.to) && promotionSymbol() == move.promotionSymbol();
    }

    @Override
    public int hashCode() {
        return (from.hashCode() * 31 + to.hashCode()) * 31 + promotionSymbol();
    }

    private char promotionSymbol() {
        return promotion == null ? ' ' : promotion.getSymbol();
    }

    @Override
    public String toString() {
        return toUci();
    }
}
