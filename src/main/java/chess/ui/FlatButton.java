package chess.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/** A rounded, flat button that looks the same under every Look and Feel. */
public class FlatButton extends JButton {

    private final Color baseColor;
    private final Color hoverColor;
    private final Color textColor;
    private boolean hovered;

    public FlatButton(String text, Color baseColor, Color hoverColor, Color textColor) {
        super(text);
        this.baseColor = baseColor;
        this.hoverColor = hoverColor;
        this.textColor = textColor;
        setFont(Theme.HEADING_FONT.deriveFont(14f));
        setForeground(textColor);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    public static FlatButton primary(String text) {
        return new FlatButton(text, Theme.ACCENT, Theme.ACCENT_HOVER, Color.WHITE);
    }

    public static FlatButton secondary(String text) {
        return new FlatButton(text, Theme.SURFACE_RAISED, Theme.BORDER, Theme.TEXT);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        return new Dimension(metrics.stringWidth(getText()) + 32, 38);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color fill = hovered && isEnabled() ? hoverColor : baseColor;
        if (!isEnabled()) {
            fill = fill.darker();
        }
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        g.setFont(getFont());
        g.setColor(isEnabled() ? textColor : Theme.TEXT_MUTED);
        FontMetrics metrics = g.getFontMetrics();
        int x = (getWidth() - metrics.stringWidth(getText())) / 2;
        int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(getText(), x, y);
        g.dispose();
    }
}
