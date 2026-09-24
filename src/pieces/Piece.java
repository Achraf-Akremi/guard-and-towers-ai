package pieces;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;


import game.Board;
import game.GamePanel;

import javax.imageio.ImageIO;

public class Piece {
    public static boolean skipLoadingImages = false;
    public BufferedImage image;
    public int x, y;
    public int col, row, preCol, preRow;
    public int color;
    public Piece hittingP;
    public int Height;

    public Piece(int color, int col, int row, int height) {
        this.color = color;
        this.col = col;
        this.row = row;
        x = getX(col);
        y = getY(row);
        preCol = col;
        preRow = row;

        Height = height;
    }

public BufferedImage getImage(String imagePath) {
    BufferedImage image = null;

    try {
        // Remove leading slash if present
        if (imagePath.startsWith("/")) {
            imagePath = imagePath.substring(1);
        }

        File imgFile = new File("res/" + imagePath + ".png");

        if (!imgFile.exists()) {
            System.err.println("Image file not found: " + imgFile.getAbsolutePath());
            return null;
        }

        image = ImageIO.read(imgFile);
    } catch (IOException e) {
        System.err.println("Error loading image: " + imagePath + ".png");
        e.printStackTrace();
    }

    return image;
}
   

    public int getX(int col) {
        return col * Board.SQUARE_SIZE;
    }

    public int getY(int row) {
        return row * Board.SQUARE_SIZE;
    }

    public int getCol(int x) {
        return (x + Board.HALF_SQUARE_SIZE) / Board.SQUARE_SIZE;
    }

    public int getRow(int y) {
        return (y + Board.HALF_SQUARE_SIZE) / Board.SQUARE_SIZE;
    }

    public int getIndex() {
        for (int index = 0; index < GamePanel.simPieces.size(); index++) {
            if (GamePanel.simPieces.get(index) == this) {
                return index;
            }
        }
        return 0;
    }

    public void updatePosition() {
        x = getX(col);
        y = getY(row);
        preCol = getCol(x);
        preRow = getRow(y);
    }

    public void resetPosition() {
        col = preCol;
        row = preRow;
        x = getX(col);
        y = getY(row);
    }

    public boolean canMove(int targetCol, int targetRow) {
        return false;
    }

    public boolean isWihinBoard(int targetCol, int targetRow) {
        return targetCol >= 0 && targetCol <= 6 && targetRow >= 0 && targetRow <= 6;
    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        return targetCol == preCol && targetRow == preRow;
    }

    public Piece getHittingP(int targetCol, int targetRow) {
        for (Piece piece : GamePanel.simPieces) {
            if (piece.col == targetCol && piece.row == targetRow && piece != this) {
                return piece;
            }
        }
        return null;
    }

    public boolean isValidSquare(int targetCol, int targetRow) {
        hittingP = getHittingP(targetCol, targetRow);

        if (hittingP == null) {
            return true;
        }
        else {
            if (hittingP.color != this.color) {
                return true;
            }
            else {
                hittingP = null;
            }
        }
        return false;
    }

    public boolean pieceIsOnLine(int targetCol, int targetRow) {
        for (int c = preCol - 1; c > targetCol; c--) {
            for (Piece piece : GamePanel.simPieces) {
                if (piece.col == c && piece.row == targetRow) {
                    hittingP = piece;
                    return true;
                }
            }
        }

        for (int c = preCol + 1; c < targetCol; c++) {
            for (Piece piece : GamePanel.simPieces) {
                if (piece.col == c && piece.row == targetRow) {
                    hittingP = piece;
                    return true;
                }
            }
        }

        for (int r = preRow - 1; r > targetRow; r--) {
            for (Piece piece : GamePanel.simPieces) {
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }

        for (int r = preRow + 1; r < targetRow; r++) {
            for (Piece piece : GamePanel.simPieces) {
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        return false;
    }

    public void setImage() {}

    public void draw(Graphics2D g2) {
        g2.drawImage(image, x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
    }
}
