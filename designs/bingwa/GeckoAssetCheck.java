import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.JsonUtil;

/** Parse the proposed assets with the actual GeckoLib library, without starting Minecraft. */
public final class GeckoAssetCheck {
    public static void main(String[] args) throws Exception {
        Model model = JsonUtil.GEO_GSON.fromJson(Files.readString(Path.of(args[0])), Model.class);
        var geometry = model.minecraftGeometry()[0];
        int cubes = 0;
        for (var bone : geometry.bones()) {
            if (bone.cubes() != null) cubes += bone.cubes().length;
        }
        JsonObject file = JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();
        BakedAnimations animations = JsonUtil.GEO_GSON.fromJson(file.getAsJsonObject("animations"), BakedAnimations.class);
        if (geometry.bones().length != 20 || cubes != 75 || animations.animations().size() != 7)
            throw new AssertionError("Unexpected geometry or animation count");
        System.out.println("PASS GeckoLib asset parser: bones=" + geometry.bones().length
                + ", cubes=" + cubes + ", animations=" + animations.animations().keySet());
    }
}
