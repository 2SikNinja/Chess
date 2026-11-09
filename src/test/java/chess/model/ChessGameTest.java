package chess.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChessGameTest {

    /** Plays a move given in UCI form such as "e2e4" or "e7e8q". */
    private static void play(ChessGame game, String uci) {
        Position from = Position.fromAlgebraic(uci.substring(0, 2));
        Position to = Position.fromAlgebraic(uci.substring(2, 4));
        char promotion = uci.length() > 4 ? Character.toUpperCase(uci.charAt(4)) : ' ';
        for (Move move : game.getLegalMoves(from)) {
            char moveSymbol = move.isPromotion() ? move.promotion().getSymbol() : ' ';
            if (move.to().equals(to) && moveSymbol == promotion) {
                game.play(move);
                return;
            }
        }
        throw new AssertionError("Illegal move in test: " + uci);
    }

    @Test
    void foolsMateIsCheckmate() {
        ChessGame game = new ChessGame();
        for (String move : List.of("f2f3", "e7e5", "g2g4", "d8h4")) {
            play(game, move);
        }
        assertEquals(GameStatus.CHECKMATE, game.getStatus());
        assertEquals(PieceColor.BLACK, game.getWinner());
        assertEquals(List.of("f3", "e5", "g4", "Qh4#"), game.getSanMoves());
    }

    @Test
    void stalemateIsADraw() {
        ChessGame game = new ChessGame(Board.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"));
        assertEquals(GameStatus.STALEMATE, game.getStatus());
        assertNull(game.getWinner());
    }

    @Test
    void threefoldRepetitionIsDetected() {
        ChessGame game = new ChessGame();
        for (int i = 0; i < 2; i++) {
            play(game, "g1f3");
            play(game, "g8f6");
            play(game, "f3g1");
            play(game, "f6g8");
        }
        assertEquals(GameStatus.THREEFOLD_REPETITION, game.getStatus());
    }

    @Test
    void insufficientMaterialIsDetected() {
        assertTrue(Board.fromFen("8/8/4k3/8/8/2B5/4K3/8 w - - 0 1").hasInsufficientMaterial());
        assertFalse(Board.fromFen("8/8/4k3/8/8/2R5/4K3/8 w - - 0 1").hasInsufficientMaterial());
    }

    @Test
    void sanCoversCastlingCapturesPromotionAndDisambiguation() {
        ChessGame castle = new ChessGame(Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"));
        play(castle, "e1g1");
        play(castle, "e8c8");
        assertEquals(List.of("O-O", "O-O-O"), castle.getSanMoves());

        ChessGame promote = new ChessGame(Board.fromFen("1r5k/P7/8/8/8/8/8/K7 w - - 0 1"));
        play(promote, "a7b8n");
        assertEquals("axb8=N", promote.getSanMoves().get(0));

        ChessGame knights = new ChessGame(Board.fromFen("7k/8/8/8/8/8/8/1N3N1K w - - 0 1"));
        play(knights, "b1d2");
        assertEquals("Nbd2", knights.getSanMoves().get(0));
    }

    @Test
    void enPassantCapturesThePassedPawn() {
        ChessGame game = new ChessGame();
        for (String move : List.of("e2e4", "a7a6", "e4e5", "d7d5", "e5d6")) {
            play(game, move);
        }
        assertNull(game.getBoard().getPiece(Position.fromAlgebraic("d5")));
        assertEquals("exd6", game.getSanMoves().get(4));
    }

    @Test
    void undoTakesBackMoves() {
        ChessGame game = new ChessGame();
        play(game, "e2e4");
        play(game, "e7e5");
        assertTrue(game.undo());
        assertTrue(game.undo());
        assertFalse(game.undo());
        assertEquals(Board.START_FEN, game.getBoard().toFen());
    }

    @Test
    void fenRoundTrips() {
        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        assertEquals(fen, Board.fromFen(fen).toFen());
    }
}
