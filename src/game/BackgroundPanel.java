package game;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BackgroundPanel extends JPanel {
    private final BufferedImage original;
    private final ConvolveOp blurOp;

    /**
     * @param imagePath   filesystem path to your JPEG/PNG
     * @param panelWidth  width to scale the image to
     * @param panelHeight height to scale the image to
     */
    public BackgroundPanel(String imagePath, int panelWidth, int panelHeight) {
        // Load & scale the image once
        BufferedImage loaded;
        try {
            loaded = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            throw new RuntimeException("Cannot load background image: " + imagePath, e);
        }
        Image scaled = loaded.getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
        original = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = original.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();

        // Build a 7×7 box-blur kernel
        int kSize = 7;
        float[] kernel = new float[kSize * kSize];
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] = 1.0f / kernel.length;
        }
        blurOp = new ConvolveOp(new Kernel(kSize, kSize, kernel),
                                ConvolveOp.EDGE_NO_OP, null);

        // We’ll draw the image ourselves, so don’t clear background
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 1) Draw the blurred version of the original image
        Graphics2D g2 = (Graphics2D) g.create();
        BufferedImage blurred = blurOp.filter(original, null);
        g2.drawImage(blurred, 0, 0, getWidth(), getHeight(), null);
        g2.dispose();

        // 2) Then paint child components (labels, buttons, etc.)
        super.paintComponent(g);
    }
}
