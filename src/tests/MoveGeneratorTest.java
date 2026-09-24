package tests;
//import res.piece;
import main.GamePanel;
import main.Move;
import main.MoveGenerator;
import pieces.Piece;
import javax.swing.SwingUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoveGeneratorTest {

    @ParameterizedTest
    @MethodSource("fenTestCases")
    public void testMultipleFENs(String fen, int expectedMoves) {
        Piece.skipLoadingImages = true;  // <--- disable images when testing
        GamePanel gp = new GamePanel(fen);
        int color = gp.getCurrentColor();
        List<Piece> pieces = GamePanel.pieces;

        List<Move> legalMoves = MoveGenerator.generate(pieces, color);
        int actual = legalMoves.size();

        System.out.println("\n🔍 FEN: " + fen);
        System.out.println("   ➤ Gefundene Züge: " + actual);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < legalMoves.size(); i++) {
            sb.append(formatMove(legalMoves.get(i)));
            if (i + 1 < legalMoves.size()) sb.append(", ");
        }
        System.out.println("   ➤ Züge: "+sb);
        if (expectedMoves == actual) {
            System.out.println("✅ Test bestanden: Erwartet = Gefunden = " + expectedMoves);
        } else {
            System.out.println("❌ Fehler: Erwartet " + expectedMoves + ", aber gefunden " + actual);

        }

        assertEquals(expectedMoves, actual, "Zuganzahl stimmt nicht mit Erwartung überein");
    }

    static Stream<Arguments> fenTestCases() {
        return Stream.of(
                Arguments.of("3RG3/2r14/b2r11b22r2/1r15/7/2b14/3BG2b2 b", 24),
                Arguments.of("7/3RG3/7/3r23/7/3BG3/7 r", 11),
                Arguments.of("3RG1r11/3r33/r36/7/b32b33/7/3BG2b1 b", 21),
                Arguments.of("4RG2/4r12/1b21b23/2r22r21/5b21/3BGb12/7 b",26),
                Arguments.of("r1r11RG1r1r1/2r11r12/3r13/7/3b13/2b11b12/b1b11BG1b1b1 r",25),
                Arguments.of("3RG3/2r1BG3/b2r11b22r2/1r15/7/2b14/6b2 b",22),
                Arguments.of("7/7/3r1BG2/4r1RG1/7/7/7 r",10),
                Arguments.of("3RG1r1r1/7/7/7/7/7/3BG1b1b1 r",8),
                Arguments.of("1RG5/7/r22r22r2/7/b12b32b1/1b11BG3/7 b",21),
                Arguments.of("r14r21/1r1r1RG3/4r12/7/2b1r1b12/1b22b22/3BG3 r",25),
                Arguments.of("RGBG5/7/7/7/7/7/7 r",2),
                Arguments.of("RGr2b24/r2b35/b21BG4/7/7/7/7 r",0),
                Arguments.of("7/2r4RG3/3r13/7/7/2b11b32/1b21BG3 b", 19),
                Arguments.of("3RG3/7/7/7/7/3b53/3BG3 b ",13),
                Arguments.of("3RG3/7/3r73/7/3b73/7/3BG3 r",11),
                Arguments.of("3RG3/3r33/3b33/7/7/7/3BG3 r",8),
                Arguments.of("7/BG6/7/RG6/6r1/7/7 r",6),
                Arguments.of("3RG3/7/7/7/7/3b53/3BG3 b ",13),
                Arguments.of("3RG3/7/7/1b11b21r11/7/3BG3/7 b",15),
                Arguments.of("1r1b3RGr12/3r13/3r13/7/3r21b21/3b13/3BG3 b",16)
        );
    }
    public static String formatMove(Move m) {
        // from
        int fc = m.piece.preCol;
        int fr = m.piece.preRow;
        // to
        int tc = m.destCol;
        int tr = m.destRow;

        // File: 'A' + col
        char fileFrom = (char)('A' + fc);
        char fileTo   = (char)('A' + tc);
        // Rank (algebraic): 7 - rowIndex
        int rankFrom = 7 - fr;
        int rankTo   = 7 - tr;
        // Distance = manhattan
        int dist = Math.abs(fc - tc) + Math.abs(fr - tr);

        return String.format("%c%d-%c%d-%d",
                fileFrom, rankFrom,
                fileTo,   rankTo,
                dist
        );
    }
}
