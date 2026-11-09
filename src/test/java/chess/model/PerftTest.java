package chess.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Perft ("performance test") counts every leaf of the move tree to a fixed
 * depth and compares it with known-correct totals. A single bug in castling,
 * en passant, promotion or check detection changes these numbers, so this is
 * the standard way to prove a move generator is correct.
 *
 * <p>Expected values come from the Chess Programming Wiki perft results page.
 */
class PerftTest {

    static long perft(Board board, int depth) {
        if (depth == 0) {
            return 1;
        }
        long nodes = 0;
        for (Move move : board.getLegalMoves()) {
            board.makeMove(move);
            nodes += perft(board, depth - 1);
            board.undoMove();
        }
        return nodes;
    }

    @Test
    void startingPosition() {
        Board board = Board.startingPosition();
        assertEquals(20, perft(board, 1));
        assertEquals(400, perft(board, 2));
        assertEquals(8_902, perft(board, 3));
        assertEquals(197_281, perft(board, 4));
    }

    @Test
    void kiwipeteCastlingAndPins() {
        Board board = Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        assertEquals(48, perft(board, 1));
        assertEquals(2_039, perft(board, 2));
        assertEquals(97_862, perft(board, 3));
    }

    @Test
    void enPassantAndDiscoveredChecks() {
        Board board = Board.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        assertEquals(14, perft(board, 1));
        assertEquals(191, perft(board, 2));
        assertEquals(2_812, perft(board, 3));
        assertEquals(43_238, perft(board, 4));
    }

    @Test
    void promotions() {
        Board board = Board.fromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
        assertEquals(6, perft(board, 1));
        assertEquals(264, perft(board, 2));
        assertEquals(9_467, perft(board, 3));
    }

    @Test
    void undoRestoresThePosition() {
        Board board = Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        String before = board.toFen();
        perft(board, 3);
        assertEquals(before, board.toFen());
    }
}
