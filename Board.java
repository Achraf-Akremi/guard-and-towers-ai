package game;

import java.awt.*;

public class Board {
    public static final int MAX_COL = 7;
    public static final int MAX_ROW = 7;
    public static final int SQUARE_SIZE = 100;
    public static final int HALF_SQUARE_SIZE = SQUARE_SIZE / 2;

    // Pick your two colors here
    private static final Color LIGHT_SQUARE = new Color(255, 255, 153); // pale yellow
    private static final Color DARK_SQUARE  = new Color(255, 165,   0); // orange

    public void draw(Graphics2D g2) {
        // draw the checkerboard
        for (int row = 0; row < MAX_ROW; row++) {
            for (int col = 0; col < MAX_COL; col++) {
                // (row+col)%2==0 gives a standard checker pattern
                boolean light = ((row + col) % 2 == 0);
                g2.setColor(light ? LIGHT_SQUARE : DARK_SQUARE);
                g2.fillRect(
                    col * SQUARE_SIZE,
                    row * SQUARE_SIZE,
                    SQUARE_SIZE,
                    SQUARE_SIZE
                );
            }
        }

        // draw the two home squares in green (or any color you like)
        g2.setColor(new Color(100, 200, 100));
        g2.fillRect(3 * SQUARE_SIZE, 0,                     SQUARE_SIZE, SQUARE_SIZE);
        g2.fillRect(3 * SQUARE_SIZE, 6 * SQUARE_SIZE,       SQUARE_SIZE, SQUARE_SIZE);
    }
}
