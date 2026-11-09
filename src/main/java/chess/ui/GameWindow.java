package chess.ui;

import chess.ai.MinimaxAI;
import chess.model.Board;
import chess.model.ChessGame;
import chess.model.Clock;
import chess.model.GameStatus;
import chess.model.Move;
import chess.model.Piece;
import chess.model.PieceColor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * The main game screen. It owns the {@link ChessGame}, decides whose turn it
 * is, runs the AI on a background thread and keeps the side panel in sync.
 */
public class GameWindow {

    private static final long LOW_TIME_MILLIS = 20_000;
    private static final long MIN_AI_DELAY_MILLIS = 400;
    private static final char[] CAPTURE_ORDER = {'Q', 'R', 'B', 'N', 'P'};
    private static final Map<Character, Integer> STARTING_COUNTS = Map.of(
            'Q', 1, 'R', 2, 'B', 2, 'N', 2, 'P', 8);

    private final GameSettings settings;
    private final ChessGame game = new ChessGame();
    private final Map<PieceColor, Clock> clocks = new EnumMap<>(PieceColor.class);
    private final PieceColor aiColor;
    private final MinimaxAI ai;

    private final JFrame frame;
    private final BoardPanel boardPanel;
    private final PlayerCard topCard;
    private final PlayerCard bottomCard;
    private final JLabel statusLabel = new JLabel();
    private final JLabel detailLabel = new JLabel(" ");
    private final MoveListPanel moveList = new MoveListPanel();
    private final Timer refreshTimer;

    private SwingWorker<Move, Void> aiWorker;
    private int searchGeneration;
    private boolean gameOver;

