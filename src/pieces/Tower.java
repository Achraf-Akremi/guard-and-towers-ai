package pieces;

import game.GamePanel;

public class Tower extends Piece {

    public Tower(int color, int col, int row, int height) {
        super(color, col, row, height);
        setImage();
    }

    @Override
    public void setImage() {
        if (skipLoadingImages) return;
        if(color == GamePanel.BLUE) {
            image = getImage("/piece/b-tower-" + Height);
        }
        else {
            image = getImage("/piece/r-tower-" + Height);
        }
    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWihinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if (targetCol == preCol || targetRow == preRow) {
                if (!pieceIsOnLine(targetCol, targetRow)) {
                    hittingP = getHittingP(targetCol, targetRow);
                    if (hittingP == null || (!(hittingP instanceof Guard) && hittingP.color == this.color) || hittingP.Height <= Math.abs(targetRow - preRow) + Math.abs(targetCol - preCol)) {
                        if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) <= Height) {
                            if (!(hittingP instanceof Guard && hittingP.color == color)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
