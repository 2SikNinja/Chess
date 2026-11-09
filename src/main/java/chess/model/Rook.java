package chess.model;

import java.util.List;

public class Rook extends Piece {

    public Rook(PieceColor color) {
        super(color);
    }

    @Override
    public char getSymbol() {
        return 'R';
    }

    @Override
    public int getValue() {
        return 500;
    }

    @Override
    public void generateMoves(Board board, Position from, List<Move> moves) {
        addSlidingMoves(board, from, ORTHOGONAL, moves);
    }
}
