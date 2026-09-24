package game;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import pieces.Guard;
import pieces.Piece;
import pieces.Tower;
import benchmark.MoveGenerator;
import benchmark.Move;
import ki.AlphaBetaAI;

public class GamePanel extends JPanel implements Runnable {
    // Match board size exactly: 7 cols × 100px
    public static final int WIDTH  = Board.SQUARE_SIZE * 7;
    public static final int HEIGHT = Board.SQUARE_SIZE * 7;
    private final int FPS = 60;
    private static final boolean USE_SMART_AI = true;

    private Thread gameThread;
    private final Board board = new Board();
    private final Mouse mouse = new Mouse();

    private int lastLegalMoveCount = 0;

    // Game state
    public static java.util.ArrayList<Piece> pieces    = new java.util.ArrayList<>();
    public static java.util.ArrayList<Piece> simPieces = new java.util.ArrayList<>();
    private Piece activeP;

    // Players
    public static final int BLUE = 0;
    public static final int RED  = 1;
    private int currentColor = RED;

    // Input flags
    private boolean canMove;
    private boolean validSquare;
    private boolean gameover = false;

    // --- AI controls ---
    private boolean selfPlay = false;
    private boolean humanVsAi = false;
    private int aiColor = -1;
    private long lastMoveTime = System.currentTimeMillis();
    private static final long MOVE_DELAY_MS = 1_000; // ms between AI moves

    // --- Benchmark instrumentation ---
    private final AtomicLong totalGenTime = new AtomicLong();
    private final AtomicLong genCount      = new AtomicLong();

    public GamePanel() {
        this(null);
    }

    public GamePanel(String fen) {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        pieces.clear();
        if (fen == null || fen.isBlank()) {
            setPieces();
        } else {
            setPiecesFromFEN(fen);
        }
        copyPieces(pieces, simPieces);
    }

    // --- Getters for UI/benchmark ---
    public int getCurrentColor() { return currentColor; }
    public int getLastLegalMoveCount() { return lastLegalMoveCount; }
    public long getTotalGenTime() { return totalGenTime.get(); }
    public long getGenCount() { return genCount.get(); }

    // --- Control modes ---
    public void setSelfPlay(boolean selfPlay) { this.selfPlay = selfPlay; }
    public void setAiColor(int aiColor) {
        this.humanVsAi = true;
        this.aiColor   = aiColor;
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1_000_000_000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - lastTime) / drawInterval;
            lastTime = now;
            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        if (gameover) return;

        // 1) Move generation + timing
        long t0 = System.nanoTime();
        List<Move> legalAll = MoveGenerator.generate(pieces, currentColor);
        long t1 = System.nanoTime();
        totalGenTime.addAndGet(t1 - t0);
        genCount.incrementAndGet();
        lastLegalMoveCount = legalAll.size();

