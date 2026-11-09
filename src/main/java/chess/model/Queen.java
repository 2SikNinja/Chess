package chess.model;

import java.util.List;

public class Queen extends Piece {

    public Queen(PieceColor color) {
        super(color);
    }

    @Override
    public char getSymbol() {
        return 'Q';
    }

    @Override
    public int getValue() {
        return 900;
    }

    @Override
    public void generateMoves(Board board, Position from, List<Move> moves) {
        addSlidingMoves(board, from, ALL_DIRECTIONS, moves);
    }
}
