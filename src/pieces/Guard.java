package pieces;

import game.GamePanel;

public class Guard extends Piece {
    public Guard(int color, int col, int row, int height) {
        super(color, col, row, height);
        if (Piece.skipLoadingImages) return;
        if(color == GamePanel.BLUE) {
            image = getImage("/piece/b-guard");
        }
        else {
            image = getImage("/piece/r-guard");
        }
    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWihinBoard(targetCol, targetRow)) {
            if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1) {
                return isValidSquare(targetCol, targetRow);
            }
        }
        return false;
    }
}
