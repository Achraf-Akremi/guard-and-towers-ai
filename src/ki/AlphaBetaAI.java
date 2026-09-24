package ki;

import java.util.*;
import pieces.Guard;
import pieces.Piece;
import pieces.Tower;
import benchmark.MoveGenerator;
import benchmark.Move;
import game.GamePanel;
import ki.Zobrist;
import ki.TranspositionTable;
import java.util.stream.Collectors;

/**
 * Alpha-Beta search with:
 *  - Iterative Deepening
 *  - Immediate guard-capture check
 *  - Leaf-count statistics
 *  - Zobrist hashing + Transposition Table
 *  - Null-Move Pruning
 *  - Zugsortierung (move ordering: killer moves, history heuristic, capture sorting)
 */
public class AlphaBetaAI {
    private static final boolean DEBUG = false;
    private static final int INF = Integer.MAX_VALUE;
    private static final int BOARD_SIZE = 7;

    private static final int MAX_DEPTH = 64;
    private static final Move[][] killerMoves = new Move[MAX_DEPTH][2];
    private static final int[][] historyHeuristic = new int[BOARD_SIZE * BOARD_SIZE][BOARD_SIZE * BOARD_SIZE];

    private static final Map<Integer, Integer> nullMoveCounts = new HashMap<>();

    private static final TranspositionTable tt = new TranspositionTable(20);

    public static Move findBestMove(List<Piece> pieces, int color, long timeLimitMs) {
    long endTime = System.currentTimeMillis() + timeLimitMs;
    Move bestMove  = null;
    int  bestScore = -INF;
    int  depth     = 1;

    List<Long> leafCounts = new ArrayList<>();
    List<Long> depthTimes = new ArrayList<>();  // NEW: to store timing per depth

    long rootZKey = Zobrist.computeHash(pieces);

    for (int d = 0; d < MAX_DEPTH; d++) {
        killerMoves[d][0] = null;
        killerMoves[d][1] = null;
    }
    for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
        Arrays.fill(historyHeuristic[i], 0);
    }

