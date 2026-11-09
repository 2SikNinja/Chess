package chess.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import chess.model.Board;
import chess.model.Move;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MinimaxAITest {

    @Test
    void findsBackRankMateInOne() {
        Board board = Board.fromFen("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1");
        Move move = new MinimaxAI(2, new Random(1)).findBestMove(board);
        assertEquals("a1a8", move.toUci());
    }

    @Test
    void capturesAnUndefendedQueen() {
        Board board = Board.fromFen("4k3/8/8/3q4/8/8/3R4/4K3 w - - 0 1");
        Move move = new MinimaxAI(1, new Random(1)).findBestMove(board);
        assertEquals("d2d5", move.toUci());
    }

    @Test
    void blackAlsoFindsMate() {
        Board board = Board.fromFen("r5k1/8/8/8/8/8/5PPP/6K1 b - - 0 1");
        Move move = new MinimaxAI(2, new Random(1)).findBestMove(board);
        assertEquals("a8a1", move.toUci());
    }

    @Test
    void searchDoesNotChangeTheBoard() {
        Board board = Board.startingPosition();
        new MinimaxAI(3, new Random(1)).findBestMove(board);
        assertEquals(Board.START_FEN, board.toFen());
    }

    @Test
    void returnsNullWhenThereAreNoMoves() {
        Board checkmated = Board.fromFen("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
        assertNull(new MinimaxAI(2).findBestMove(checkmated));
    }
}
