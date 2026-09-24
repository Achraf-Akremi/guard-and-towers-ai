package ki;

import java.util.List;
import java.util.Random;
import pieces.Guard;
import pieces.Piece;
import pieces.Tower;
import game.GamePanel;
/**
 * Zobrist hashing for a 7×7 board of Guards & Towers.
 * No bitboards are needed—just your List<Piece> with (row, col, color, Height).
 */
public class Zobrist {
    private static final int BOARD_SIZE = 7;
    private static final int NUM_SQUARES = BOARD_SIZE * BOARD_SIZE;
    private static final int MAX_TOWER_HEIGHT = 7;
    // TYPES = { RedGuard, BlueGuard, RedTower_h=1..7, BlueTower_h=1..7 }
    private static final int NUM_TYPES = 2 + 2 * MAX_TOWER_HEIGHT;

    /** zobristKeys[typeID][squareIndex] = random 64-bit for that pieceType on that square */
    private static final long[][] zobristKeys = new long[NUM_TYPES][NUM_SQUARES];
    static {
        Random rnd = new Random(0xC0FFEE);  // fixed seed for reproducibility
        for (int t = 0; t < NUM_TYPES; t++) {
            for (int s = 0; s < NUM_SQUARES; s++) {
                zobristKeys[t][s] = rnd.nextLong();
            }
        }
    }

    /**
     * Map a Piece to its typeID in [0..NUM_TYPES-1].
     *  0 = Red Guard
     *  1 = Blue Guard
     *  2..(2+MAX_TOWER_HEIGHT-1) = Red Tower height=1..MAX
     *  (2+MAX_TOWER_HEIGHT)..(2+2*MAX_TOWER_HEIGHT-1) = Blue Tower height=1..MAX
     */
    private static int pieceTypeID(Piece p) {
        if (p instanceof Guard) {
            return (p.color == GamePanel.RED ? 0 : 1);
        } else {
            int base = (p.color == GamePanel.RED ? 2 : 2 + MAX_TOWER_HEIGHT);
            return base + (((Tower) p).Height - 1);
        }
    }

    /**
     * Convert (row, col) in 0..6 into a squareIndex 0..48
     * (row-major order: row*7 + col).
     */
    private static int squareIndex(int row, int col) {
        return row * BOARD_SIZE + col;
    }

    /**
     * Compute the 64-bit Zobrist hash of an entire board (List<Piece>) in O(n_pieces).
     */
    public static long computeHash(List<Piece> board) {
        long h = 0L;
        for (Piece p : board) {
            int t = pieceTypeID(p);
            int s = squareIndex(p.row, p.col);
            h ^= zobristKeys[t][s];
        }
        return h;
    }

    /**
     * Incrementally update a hash when moving a piece:
     *   - XOR out the piece from its old square
     *   - If a capture occurred, XOR out the captured piece at the destination
     *   - XOR in the moving piece at its new square
     * Returns the new 64-bit hash.
     */
    public static long updateHash(
            long oldHash,
            Piece movedPiece,
            int fromRow, int fromCol,
            int toRow,   int toCol,
            Piece capturedPiece  // null if no capture
    ) {
        int tMoved = pieceTypeID(movedPiece);
        int sq0    = squareIndex(fromRow, fromCol);
        long h = oldHash;
        // remove moved piece from origin
        h ^= zobristKeys[tMoved][sq0];

        // if a capture happened, remove it
        if (capturedPiece != null) {
            int tCap = pieceTypeID(capturedPiece);
            int sqCap = squareIndex(toRow, toCol);
            h ^= zobristKeys[tCap][sqCap];
        }

        // add moved piece at new square
        int sq1 = squareIndex(toRow, toCol);
        h ^= zobristKeys[tMoved][sq1];

        return h;
    }
}
