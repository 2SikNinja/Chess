package chess.ui;

import chess.model.Piece;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Loads piece images from the classpath and caches smoothly scaled copies. */
public final class PieceImages {

    private static final Map<String, BufferedImage> ORIGINALS = new HashMap<>();
    private static final Map<String, BufferedImage> SCALED = new HashMap<>();

    private PieceImages() {
    }

    /** The image for {@code piece}, scaled to {@code size} pixels square. */
    public static BufferedImage get(Piece piece, int size) {
        return get(piece.getImageName(), size);
    }

    public static synchronized BufferedImage get(String imageName, int size) {
        String key = imageName + "@" + size;
        BufferedImage cached = SCALED.get(key);
        if (cached == null) {
            cached = scale(original(imageName), size);
            SCALED.put(key, cached);
        }
        return cached;
    }

    private static BufferedImage original(String imageName) {
        return ORIGINALS.computeIfAbsent(imageName, name -> {
            String path = "/pieces/" + name + ".png";
            try (InputStream stream = PieceImages.class.getResourceAsStream(path)) {
                if (stream == null) {
                    throw new IllegalStateException("Missing resource " + path);
                }
                return ImageIO.read(stream);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + path, e);
            }
        });
    }

    private static BufferedImage scale(Image source, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, size, size, null);
        g.dispose();
        return scaled;
    }
}
