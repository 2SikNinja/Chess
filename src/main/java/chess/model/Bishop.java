package chess.model;

import java.util.List;

public class Bishop extends Piece {

    public Bishop(PieceColor color) {
        super(color);
    }

    @Override
    public char getSymbol() {
        return 'B';
    }

    @Override
    public int getValue() {
        return 330;
    }

    @Override
    public void generateMoves(Board board, Position from, List<Move> moves) {
        addSlidingMoves(board, from, DIAGONAL, moves);
    }
}