    public GameWindow(GameSettings settings) {
        this.settings = settings;
        this.aiColor = settings.vsComputer() ? settings.playerColor().opposite() : null;
        this.ai = settings.vsComputer() ? new MinimaxAI(settings.difficulty().getSearchDepth()) : null;
        if (settings.isTimed()) {
            clocks.put(PieceColor.WHITE, new Clock(settings.timeMillis()));
            clocks.put(PieceColor.BLACK, new Clock(settings.timeMillis()));
        }

        boardPanel = new BoardPanel(game, this::onMoveChosen);
        boardPanel.setFlipped(settings.playerColor() == PieceColor.BLACK);
        bottomCard = new PlayerCard(settings.playerColor());
        topCard = new PlayerCard(settings.playerColor().opposite());

        frame = new JFrame(windowTitle());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setIconImage(PieceImages.get("wking", 64));
        frame.setContentPane(buildContent());
        installShortcuts();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                shutdown();
            }
        });

        refreshTimer = new Timer(100, e -> onTick());
        refreshTimer.start();

        frame.pack();
        frame.setMinimumSize(new Dimension(820, 560));
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
        startTurn();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(18, 0));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.add(boardPanel, BorderLayout.CENTER);
        root.add(buildSidePanel(), BorderLayout.EAST);
        return root;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 12));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(320, 576));

        side.add(topCard, BorderLayout.NORTH);

        // Status and move list
        JPanel middle = new JPanel(new BorderLayout(0, 8));
        middle.setOpaque(false);
        JPanel statusBox = new JPanel(new GridLayout(2, 1, 0, 2));
        statusBox.setOpaque(false);
        statusLabel.setFont(Theme.HEADING_FONT.deriveFont(17f));
        statusLabel.setForeground(Theme.TEXT);
        detailLabel.setFont(Theme.SMALL_FONT);
        detailLabel.setForeground(Theme.TEXT_MUTED);
        statusBox.add(statusLabel);
        statusBox.add(detailLabel);
        middle.add(statusBox, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(moveList);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        middle.add(scroll, BorderLayout.CENTER);
        side.add(middle, BorderLayout.CENTER);

        // Buttons and the bottom player
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        FlatButton undoButton = FlatButton.secondary("Undo");
        undoButton.setToolTipText("Take back your last move (Ctrl+Z)");
        undoButton.addActionListener(e -> undo());
        FlatButton flipButton = FlatButton.secondary("Flip Board");
        flipButton.setToolTipText("Turn the board around (F)");
        flipButton.addActionListener(e -> flipBoard());
        FlatButton resignButton = new FlatButton("Resign", Theme.SURFACE_RAISED, Theme.DANGER, Theme.TEXT);
        resignButton.addActionListener(e -> resign());
        FlatButton newGameButton = FlatButton.primary("New Game");
        newGameButton.setToolTipText("Back to the start menu (Ctrl+N)");
        newGameButton.addActionListener(e -> newGame());
        buttons.add(undoButton);
        buttons.add(flipButton);
        buttons.add(resignButton);
        buttons.add(newGameButton);
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(bottomCard, BorderLayout.SOUTH);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private void installShortcuts() {
        JComponent root = frame.getRootPane();
        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuKey), "undo", this::undo);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "flip", this::flipBoard);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, menuKey), "newGame", this::newGame);
    }

    private static void bind(JComponent component, KeyStroke key, String name, Runnable action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private String windowTitle() {
        String mode = settings.vsComputer()
                ? "vs Computer (" + settings.difficulty().getDisplayName() + ")"
                : "Two players";
        return "Chess  |  " + mode;
    }

    // ------------------------------------------------------------------
    // Turn flow
    // ------------------------------------------------------------------

    private void startTurn() {
        refreshView();
        GameStatus status = game.getStatus();
        if (status.isGameOver()) {
            if (status == GameStatus.CHECKMATE) {
                PieceColor winner = game.getWinner();
                finishGame(settings.nameFor(winner) + " wins!", "by checkmate");
            } else {
                finishGame("Draw", status.getDescription().replace("Draw by ", "by "));
            }
            return;
        }

        PieceColor toMove = game.getSideToMove();
        if (settings.isTimed()) {
            clocks.get(toMove.opposite()).stop();
            clocks.get(toMove).start();
        }
        if (toMove == aiColor) {
            requestAiMove();
        } else {
            boardPanel.setInteractiveColors(EnumSet.of(toMove));
        }
    }

    private void onMoveChosen(List<Move> candidates) {
        if (gameOver || candidates.isEmpty() || game.getSideToMove() == aiColor) {
            return;
        }
        Move move = candidates.size() == 1 ? candidates.get(0) : askForPromotion(candidates);
        if (move != null) {
            playMove(move);
        }
    }

    /** Plays a move for whoever is on turn. Package-private so screenshots can script games. */
    void playMove(Move move) {
        detailLabel.setText(" ");
        game.play(move);
        startTurn();
    }

    private Move askForPromotion(List<Move> candidates) {
        Icon[] icons = new Icon[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            icons[i] = new ImageIcon(PieceImages.get(candidates.get(i).promotion(), 56));
        }
        int choice = JOptionPane.showOptionDialog(frame, "Promote your pawn to:", "Promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, icons, icons[0]);
        return choice < 0 ? null : candidates.get(choice);
    }

    private void requestAiMove() {
        boardPanel.setInteractiveColors(EnumSet.noneOf(PieceColor.class));
        statusLabel.setText("Computer is thinking...");
        final int generation = ++searchGeneration;
        final Board snapshot = game.getBoard().copy();

        aiWorker = new SwingWorker<>() {
            private long elapsedMillis;

            @Override
            protected Move doInBackground() throws InterruptedException {
                long start = System.currentTimeMillis();
                Move move = ai.findBestMove(snapshot);
                elapsedMillis = System.currentTimeMillis() - start;
                // A tiny pause keeps instant replies from feeling jarring
                long remaining = MIN_AI_DELAY_MILLIS - elapsedMillis;
                if (remaining > 0) {
                    Thread.sleep(remaining);
                }
                return move;
            }

            @Override
            protected void done() {
                if (generation != searchGeneration || gameOver || isCancelled()) {
                    return;
                }
                try {
                    Move move = get();
                    if (move != null) {
                        playMove(move);
                        String time = elapsedMillis < 1000
                                ? elapsedMillis + " ms"
                                : String.format("%.1f s", elapsedMillis / 1000.0);
                        detailLabel.setText(String.format("Computer searched %,d positions in %s",
                                ai.getNodesSearched(), time));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    statusLabel.setText("The computer hit an error: " + e.getCause().getMessage());
                }
            }
        };
        aiWorker.execute();
    }

    private void cancelSearch() {
        searchGeneration++;
        if (ai != null) {
            ai.cancel();
        }
        if (aiWorker != null) {
            aiWorker.cancel(true);
            aiWorker = null;
        }
    }

    private void onTick() {
        if (!gameOver && settings.isTimed()) {
            PieceColor toMove = game.getSideToMove();
            if (clocks.get(toMove).isOutOfTime()) {
                PieceColor winner = toMove.opposite();
                finishGame(settings.nameFor(winner) + " wins!", "on time");
                return;
            }
        }
        refreshCards();
    }

    private void finishGame(String title, String subtitle) {
        gameOver = true;
        cancelSearch();
        clocks.values().forEach(Clock::stop);
        boardPanel.setInteractiveColors(EnumSet.noneOf(PieceColor.class));
        boardPanel.showOverlay(title, subtitle);
        statusLabel.setText(title);
        detailLabel.setText(capitalize(subtitle) + ". Press New Game to play again.");
        refreshCards();
    }

    // ------------------------------------------------------------------
    // Buttons
    // ------------------------------------------------------------------

    private void undo() {
        cancelSearch();
        int plies = 1;
        if (settings.vsComputer() && game.getSideToMove() != aiColor) {
            // Take back the computer's reply and the player's move before it
            plies = 2;
        }
        boolean undone = false;
        for (int i = 0; i < plies && game.getPlyCount() > 0; i++) {
            undone |= game.undo();
        }
        if (!undone && !gameOver) {
            startTurn();
            return;
        }
        gameOver = false;
        boardPanel.clearOverlay();
        boardPanel.clearSelection();
        detailLabel.setText(" ");
        startTurn();
    }

    private void flipBoard() {
        boardPanel.setFlipped(!boardPanel.isFlipped());
        refreshCards();
    }

    private void resign() {
        if (gameOver) {
            return;
        }
        PieceColor resigning = settings.vsComputer() ? settings.playerColor() : game.getSideToMove();
        int answer = JOptionPane.showConfirmDialog(frame,
                settings.nameFor(resigning) + ", are you sure you want to resign?",
                "Resign", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            finishGame(settings.nameFor(resigning.opposite()) + " wins!", "by resignation");
        }
    }

    private void newGame() {
        if (!gameOver && game.getPlyCount() > 0) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    "Leave this game and start a new one?", "New game", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        frame.dispose();
        new StartMenu().show();
    }

    private void shutdown() {
        refreshTimer.stop();
        cancelSearch();
        clocks.values().forEach(Clock::stop);
    }

    // ------------------------------------------------------------------
    // View updates
    // ------------------------------------------------------------------

    private void refreshView() {
        moveList.setMoves(game.getSanMoves());
        PieceColor toMove = game.getSideToMove();
        String name = settings.nameFor(toMove);
        String turn = toMove == aiColor ? "Computer to move" : name + " to move";
        statusLabel.setText(game.isInCheck() ? "Check!  " + turn : turn);
        refreshCards();
        boardPanel.repaint();
    }

    private void refreshCards() {
        PieceColor bottomColor = boardPanel.isFlipped() ? PieceColor.BLACK : PieceColor.WHITE;
        updateCard(bottomCard, bottomColor);
        updateCard(topCard, bottomColor.opposite());
    }

    private void updateCard(PlayerCard card, PieceColor color) {
        Clock clock = clocks.get(color);
        String clockText = clock == null ? null : clock.format();
        boolean lowTime = clock != null && clock.getRemainingMillis() < LOW_TIME_MILLIS;
        boolean active = !gameOver && game.getSideToMove() == color;
        card.update(color, settings.nameFor(color), clockText, active, lowTime,
                capturedBy(color), materialAdvantage(color));
    }

    /** Image names of the opponent's pieces that {@code color} has captured. */
    private List<String> capturedBy(PieceColor color) {
        PieceColor victim = color.opposite();
        Map<Character, Integer> onBoard = new java.util.HashMap<>();
        Board board = game.getBoard();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == victim) {
                    onBoard.merge(piece.getSymbol(), 1, Integer::sum);
                }
            }
        }
        List<String> captured = new ArrayList<>();
        String prefix = victim == PieceColor.WHITE ? "w" : "b";
        for (char symbol : CAPTURE_ORDER) {
            int missing = STARTING_COUNTS.get(symbol) - onBoard.getOrDefault(symbol, 0);
            String imageName = prefix + Piece.fromSymbol(symbol, victim).getName().toLowerCase();
            for (int i = 0; i < missing; i++) {
                captured.add(imageName);
            }
        }
        return captured;
    }

    /** How many pawns' worth of material {@code color} is ahead, or 0. */
    private int materialAdvantage(PieceColor color) {
        int balance = 0;
        Board board = game.getBoard();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null) {
                    balance += piece.getColor() == color ? piece.getValue() : -piece.getValue();
                }
            }
        }
        return Math.max(0, Math.round(balance / 100f));
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // Package-private accessors for scripted demos and screenshots
    ChessGame getGame() {
        return game;
    }

    BoardPanel getBoardPanel() {
        return boardPanel;
    }

    JFrame getFrame() {
        return frame;
    }
}
