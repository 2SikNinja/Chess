package chess.model;

import java.util.List;

public class King extends Piece {

    public King(PieceColor color) {
        super(color);
    }

    @Override
    public char getSymbol() {
        return 'K';
    }

    @Override
    public int getValue() {
        // The king is never traded, so it carries no material value
        return 0;
    }

    @Override
    public void generateMoves(Board board, Position from, List<Move> moves) {
        addStepMoves(board, from, ALL_DIRECTIONS, moves);
        addCastlingMoves(board, from, moves);
    }

    private void addCastlingMoves(Board board, Position from, List<Move> moves) {
        PieceColor color = getColor();
        int row = color.backRank();
        if (!from.equals(new Position(row, 4))) {
            return;
        }
        PieceColor enemy = color.opposite();
        boolean kingside = board.canCastle(color, true);
        boolean queenside = board.canCastle(color, false);
        if (!kingside && !queenside) {
            return;
        }
        // A king may not castle out of check
        if (board.isSquareAttacked(from, enemy)) {
            return;
        }

        if (kingside
                && hasOwnRook(board, row, 7)
                && board.isEmpty(row, 5) && board.isEmpty(row, 6)
                && !board.isSquareAttacked(new Position(row, 5), enemy)
                && !board.isSquareAttacked(new Position(row, 6), enemy)) {
            moves.add(new Move(from, new Position(row, 6), this, null, null, MoveType.CASTLE_KINGSIDE));
        }

        if (queenside
                && hasOwnRook(board, row, 0)
                && board.isEmpty(row, 1) && board.isEmpty(row, 2) && board.isEmpty(row, 3)
                && !board.isSquareAttacked(new Position(row, 3), enemy)
                && !board.isSquareAttacked(new Position(row, 2), enemy)) {
            moves.add(new Move(from, new Position(row, 2), this, null, null, MoveType.CASTLE_QUEENSIDE));
        }
    }

    private boolean hasOwnRook(Board board, int row, int col) {
        Piece piece = board.getPiece(row, col);
        return piece instanceof Rook && piece.getColor() == getColor();
    }
}
