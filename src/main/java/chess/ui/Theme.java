package chess.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/** Colors, fonts and Look and Feel setup shared by every window. */
public final class Theme {

    // Window chrome
    public static final Color BACKGROUND = new Color(0x22, 0x20, 0x1e);
    public static final Color SURFACE = new Color(0x2e, 0x2b, 0x28);
    public static final Color SURFACE_RAISED = new Color(0x3a, 0x37, 0x33);
    public static final Color BORDER = new Color(0x45, 0x41, 0x3c);
    public static final Color TEXT = new Color(0xec, 0xe9, 0xe4);
    public static final Color TEXT_MUTED = new Color(0xa3, 0x9e, 0x97);
    public static final Color ACCENT = new Color(0x7f, 0xa6, 0x50);
    public static final Color ACCENT_HOVER = new Color(0x93, 0xbb, 0x62);
    public static final Color DANGER = new Color(0xc9, 0x4f, 0x4f);

    // Board
    public static final Color LIGHT_SQUARE = new Color(0xf0, 0xd9, 0xb5);
    public static final Color DARK_SQUARE = new Color(0xb5, 0x88, 0x63);
    public static final Color LAST_MOVE = new Color(0xcd, 0xd2, 0x6a, 150);
    public static final Color SELECTED = new Color(0x82, 0x97, 0x69, 200);
    public static final Color MOVE_HINT = new Color(0x14, 0x55, 0x1e, 90);
    public static final Color CHECK = new Color(0xe0, 0x30, 0x30);

    // Clocks
    public static final Color CLOCK_ACTIVE = new Color(0xec, 0xe9, 0xe4);
    public static final Color CLOCK_ACTIVE_TEXT = new Color(0x22, 0x20, 0x1e);
    public static final Color CLOCK_LOW = new Color(0xc9, 0x4f, 0x4f);

    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 30);
    public static final Font HEADING_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font CLOCK_FONT = new Font(Font.MONOSPACED, Font.BOLD, 22);
    public static final Font MOVE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

    private Theme() {
    }

    /** Applies a dark Nimbus theme so dialogs and combo boxes match the custom panels. */
    public static void install() {
        try {
            UIManager.put("control", SURFACE);
            UIManager.put("info", SURFACE_RAISED);
            UIManager.put("nimbusBase", new Color(0x1c, 0x1a, 0x18));
            UIManager.put("nimbusBlueGrey", SURFACE_RAISED);
            UIManager.put("nimbusFocus", ACCENT);
            UIManager.put("nimbusLightBackground", SURFACE_RAISED);
            UIManager.put("nimbusSelectionBackground", ACCENT);
            UIManager.put("nimbusSelectedText", Color.WHITE);
            UIManager.put("nimbusDisabledText", TEXT_MUTED);
            UIManager.put("text", TEXT);
            UIManager.put("defaultFont", BODY_FONT);
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception e) {
            // The default Look and Feel still works; the custom-painted parts keep their colors
            System.err.println("Could not apply theme: " + e.getMessage());
        }
    }
}
