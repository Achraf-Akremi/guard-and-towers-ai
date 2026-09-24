package tests;

import java.util.*;

public class ZugsortierungTestUtils {

    public static List<Move> orderMovesForTest(
            List<Move> legalMoves,
            TranspositionTable.Entry entry,
            List<Piece> board,
            int currentColor,
            int ply
    ) {
        Move ttMove = null;  // For test, we set TT move to null.

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
            } else {
                quiets.add(m);
            }
        }

        // Captures ordered: Guard capture first, then Tower by Height
        captures.sort((a, b) -> {
            int evalA = simpleCaptureValue(board, a, currentColor);
            int evalB = simpleCaptureValue(board, b, currentColor);
            return Integer.compare(evalB, evalA);
        });

        // Quiets not ordered for now — for full test you could add history heuristic here

        List<Move> orderedMoves = new ArrayList<>();
        if (ttMove != null) orderedMoves.add(ttMove);
        orderedMoves.addAll(captures);
        orderedMoves.addAll(killers);  // For now killers is empty
        orderedMoves.addAll(quiets);

        return orderedMoves;
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
}
