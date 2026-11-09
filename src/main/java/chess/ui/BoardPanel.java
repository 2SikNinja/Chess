package chess.ui;

import chess.model.Board;
import chess.model.ChessGame;
import chess.model.Move;
import chess.model.Piece;
import chess.model.PieceColor;
import chess.model.Position;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;

/**
 * Draws the board and lets the user move pieces by clicking a piece and then a
 * square, or by dragging. It never changes the game itself: chosen moves are
 * passed to a {@link MoveHandler}.
 */
public class BoardPanel extends JPanel {

    /** Receives the legal moves matching what the user picked (several for a promotion). */
    public interface MoveHandler {
        void onMoveChosen(List<Move> candidates);
    }

    private static final int DEFAULT_SQUARE_SIZE = 72;

    private final ChessGame game;
    private final MoveHandler moveHandler;
    private Set<PieceColor> interactiveColors = EnumSet.noneOf(PieceColor.class);
    private boolean flipped;

    private Position selected;
    private List<Move> selectedMoves = List.of();
    private Position dragFrom;
    private Point dragPoint;

    private String overlayTitle;
    private String overlaySubtitle;

    public BoardPanel(ChessGame game, MoveHandler moveHandler) {
        this.game = game;
        this.moveHandler = moveHandler;
        setBackground(Theme.BACKGROUND);
        setPreferredSize(new Dimension(8 * DEFAULT_SQUARE_SIZE, 8 * DEFAULT_SQUARE_SIZE));
        setMinimumSize(new Dimension(8 * 40, 8 * 40));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePress(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragFrom != null) {
                    dragPoint = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleRelease(e.getPoint());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // ------------------------------------------------------------------
    // State set by the game window
    // ------------------------------------------------------------------

    /** Which colors the user may currently move. Empty while the AI thinks or after the game ends. */
    public void setInteractiveColors(Set<PieceColor> colors) {
        interactiveColors = colors.isEmpty() ? EnumSet.noneOf(PieceColor.class) : EnumSet.copyOf(colors);
        if (selected != null) {
            Piece piece = game.getBoard().getPiece(selected);
            if (piece == null || !interactiveColors.contains(piece.getColor())) {
                clearSelection();
            }
        }
        repaint();
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void showOverlay(String title, String subtitle) {
        overlayTitle = title;
        overlaySubtitle = subtitle;
        repaint();
    }

    public void clearOverlay() {
        overlayTitle = null;
        overlaySubtitle = null;
        repaint();
    }

    /** Selects a square as if the user had clicked it (used for screenshots and tests). */
    public void select(Position position) {
        selected = position;
        selectedMoves = game.getLegalMoves(position);
        repaint();
    }

    public void clearSelection() {
        selected = null;
        selectedMoves = List.of();
        dragFrom = null;
        dragPoint = null;
        repaint();
    }

    // ------------------------------------------------------------------
    // Mouse handling
    // ------------------------------------------------------------------

    private void handlePress(Point point) {
        if (overlayTitle != null) {
            clearOverlay();
            return;
        }
        Position clicked = positionAt(point);
        if (clicked == null) {
            clearSelection();
            return;
        }
        if (selected != null && isTarget(clicked)) {
            commit(clicked);
            return;
        }
        Piece piece = game.getBoard().getPiece(clicked);
        if (piece != null && piece.getColor() == game.getSideToMove()
                && interactiveColors.contains(piece.getColor())) {
            select(clicked);
            dragFrom = clicked;
            dragPoint = point;
        } else {
            clearSelection();
        }
        repaint();
    }

    private void handleRelease(Point point) {
        if (dragFrom == null) {
            return;
        }
        Position from = dragFrom;
        Position released = positionAt(point);
        dragFrom = null;
        dragPoint = null;
        if (released != null && !released.equals(from) && isTarget(released)) {
            commit(released);
        }
        repaint();
    }

    private boolean isTarget(Position position) {
        for (Move move : selectedMoves) {
            if (move.to().equals(position)) {
                return true;
            }
        }
        return false;
    }

    private void commit(Position target) {
        List<Move> candidates = new ArrayList<>();
        for (Move move : selectedMoves) {
            if (move.to().equals(target)) {
                candidates.add(move);
            }
        }
        clearSelection();
        moveHandler.onMoveChosen(candidates);
    }

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------

    private int squareSize() {
        return Math.max(20, Math.min(getWidth(), getHeight()) / 8);
    }

    private int originX() {
        return (getWidth() - 8 * squareSize()) / 2;
    }

    private int originY() {
        return (getHeight() - 8 * squareSize()) / 2;
    }

    private Position positionAt(Point point) {
        int size = squareSize();
        int x = point.x - originX();
        int y = point.y - originY();
        if (x < 0 || y < 0 || x >= 8 * size || y >= 8 * size) {
            return null;
        }
        int displayRow = y / size;
        int displayCol = x / size;
        return flipped ? new Position(7 - displayRow, 7 - displayCol) : new Position(displayRow, displayCol);
    }

    private Point topLeftOf(Position position) {
        int size = squareSize();
        int displayRow = flipped ? 7 - position.row() : position.row();
        int displayCol = flipped ? 7 - position.col() : position.col();
        return new Point(originX() + displayCol * size, originY() + displayRow * size);
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Board board = game.getBoard();
        int size = squareSize();

        paintSquares(g, size);
        paintHighlights(g, board, size);
        paintPieces(g, board, size);
        paintMoveHints(g, board, size);
        paintCoordinates(g, size);
        paintDraggedPiece(g, board, size);
        paintOverlay(g, size);
        g.dispose();
    }

    private void paintSquares(Graphics2D g, int size) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position position = new Position(row, col);
                Point corner = topLeftOf(position);
                g.setColor(position.isDarkSquare() ? Theme.DARK_SQUARE : Theme.LIGHT_SQUARE);
                g.fillRect(corner.x, corner.y, size, size);
            }
        }
    }

