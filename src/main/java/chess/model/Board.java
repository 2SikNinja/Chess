package chess.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The chess board and the full rules of movement: castling, en passant,
 * promotion, check detection and legal move generation.
 *
 * <p>Moves are applied with {@link #makeMove(Move)} and reverted with
 * {@link #undoMove()}. The AI searches on a {@link #copy()} so it never
 * touches the board the user is looking at.
 */
public class Board {

    public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final int WHITE_KINGSIDE = 1;
    private static final int WHITE_QUEENSIDE = 2;
    private static final int BLACK_KINGSIDE = 4;
    private static final int BLACK_QUEENSIDE = 8;

    /** Everything makeMove changes that cannot be recomputed from the move itself. */
    private record UndoInfo(Move move, int castlingRights, Position enPassantTarget,
                            int halfmoveClock, int fullmoveNumber) {
    }

    private final Piece[][] squares = new Piece[8][8];
    private final Deque<UndoInfo> history = new ArrayDeque<>();
    private PieceColor sideToMove = PieceColor.WHITE;
    private int castlingRights;
    private Position enPassantTarget;
    private int halfmoveClock;
    private int fullmoveNumber = 1;
    private Position whiteKing;
    private Position blackKing;

    public static Board startingPosition() {
        return fromFen(START_FEN);
    }

    /** Builds a board from a FEN string, for example {@link #START_FEN}. */
    public static Board fromFen(String fen) {
        String[] fields = fen.trim().split("\\s+");
        if (fields.length < 4) {
            throw new IllegalArgumentException("Invalid FEN: " + fen);
        }
        Board board = new Board();

        String[] ranks = fields[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("Invalid FEN placement: " + fields[0]);
        }
        for (int row = 0; row < 8; row++) {
            int col = 0;
            for (char symbol : ranks[row].toCharArray()) {
                if (Character.isDigit(symbol)) {
                    col += symbol - '0';
                } else {
                    board.setPiece(new Position(row, col), Piece.fromFenChar(symbol));
                    col++;
                }
            }
        }

        board.sideToMove = fields[1].equals("b") ? PieceColor.BLACK : PieceColor.WHITE;

        for (char right : fields[2].toCharArray()) {
            switch (right) {
                case 'K' -> board.castlingRights |= WHITE_KINGSIDE;
                case 'Q' -> board.castlingRights |= WHITE_QUEENSIDE;
                case 'k' -> board.castlingRights |= BLACK_KINGSIDE;
                case 'q' -> board.castlingRights |= BLACK_QUEENSIDE;
                default -> { }
            }
        }

        board.enPassantTarget = fields[3].equals("-") ? null : Position.fromAlgebraic(fields[3]);
        board.halfmoveClock = fields.length > 4 ? Integer.parseInt(fields[4]) : 0;
        board.fullmoveNumber = fields.length > 5 ? Integer.parseInt(fields[5]) : 1;
        return board;
    }

    /** An independent copy of the current position (without move history). */
    public Board copy() {
        Board copy = new Board();
        for (int row = 0; row < 8; row++) {
            System.arraycopy(squares[row], 0, copy.squares[row], 0, 8);
        }
        copy.sideToMove = sideToMove;
        copy.castlingRights = castlingRights;
        copy.enPassantTarget = enPassantTarget;
        copy.halfmoveClock = halfmoveClock;
        copy.fullmoveNumber = fullmoveNumber;
        copy.whiteKing = whiteKing;
        copy.blackKing = blackKing;
        return copy;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public Piece getPiece(int row, int col) {
        return squares[row][col];
    }

    public Piece getPiece(Position position) {
        return squares[position.row()][position.col()];
    }

    public boolean isEmpty(int row, int col) {
        return squares[row][col] == null;
    }

    public boolean isEmpty(Position position) {
        return getPiece(position) == null;
    }

    public PieceColor getSideToMove() {
        return sideToMove;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    public Position getKingPosition(PieceColor color) {
        return color == PieceColor.WHITE ? whiteKing : blackKing;
    }

    public boolean canCastle(PieceColor color, boolean kingside) {
        int flag;
        if (color == PieceColor.WHITE) {
            flag = kingside ? WHITE_KINGSIDE : WHITE_QUEENSIDE;
        } else {
            flag = kingside ? BLACK_KINGSIDE : BLACK_QUEENSIDE;
        }
        return (castlingRights & flag) != 0;
    }

    /** The most recent move made on this board, or null. */
    public Move getLastMove() {
        UndoInfo last = history.peek();
        return last == null ? null : last.move();
    }

    public int getPlyCount() {
        return history.size();
    }

    // ------------------------------------------------------------------
    // Making and undoing moves
    // ------------------------------------------------------------------

    public void makeMove(Move move) {
        history.push(new UndoInfo(move, castlingRights, enPassantTarget, halfmoveClock, fullmoveNumber));

        Piece piece = move.piece();
        Position from = move.from();
        Position to = move.to();
        int row = from.row();

        setPiece(from, null);
        switch (move.type()) {
            case EN_PASSANT -> setPiece(new Position(row, to.col()), null);
            case CASTLE_KINGSIDE -> movePiece(new Position(row, 7), new Position(row, 5));
            case CASTLE_QUEENSIDE -> movePiece(new Position(row, 0), new Position(row, 3));
            default -> { }
        }
        setPiece(to, move.isPromotion() ? move.promotion() : piece);

        updateCastlingRights(piece, from, to);
        enPassantTarget = move.type() == MoveType.DOUBLE_PAWN_PUSH
                ? from.offset(piece.getColor().forwardDirection(), 0)
                : null;
        halfmoveClock = (piece instanceof Pawn || move.isCapture()) ? 0 : halfmoveClock + 1;
        if (sideToMove == PieceColor.BLACK) {
            fullmoveNumber++;
        }
        sideToMove = sideToMove.opposite();
    }

    /** Reverts the last move made with {@link #makeMove(Move)}. */
    public void undoMove() {
        UndoInfo info = history.pop();
        Move move = info.move();
        Position from = move.from();
        Position to = move.to();
        int row = from.row();

        setPiece(to, null);
        setPiece(from, move.piece());
        switch (move.type()) {
            case EN_PASSANT -> setPiece(new Position(row, to.col()), move.captured());
            case CASTLE_KINGSIDE -> movePiece(new Position(row, 5), new Position(row, 7));
            case CASTLE_QUEENSIDE -> movePiece(new Position(row, 3), new Position(row, 0));
            default -> {
                if (move.captured() != null) {
                    setPiece(to, move.captured());
                }
            }
        }

        castlingRights = info.castlingRights();
        enPassantTarget = info.enPassantTarget();
        halfmoveClock = info.halfmoveClock();
        fullmoveNumber = info.fullmoveNumber();
        sideToMove = sideToMove.opposite();
    }

    private void movePiece(Position from, Position to) {
        Piece piece = getPiece(from);
        setPiece(from, null);
        setPiece(to, piece);
    }

    private void setPiece(Position position, Piece piece) {
        squares[position.row()][position.col()] = piece;
        if (piece instanceof King) {
            if (piece.getColor() == PieceColor.WHITE) {
                whiteKing = position;
            } else {
                blackKing = position;
            }
        }
    }

    private void updateCastlingRights(Piece piece, Position from, Position to) {
        if (piece instanceof King) {
            castlingRights &= piece.getColor() == PieceColor.WHITE
                    ? ~(WHITE_KINGSIDE | WHITE_QUEENSIDE)
                    : ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
        }
        // A rook leaving its corner, or being captured there, removes that right
        for (Position corner : new Position[] {from, to}) {
            if (corner.equals(new Position(7, 7))) {
                castlingRights &= ~WHITE_KINGSIDE;
            } else if (corner.equals(new Position(7, 0))) {
                castlingRights &= ~WHITE_QUEENSIDE;
            } else if (corner.equals(new Position(0, 7))) {
                castlingRights &= ~BLACK_KINGSIDE;
            } else if (corner.equals(new Position(0, 0))) {
                castlingRights &= ~BLACK_QUEENSIDE;
            }
        }
    }

    // ------------------------------------------------------------------
    // Move generation
    // ------------------------------------------------------------------

    /** Every legal move for the side to move. */
    public List<Move> getLegalMoves() {
        List<Move> pseudoLegal = new ArrayList<>(48);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                if (piece != null && piece.getColor() == sideToMove) {
                    piece.generateMoves(this, new Position(row, col), pseudoLegal);
                }
            }
        }
        return filterLegal(pseudoLegal);
    }

    /** Legal moves for the piece on {@code from} (empty if it is not that side's turn). */
    public List<Move> getLegalMoves(Position from) {
        Piece piece = getPiece(from);
        if (piece == null || piece.getColor() != sideToMove) {
            return List.of();
        }
        List<Move> pseudoLegal = new ArrayList<>();
        piece.generateMoves(this, from, pseudoLegal);
        return filterLegal(pseudoLegal);
    }

    private List<Move> filterLegal(List<Move> pseudoLegal) {
        PieceColor mover = sideToMove;
        List<Move> legal = new ArrayList<>(pseudoLegal.size());
        for (Move move : pseudoLegal) {
            makeMove(move);
            if (!isInCheck(mover)) {
                legal.add(move);
            }
            undoMove();
        }
        return legal;
    }

    public boolean isInCheck(PieceColor color) {
        Position king = getKingPosition(color);
        return king != null && isSquareAttacked(king, color.opposite());
    }

    /** Whether any piece of {@code attacker} attacks {@code target}. */
    public boolean isSquareAttacked(Position target, PieceColor attacker) {
        int row = target.row();
        int col = target.col();

        // Pawns attack diagonally forward, so look one row "behind" the target
        int pawnRow = row - attacker.forwardDirection();
        for (int colDelta = -1; colDelta <= 1; colDelta += 2) {
            if (isPieceAt(pawnRow, col + colDelta, attacker, Pawn.class)) {
                return true;
            }
        }
        for (int[] jump : Knight.JUMPS) {
            if (isPieceAt(row + jump[0], col + jump[1], attacker, Knight.class)) {
                return true;
            }
        }
        for (int[] step : Piece.ALL_DIRECTIONS) {
            if (isPieceAt(row + step[0], col + step[1], attacker, King.class)) {
                return true;
            }
        }
        return isAttackedAlongRays(row, col, attacker, Piece.ORTHOGONAL, Rook.class)
                || isAttackedAlongRays(row, col, attacker, Piece.DIAGONAL, Bishop.class);
    }

    private boolean isAttackedAlongRays(int row, int col, PieceColor attacker, int[][] directions,
                                        Class<? extends Piece> sliderType) {
        for (int[] direction : directions) {
            int r = row + direction[0];
            int c = col + direction[1];
            while (Position.isOnBoard(r, c)) {
                Piece piece = squares[r][c];
                if (piece != null) {
                    if (piece.getColor() == attacker
                            && (sliderType.isInstance(piece) || piece instanceof Queen)) {
                        return true;
                    }
                    break;
                }
                r += direction[0];
                c += direction[1];
            }
        }
        return false;
    }

    private boolean isPieceAt(int row, int col, PieceColor color, Class<? extends Piece> type) {
        if (!Position.isOnBoard(row, col)) {
            return false;
        }
        Piece piece = squares[row][col];
        return piece != null && piece.getColor() == color && type.isInstance(piece);
    }

    // ------------------------------------------------------------------
    // Draw detection and notation
    // ------------------------------------------------------------------

    /** True when neither side can possibly deliver checkmate. */
    public boolean hasInsufficientMaterial() {
        int minorPieces = 0;
        List<Position> bishops = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                if (piece == null || piece instanceof King) {
                    continue;
                }
                if (piece instanceof Pawn || piece instanceof Rook || piece instanceof Queen) {
                    return false;
                }
                minorPieces++;
                if (piece instanceof Bishop) {
                    bishops.add(new Position(row, col));
                }
            }
        }
        if (minorPieces <= 1) {
            return true;
        }
        // King and bishop versus king and bishop with both bishops on the same color
        return minorPieces == 2 && bishops.size() == 2
                && getPiece(bishops.get(0)).getColor() != getPiece(bishops.get(1)).getColor()
                && bishops.get(0).isDarkSquare() == bishops.get(1).isDarkSquare();
    }

    /** FEN without the move counters, used to detect repeated positions. */
    public String getPositionKey() {
        String fen = toFen();
        int cut = fen.lastIndexOf(' ', fen.lastIndexOf(' ') - 1);
        return fen.substring(0, cut);
    }

    public String toFen() {
        StringBuilder fen = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            int emptyRun = 0;
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                if (piece == null) {
                    emptyRun++;
                } else {
                    if (emptyRun > 0) {
                        fen.append(emptyRun);
                        emptyRun = 0;
                    }
                    fen.append(piece.getFenChar());
                }
            }
            if (emptyRun > 0) {
                fen.append(emptyRun);
            }
            if (row < 7) {
                fen.append('/');
            }
        }
        fen.append(sideToMove == PieceColor.WHITE ? " w " : " b ");

        String rights = (canCastle(PieceColor.WHITE, true) ? "K" : "")
                + (canCastle(PieceColor.WHITE, false) ? "Q" : "")
                + (canCastle(PieceColor.BLACK, true) ? "k" : "")
                + (canCastle(PieceColor.BLACK, false) ? "q" : "");
        fen.append(rights.isEmpty() ? "-" : rights);
        fen.append(' ').append(enPassantTarget == null ? "-" : enPassantTarget.toAlgebraic());
        fen.append(' ').append(halfmoveClock).append(' ').append(fullmoveNumber);
        return fen.toString();
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            text.append(8 - row).append(' ');
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                text.append(piece == null ? '.' : piece.getFenChar()).append(' ');
            }
            text.append('\n');
        }
        return text.append("  a b c d e f g h").toString();
    }
}
