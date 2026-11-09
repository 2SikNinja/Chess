package chess.model;

import java.util.List;

public class Knight extends Piece {

    static final int[][] JUMPS = {
        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    public Knight(PieceColor color) {
        super(color);
    }

    @Override
    public char getSymbol() {
        return 'N';
    }

    @Override
    public int getValue() {
        return 320;
    }

    @Override
    public void generateMoves(Board board, Position from, List<Move> moves) {
        addStepMoves(board, from, JUMPS, moves);
    }
}