    outer:
    while (System.currentTimeMillis() < endTime) {
        leafCounts.add(0L);
        final int idx = leafCounts.size() - 1;

        List<Move> legalMoves = MoveGenerator.generate(pieces, color).stream()
                .filter(m -> {
                    // 1) NO guard → tower (already there)
                    if (m.piece instanceof Guard) {
                        for (Piece p : pieces) {
                            if (p.col == m.destCol && p.row == m.destRow && p instanceof Tower) {
                                return false;
                            }
                        }
                    }
                    // 2) NO tower (or guard) → friendly guard
                    for (Piece p : pieces) {
                        if (p.col == m.destCol && p.row == m.destRow
                                && p instanceof Guard
                                && p.color == m.piece.color) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (legalMoves.isEmpty()) break;

        /*for (Move m : legalMoves) {
            Piece target = pieces.stream()
                    .filter(p -> p.col == m.destCol &&
                            p.row == m.destRow &&
                            p.color != color &&
                            p instanceof Guard)
                    .findFirst().orElse(null);
           /* if (target != null) {
              //  System.out.println("Immediate guard capture found: " + m);
                return m;
            }*/
   // }
        for (Move m : legalMoves) {
            if (pieces.stream().anyMatch(p ->
                    p.col == m.destCol &&
                            p.row == m.destRow &&
                            p.color != color &&
                            p instanceof Guard)) {
                return m;
            }
        }

        Move currentBest       = null;
        int   currentBestScore = -INF;
        long  startNano        = System.nanoTime();

        for (Move m : legalMoves) {
            if (System.currentTimeMillis() >= endTime) {
               // System.out.println("Time up during depth " + depth);
                break;
            }

            Piece captured = pieces.stream()
                    .filter(p -> p.col == m.destCol &&
                            p.row == m.destRow &&
                            p.color != color)
                    .findFirst().orElse(null);

            long childZKey = Zobrist.updateHash(
                    rootZKey,
                    m.piece,
                    m.piece.preRow, m.piece.preCol,
                    m.destRow,     m.destCol,
                    captured
            );

            List<Piece> nextState = simulateMove(pieces, m);
            int eval = alphaBeta(
                    nextState,
                    depth - 1,
                    -INF, INF,
                    false,
                    color,
                    endTime,
                    leafCounts,
                    idx,
                    childZKey,
                    depth
            );
            if (eval > currentBestScore) {
                currentBestScore = eval;
                currentBest      = m;
            }
        }

        if (System.currentTimeMillis() < endTime && currentBest != null) {
            bestMove  = currentBest;
            bestScore = currentBestScore;
            long tookMs = (System.nanoTime() - startNano) / 1_000_000;
            depthTimes.add(tookMs);  //  NEW: save timing for this depth
            if (DEBUG) {
                System.out.printf(
                        "Depth %d complete in %d ms → %s (score=%d); Blattzustände Tiefe %d: %d%n",
                        depth, tookMs, bestMove, bestScore,
                        depth, leafCounts.get(idx)
                );
            }
            int nmTests = nullMoveCounts.getOrDefault(depth, 0);
            if (DEBUG) {
            System.out.printf("  Null-move tests at depth %d: %d%n", depth, nmTests);
            nullMoveCounts.remove(depth);}

            depth++;
        } else {
            break outer;
        }
    }
       // System.out.printf("%s",bestMove);
   // System.out.printf(" Returning: %s (score=%d) searched to depth %d%n", bestMove, bestScore, depth - 1);

    // NEW: print full depth timing summary
   /* System.out.println("Depth timing summary:");
    for (int d = 1; d <= depthTimes.size(); d++) {
        System.out.printf("Depth %d: %d ms%n", d, depthTimes.get(d - 1));
    }*/

    return bestMove;
}


    private static int alphaBeta(
            List<Piece> board,
            int depth,
            int alpha,
            int beta,
            boolean maximizing,
            int playerColor,
            long endTime,
            List<Long> leafCounts,
            int idx,
            long zKey,
            int ply
    ) {
        TranspositionTable.Entry entry = tt.get(zKey);
        if (entry != null && entry.depth >= depth) {
            switch (entry.flag) {
                case TranspositionTable.FLAG_EXACT:
                    return entry.score;
                case TranspositionTable.FLAG_LOWER:
                    if (entry.score > alpha) alpha = entry.score;
                    break;
                case TranspositionTable.FLAG_UPPER:
                    if (entry.score < beta) beta = entry.score;
                    break;
            }
            if (alpha >= beta) {
                return entry.score;
            }
        }

        if (depth == 0 || System.currentTimeMillis() >= endTime || isTerminal(board)) {
            int val = evaluate(board, playerColor);
            tt.put(zKey, depth, val, TranspositionTable.FLAG_EXACT);
            leafCounts.set(idx, leafCounts.get(idx) + 1);
            return val;
        }

        int currentColor = maximizing ? playerColor : 1 - playerColor;

        // Null-Move Pruning
        final boolean inCheck = false;
        final int R = 2;
        if (depth >= 3 && !inCheck) {
            nullMoveCounts.merge(depth, 1, Integer::sum);

            int score = -alphaBeta(
                    board,
                    depth - 1 - R,
                    -beta,
                    -beta + 1,
                    !maximizing,
                    playerColor,
                    endTime,
                    leafCounts,
                    idx,
                    zKey,
                    ply + 1
            );

            if (score >= beta) {
               /* System.out.printf(
                        "[NullMove→Cutoff] depth=%d, score=%d, beta=%d%n",
                        depth, score, beta
                );*/
                return beta;
            }
        }

        List<Move> legalMoves = MoveGenerator.generate(board, currentColor);

        Move ttMove = null;

        List<Move> captures = new ArrayList<>();
        List<Move> killers  = new ArrayList<>();
        List<Move> quiets   = new ArrayList<>();

        for (Move m : legalMoves) {
            Piece target = board.stream()
                    .filter(p -> p.col == m.destCol &&
                            p.row == m.destRow &&
                            p.color != currentColor)
                    .findFirst().orElse(null);

            if (ttMove != null && m.equals(ttMove)) {
                continue;
            } else if (target != null) {
                captures.add(m);
            } else if (isKillerMove(m, ply)) {
                killers.add(m);
            } else {
                quiets.add(m);
            }
        }

        captures.sort((a, b) -> Integer.compare(
                simpleCaptureValue(board, b, currentColor),
                simpleCaptureValue(board, a, currentColor)
        ));

        quiets.sort((a, b) -> Integer.compare(
                historyScore(b),
                historyScore(a)
        ));

        List<Move> orderedMoves = new ArrayList<>();
        if (ttMove != null) orderedMoves.add(ttMove);
        orderedMoves.addAll(captures);
        orderedMoves.addAll(killers);
        orderedMoves.addAll(quiets);

        int originalAlpha = alpha;
        int bestScore;

        if (maximizing) {
            bestScore = -INF;
            for (Move m : orderedMoves) {
                if (System.currentTimeMillis() >= endTime) break;

                Piece captured = board.stream()
                        .filter(p -> p.col == m.destCol &&
                                p.row == m.destRow &&
                                p.color != currentColor)
                        .findFirst().orElse(null);

                long childKey = Zobrist.updateHash(
                        zKey,
                        m.piece,
                        m.piece.preRow, m.piece.preCol,
                        m.destRow,     m.destCol,
                        captured
                );

                List<Piece> nextState = simulateMove(board, m);
                int val = alphaBeta(
                        nextState,
                        depth - 1,
                        alpha,
                        beta,
                        false,
                        playerColor,
                        endTime,
                        leafCounts,
                        idx,
                        childKey,
                        ply + 1
                );

                if (val > bestScore) {
                    bestScore = val;
                    if (depth > 0 && !isCapture(board, m)) {
                        storeKillerMove(m, ply);
                        updateHistoryHeuristic(m, depth);
                    }
                }

                alpha = Math.max(alpha, val);
                if (beta <= alpha) break;
            }
        } else {
            bestScore = INF;
            for (Move m : orderedMoves) {
                if (System.currentTimeMillis() >= endTime) break;

                Piece captured = board.stream()
                        .filter(p -> p.col == m.destCol &&
                                p.row == m.destRow &&
                                p.color != currentColor)
                        .findFirst().orElse(null);

                long childKey = Zobrist.updateHash(
                        zKey,
                        m.piece,
                        m.piece.preRow, m.piece.preCol,
                        m.destRow,     m.destCol,
                        captured
                );

                List<Piece> nextState = simulateMove(board, m);
                int val = alphaBeta(
                        nextState,
                        depth - 1,
                        alpha,
                        beta,
                        true,
                        playerColor,
                        endTime,
                        leafCounts,
                        idx,
                        childKey,
                        ply + 1
                );

                if (val < bestScore) {
                    bestScore = val;
                    if (depth > 0 && !isCapture(board, m)) {
                        storeKillerMove(m, ply);
                        updateHistoryHeuristic(m, depth);
                    }
                }

                beta = Math.min(beta, val);
                if (beta <= alpha) break;
            }
        }

        byte flag;
        if (bestScore <= originalAlpha) {
            flag = TranspositionTable.FLAG_UPPER;
        } else if (bestScore >= beta) {
            flag = TranspositionTable.FLAG_LOWER;
        } else {
            flag = TranspositionTable.FLAG_EXACT;
        }
        tt.put(zKey, depth, bestScore, flag);

        return bestScore;
    }

    private static boolean isKillerMove(Move m, int ply) {
        return (killerMoves[ply][0] != null && killerMoves[ply][0].equals(m)) ||
                (killerMoves[ply][1] != null && killerMoves[ply][1].equals(m));
    }

    private static void storeKillerMove(Move m, int ply) {
        if (killerMoves[ply][0] == null || !killerMoves[ply][0].equals(m)) {
            killerMoves[ply][1] = killerMoves[ply][0];
            killerMoves[ply][0] = m;
        }
    }

    private static void updateHistoryHeuristic(Move m, int depth) {
        int from = m.piece.preRow * BOARD_SIZE + m.piece.preCol;
        int to   = m.destRow * BOARD_SIZE + m.destCol;
        historyHeuristic[from][to] += depth * depth;
    }

    private static int historyScore(Move m) {
        int from = m.piece.preRow * BOARD_SIZE + m.piece.preCol;
        int to   = m.destRow * BOARD_SIZE + m.destCol;
        return historyHeuristic[from][to];
    }

    private static boolean isCapture(List<Piece> board, Move m) {
        return board.stream()
                .anyMatch(p -> p.col == m.destCol && p.row == m.destRow &&
                        p.color != m.piece.color);
    }

    private static int simpleCaptureValue(List<Piece> board, Move m, int enemyColor) {
        Piece target = board.stream()
                .filter(p -> p.col == m.destCol &&
                        p.row == m.destRow &&
                        p.color == enemyColor)
                .findFirst().orElse(null);

        if (target == null) return 0;
        if (target instanceof Guard) return 1_000_000;
        else return target.Height * 100;
    }

    public static boolean isTerminal(List<Piece> board) {
        return board.stream().filter(p -> p instanceof Guard).count() < 2;
    }

    public static List<Piece> simulateMove(List<Piece> original, Move move) {
        List<Piece> newBoard = new ArrayList<>();
        for (Piece p : original) {
            Piece copy = (p instanceof Guard)
                    ? new Guard(p.color, p.col, p.row, p.Height)
                    : new Tower(p.color, p.col, p.row, p.Height);
            newBoard.add(copy);
        }

        Piece toMove = newBoard.stream()
                .filter(p ->
                        p.color == move.piece.color &&
                                p.col   == move.piece.preCol &&
                                p.row   == move.piece.preRow
                ).findFirst().orElse(null);
        if (toMove == null) return newBoard;

        int steps     = Math.abs(move.piece.preCol - move.destCol)
                + Math.abs(move.piece.preRow - move.destRow);
        int originalH = toMove.Height;

        newBoard.removeIf(p ->
                p != toMove &&
                        p.col == move.destCol &&
                        p.row == move.destRow &&
                        p.color != toMove.color
        );

        toMove.col = move.destCol;
        toMove.row = move.destRow;
        toMove.updatePosition();

        if (toMove instanceof Tower) {
            toMove.Height = steps;
            if (originalH > steps) {
                newBoard.add(new Tower(
                        toMove.color,
                        move.piece.preCol,
                        move.piece.preRow,
                        originalH - steps
                ));
            }
            for (Piece p : newBoard) {
                if (p != toMove
                        && p instanceof Tower
                        && p.color == toMove.color
                        && p.col   == toMove.col
                        && p.row   == toMove.row) {
                    toMove.Height += p.Height;
                    newBoard.remove(p);
                    break;
                }
            }
        }
        return newBoard;
    }

    private static final int[][] PST = {
            {1,2,3,3,3,2,1},
            {2,4,6,6,6,4,2},
            {3,6,8,8,8,6,3},
            {3,6,8,8,8,6,3},
            {3,6,8,8,8,6,3},
            {2,4,6,6,6,4,2},
            {1,2,3,3,3,2,1}
    };

    enum GamePhase { OPENING, MIDDLEGAME, ENDGAME }

    private static final int[] mobilityWeights = { 5, 3, 2 };
    private static final int[] centerControlWeights = { 10, 6, 2 };
    private static final int[] towerProximityWeights = { 6, 4, 2 };
    private static final int[] towerHeightWeights = { 5, 4, 6 };
    private static final int[] threatWeights = { 20, 30, 40 };

    // ... rest of your existing constants, fields, opening book, etc. remain unchanged.

    // Replace only the evaluate() method:
    public static int evaluate(List<Piece> board, int color) {
        GamePhase phase = detectPhase(board);
        int phaseIdx = getPhaseIndex(phase);

        int score = 0;
        Guard myGuard = null, enemyGuard = null;

        for (Piece p : board) {
            if (p instanceof Guard) {
                if (p.color == color)      myGuard = (Guard)p;
                else                       enemyGuard = (Guard)p;
            }
        }

        if (myGuard != null) {
            int myGoalRow = (color == GamePanel.RED) ? 6 : 0;
            if (myGuard.col == 3 && myGuard.row == myGoalRow) return Integer.MAX_VALUE;
        }
        if (enemyGuard != null) {
            int theirGoalRow = (enemyGuard.color == GamePanel.RED) ? 6 : 0;
            if (enemyGuard.col == 3 && enemyGuard.row == theirGoalRow) return Integer.MIN_VALUE;
        }

        int oppPieces = 0;
        int ownTowerHeight = 0;
        for (Piece p : board) {
            if (p.color != color) oppPieces++;
            else if (p instanceof Tower) ownTowerHeight += p.Height;
        }
        score -= oppPieces * 10;
        score += ownTowerHeight * towerHeightWeights[phaseIdx];

        if (myGuard != null) {
            int distCenter = Math.abs(myGuard.col - 3) + Math.abs(myGuard.row - 3);
            score -= distCenter * centerControlWeights[phaseIdx];

            // Anticipate enemy capture of our Guard
            for (Piece enemy : board) {
                if (enemy.color != color && enemy.canMove(myGuard.col, myGuard.row)) {
                    score -= 2000; // Big penalty: Guard is in danger
                }
            }
        }

        for (Piece p : board) {
            if (!(p instanceof Tower)) continue;
            int factor = (p.color == color) ? 1 : -1;
            Tower t = (Tower)p;

            if (enemyGuard != null && (p.col == enemyGuard.col || p.row == enemyGuard.row)) {
                score += factor * threatWeights[phaseIdx];
            }

            if (p.color == color && myGuard != null) {
                int d = Math.abs(p.row - myGuard.row) + Math.abs(p.col - myGuard.col);
                if (d <= 2) score += towerProximityWeights[phaseIdx];
            }
        }

        // Defense: encourage tower alignment to block enemy Guard
        if (enemyGuard != null) {
            for (Piece p : board) {
                if (p instanceof Tower && p.color == color) {
                    if ((p.col == enemyGuard.col || p.row == enemyGuard.row) &&
                            Math.abs(p.col - enemyGuard.col) + Math.abs(p.row - enemyGuard.row) <= 2) {
                        score += 50; // Tower is defending against enemy Guard advance
                    }
                }
            }
        }

        int myMob = 0, enMob = 0;
        for (Piece p : board) {
            int cnt = 0;
            for (int c = 0; c < 7; c++)
                for (int r = 0; r < 7; r++)
                    if (p.canMove(c, r)) cnt++;
            if (p.color == color) myMob += cnt;
            else                  enMob += cnt;
        }
        score += (myMob - enMob) * mobilityWeights[phaseIdx];

        return score;
    }


    private static GamePhase detectPhase(List<Piece> board) {
        long towerCount = board.stream().filter(p -> p instanceof Tower).count();
        if (towerCount >= 15) return GamePhase.OPENING;
        else if (towerCount >= 8) return GamePhase.MIDDLEGAME;
        else return GamePhase.ENDGAME;
    }

    private static int getPhaseIndex(GamePhase phase) {
        switch (phase) {
            case OPENING: return 0;
            case MIDDLEGAME: return 1;
            case ENDGAME: return 2;
            default: return 1;
        }
    }

    public static void benchmarkDepths(List<Piece> pieces, int color, int maxDepth) {
        System.out.println("Benchmarking AlphaBetaAI up to depth " + maxDepth);

        List<Long> depthTimes = new ArrayList<>();

        for (int depth = 1; depth <= maxDepth; depth++) {
            long start = System.nanoTime();

            Move bestMove = null;
            int bestScore = -INF;

            long rootZKey = Zobrist.computeHash(pieces);

            for (int d = 0; d < MAX_DEPTH; d++) {
                killerMoves[d][0] = null;
                killerMoves[d][1] = null;
            }
            for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
                Arrays.fill(historyHeuristic[i], 0);
            }

            List<Long> leafCounts = new ArrayList<>();
            leafCounts.add(0L);

            List<Move> legalMoves = MoveGenerator.generate(pieces, color);
            if (legalMoves.isEmpty()) break;

            for (Move m : legalMoves) {
                Piece captured = pieces.stream()
                        .filter(p -> p.col == m.destCol &&
                                p.row == m.destRow &&
                                p.color != color)
                        .findFirst().orElse(null);

                long childZKey = Zobrist.updateHash(
                        rootZKey,
                        m.piece,
                        m.piece.preRow, m.piece.preCol,
                        m.destRow,     m.destCol,
                        captured
                );

                List<Piece> nextState = simulateMove(pieces, m);
                int eval = alphaBeta(
                        nextState,
                        depth - 1,
                        -INF, INF,
                        false,
                        color,
                        Long.MAX_VALUE, // no time limit
                        leafCounts,
                        0,
                        childZKey,
                        depth
                );

                if (eval > bestScore) {
                    bestScore = eval;
                    bestMove = m;
                }
            }

            long tookMs = (System.nanoTime() - start) / 1_000_000;
            depthTimes.add(tookMs);

            // Only clean summary per depth:
            System.out.printf("Depth %d complete: %d ms%n", depth, tookMs);
        }

        // Final benchmark summary:
        System.out.println("Benchmark summary:");
        for (int d = 1; d <= depthTimes.size(); d++) {
            System.out.printf("Depth %d: %d ms%n", d, depthTimes.get(d - 1));
        }
    }


}
