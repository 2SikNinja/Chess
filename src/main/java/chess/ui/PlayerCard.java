package chess.ui;

import chess.model.PieceColor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JComponent;

/**
 * Shows one player: name, the pieces they have captured, material advantage
 * and their clock. The clock box lights up while it is that player's turn.
 */
public class PlayerCard extends JComponent {

    private static final int HEIGHT = 68;
    private static final int CAPTURED_ICON = 20;
    private static final int SAME_KIND_STEP = 8;

    private PieceColor color;
    private String name = "";
    private String clockText;
    private boolean active;
    private boolean lowTime;
    private List<String> capturedImages = List.of();
    private int advantage;

    public PlayerCard(PieceColor color) {
        this.color = color;
        setPreferredSize(new Dimension(320, HEIGHT));
        setMinimumSize(new Dimension(260, HEIGHT));
    }

    /** Updates everything the card shows. {@code clockText} is null for untimed games. */
    public void update(PieceColor color, String name, String clockText, boolean active, boolean lowTime,
                       List<String> capturedImages, int advantage) {
        this.color = color;
        this.name = name;
        this.clockText = clockText;
        this.active = active;
        this.lowTime = lowTime;
        this.capturedImages = capturedImages;
        this.advantage = advantage;
        repaint();
    }

    public PieceColor getColor() {
        return color;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();

        g.setColor(Theme.SURFACE);
        g.fillRoundRect(0, 0, width, height, 12, 12);

        // Avatar: the king of this player's color on a raised tile
        int avatar = 44;
        int avatarY = (height - avatar) / 2;
        g.setColor(Theme.SURFACE_RAISED);
        g.fillRoundRect(10, avatarY, avatar, avatar, 10, 10);
        String kingImage = color == PieceColor.WHITE ? "wking" : "bking";
        g.drawImage(PieceImages.get(kingImage, avatar - 6), 13, avatarY + 3, null);

        int clockWidth = clockText == null ? 0 : 104;
        int textX = 10 + avatar + 12;
        int textWidth = width - textX - clockWidth - 16;

        // Name
        g.setFont(Theme.HEADING_FONT);
        g.setColor(Theme.TEXT);
        FontMetrics nameMetrics = g.getFontMetrics();
        g.drawString(ellipsize(name, nameMetrics, textWidth), textX, 27);

        // Captured pieces and material advantage
        // Pieces of the same kind overlap; a small gap separates different kinds
        int iconX = textX - 2;
        int iconY = 37;
        String previous = null;
        for (String image : capturedImages) {
            if (previous != null) {
                iconX += image.equals(previous) ? SAME_KIND_STEP : CAPTURED_ICON + 1;
            }
            if (iconX + CAPTURED_ICON > textX + textWidth - 24) {
                break;
            }
            g.drawImage(PieceImages.get(image, CAPTURED_ICON), iconX, iconY, null);
            previous = image;
        }
        if (previous != null) {
            iconX += CAPTURED_ICON;
        }
        if (advantage > 0) {
            g.setFont(Theme.SMALL_FONT);
            g.setColor(Theme.TEXT_MUTED);
            g.drawString("+" + advantage, iconX + 4, iconY + 15);
        } else if (capturedImages.isEmpty()) {
            g.setFont(Theme.SMALL_FONT);
            g.setColor(Theme.TEXT_MUTED);
            g.drawString(color.getDisplayName(), textX, iconY + 15);
        }

        // Clock
        if (clockText != null) {
            int clockX = width - clockWidth - 10;
            int clockY = (height - 44) / 2;
            if (active) {
                g.setColor(lowTime ? Theme.CLOCK_LOW : Theme.CLOCK_ACTIVE);
            } else {
                g.setColor(Theme.SURFACE_RAISED);
            }
            g.fillRoundRect(clockX, clockY, clockWidth, 44, 10, 10);
            g.setFont(Theme.CLOCK_FONT);
            FontMetrics clockMetrics = g.getFontMetrics();
            if (active) {
                g.setColor(lowTime ? java.awt.Color.WHITE : Theme.CLOCK_ACTIVE_TEXT);
            } else {
                g.setColor(lowTime ? Theme.CLOCK_LOW : Theme.TEXT_MUTED);
            }
            int textLeft = clockX + (clockWidth - clockMetrics.stringWidth(clockText)) / 2;
            g.drawString(clockText, textLeft, clockY + (44 - clockMetrics.getHeight()) / 2 + clockMetrics.getAscent());
        } else if (active) {
            // Untimed games still show whose turn it is
            g.setColor(Theme.ACCENT);
            g.fillOval(width - 26, height / 2 - 6, 12, 12);
        }
        g.dispose();
    }

    private static String ellipsize(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String shortened = text;
        while (!shortened.isEmpty() && metrics.stringWidth(shortened + "...") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "...";
    }
}