        // 2) AI vs AI
        if (selfPlay) {
            doAIMove(legalAll);
            return;
        }
        // 3) Human vs AI
        if (humanVsAi && currentColor == aiColor) {
            doAIMove(legalAll);
            return;
        }
        // 4) Human input
        if (mouse.pressed) {
            if (activeP == null) {
                pickPiece();
            } else {
                simulate();
            }
        } else if (activeP != null) {
            if (validSquare) {
                applyMove(new Move(activeP, activeP.col, activeP.row));
                if (!gameover) changePlayer();
            } else {
                copyPieces(pieces, simPieces);
                activeP.resetPosition();
            }
            activeP = null;
        }
    }

    private void doAIMove(List<Move> legalAll) {
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastMoveTime < MOVE_DELAY_MS) return;
        lastMoveTime = nowMs;

        if (legalAll.isEmpty()) {
            gameover = true;
            return;
        }

        Move m;
        if (USE_SMART_AI) {
            m = AlphaBetaAI.findBestMove(pieces, currentColor, 1000);
        } else {
            m = legalAll.get(ThreadLocalRandom.current().nextInt(legalAll.size()));
        }

        applyMove(m);
        if (!gameover) changePlayer();
    }


    private void pickPiece() {
        for (Piece p : simPieces) {
            if (p.color == currentColor
                    && p.col == mouse.x / Board.SQUARE_SIZE
                    && p.row == mouse.y / Board.SQUARE_SIZE) {
                activeP = p;
                break;
            }
        }
    }

    private void simulate() {
        canMove = false;
        validSquare = false;
        copyPieces(pieces, simPieces);
        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);
        if (activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;
            if (activeP.hittingP != null
                    && activeP.hittingP.color != activeP.color
                    && (activeP instanceof Guard || activeP.Height >= activeP.hittingP.Height)) {
                simPieces.remove(activeP.hittingP.getIndex());
            }
            validSquare = true;
        }
    }

    private void applyMove(Move move) {
        // 1) Copy current into simulation
        copyPieces(pieces, simPieces);

        // 2) Perform the move
        Piece p = move.piece;
        int prevH = p.Height;

        p.col = move.destCol;
        p.row = move.destRow;
        p.hittingP = p.getHittingP(p.col, p.row);

        // 3) Capture enemy guard/tower
        if (p.hittingP != null && p.hittingP.color != p.color) {
            simPieces.remove(p.hittingP.getIndex());
        }

        // 4) Recompute height (steps moved)
        int steps = Math.abs(p.preCol - p.col) + Math.abs(p.preRow - p.row);
        p.Height = steps;

        // 5) Split off leftover stack, if any
        if (p instanceof Tower && prevH > steps) {
            simPieces.add(new Tower(p.color, p.preCol, p.preRow, prevH - steps));
        }

        // 6) Merge friendly stacks
        if (p.hittingP != null
                && p.hittingP.color == p.color
                && p instanceof Tower
                && p.hittingP instanceof Tower) {
            p.Height += p.hittingP.Height;
            simPieces.remove(p.hittingP.getIndex());
        }

        // 7) Finalize
        p.setImage();
        copyPieces(simPieces, pieces);
        p.updatePosition();

        // 8) Check for game over
        gameover = gameIsOver();

        // 9) If the game just ended, pop up a festive dialog
        if (gameover) {
            final String winner = currentColor == BLUE ? "Blue" : "Red";
            SwingUtilities.invokeLater(() -> {
                // Find the top-level Window (your JFrame)
                Window parent = SwingUtilities.getWindowAncestor(GamePanel.this);

                // Create a modal JDialog with that Window as owner
                JDialog dialog = new JDialog(parent, "🎉 Victory! 🎉", Dialog.ModalityType.APPLICATION_MODAL);
                dialog.setUndecorated(true);
                dialog.getContentPane().setBackground(new Color(255, 239, 213)); // PapayaWhip
                dialog.setLayout(new BorderLayout(10, 10));

                JLabel trophy = new JLabel("\uD83C\uDFC6", SwingConstants.CENTER);
                trophy.setFont(new Font("SansSerif", Font.PLAIN, 72));
                dialog.add(trophy, BorderLayout.NORTH);


                // Winner message
                JLabel msg = new JLabel(winner + " wins!", SwingConstants.CENTER);
                msg.setFont(new Font("Comic Sans MS", Font.BOLD, 48));
                msg.setForeground(new Color(255, 139, 0)); // ForestGreen
                dialog.add(msg, BorderLayout.CENTER);

                // Dismiss button
                JButton ok = new JButton("Yay!");
                ok.setFont(new Font("SansSerif", Font.BOLD, 24));
                ok.setBackground(new Color(255, 238, 0)); // LightGreen
                ok.addActionListener(e -> dialog.dispose());
                JPanel btnPanel = new JPanel();
                btnPanel.setBackground(new Color(255, 239, 0));
                btnPanel.add(ok);
                dialog.add(btnPanel, BorderLayout.SOUTH);

                dialog.pack();
                dialog.setLocationRelativeTo(parent);
                dialog.setVisible(true);
            });
        }
    }



    private void changePlayer() {
        currentColor = (currentColor == BLUE) ? RED : BLUE;
        activeP = null;
    }

    private boolean gameIsOver() {
        int guards = 0;
        for (Piece p : pieces) {
            if (p instanceof Guard) {
                if (p.color == RED && p.col == 3 && p.row == 6) return true;
                if (p.color == BLUE && p.col == 3 && p.row == 0) return true;
                guards++;
            }
        }
        return guards != 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        board.draw(g2);
        for (Piece p : simPieces) p.draw(g2);
        if (activeP != null) {
            if (canMove) {
                g2.setColor(Color.white);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2.fillRect(activeP.col * Board.SQUARE_SIZE,
                        activeP.row * Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            activeP.draw(g2);
        }
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Arial", Font.PLAIN, 40));
        g2.setColor(Color.white);
        g2.drawString(
                currentColor == BLUE ? "Blue's turn" : "Red's turn",
                750,
                currentColor == BLUE ? 550 : 170
        );
        g2.setFont(new Font("Arial", Font.PLAIN, 24));
        g2.drawString("Züge am Zug: " + lastLegalMoveCount, 750, 240);
        if (gameover) {
            g2.setFont(new Font("Arial", Font.PLAIN, 50));
            g2.drawString(currentColor == BLUE ? "Blue Wins!" : "Red Wins!", 730, 350);
        }
    }

    private void setPieces() {
        // Blue
        pieces.add(new Guard(BLUE, 3, 6, 1));
        pieces.add(new Tower(BLUE, 0, 6, 1));
        pieces.add(new Tower(BLUE, 1, 6, 1));
        pieces.add(new Tower(BLUE, 2, 5, 1));
        pieces.add(new Tower(BLUE, 3, 4, 1));
        pieces.add(new Tower(BLUE, 4, 5, 1));
        pieces.add(new Tower(BLUE, 5, 6, 1));
        pieces.add(new Tower(BLUE, 6, 6, 1));
        // Red
        pieces.add(new Guard(RED, 3, 0, 1));
        pieces.add(new Tower(RED, 0, 0, 1));
        pieces.add(new Tower(RED, 1, 0, 1));
        pieces.add(new Tower(RED, 2, 1, 1));
        pieces.add(new Tower(RED, 3, 2, 1));
        pieces.add(new Tower(RED, 4, 1, 1));
        pieces.add(new Tower(RED, 5, 0, 1));
        pieces.add(new Tower(RED, 6, 0, 1));
    }

    private void setPiecesFromFEN(String fen) {
        String[] parts = fen.trim().split("\\s+");
        String placement = parts[0];
        if (parts.length == 2) {
            String s = parts[1].toLowerCase();
            if (s.equals("r")) currentColor = RED;
            else if (s.equals("b")) currentColor = BLUE;
            else throw new IllegalArgumentException("Side must be 'r' or 'b', got: " + parts[1]);
        }
        String[] ranks = placement.split("/");
        if (ranks.length != 7) throw new IllegalArgumentException("Expected 7 ranks, got: " + ranks.length);
        pieces.clear();
        for (int i = 0; i < 7; i++) {
            String rank = ranks[i];
            int col = 0;
            for (int j = 0; j < rank.length();) {
                char c = rank.charAt(j);
                if (Character.isDigit(c)) {
                    col += Character.getNumericValue(c);
                    j++;
                } else if ((c == 'R' || c == 'B') && j+1 < rank.length() && rank.charAt(j+1)=='G') {
                    int color = c=='R'?RED:BLUE;
                    pieces.add(new Guard(color, col, i, 1));
                    col++; j+=2;
                } else {
                    int color = Character.toLowerCase(c)=='r'?RED:BLUE;
                    int height = 1;
                    if (j+1 < rank.length() && Character.isDigit(rank.charAt(j+1))) {
                        height = Character.getNumericValue(rank.charAt(j+1));
                        j+=2;
                    } else { j++; }
                    pieces.add(new Tower(color, col, i, height));
                    col++;
                }
            }
            if (col != 7) throw new IllegalArgumentException("Rank " + i + " expanded to " + col + " cols (must be 7)");
        }
    }

    private void copyPieces(List<Piece> src, List<Piece> dst) {
        dst.clear();
        dst.addAll(src);
    }
}