    private void paintHighlights(Graphics2D g, Board board, int size) {
        Move lastMove = game.getLastMove();
        if (lastMove != null) {
            fillSquare(g, lastMove.from(), Theme.LAST_MOVE, size);
            fillSquare(g, lastMove.to(), Theme.LAST_MOVE, size);
        }
        if (selected != null) {
            fillSquare(g, selected, Theme.SELECTED, size);
        }
        if (game.isInCheck()) {
            Position king = board.getKingPosition(game.getSideToMove());
            Point corner = topLeftOf(king);
            float radius = size * 0.72f;
            g.setPaint(new RadialGradientPaint(
                    corner.x + size / 2f, corner.y + size / 2f, radius,
                    new float[] {0f, 0.45f, 1f},
                    new Color[] {Theme.CHECK, new Color(0xe0, 0x30, 0x30, 170), new Color(0xe0, 0x30, 0x30, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g.fillRect(corner.x, corner.y, size, size);
        }
    }

    private void fillSquare(Graphics2D g, Position position, Color color, int size) {
        Point corner = topLeftOf(position);
        g.setColor(color);
        g.fillRect(corner.x, corner.y, size, size);
    }

    private void paintPieces(Graphics2D g, Board board, int size) {
        int imageSize = Math.round(size * 0.88f);
        int inset = (size - imageSize) / 2;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position position = new Position(row, col);
                Piece piece = board.getPiece(position);
                if (piece == null || (dragPoint != null && position.equals(dragFrom))) {
                    continue;
                }
                Point corner = topLeftOf(position);
                g.drawImage(PieceImages.get(piece, imageSize), corner.x + inset, corner.y + inset, null);
            }
        }
    }

    private void paintMoveHints(Graphics2D g, Board board, int size) {
        g.setColor(Theme.MOVE_HINT);
        for (Move move : selectedMoves) {
            Point corner = topLeftOf(move.to());
            if (move.isCapture()) {
                // A ring around pieces that can be captured
                float stroke = size * 0.09f;
                g.setStroke(new BasicStroke(stroke));
                float inset = stroke / 2 + 1;
                g.drawOval(Math.round(corner.x + inset), Math.round(corner.y + inset),
                        Math.round(size - 2 * inset), Math.round(size - 2 * inset));
            } else {
                int dot = Math.round(size * 0.3f);
                g.fillOval(corner.x + (size - dot) / 2, corner.y + (size - dot) / 2, dot, dot);
            }
        }
    }

    private void paintCoordinates(Graphics2D g, int size) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, size / 6)));
        FontMetrics metrics = g.getFontMetrics();
        int pad = Math.max(3, size / 18);
        for (int i = 0; i < 8; i++) {
            // Rank numbers down the left edge
            Position leftSquare = flipped ? new Position(7 - i, 7) : new Position(i, 0);
            Point left = topLeftOf(leftSquare);
            g.setColor(leftSquare.isDarkSquare() ? Theme.LIGHT_SQUARE : Theme.DARK_SQUARE);
            g.drawString(String.valueOf(leftSquare.rankChar()), left.x + pad, left.y + pad + metrics.getAscent());

            // File letters along the bottom edge
            Position bottomSquare = flipped ? new Position(0, 7 - i) : new Position(7, i);
            Point bottom = topLeftOf(bottomSquare);
            String file = String.valueOf(bottomSquare.fileChar());
            g.setColor(bottomSquare.isDarkSquare() ? Theme.LIGHT_SQUARE : Theme.DARK_SQUARE);
            g.drawString(file, bottom.x + size - pad - metrics.stringWidth(file), bottom.y + size - pad - metrics.getDescent());
        }
    }

    private void paintDraggedPiece(Graphics2D g, Board board, int size) {
        if (dragFrom == null || dragPoint == null) {
            return;
        }
        Piece piece = board.getPiece(dragFrom);
        if (piece == null) {
            return;
        }
        int imageSize = Math.round(size * 1.0f);
        g.drawImage(PieceImages.get(piece, imageSize), dragPoint.x - imageSize / 2, dragPoint.y - imageSize / 2, null);
    }

    private void paintOverlay(Graphics2D g, int size) {
        if (overlayTitle == null) {
            return;
        }
        int x = originX();
        int y = originY();
        int boardSize = 8 * size;
        g.setColor(new Color(0, 0, 0, 165));
        g.fillRect(x, y, boardSize, boardSize);

        int centerY = y + boardSize / 2;
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(22, size / 2)));
        drawCentered(g, overlayTitle, x, boardSize, centerY - size / 6);

        g.setColor(Theme.LIGHT_SQUARE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(14, size / 4)));
        drawCentered(g, overlaySubtitle, x, boardSize, centerY + size / 3);

        g.setColor(Theme.TEXT_MUTED);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(11, size / 6)));
        drawCentered(g, "Click the board to review the final position", x, boardSize, centerY + size);
    }

    private void drawCentered(Graphics2D g, String text, int left, int width, int baseline) {
        if (text == null) {
            return;
        }
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, left + (width - metrics.stringWidth(text)) / 2, baseline);
    }
}
