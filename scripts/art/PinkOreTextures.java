import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Recolor the local vanilla 1.20.1 pixel art without changing silhouettes or stone pixels.
 * Usage: java scripts/art/PinkOreTextures.java <vanilla-client.jar>
 */
public class PinkOreTextures {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected local vanilla 1.20.1 client JAR");
        String[][] entries = {
            {"block/diamond_ore", "block/pink_ore", "ore"},
            {"block/deepslate_diamond_ore", "block/deepslate_pink_ore", "ore"},
            {"item/raw_iron", "item/raw_pink", "raw"},
            {"item/iron_ingot", "item/pink_ingot", "metal"},
            {"item/iron_nugget", "item/pink_nugget", "metal"},
            {"block/iron_block", "block/pink_block", "metal"},
            {"block/raw_iron_block", "block/raw_pink_block", "raw"}
        };
        var preview = new BufferedImage(entries.length * 128, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = preview.createGraphics();
        try (var jar = new ZipFile(args[0])) {
            for (int index = 0; index < entries.length; index++) {
                var entry = entries[index];
                BufferedImage source;
                try (var stream = jar.getInputStream(jar.getEntry("assets/minecraft/textures/" + entry[0] + ".png"))) {
                    source = ImageIO.read(stream);
                }
                if (source.getWidth() != 16 || source.getHeight() != 16) throw new IllegalStateException("Expected 16x16 texture");
                var output = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                    int rgba = source.getRGB(x, y);
                    int r = (rgba >> 16) & 255, g = (rgba >> 8) & 255, b = rgba & 255;
                    boolean recolor = switch (entry[2]) {
                        case "ore" -> b > r + 12 && g > r + 12;
                        case "raw" -> r > b + 12;
                        default -> true;
                    };
                    if ((rgba >>> 24) != 0 && recolor) {
                        // Preserve each pixel's lightness, shadows and alpha; use a rose-pink hue.
                        float high = Math.max(r, Math.max(g, b)) / 255f;
                        float low = Math.min(r, Math.min(g, b)) / 255f;
                        float light = (high + low) / 2f;
                        float saturation = entry[2].equals("metal") ? 0.55f
                                : Math.max(0.45f, (high - low) / Math.max(0.001f, 1 - Math.abs(2 * light - 1)));
                        float value = light + saturation * Math.min(light, 1 - light);
                        float hsvSaturation = value == 0 ? 0 : 2 * (1 - light / value);
                        rgba = (rgba & 0xff000000) | (Color.HSBtoRGB(0.94f, hsvSaturation, value) & 0xffffff);
                    }
                    output.setRGB(x, y, rgba);
                    if (!recolor && output.getRGB(x, y) != source.getRGB(x, y)) throw new AssertionError("Changed stone pixel");
                    if ((output.getRGB(x, y) >>> 24) != (source.getRGB(x, y) >>> 24)) throw new AssertionError("Changed alpha");
                }
                Path target = Path.of("versions/1.20.1/src/main/resources/assets/pinkkicore/textures/" + entry[1] + ".png");
                Files.createDirectories(target.getParent());
                ImageIO.write(output, "png", target.toFile());
                Files.copy(target, Path.of("wiki/public/images/" + entry[1].split("/")[1] + ".png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                graphics.drawImage(source, index * 128, 0, 128, 128, null);
                graphics.drawImage(output, index * 128, 128, 128, 128, null);
            }
        }
        graphics.dispose();
        Files.createDirectories(Path.of(".local/pink-ore"));
        ImageIO.write(preview, "png", Path.of(".local/pink-ore/textures-preview.png").toFile());
        ImageIO.write(preview.getSubimage(0, 128, entries.length * 128, 128), "png",
                Path.of(".local/pink-ore/pink-textures.png").toFile());
    }
}
