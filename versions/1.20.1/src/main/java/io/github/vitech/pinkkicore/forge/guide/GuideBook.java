package io.github.vitech.pinkkicore.forge.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Content model only: also loadable on a dedicated server for artifact validation. */
public record GuideBook(List<Entry> entries) {
    public record Block(String type, String text, String id, String anchor) {}
    public record Entry(String id, String title, List<Block> blocks) {
        public boolean matches(String query) {
            String needle = query.strip().toLowerCase(Locale.ROOT);
            return (title + " " + blocks.stream().map(Block::text).reduce("", (a, b) -> a + " " + b))
                    .toLowerCase(Locale.ROOT).contains(needle);
        }
    }
    private static String string(JsonObject obj, String name) {
        return obj.has(name) ? obj.get(name).getAsString() : "";
    }
    public static GuideBook read(Reader reader) {
        var root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported guide format");
        var entries = new ArrayList<Entry>();
        var ids = new java.util.HashSet<String>();
        for (var value : root.getAsJsonArray("entries")) {
            var object = value.getAsJsonObject();
            var blocks = new ArrayList<Block>();
            for (var blockValue : object.getAsJsonArray("blocks")) {
                var block = blockValue.getAsJsonObject();
                String type = string(block, "type");
                if (!List.of("text", "heading", "item", "recipe", "brewing", "potion_stats").contains(type))
                    throw new IllegalArgumentException("Unknown guide block: " + type);
                blocks.add(new Block(type, string(block, "text"), string(block, "id"), string(block, "anchor")));
            }
            String id = string(object, "id");
            if (id.isBlank() || !ids.add(id)) throw new IllegalArgumentException("Invalid/duplicate guide entry: " + id);
            entries.add(new Entry(id, string(object, "title"), List.copyOf(blocks)));
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("Empty guide");
        return new GuideBook(List.copyOf(entries));
    }
}
