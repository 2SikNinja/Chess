package chess.model;

import java.util.List;

/**
 * Base class for every chess piece.
 *
 * <p>Pieces are immutable and do not know where they stand. The {@link Board}
 * tracks positions, which keeps the board as the single source of truth and
 * lets the AI make and undo thousands of moves without any piece getting out
 * of sync.
 */
public abstract class Piece {

    protected static final int[][] ORTHOGONAL = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    protected static final int[][] DIAGONAL = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    protected static final int[][] ALL_DIRECTIONS = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private final PieceColor color;

    protected Piece(PieceColor color) {
        this.color = color;
    }

    public PieceColor getColor() {
        return color;
    }

    /** Uppercase letter used in algebraic notation: K, Q, R, B, N or P. */
    public abstract char getSymbol();

    /** Material value in centipawns. */
    public abstract int getValue();

    /**
     * Adds every pseudo-legal move for this piece to {@code moves}. Moves that
     * would leave the mover's own king in check are filtered out by the board.
     */
    public abstract void generateMoves(Board board, Position from, List<Move> moves);

    /** Lowercase for Black, uppercase for White, as used in FEN strings. */
    public char getFenChar() {
        return color == PieceColor.WHITE ? getSymbol() : Character.toLowerCase(getSymbol());
    }

    /** Resource name of this piece's image, for example "wknight". */
    public String getImageName() {
        String prefix = color == PieceColor.WHITE ? "w" : "b";
        return prefix + getClass().getSimpleName().toLowerCase();
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    /** Rook, bishop and queen movement: keep going until something is in the way. */
    protected void addSlidingMoves(Board board, Position from, int[][] directions, List<Move> moves) {
        for (int[] direction : directions) {
            int row = from.row() + direction[0];
            int col = from.col() + direction[1];
            while (Position.isOnBoard(row, col)) {
                Piece target = board.getPiece(row, col);
                Position to = new Position(row, col);
                if (target == null) {
                    moves.add(Move.normal(from, to, this, null));
                } else {
                    if (target.getColor() != color) {
                        moves.add(Move.normal(from, to, this, target));
                    }
                    break;
                }
                row += direction[0];
                col += direction[1];
            }
        }
    }

    /** King and knight movement: a fixed list of single jumps. */
    protected void addStepMoves(Board board, Position from, int[][] offsets, List<Move> moves) {
        for (int[] offset : offsets) {
            int row = from.row() + offset[0];
            int col = from.col() + offset[1];
            if (!Position.isOnBoard(row, col)) {
                continue;
            }
            Piece target = board.getPiece(row, col);
            if (target == null || target.getColor() != color) {
                moves.add(Move.normal(from, new Position(row, col), this, target));
            }
        }
    }

    /** Creates a piece from a FEN letter such as 'N' or 'q'. */
    public static Piece fromFenChar(char fenChar) {
        PieceColor color = Character.isUpperCase(fenChar) ? PieceColor.WHITE : PieceColor.BLACK;
        return fromSymbol(Character.toUpperCase(fenChar), color);
    }

    public static Piece fromSymbol(char symbol, PieceColor color) {
        return switch (symbol) {
            case 'K' -> new King(color);
            case 'Q' -> new Queen(color);
            case 'R' -> new Rook(color);
            case 'B' -> new Bishop(color);
            case 'N' -> new Knight(color);
            case 'P' -> new Pawn(color);
            default -> throw new IllegalArgumentException("Unknown piece: " + symbol);
        };
    }

    @Override
    public String toString() {
        return color.getDisplayName() + " " + getName();
    }
}
