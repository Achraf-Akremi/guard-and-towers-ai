package tests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ZugsortierungTest {

    @ParameterizedTest
    @MethodSource("fenTestCases")
    void testZugsortierungViaAlphaBeta(String fen) {
        Piece.skipLoadingImages = true;

        GamePanel gp = new GamePanel(fen);
        int color = gp.getCurrentColor();
        List<Piece> pieces = GamePanel.pieces;

        System.out.println("\n🔍 FEN: " + fen);
        Move bestMove = AlphaBetaAI.findBestMove(pieces, color, 1000);  // 1 second time limit

        System.out.println("   ➤ Best move chosen: " + bestMove);

        // Check possible captures:
        boolean hasGuardCapture = pieces.stream()
                .anyMatch(p -> p instanceof Guard &&
                        p.color != color &&
                        MoveGenerator.generate(pieces, color).stream()
                                .anyMatch(m -> m.destCol == p.col && m.destRow == p.row));

        boolean hasTowerCapture = pieces.stream()
                .anyMatch(p -> p instanceof Tower &&
                        p.color != color &&
                        MoveGenerator.generate(pieces, color).stream()
                                .anyMatch(m -> m.destCol == p.col && m.destRow == p.row));

        Piece target = pieces.stream()
                .filter(p -> p.col == bestMove.destCol &&
                        p.row == bestMove.destRow &&
                        p.color != color)
                .findFirst().orElse(null);

        if (hasGuardCapture) {
            assertNotNull(target, "Expected a capture of something.");
            assertTrue(target instanceof Guard, "Expected to capture a Guard first.");
            System.out.println("   ✅ Guard capture chosen as best move.");
        } else if (hasTowerCapture) {
            assertNotNull(target, "Expected a capture of something.");
            assertTrue(target instanceof Tower, "Expected to capture a Tower if no Guard.");
            System.out.println("   ✅ Tower capture chosen as best move (no Guard available).");
        } else {
            System.out.println("   ℹ️ No captures available → any move is fine.");
            // No assert → move is free
        }
    }

    static Stream<Arguments> fenTestCases() {
        return Stream.of(
                Arguments.of("3RG3/2r14/b2r11b22r2/1r15/7/2b14/3BG2b2 b"), // Guard capturable
                Arguments.of("3RG3/7/7/7/7/3b53/3BG3 b "), // Guard capturable
                Arguments.of("3RG3/7/3r13/7/3b73/7/3BG3 r"), // Guard capturable
                Arguments.of("3RG3/3r33/7/7/3b33/7/3BG3 r"), // Tower capturable
                Arguments.of("3RG3/7/7/1b11b21r11/7/3BG3/7 b"), // Tower capturable
                Arguments.of("RGBG5/7/7/7/7/7/7 r"), // Guard capturable
                Arguments.of("3RG3/3r23/7/3b23/7/3BG3/7 r"), // Tower capturable
                Arguments.of("3RG1r1r1/7/7/7/7/7/3BG1b1b1 r"), // No captures
                Arguments.of("3RG3/2r14/2b14/7/7/7/3BG3 b") // Tower capturable
        );
    }
}
