import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateInvoiceBackground {
    public static void main(String[] args) throws Exception {
        int width = 2480;  // A4 at 300 DPI
        int height = 3508;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        // High quality rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        // 1. Base Paper: Clean White
        g2.setColor(new Color(255, 255, 255));
        g2.fillRect(0, 0, width, height);

        // 2. Top Header Aurora Mesh Gradient (Hibana Light Blues)
        // Primary Top Left-Center Aurora Glow
        Point2D center1 = new Point2D.Float(width * 0.35f, height * 0.08f);
        float radius1 = width * 0.75f;
        float[] dist1 = {0.0f, 0.25f, 0.55f, 0.85f, 1.0f};
        Color[] colors1 = {
            new Color(0, 104, 143, 235),     // #00688f (Hibana Deep Blue/Teal)
            new Color(56, 189, 248, 220),    // #38bdf8 (Sky Blue)
            new Color(134, 207, 251, 160),   // #86cffb (Hibana Primary Light Blue)
            new Color(224, 242, 254, 80),    // Soft light blue mist
            new Color(255, 255, 255, 0)      // Fade to transparent white
        };
        g2.setPaint(new RadialGradientPaint(center1, radius1, dist1, colors1));
        g2.fillRect(0, 0, width, (int)(height * 0.45f));

        // Secondary Top-Right Cyan/Teal Flare
        Point2D center2 = new Point2D.Float(width * 0.90f, height * 0.02f);
        float radius2 = width * 0.65f;
        float[] dist2 = {0.0f, 0.35f, 0.75f, 1.0f};
        Color[] colors2 = {
            new Color(79, 219, 200, 190),    // #4fdbc8 (Hibana Secondary Teal)
            new Color(56, 189, 248, 140),    // #38bdf8 (Sky Blue)
            new Color(186, 230, 253, 60),    // Soft Cyan Mist
            new Color(255, 255, 255, 0)
        };
        g2.setPaint(new RadialGradientPaint(center2, radius2, dist2, colors2));
        g2.fillRect(0, 0, width, (int)(height * 0.40f));

        // Top-Left Subtle Deep Blue Edge Anchor
        Point2D center3 = new Point2D.Float(0, 0);
        float radius3 = width * 0.50f;
        float[] dist3 = {0.0f, 0.50f, 1.0f};
        Color[] colors3 = {
            new Color(20, 66, 101, 150),     // Deep atmospheric navy
            new Color(56, 189, 248, 80),
            new Color(255, 255, 255, 0)
        };
        g2.setPaint(new RadialGradientPaint(center3, radius3, dist3, colors3));
        g2.fillRect(0, 0, width, (int)(height * 0.35f));

        // 3. Bottom-Right Corner Soft Light-Blue Glow (Matches user template)
        Point2D centerBottom = new Point2D.Float(width * 1.05f, height * 0.95f);
        float radiusBottom = width * 0.65f;
        float[] distBottom = {0.0f, 0.30f, 0.65f, 1.0f};
        Color[] colorsBottom = {
            new Color(134, 207, 251, 140),   // #86cffb (Light Blue Glow)
            new Color(186, 230, 253, 90),    // #bae6fd
            new Color(224, 242, 254, 40),
            new Color(255, 255, 255, 0)
        };
        g2.setPaint(new RadialGradientPaint(centerBottom, radiusBottom, distBottom, colorsBottom));
        g2.fillRect((int)(width * 0.30f), (int)(height * 0.65f), (int)(width * 0.70f), (int)(height * 0.35f));

        g2.dispose();

        // Save outputs
        File targetOrders = new File("ticketing-app-orders/src/main/resources/invoice-background.png");
        targetOrders.getParentFile().mkdirs();
        ImageIO.write(img, "png", targetOrders);
        System.out.println("Generated invoice background: " + targetOrders.getAbsolutePath() + " (" + targetOrders.length() + " bytes)");

        File targetFrontend = new File("ticketing-frontend/public/images/invoice-background.png");
        targetFrontend.getParentFile().mkdirs();
        ImageIO.write(img, "png", targetFrontend);
        System.out.println("Generated frontend copy: " + targetFrontend.getAbsolutePath());
    }
}
