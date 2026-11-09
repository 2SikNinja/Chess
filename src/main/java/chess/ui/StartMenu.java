package chess.ui;

import chess.ai.Difficulty;
import chess.model.PieceColor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** The first window: pick an opponent, a side, a difficulty and a time control. */
public class StartMenu {

    private static final String VS_COMPUTER = "Computer";
    private static final String VS_FRIEND = "Friend (same computer)";

    private record TimeOption(String label, long millis) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final TimeOption[] TIME_OPTIONS = {
        new TimeOption("Untimed", 0),
        new TimeOption("1 minute", 60_000),
        new TimeOption("3 minutes", 3 * 60_000),
        new TimeOption("5 minutes", 5 * 60_000),
        new TimeOption("10 minutes", 10 * 60_000),
        new TimeOption("15 minutes", 15 * 60_000),
        new TimeOption("30 minutes", 30 * 60_000),
        new TimeOption("1 hour", 60 * 60_000)
    };

    private final JFrame frame = new JFrame("Chess");
    private final JTextField playerName = new JTextField("Player 1", 16);
    private final JComboBox<String> opponent = new JComboBox<>(new String[] {VS_COMPUTER, VS_FRIEND});
    private final JTextField opponentName = new JTextField("Computer", 16);
    private final JComboBox<String> playAs = new JComboBox<>(new String[] {"White", "Black", "Random"});
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JComboBox<TimeOption> timeControl = new JComboBox<>(TIME_OPTIONS);

    public StartMenu() {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setIconImage(PieceImages.get("wking", 64));
        frame.setResizable(false);
        frame.setContentPane(buildContent());
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
    }

    JFrame getFrame() {
        return frame;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(28, 32, 26, 32));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);
        return root;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JLabel icon = new JLabel(new ImageIcon(PieceImages.get("wknight", 64)));
        header.add(icon, BorderLayout.WEST);

        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        JLabel title = new JLabel("Chess");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("Play a friend or a minimax AI opponent");
        subtitle.setFont(Theme.BODY_FONT);
        subtitle.setForeground(Theme.TEXT_MUTED);
        text.add(title, BorderLayout.CENTER);
        text.add(subtitle, BorderLayout.SOUTH);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        difficulty.setSelectedItem(Difficulty.HARD);
        timeControl.setSelectedIndex(4);
        opponentName.setEnabled(false);
        opponent.addActionListener(e -> onOpponentChanged());

        addRow(form, 0, "Your name", playerName);
        addRow(form, 1, "Opponent", opponent);
        addRow(form, 2, "Opponent name", opponentName);
        addRow(form, 3, "Play as", playAs);
        addRow(form, 4, "Difficulty", difficulty);
        addRow(form, 5, "Time per side", timeControl);
        return form;
    }

    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(6, 0, 6, 18);
        JLabel text = new JLabel(label);
        text.setFont(Theme.BODY_FONT);
        text.setForeground(Theme.TEXT_MUTED);
        form.add(text, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        fieldConstraints.insets = new Insets(6, 0, 6, 0);
        field.setPreferredSize(new Dimension(220, 32));
        form.add(field, fieldConstraints);
    }

    private JComponent buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        FlatButton howToPlay = FlatButton.secondary("How to Play");
        howToPlay.addActionListener(e -> showInstructions());
        FlatButton quit = FlatButton.secondary("Quit");
        quit.addActionListener(e -> frame.dispose());
        FlatButton start = FlatButton.primary("Start Game");
        start.addActionListener(e -> startGame());
        frame.getRootPane().setDefaultButton(start);

        buttons.add(howToPlay);
        buttons.add(quit);
        buttons.add(start);
        return buttons;
    }

    private void onOpponentChanged() {
        boolean vsComputer = VS_COMPUTER.equals(opponent.getSelectedItem());
        difficulty.setEnabled(vsComputer);
        opponentName.setEnabled(!vsComputer);
        opponentName.setText(vsComputer ? "Computer" : "Player 2");
    }

    private void startGame() {
        boolean vsComputer = VS_COMPUTER.equals(opponent.getSelectedItem());
        PieceColor color = switch (playAs.getSelectedIndex()) {
            case 0 -> PieceColor.WHITE;
            case 1 -> PieceColor.BLACK;
            default -> new Random().nextBoolean() ? PieceColor.WHITE : PieceColor.BLACK;
        };
        String name = playerName.getText().isBlank() ? "Player 1" : playerName.getText().trim();
        String other = opponentName.getText().isBlank() ? "Player 2" : opponentName.getText().trim();
        if (vsComputer) {
            other = "Computer";
        }
        TimeOption time = (TimeOption) timeControl.getSelectedItem();
        GameSettings settings = new GameSettings(name, other, vsComputer, color,
                (Difficulty) difficulty.getSelectedItem(), time == null ? 0 : time.millis());

        frame.dispose();
        new GameWindow(settings).show();
    }

    private void showInstructions() {
        JOptionPane.showMessageDialog(frame,
                "<html><body style='width: 300px'>"
                        + "<p><b>Moving:</b> click a piece, then click a highlighted square. "
                        + "You can also drag pieces.</p><br>"
                        + "<p><b>Hints:</b> dots show quiet moves, rings show captures. "
                        + "A red glow means your king is in check.</p><br>"
                        + "<p><b>Rules:</b> castling, en passant and promotion are all supported. "
                        + "Games end by checkmate, stalemate, resignation, time, threefold repetition, "
                        + "the fifty-move rule or insufficient material.</p><br>"
                        + "<p><b>Shortcuts:</b> Ctrl+Z undo, F flip the board, Ctrl+N new game.</p>"
                        + "</body></html>",
                "How to play", JOptionPane.PLAIN_MESSAGE);
    }
}
