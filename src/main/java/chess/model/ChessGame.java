package chess.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A game in progress: the board plus the move record, repetition tracking and
 * the result. This is what the user interface talks to.
 */
public class ChessGame {

    private final Board board;
    private final List<String> sanMoves = new ArrayList<>();
    private final List<String> positionKeys = new ArrayList<>();

    public ChessGame() {
        this(Board.startingPosition());
    }

    public ChessGame(Board board) {
        this.board = board;
        positionKeys.add(board.getPositionKey());
    }

    /** The live board. Treat it as read-only; use {@link #play(Move)} to change it. */
    public Board getBoard() {
        return board;
    }

    public PieceColor getSideToMove() {
        return board.getSideToMove();
    }

    public List<Move> getLegalMoves() {
        return board.getLegalMoves();
    }

    public List<Move> getLegalMoves(Position from) {
        return board.getLegalMoves(from);
    }

    /** Plays a legal move and records it. */
    public void play(Move move) {
        if (!board.getLegalMoves(move.from()).contains(move)) {
            throw new IllegalArgumentException("Illegal move: " + move);
        }
        String san = SanFormatter.toSan(board, move);
        board.makeMove(move);
        sanMoves.add(san);
        positionKeys.add(board.getPositionKey());
    }

    /** Takes back the last move. Returns false if there was nothing to undo. */
    public boolean undo() {
        if (sanMoves.isEmpty()) {
            return false;
        }
        board.undoMove();
        sanMoves.remove(sanMoves.size() - 1);
        positionKeys.remove(positionKeys.size() - 1);
        return true;
    }

    public Move getLastMove() {
        return board.getLastMove();
    }

    public List<String> getSanMoves() {
        return Collections.unmodifiableList(sanMoves);
    }

    public int getPlyCount() {
        return sanMoves.size();
    }

    public boolean isInCheck() {
        return board.isInCheck(board.getSideToMove());
    }

    public GameStatus getStatus() {
        if (board.getLegalMoves().isEmpty()) {
            return isInCheck() ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }
        if (board.hasInsufficientMaterial()) {
            return GameStatus.INSUFFICIENT_MATERIAL;
        }
        if (board.getHalfmoveClock() >= 100) {
            return GameStatus.FIFTY_MOVE_RULE;
        }
        String currentKey = positionKeys.get(positionKeys.size() - 1);
        if (Collections.frequency(positionKeys, currentKey) >= 3) {
            return GameStatus.THREEFOLD_REPETITION;
        }
        return GameStatus.IN_PROGRESS;
    }

    /** The winner after checkmate, or null if the game is not won. */
    public PieceColor getWinner() {
        return getStatus() == GameStatus.CHECKMATE ? board.getSideToMove().opposite() : null;
    }
}
