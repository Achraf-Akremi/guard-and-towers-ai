package benchmark;
import java.util.ArrayList;
import java.util.List;

import pieces.Guard;
import pieces.Piece;
import pieces.Tower;



/**
 * Generates all legal moves for a given side (color) on the current board.
 */
public class MoveGenerator {

    /**
     * @param boardPieces  the list of current pieces (GamePanel.pieces)
     * @param color        GamePanel.RED or GamePanel.BLUE
     * @return             a list of all legal Moves for that color
     */
    
    public static List<Move> generate(List<Piece> boardPieces, int color) {
        List<Move> moves = new ArrayList<>();

        for (Piece p : boardPieces) {
            if (p.color != color) continue;

            // Guard: can move one orthogonal square
            if (p instanceof Guard) {
                int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
                for (int[] d : dirs) {
                    int c = p.preCol + d[0];
                    int r = p.preRow + d[1];
                    if (p.canMove(c, r)) {
                        moves.add(new Move(p, c, r));
                    }
                }
            }
            // Tower: can slide up to Height squares orthogonally
            else if (p instanceof Tower) {
                int H = p.Height;
                int[] dc = {1, -1, 0, 0};
                int[] dr = {0, 0, 1, -1};

                for (int dir = 0; dir < 4; dir++) {
                    for (int step = 1; step <= H; step++) {
                        int c = p.preCol + dc[dir] * step;
                        int r = p.preRow + dr[dir] * step;
                        if (p.canMove(c, r)) {
                            moves.add(new Move(p, c, r));
                        } else {
                            // blocked beyond this point
                            break;
                        }
                    }
                }
            }
        }

        return moves;
    }
}
