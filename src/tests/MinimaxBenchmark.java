package tests;

import java.util.List;
import main.GamePanel;
import main.Move;
import main.MoveGenerator;
import pieces.Piece;
import main.AlphaBetaAI;

public class MinimaxBenchmark {
    private static final int INF = Integer.MAX_VALUE;
    private static final long MAX_TIME_MS = 120_000;  // 2 Minuten
    private static long nodeCount;                   // Zähler für alle besuchten Knoten

    public static void main(String[] args) {
        // keine Bild-Initialisierung
        Piece.skipLoadingImages = true;

        // 1) Deine Test-Stellungen (FEN + Seite) – jede Rank muss 7 Felder haben!
        String[] testFens = {
                "7/2r1RG3/2r1r13/7/b12BG2b1/1b12b12/4b12 b"
                // … weitere korrekt formattierte FENs
        };

        for (String fenString : testFens) {
            // splitte "FEN Seite"
            String[] parts = fenString.split(" ");
            if (parts.length != 2) {
                System.err.println("Ungültige FEN-Angabe: " + fenString);
                continue;
            }
            String fen  = parts[0];
            int color   = parts[1].equalsIgnoreCase("r")
                    ? GamePanel.RED
                    : GamePanel.BLUE;

            System.out.printf("== Position: %s, Spieler: %s ==%n",
                    fen,
                    (color == GamePanel.RED ? "ROT" : "BLAU"));

            runMinimaxTests(fen, color, /*maxDepth=*/6);
            System.out.println();
        }
    }

    private static void runMinimaxTests(String fen, int color, int maxDepth) {
        // a) Ausgangsstellung aufbauen
        GamePanel gp = new GamePanel(fen);
        List<Piece> rootBoard = List.copyOf(GamePanel.pieces);

        // b) Tiefenschleife
        for (int depth = 1; depth <= maxDepth; depth++) {
            nodeCount = 0;
            long start = System.nanoTime();

            // c) Root: alle Züge generieren und Minimax aufrufen
            Move bestMove  = null;
            int  bestScore = -INF;
            List<Move> moves = MoveGenerator.generate(rootBoard, color);


            for (Move m : moves) {
                // 1) simulieren
                List<Piece> next = AlphaBetaAI.simulateMove(rootBoard, m);
                // 2) minimax ohne Cutoffs aufrufen
                int score = minimax(next, depth - 1, /*maximizing=*/false, color);
                // 3) besten Zug merken
                if (score > bestScore) {
                    bestScore = score;
                    bestMove  = m;
                }
            }

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            System.out.printf(
                    "Tiefe %d → Zeit: %4d ms, Zustände: %6d, bester Zug: %s (Score=%d)%n",
                    depth, elapsedMs, nodeCount, bestMove, bestScore
            );

            // d) Abbruch, falls länger als 2 Minuten
            if (elapsedMs > MAX_TIME_MS) {
                System.out.println("  [Abbruch – 2 Minuten überschritten]");
                break;
            }
        }
    }

    /**
     * Reines Minimax (kein Alpha-Beta), zählt jeden Aufruf als einen Knoten.
     */
    private static int minimax(
            List<Piece> board,
            int depth,
            boolean maximizing,
            int playerColor
    ) {
        nodeCount++;  // jeder Aufruf ist ein besuchter Knoten

        // Blatt oder Terminal?
        if (depth == 0 || AlphaBetaAI.isTerminal(board)) {
            return AlphaBetaAI.evaluate(board, playerColor);
        }

        int currentColor = maximizing ? playerColor : 1 - playerColor;
        List<Move> moves = MoveGenerator.generate(board, currentColor);

        if (maximizing) {
            int maxEval = -INF;
            for (Move m : moves) {
                List<Piece> next = AlphaBetaAI.simulateMove(board, m);
                int eval = minimax(next, depth - 1, false, playerColor);
                if (eval > maxEval) maxEval = eval;
            }
            return maxEval;
        } else {
            int minEval = INF;
            for (Move m : moves) {
                List<Piece> next = AlphaBetaAI.simulateMove(board, m);
                int eval = minimax(next, depth - 1, true, playerColor);
                if (eval < minEval) minEval = eval;
            }
            return minEval;
        }
    }
}
