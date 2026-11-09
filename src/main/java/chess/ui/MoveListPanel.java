package chess.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** The game score in two columns: move number, White's move, Black's move. */
public class MoveListPanel extends JComponent {

    private static final int ROW_HEIGHT = 28;

    private List<String> moves = List.of();

    public void setMoves(List<String> sanMoves) {
        this.moves = List.copyOf(sanMoves);
        revalidate();
        repaint();
        // Keep the newest move in view
        SwingUtilities.invokeLater(() -> scrollRectToVisible(new Rectangle(0, getPreferredSize().height - 1, 1, 1)));
    }

    private int rowCount() {
        return (moves.size() + 1) / 2;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(280, Math.max(ROW_HEIGHT, rowCount() * ROW_HEIGHT + 8));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.SURFACE);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (moves.isEmpty()) {
            g.setFont(Theme.BODY_FONT);
            g.setColor(Theme.TEXT_MUTED);
            g.drawString("Moves will appear here", 14, 22);
            g.dispose();
            return;
        }

        int width = getWidth();
        int numberWidth = 44;
        int columnWidth = (width - numberWidth - 8) / 2;
        g.setFont(Theme.MOVE_FONT);
        FontMetrics metrics = g.getFontMetrics();

        for (int row = 0; row < rowCount(); row++) {
            int y = 4 + row * ROW_HEIGHT;
            if (row % 2 == 1) {
                g.setColor(Theme.BACKGROUND);
                g.fillRect(0, y, width, ROW_HEIGHT);
            }
            int baseline = y + (ROW_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
            g.setColor(Theme.TEXT_MUTED);
            g.drawString((row + 1) + ".", 14, baseline);

            for (int side = 0; side < 2; side++) {
                int index = row * 2 + side;
                if (index >= moves.size()) {
                    break;
                }
                int x = numberWidth + side * columnWidth;
                boolean latest = index == moves.size() - 1;
                if (latest) {
                    g.setColor(Theme.SURFACE_RAISED);
                    g.fillRoundRect(x - 6, y + 3, metrics.stringWidth(moves.get(index)) + 12, ROW_HEIGHT - 6, 8, 8);
                }
                g.setColor(Theme.TEXT);
                g.drawString(moves.get(index), x, baseline);
            }
        }
        g.dispose();
    }
}
