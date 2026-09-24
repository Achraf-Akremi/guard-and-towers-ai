// src/main/java/game/BoardParser.java
package game;

import java.util.ArrayList;
import java.util.List;

import pieces.Piece;
import pieces.Guard;
import pieces.Tower;
import game.GamePanel;

/**
 * Parses the server’s FEN (e.g. "r1r11RG1r1r1/…/b1b11BG1b1b1 r") into pieces.
 *
 * Uses exactly the same rules as GamePanel.setPiecesFromFEN:
 *  - Digits = skip that many columns
 *  - 'RG' or 'BG' = a Guard
 *  - 'r' or 'b' optionally followed by a digit = a Tower with that height
 *  - After parsing each rank, col must be exactly 7
 */
public class BoardParser {
    public static List<Piece> fromString(String fen) {
        // strip off the trailing turn-letter if present
        String placement = fen.trim().split("\\s+")[0];
        String[] ranks = placement.split("/");
        if (ranks.length != 7) {
            throw new IllegalArgumentException("Expected 7 ranks, got: " + ranks.length);
        }

        List<Piece> pieces = new ArrayList<>();
        for (int row = 0; row < 7; row++) {
            String rank = ranks[row];
            int col = 0;
            for (int j = 0; j < rank.length();) {
                char c = rank.charAt(j);
                if (Character.isDigit(c)) {
                    // skip empty squares
                    col += Character.getNumericValue(c);
                    j++;
                }
                else if ((c == 'R' || c == 'B')
                        && j + 1 < rank.length()
                        && rank.charAt(j + 1) == 'G') {
                    // Guard: "RG" or "BG"
                    int color = (c == 'R') ? GamePanel.RED : GamePanel.BLUE;
                    pieces.add(new Guard(color, col, row, 1));
                    col++;
                    j += 2;
                }
                else {
                    // Tower: letter 'r' or 'b', optional height digit
                    int color = (Character.toLowerCase(c) == 'r')
                            ? GamePanel.RED
                            : GamePanel.BLUE;
                    int height = 1;
                    if (j + 1 < rank.length() && Character.isDigit(rank.charAt(j+1))) {
                        height = Character.getNumericValue(rank.charAt(j+1));
                        j += 2;
                    } else {
                        j++;
                    }
                    pieces.add(new Tower(color, col, row, height));
                    col++;
                }
            }
            if (col != 7) {
                throw new IllegalArgumentException(
                        "Rank " + row + " expanded to " + col + " cols (must be 7)"
                );
            }
        }
        return pieces;
    }
}
