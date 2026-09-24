// src/main/java/main/Move.java
package benchmark;

import pieces.Piece;

/**
 * Represents a single move: take `piece` and move it to (destCol, destRow).
 */
public class Move {
    public final Piece piece;
    public final int destCol, destRow;

    public Move(Piece piece, int destCol, int destRow) {
        this.piece   = piece;
        this.destCol = destCol;
        this.destRow = destRow;
    }

    @Override
    public String toString() {
        char fromCol = (char) ('A' + piece.preCol);
        char toCol   = (char) ('A' + destCol);
        int fromRow  = 7 - piece.preRow;
        int toRow    = 7 - destRow;
        int steps    = Math.abs(piece.preCol - destCol) + Math.abs(piece.preRow - destRow);
    
        return String.format("%c%d-%c%d-%d", fromCol, fromRow, toCol, toRow, steps);
    }
}
