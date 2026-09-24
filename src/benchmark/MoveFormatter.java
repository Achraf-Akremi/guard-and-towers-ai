package benchmark;

public class MoveFormatter {
    /**
     * Turn your Move object into the server’s "A7-B7-1" string.
     */
    public static String toServerString(Move move) {
        // source square:
        char fromCol = (char) ('A' + move.piece.preCol);
        int  fromRow = 8 - move.piece.preRow;

        // destination square:
        char toCol   = (char) ('A' + move.destCol);
        int  toRow   = 8 - move.destRow;

        // always 1 (one soldier or one guard)
        return String.format("%c%d-%c%d-1", fromCol, fromRow, toCol, toRow);
    }
}
