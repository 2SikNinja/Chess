package chess.ai;

import chess.model.Bishop;
import chess.model.Board;
import chess.model.King;
import chess.model.Knight;
import chess.model.Pawn;
import chess.model.Piece;
import chess.model.PieceColor;
import chess.model.Queen;
import chess.model.Rook;

/**
 * Scores a position in centipawns from the point of view of the side to move.
 *
 * <p>The score is material plus piece-square tables, which reward pieces for
 * standing on good squares (knights in the center, rooks on the seventh rank,
 * a sheltered king in the middlegame and an active king in the endgame). The
 * tables are adapted from Tomasz Michniewski's "Simplified Evaluation Function".
 */
public final class Evaluator {

    // Tables are written from White's point of view: row 0 is rank 8.
    private static final int[][] PAWN_TABLE = {
        {0, 0, 0, 0, 0, 0, 0, 0},
        {50, 50, 50, 50, 50, 50, 50, 50},
        {10, 10, 20, 30, 30, 20, 10, 10},
        {5, 5, 10, 25, 25, 10, 5, 5},
        {0, 0, 0, 20, 20, 0, 0, 0},
        {5, -5, -10, 0, 0, -10, -5, 5},
        {5, 10, 10, -20, -20, 10, 10, 5},
        {0, 0, 0, 0, 0, 0, 0, 0}
    };

    private static final int[][] KNIGHT_TABLE = {
        {-50, -40, -30, -30, -30, -30, -40, -50},
        {-40, -20, 0, 0, 0, 0, -20, -40},
        {-30, 0, 10, 15, 15, 10, 0, -30},
        {-30, 5, 15, 20, 20, 15, 5, -30},
        {-30, 0, 15, 20, 20, 15, 0, -30},
        {-30, 5, 10, 15, 15, 10, 5, -30},
        {-40, -20, 0, 5, 5, 0, -20, -40},
        {-50, -40, -30, -30, -30, -30, -40, -50}
    };

    private static final int[][] BISHOP_TABLE = {
        {-20, -10, -10, -10, -10, -10, -10, -20},
        {-10, 0, 0, 0, 0, 0, 0, -10},
        {-10, 0, 5, 10, 10, 5, 0, -10},
        {-10, 5, 5, 10, 10, 5, 5, -10},
        {-10, 0, 10, 10, 10, 10, 0, -10},
        {-10, 10, 10, 10, 10, 10, 10, -10},
        {-10, 5, 0, 0, 0, 0, 5, -10},
        {-20, -10, -10, -10, -10, -10, -10, -20}
    };

    private static final int[][] ROOK_TABLE = {
        {0, 0, 0, 0, 0, 0, 0, 0},
        {5, 10, 10, 10, 10, 10, 10, 5},
        {-5, 0, 0, 0, 0, 0, 0, -5},
        {-5, 0, 0, 0, 0, 0, 0, -5},
        {-5, 0, 0, 0, 0, 0, 0, -5},
        {-5, 0, 0, 0, 0, 0, 0, -5},
        {-5, 0, 0, 0, 0, 0, 0, -5},
        {0, 0, 0, 5, 5, 0, 0, 0}
    };

    private static final int[][] QUEEN_TABLE = {
        {-20, -10, -10, -5, -5, -10, -10, -20},
        {-10, 0, 0, 0, 0, 0, 0, -10},
        {-10, 0, 5, 5, 5, 5, 0, -10},
        {-5, 0, 5, 5, 5, 5, 0, -5},
        {0, 0, 5, 5, 5, 5, 0, -5},
        {-10, 5, 5, 5, 5, 5, 0, -10},
        {-10, 0, 5, 0, 0, 0, 0, -10},
        {-20, -10, -10, -5, -5, -10, -10, -20}
    };

    private static final int[][] KING_MIDDLEGAME_TABLE = {
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30},
        {-20, -30, -30, -40, -40, -30, -30, -20},
        {-10, -20, -20, -20, -20, -20, -20, -10},
        {20, 20, 0, 0, 0, 0, 20, 20},
        {20, 30, 10, 0, 0, 10, 30, 20}
    };

    private static final int[][] KING_ENDGAME_TABLE = {
        {-50, -40, -30, -20, -20, -30, -40, -50},
        {-30, -20, -10, 0, 0, -10, -20, -30},
        {-30, -10, 20, 30, 30, 20, -10, -30},
        {-30, -10, 30, 40, 40, 30, -10, -30},
        {-30, -10, 30, 40, 40, 30, -10, -30},
        {-30, -10, 20, 30, 30, 20, -10, -30},
        {-30, -30, 0, 0, 0, 0, -30, -30},
        {-50, -30, -30, -30, -30, -30, -30, -50}
    };

    /** Below this much non-pawn material (both sides combined) the king should come out. */
    private static final int ENDGAME_MATERIAL_THRESHOLD = 1_600;

    private Evaluator() {
    }

    /** Positive scores are good for the side to move. */
    public static int evaluate(Board board) {
        boolean endgame = nonPawnMaterial(board) <= ENDGAME_MATERIAL_THRESHOLD;
        int whiteScore = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null) {
                    continue;
                }
                int tableRow = piece.getColor() == PieceColor.WHITE ? row : 7 - row;
                int value = piece.getValue() + tableFor(piece, endgame)[tableRow][col];
                whiteScore += piece.getColor() == PieceColor.WHITE ? value : -value;
            }
        }
        return board.getSideToMove() == PieceColor.WHITE ? whiteScore : -whiteScore;
    }

    private static int[][] tableFor(Piece piece, boolean endgame) {
        if (piece instanceof Pawn) {
            return PAWN_TABLE;
        } else if (piece instanceof Knight) {
            return KNIGHT_TABLE;
        } else if (piece instanceof Bishop) {
            return BISHOP_TABLE;
        } else if (piece instanceof Rook) {
            return ROOK_TABLE;
        } else if (piece instanceof Queen) {
            return QUEEN_TABLE;
        } else if (piece instanceof King) {
            return endgame ? KING_ENDGAME_TABLE : KING_MIDDLEGAME_TABLE;
        }
        throw new IllegalStateException("Unknown piece " + piece);
    }

    private static int nonPawnMaterial(Board board) {
        int total = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && !(piece instanceof Pawn)) {
                    total += piece.getValue();
                }
            }
        }
        return total;
    }
}
