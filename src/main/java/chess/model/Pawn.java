package chess.model;

import java.util.List;

public class Pawn extends Piece {

    public Pawn(PieceColor color) {
        super(color);
    }

    @Override
    public char getSymbol() {
        return 'P';
    }

    @Override
    public int getValue() {
        return 100;
    }

    @Override
    public void generateMoves(Board board, Position from, List<Move> moves) {
        PieceColor color = getColor();
        int direction = color.forwardDirection();

        // Forward one square, and two from the starting row
        Position oneStep = from.offset(direction, 0);
        if (oneStep.isOnBoard() && board.isEmpty(oneStep)) {
            addAdvance(from, oneStep, null, moves);

            Position twoSteps = from.offset(2 * direction, 0);
            if (from.row() == color.pawnStartRow() && board.isEmpty(twoSteps)) {
                moves.add(new Move(from, twoSteps, this, null, null, MoveType.DOUBLE_PAWN_PUSH));
            }
        }

        // Diagonal captures, including en passant
        for (int colDelta = -1; colDelta <= 1; colDelta += 2) {
            Position target = from.offset(direction, colDelta);
            if (!target.isOnBoard()) {
                continue;
            }
            Piece occupant = board.getPiece(target);
            if (occupant != null && occupant.getColor() != color) {
                addAdvance(from, target, occupant, moves);
            } else if (occupant == null && target.equals(board.getEnPassantTarget())) {
                Piece passedPawn = board.getPiece(from.row(), target.col());
                moves.add(new Move(from, target, this, passedPawn, null, MoveType.EN_PASSANT));
            }
        }
    }

    /** Adds a normal pawn move, or one move per promotion choice on the last rank. */
    private void addAdvance(Position from, Position to, Piece captured, List<Move> moves) {
        if (to.row() == getColor().promotionRow()) {
            for (char symbol : new char[] {'Q', 'R', 'B', 'N'}) {
                Piece promotion = Piece.fromSymbol(symbol, getColor());
                moves.add(new Move(from, to, this, captured, promotion, MoveType.PROMOTION));
            }
        } else {
            moves.add(Move.normal(from, to, this, captured));
        }
    }
}
