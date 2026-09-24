package main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import game.GamePanel;
import javax.swing.*;
import benchmark.Move;
import game.Board;
import ki.AlphaBetaAI;
import pieces.Guard;
import pieces.Piece;
import pieces.Tower;
import game.BoardParser;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            // turn off all image‐loading
            Piece.skipLoadingImages = true;

            // parse "boardFEN turn"
            String[] parts    = args[0].trim().split("\\s+");
            String   boardFEN = parts[0];
            char     turn     = (parts.length > 1 ? parts[1].charAt(0) : 'r');

            // build piece list
            List<Piece> pieces = BoardParser.fromString(boardFEN);

            // decide AI color
            int color = (turn == 'r') ? GamePanel.RED : GamePanel.BLUE;

            // run your search with a 1-second budget
            Move bestMove = AlphaBetaAI.findBestMove(pieces, color, 1000L);

            // emit exactly one move string (e.g. "A7-B7-1") and exit
            System.out.println(bestMove.toString());
            return;
        }
        // ─── 1) Scaled Splash Screen ───────────────────────────────────
        BufferedImage rawSplash = ImageIO.read(new File("res/piece/splash.png"));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = screen.width / 2;
        int maxH = screen.height / 2;
        float scale = Math.min(
                (float) maxW / rawSplash.getWidth(),
                (float) maxH / rawSplash.getHeight()
        );
        Image splashImg = rawSplash.getScaledInstance(
                Math.round(rawSplash.getWidth() * scale),
                Math.round(rawSplash.getHeight() * scale),
                Image.SCALE_SMOOTH
        );

        JWindow splash = new JWindow();
        splash.getContentPane().add(new JLabel(new ImageIcon(splashImg)));
        splash.pack();
        splash.setLocation(
                (screen.width - splash.getWidth()) / 2,
                (screen.height - splash.getHeight()) / 2
        );
        splash.setVisible(true);
        try { Thread.sleep(5_000); } catch (InterruptedException ignored) {}
        splash.dispose();

        AtomicInteger choice = new AtomicInteger(-1);

        JFrame modeFrame = new JFrame("Select Mode");
        modeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        modeFrame.setSize(400, 350);
        modeFrame.setLocationRelativeTo(null);

        JPanel modePanel = new JPanel();
        modePanel.setLayout(new GridLayout(5, 1, 15, 15));
        modePanel.setBackground(new Color(30, 30, 30));
        modeFrame.add(modePanel);

        JLabel modeLabel = new JLabel("Select mode:", SwingConstants.CENTER);
        modeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        modeLabel.setForeground(Color.WHITE);
        modePanel.add(modeLabel);

        JButton runBenchmarks = new JButton("Run Benchmarks");
        JButton aiVsAi1 = new JButton("Computer vs Computer");
        JButton humanVsHuman = new JButton("Human vs Human");
        JButton humanVsComputer = new JButton("Human vs Computer");

        JButton[] buttons = {runBenchmarks, aiVsAi1, humanVsHuman, humanVsComputer};
        Color buttonBackground = new Color(50, 50, 50);
        Color buttonForeground = Color.WHITE;
        Font buttonFont = new Font("Arial", Font.PLAIN, 18);

        for (int i = 0; i < buttons.length; i++) {
            JButton btn = buttons[i];
            btn.setBackground(buttonBackground);
            btn.setForeground(buttonForeground);
            btn.setFocusPainted(false);
            btn.setFont(buttonFont);
            modePanel.add(btn);

            final int index = i;
            btn.addActionListener(e -> { choice.set(index); modeFrame.dispose(); });
        }

        modeFrame.setVisible(true);

        while (modeFrame.isVisible()) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        // ─── 2) Handle Benchmark ─────────────────────────────────
       /*if (choice.get() == 0) {
            Benchmark.benchmarkEvaluationFunction();

            System.exit(0);
        }*/

        boolean aiVsAi = (choice.get() == 1);
        boolean humanVsAi = (choice.get() == 3);
        int aiColor = -1;

        // ─── 3) AI Side Selection (if needed) ─────────────────────────────────
        if (humanVsAi) {
            AtomicInteger sideChoice = new AtomicInteger(-1);

            JFrame sideFrame = new JFrame("Select AI Side");
            sideFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            sideFrame.setSize(400, 250);
            sideFrame.setLocationRelativeTo(null);

            JPanel sidePanel = new JPanel();
            sidePanel.setLayout(new GridLayout(3, 1, 15, 15));
            sidePanel.setBackground(new Color(30, 30, 30));
            sideFrame.add(sidePanel);

            JLabel sideLabel = new JLabel("Which side should the computer play?", SwingConstants.CENTER);
            sideLabel.setFont(new Font("Arial", Font.BOLD, 20));
            sideLabel.setForeground(Color.WHITE);
            sidePanel.add(sideLabel);

            JButton redBtn = new JButton("Red");
            JButton blueBtn = new JButton("Blue");

            JButton[] sideButtons = {redBtn, blueBtn};

            for (int i = 0; i < sideButtons.length; i++) {
                JButton btn = sideButtons[i];
                btn.setBackground(new Color(50, 50, 50));
                btn.setForeground(Color.WHITE);
                btn.setFont(new Font("Arial", Font.PLAIN, 18));
                btn.setFocusPainted(false);
                sidePanel.add(btn);

                final int index = i;
                btn.addActionListener(e -> { sideChoice.set(index); sideFrame.dispose(); });
            }

            sideFrame.setVisible(true);

            while (sideFrame.isVisible()) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }

            aiColor = (sideChoice.get() == 0) ? GamePanel.RED : GamePanel.BLUE;
        }

        // ─── 4) FEN Input ─────────────────────────────────
        AtomicInteger fenPressed = new AtomicInteger(0);
        final String[] fenHolder = {null};

        JFrame fenFrame = new JFrame("Enter FEN");
        fenFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        fenFrame.setSize(500, 200);
        fenFrame.setLocationRelativeTo(null);

        JPanel fenPanel = new JPanel();
        fenPanel.setLayout(new BoxLayout(fenPanel, BoxLayout.Y_AXIS));
        fenPanel.setBackground(new Color(30, 30, 30));
        fenPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        fenFrame.add(fenPanel);

        JLabel fenLabel = new JLabel("Enter FEN (or leave empty for standard start):");
        fenLabel.setFont(new Font("Arial", Font.BOLD, 18));
        fenLabel.setForeground(Color.WHITE);
        fenLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fenPanel.add(fenLabel);

        fenPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JTextField fenField = new JTextField();
        fenField.setMaximumSize(new Dimension(400, 30));
        fenField.setFont(new Font("Arial", Font.PLAIN, 16));
        fenField.setBackground(new Color(50, 50, 50));
        fenField.setForeground(Color.WHITE);
        fenField.setCaretColor(Color.WHITE);
        fenField.setAlignmentX(Component.CENTER_ALIGNMENT);
        fenPanel.add(fenField);

        fenPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton fenOk = new JButton("OK");
        fenOk.setFont(new Font("Arial", Font.BOLD, 16));
        fenOk.setBackground(new Color(70, 130, 180));
        fenOk.setForeground(Color.WHITE);
        fenOk.setFocusPainted(false);
        fenOk.setAlignmentX(Component.CENTER_ALIGNMENT);
        fenPanel.add(fenOk);

        fenOk.addActionListener(e -> {
            fenHolder[0] = fenField.getText().trim();
            fenPressed.set(1);
            fenFrame.dispose();
        });

        fenFrame.setVisible(true);

        while (fenFrame.isVisible()) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        String fen = (fenHolder[0] != null && !fenHolder[0].isEmpty()) ? fenHolder[0] : null;

        // ─── 5) Ready: Continue starting your game ───────────────────────
        System.out.println("Selected FEN: " + fen);
        System.out.println("AI vs AI: " + aiVsAi);
        System.out.println("Human vs AI: " + humanVsAi);
        System.out.println("AI Color: " + aiColor);

        // ─── 3) Main Window & Panels ────────────────────────────────────
        JFrame window = new JFrame("Guard and Towers");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gp = new GamePanel(fen);
        gp.setSelfPlay(aiVsAi);
        if (humanVsAi) gp.setAiColor(aiColor);

        // Info-side panel
        BufferedImage infoBg = ImageIO.read(new File("res/piece/info-background.jpg"));
        JPanel infoPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(infoBg, 0, 0, getWidth(), getHeight(), null);
            }
        };
        infoPanel.setPreferredSize(new Dimension(250, GamePanel.HEIGHT));
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        infoPanel.add(Box.createVerticalStrut(20));
        JLabel title = new JLabel("Guard and Towers");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setOpaque(false);
        infoPanel.add(title);

        infoPanel.add(Box.createVerticalStrut(30));
        JLabel turnLabel = new JLabel(" ");
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(turnLabel.getFont().deriveFont(18f));
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        turnLabel.setOpaque(false);
        infoPanel.add(turnLabel);

        infoPanel.add(Box.createVerticalStrut(20));
        JLabel countLabel = new JLabel(" ");
        countLabel.setForeground(Color.WHITE);
        countLabel.setFont(countLabel.getFont().deriveFont(16f));
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        countLabel.setOpaque(false);
        infoPanel.add(countLabel);

        infoPanel.add(Box.createVerticalStrut(20));
        JLabel avgTimeLabel = new JLabel("Avg Gen Time: 0.00 ms");
        avgTimeLabel.setForeground(Color.WHITE);
        avgTimeLabel.setFont(avgTimeLabel.getFont().deriveFont(16f));
        avgTimeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avgTimeLabel.setOpaque(false);
        infoPanel.add(avgTimeLabel);

        window.setLayout(new BorderLayout());
        window.add(gp, BorderLayout.CENTER);
        window.add(infoPanel, BorderLayout.EAST);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gp.launchGame();

        // ─── 4) Periodic UI Update & Benchmark Timer ───────────────────
        final int TARGET = 10_000;
        AtomicInteger measured = new AtomicInteger(0);
        Timer timer = new Timer(200, (ActionEvent e) -> {
            // Update turn and move count
            turnLabel.setText(
                    gp.getCurrentColor() == GamePanel.BLUE ? "Blue's turn" : "Red's turn"
            );
            countLabel.setText("Legal moves: " + gp.getLastLegalMoveCount());

            // Update average generation time until TARGET
            if (measured.get() < TARGET && gp.getGenCount() > 0) {
                long avgNs = gp.getTotalGenTime() / gp.getGenCount();
                double avgMs = avgNs / 1_000_000.0;
                avgTimeLabel.setText(String.format("Avg Gen Time: %.2f ms", avgMs));

                if (measured.incrementAndGet() == TARGET) {
                    ((Timer) e.getSource()).stop();
                    System.out.printf(
                            "Measured %d generations, avg=%.2f ms%n",
                            TARGET,
                            avgMs
                    );
                }
            }
        });
        timer.start();
    }
}