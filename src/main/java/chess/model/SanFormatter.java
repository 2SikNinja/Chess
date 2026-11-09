package chess.model;

import java.util.List;

/** Converts moves to Standard Algebraic Notation, for example "Nf3", "exd5", "O-O" or "Qh7#". */
public final class SanFormatter {

    private SanFormatter() {
    }

    /**
     * Formats {@code move}, which must be legal on {@code board}. The board is
     * left exactly as it was found.
     */
    public static String toSan(Board board, Move move) {
        StringBuilder san = new StringBuilder();
        Piece piece = move.piece();

        if (move.type() == MoveType.CASTLE_KINGSIDE) {
            san.append("O-O");
        } else if (move.type() == MoveType.CASTLE_QUEENSIDE) {
            san.append("O-O-O");
        } else if (piece instanceof Pawn) {
            if (move.isCapture()) {
                san.append(move.from().fileChar()).append('x');
            }
            san.append(move.to().toAlgebraic());
            if (move.isPromotion()) {
                san.append('=').append(move.promotion().getSymbol());
            }
        } else {
            san.append(piece.getSymbol());
            san.append(disambiguation(board, move));
            if (move.isCapture()) {
                san.append('x');
            }
            san.append(move.to().toAlgebraic());
        }

        board.makeMove(move);
        PieceColor opponent = board.getSideToMove();
        if (board.isInCheck(opponent)) {
            san.append(board.getLegalMoves().isEmpty() ? '#' : '+');
        }
        board.undoMove();
        return san.toString();
    }

    /** Adds a file and/or rank when two identical pieces can reach the same square. */
    private static String disambiguation(Board board, Move move) {
        List<Move> legalMoves = board.getLegalMoves();
        boolean ambiguous = false;
        boolean sameFile = false;
        boolean sameRank = false;
        for (Move other : legalMoves) {
            if (other.to().equals(move.to())
                    && !other.from().equals(move.from())
                    && other.piece().getClass() == move.piece().getClass()) {
                ambiguous = true;
                sameFile |= other.from().col() == move.from().col();
                sameRank |= other.from().row() == move.from().row();
            }
        }
        if (!ambiguous) {
            return "";
        }
        if (!sameFile) {
            return String.valueOf(move.from().fileChar());
        }
        if (!sameRank) {
            return String.valueOf(move.from().rankChar());
        }
        return move.from().toAlgebraic();
    }
}
