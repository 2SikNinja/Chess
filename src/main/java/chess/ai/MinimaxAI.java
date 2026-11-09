package chess.ai;

import chess.model.Board;
import chess.model.Move;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Computer opponent using minimax search in its negamax form, with
 * alpha-beta pruning, capture-first move ordering and a quiescence search.
 *
 * <ul>
 *   <li><b>Negamax</b>: a position worth +X to one side is worth -X to the
 *   other, so one recursive function covers both players.</li>
 *   <li><b>Alpha-beta pruning</b>: once a move is proven worse than an option
 *   already found, the rest of its replies are skipped.</li>
 *   <li><b>Move ordering</b>: trying captures of valuable pieces first makes
 *   pruning happen much earlier.</li>
 *   <li><b>Quiescence search</b>: at the depth limit, keep following captures
 *   so the AI does not stop counting in the middle of a trade.</li>
 * </ul>
 */
public class MinimaxAI {

    private static final int INFINITY = 1_000_000_000;
    private static final int MATE_SCORE = 1_000_000;
    private static final int MAX_QUIESCENCE_DEPTH = 6;

    private static final Comparator<Move> CAPTURES_FIRST =
            Comparator.comparingInt(MinimaxAI::orderingScore).reversed();

    private final int searchDepth;
    private final Random random;
    private volatile boolean cancelled;
    private long nodesSearched;

    public MinimaxAI(int searchDepth) {
        this(searchDepth, new Random());
    }

    public MinimaxAI(int searchDepth, Random random) {
        if (searchDepth < 1) {
            throw new IllegalArgumentException("Search depth must be at least 1");
        }
        this.searchDepth = searchDepth;
        this.random = random;
    }

    /**
     * Finds the best move for the side to move, or null if there are no legal
     * moves. The given board is not modified.
     */
    public Move findBestMove(Board position) {
        cancelled = false;
        nodesSearched = 0;
        Board board = position.copy();

        List<Move> moves = new ArrayList<>(board.getLegalMoves());
        if (moves.isEmpty()) {
            return null;
        }
        // Shuffle before the stable sort so equally good moves vary between games
        Collections.shuffle(moves, random);
        moves.sort(CAPTURES_FIRST);

        Move bestMove = moves.get(0);
        int alpha = -INFINITY;
        for (Move move : moves) {
            board.makeMove(move);
            int score = -negamax(board, searchDepth - 1, -INFINITY, -alpha, 1);
            board.undoMove();
            if (cancelled) {
                break;
            }
            if (score > alpha) {
                alpha = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /** Stops a search running on another thread as soon as possible. */
    public void cancel() {
        cancelled = true;
    }

    public long getNodesSearched() {
        return nodesSearched;
    }

    public int getSearchDepth() {
        return searchDepth;
    }

    private int negamax(Board board, int depth, int alpha, int beta, int ply) {
        if (cancelled) {
            return 0;
        }
        nodesSearched++;

        List<Move> moves = board.getLegalMoves();
        if (moves.isEmpty()) {
            // Checkmate is scored relative to ply so the AI prefers the fastest mate
            return board.isInCheck(board.getSideToMove()) ? -(MATE_SCORE - ply) : 0;
        }
        if (board.getHalfmoveClock() >= 100 || board.hasInsufficientMaterial()) {
            return 0;
        }
        if (depth == 0) {
            return quiescence(board, alpha, beta, 0);
        }

        moves.sort(CAPTURES_FIRST);
        for (Move move : moves) {
            board.makeMove(move);
            int score = -negamax(board, depth - 1, -beta, -alpha, ply + 1);
            board.undoMove();
            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return alpha;
    }

    /** Searches captures only until the position is quiet. */
    private int quiescence(Board board, int alpha, int beta, int depth) {
        nodesSearched++;
        int standPat = Evaluator.evaluate(board);
        if (standPat >= beta) {
            return beta;
        }
        if (standPat > alpha) {
            alpha = standPat;
        }
        if (depth >= MAX_QUIESCENCE_DEPTH) {
            return alpha;
        }

        List<Move> captures = new ArrayList<>();
        for (Move move : board.getLegalMoves()) {
            if (move.isCapture() || move.isPromotion()) {
                captures.add(move);
            }
        }
        captures.sort(CAPTURES_FIRST);

        for (Move move : captures) {
            board.makeMove(move);
            int score = -quiescence(board, -beta, -alpha, depth + 1);
            board.undoMove();
            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return alpha;
    }

    /** Most Valuable Victim, Least Valuable Attacker: PxQ before QxP. */
    private static int orderingScore(Move move) {
        int score = 0;
        if (move.isCapture()) {
            score += 10 * move.captured().getValue() - move.piece().getValue() + 10_000;
        }
        if (move.isPromotion()) {
            score += move.promotion().getValue();
        }
        return score;
    }
}
