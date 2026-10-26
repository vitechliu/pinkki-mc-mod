package io.github.vitech.pinkkicore.forge.client.guide;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import io.github.vitech.pinkkicore.forge.guide.GuideBook;
import io.github.vitech.pinkkicore.forge.workbench.GuabaConversionRecipe;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.registries.ForgeRegistries;

/** Native offline reader. All layout and Minecraft references stay on the client. */
public final class GuideScreen extends Screen {
    private static final ResourceLocation CONTENT = new ResourceLocation("pinkkicore", "guide/zh_cn.json");
    private static final Pattern INLINE = Pattern.compile("\\[([^]]+)\\]\\(([^)]+)\\)|\\*\\*([^*]+)\\*\\*|`([^`]+)`");
    private static String lastPage = "handbook";
    private static int lastScroll;
    private final Deque<Location> history = new ArrayDeque<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> sidebar = new ArrayList<>();
    private GuideBook book;
    private String page = lastPage, query = "";
    private int scroll = lastScroll, listScroll, contentHeight;
    private int left, top, panelWidth, panelHeight, sidebarWidth, bodyX, bodyY, bodyWidth, bodyHeight;
    private EditBox search;
    private Button back;
    private boolean loadFailed;
    private record Location(String page, int scroll) {}
    private record Row(int y, int height, FormattedCharSequence text, GuideBook.Block block) {}
    private record Hovered(ItemStack stack, int x, int y) {}
    private Hovered hovered;

    public GuideScreen() { super(Component.translatable("item.pinkkicore.guide_book")); }
    @Override protected void init() {
        if (book == null) {
            try (var reader = new InputStreamReader(minecraft.getResourceManager().open(CONTENT), StandardCharsets.UTF_8)) {
                book = GuideBook.read(reader);
            } catch (Exception error) {
                LogUtils.getLogger().error("Cannot load PinkkiCore guide", error);
                loadFailed = true;
                book = new GuideBook(List.of(new GuideBook.Entry("error", title.getString(), List.of(
                        new GuideBook.Block("text", Component.translatable("guide.pinkkicore.load_error").getString(), "", "")))));
            }
        }
        if (entry(page) == null) page = book.entries().get(0).id();
        panelWidth = Math.min(width - 12, 720); panelHeight = Math.min(height - 12, 430);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        sidebarWidth = Math.min(132, panelWidth / 3);
        bodyX = left + sidebarWidth + 18; bodyY = top + 54;
        bodyWidth = panelWidth - sidebarWidth - 30; bodyHeight = panelHeight - 72;
        search = new EditBox(font, left + 10, top + 30, sidebarWidth - 12, 18, Component.translatable("guide.pinkkicore.search"));
        search.setMaxLength(80); search.setHint(Component.translatable("guide.pinkkicore.search")); search.setValue(query);
        search.setResponder(value -> { query = value; listScroll = 0; refreshSidebar(); });
        addRenderableWidget(search);
        back = addRenderableWidget(Button.builder(Component.translatable("guide.pinkkicore.back"), button -> {
            if (!history.isEmpty()) { var previous = history.pop(); page = previous.page; scroll = previous.scroll; layout(); }
        }).bounds(bodyX, top + 28, 48, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + panelWidth - 54, top + 28, 44, 20).build());
        sidebar.clear(); refreshSidebar(); layout();
    }
    private GuideBook.Entry entry(String id) {
        return book.entries().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
    }
    private List<GuideBook.Entry> filtered() { return book.entries().stream().filter(e -> e.matches(query)).toList(); }
    private void refreshSidebar() {
        sidebar.forEach(this::removeWidget); sidebar.clear();
        List<GuideBook.Entry> matches = filtered();
        int capacity = Math.max(1, (panelHeight - 68) / 24);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, matches.size() - capacity)));
        for (int i = listScroll; i < Math.min(matches.size(), listScroll + capacity); i++) {
            var e = matches.get(i);
            var button = Button.builder(Component.literal(e.title()), b -> {
                navigate(e.id());
                if (!query.isBlank()) {
                    String needle = query.toLowerCase(Locale.ROOT);
                    rows.stream().filter(r -> r.text != null && plain(r.text).toLowerCase(Locale.ROOT).contains(needle))
                            .findFirst().ifPresent(r -> scroll = r.y);
                    clamp();
                }
            }).bounds(left + 10, top + 58 + (i - listScroll) * 24, sidebarWidth - 12, 20).build();
            sidebar.add(addRenderableWidget(button));
        }
    }
    private static String plain(FormattedCharSequence sequence) {
        StringBuilder text = new StringBuilder();
        sequence.accept((i, style, codepoint) -> { text.appendCodePoint(codepoint); return true; });
        return text.toString();
    }
    private void navigate(String target) {
        String[] parts = target.split("#", 2);
        if (entry(parts[0]) == null) return;
        history.push(new Location(page, scroll)); page = parts[0]; scroll = 0; layout();
        if (parts.length == 2) rows.stream().filter(r -> r.block != null && r.block.anchor().equals(parts[1]))
                .findFirst().ifPresent(r -> scroll = r.y);
        clamp();
    }
    private Component rich(String text) {
        var result = Component.empty();
        var matcher = INLINE.matcher(text);
        int end = 0;
        while (matcher.find()) {
            result.append(Component.literal(text.substring(end, matcher.start())));
            if (matcher.group(1) != null) {
                String target = matcher.group(2).replaceFirst("^\\./", "").replace(".md", "");
                if (target.startsWith("#")) target = page + target;
                result.append(Component.literal(matcher.group(1)).withStyle(Style.EMPTY.withColor(0x3866A6)
                        .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, target))));
            } else if (matcher.group(3) != null) result.append(Component.literal(matcher.group(3)).withStyle(ChatFormatting.BOLD));
            else result.append(Component.literal(matcher.group(4)));
            end = matcher.end();
        }
        return result.append(Component.literal(text.substring(end)));
    }
    private void textRows(String text, boolean heading, GuideBook.Block block) {
        if (heading) contentHeight += 6;
        Component component = rich(text);
        if (heading) component = component.copy().withStyle(ChatFormatting.BOLD);
        boolean first = true;
        for (var line : font.split(component, Math.max(30, bodyWidth - 12))) {
            rows.add(new Row(contentHeight, 13, line, first ? block : null)); contentHeight += 13; first = false;
        }
        contentHeight += heading ? 5 : 8;
    }
    private void layout() {
        rows.clear(); contentHeight = 0;
        for (var block : entry(page).blocks()) {
            if (block.type().equals("text") || block.type().equals("heading")) {
                textRows(block.text(), block.type().equals("heading"), block);
            } else if (block.type().equals("potion_stats")) {
                try (var reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                        "/data/pinkkicore/guaba_potions.json")), StandardCharsets.UTF_8)) {
                    var definition = JsonParser.parseReader(reader).getAsJsonObject();
                    var levels = definition.getAsJsonArray("levels");
                    double scale = block.id().equals("thunder_guaba")
                            ? definition.getAsJsonObject("thunder").get("amplitude_multiplier").getAsDouble() : 1;
                    for (int i = 0; i < levels.size(); i++) {
                        var level = levels.get(i).getAsJsonObject();
                        textRows(String.format(Locale.ROOT, "%s · %s %+.1f%% 至 %+.1f%% · %s %+.0f 至 %+.0f",
                                List.of("I", "II", "III", "IV").get(i), Component.translatable("guide.pinkkicore.speed").getString(),
                                (level.get("speed_min").getAsDouble()-1)*scale*100, (level.get("speed_max").getAsDouble()-1)*scale*100,
                                Component.translatable("guide.pinkkicore.armor").getString(),
                                level.get("armor_min").getAsDouble()*scale, level.get("armor_max").getAsDouble()*scale), false, null);
                    }
                } catch (Exception e) { textRows(Component.translatable("guide.pinkkicore.unavailable").getString(), false, null); }
            } else if (block.type().equals("brewing")) {
                // Enumerate the real Forge brewing registrations, including their NBT-bearing potion stacks.
                for (var rule : BrewingRecipeRegistry.getRecipes()) if (rule instanceof BrewingRecipe brewing
                        && brewing.getOutput().is(Items.POTION)
                        && net.minecraft.world.item.alchemy.PotionUtils.getPotion(brewing.getOutput()) != net.minecraft.world.item.alchemy.Potions.EMPTY
                        && net.minecraft.world.item.alchemy.PotionUtils.getPotion(brewing.getOutput()).getEffects().stream()
                            .anyMatch(effect -> Objects.requireNonNull(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()))
                                .equals(new ResourceLocation("pinkkicore", block.id().isBlank() ? "guaba" : block.id())))) {
                    // Store registry index, not a second copy of the recipe.
                    var rowBlock = new GuideBook.Block("brewing_rule", "", Integer.toString(BrewingRecipeRegistry.getRecipes().indexOf(rule)), "");
                    rows.add(new Row(contentHeight, 76, null, rowBlock)); contentHeight += 84;
                }
            } else {
                int height = block.type().equals("item") ? 34 : 104;
                rows.add(new Row(contentHeight, height, null, block)); contentHeight += height + 8;
            }
        }
        clamp();
    }
    private void clamp() { scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - bodyHeight))); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFFF1E9D7);
        graphics.fill(left, top, left + sidebarWidth + 7, top + panelHeight, 0xFFDDD2B9);
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), sidebarWidth-12), left + 10, top + 12, 0x42382E, false);
        graphics.drawString(font, font.plainSubstrByWidth(entry(page).title(), bodyWidth), bodyX, top + 12, 0x42382E, false);
        back.active = !history.isEmpty();
        hovered = null;
        graphics.enableScissor(bodyX, bodyY, bodyX + bodyWidth, bodyY + bodyHeight);
        for (var row : rows) {
            int y = bodyY + row.y - scroll;
            if (y + row.height < bodyY || y > bodyY + bodyHeight) continue;
            if (row.text != null) graphics.drawString(font, row.text, bodyX, y, 0x302A23, false);
            else drawBlock(graphics, row.block, bodyX, y, mouseX, mouseY);
        }
        graphics.disableScissor();
        if (contentHeight > bodyHeight) {
            int thumb = Math.max(12, bodyHeight * bodyHeight / contentHeight);
            int sy = bodyY + (bodyHeight - thumb) * scroll / Math.max(1, contentHeight - bodyHeight);
            graphics.fill(bodyX + bodyWidth - 3, bodyY, bodyX + bodyWidth, bodyY + bodyHeight, 0xFFCEC1A7);
            graphics.fill(bodyX + bodyWidth - 3, sy, bodyX + bodyWidth, sy + thumb, 0xFF857052);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (filtered().isEmpty()) graphics.drawString(font, Component.translatable("guide.pinkkicore.no_results"), left+10, top+60, 0x42382E, false);
        if (hovered != null) graphics.renderTooltip(font, hovered.stack, mouseX, mouseY);
    }
    private ItemStack sample(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[(int)((Util.getMillis()/1500) % items.length)].copy();
    }
    private void slot(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x, y, x+20, y+20, 0xFF79756E); graphics.fill(x+1, y+1, x+20, y+20, 0xFFFEFAEF);
        graphics.fill(x+1, y+1, x+19, y+19, 0xFFA39E93);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x+2, y+2); graphics.renderItemDecorations(font, stack, x+2, y+2);
            if (mouseY >= bodyY && mouseY < bodyY+bodyHeight && mouseX >= x && mouseX < x+20 && mouseY >= y && mouseY < y+20)
                hovered = new Hovered(stack, x, y);
        }
    }
    private void label(GuiGraphics g, Component label, int x, int y) {
        g.drawString(font, font.plainSubstrByWidth(label.getString(), Math.max(30, bodyX+bodyWidth-x-12)), x, y, 0x594B38, false);
    }
    private void drawBlock(GuiGraphics g, GuideBook.Block block, int x, int y, int mx, int my) {
        if (block.type().equals("item")) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(block.id()));
            if (item != null) { var stack = new ItemStack(item); slot(g, stack, x, y, mx, my); label(g, stack.getHoverName(), x+26, y+6); }
            return;
        }
        if (block.type().equals("brewing_rule")) {
            var recipe = (BrewingRecipe)BrewingRecipeRegistry.getRecipes().get(Integer.parseInt(block.id()));
            label(g, Component.translatable("guide.pinkkicore.brewing"), x, y);
            slot(g, sample(recipe.getInput()), x, y+18, mx,my);
            g.drawString(font, "+", x+25, y+24, 0x594B38, false);
            slot(g, sample(recipe.getIngredient()), x+38, y+18, mx,my);
            g.drawString(font, "→", x+65, y+24, 0x594B38, false);
            slot(g, recipe.getOutput(), x+84, y+18, mx,my);
            label(g, recipe.getOutput().getHoverName(), x, y+46);
            return;
        }
        if (minecraft.level == null) return;
        var recipe = minecraft.level.getRecipeManager().byKey(new ResourceLocation(block.id())).orElse(null);
        if (recipe instanceof CraftingRecipe crafting) {
            label(g, Component.translatable(crafting instanceof ShapedRecipe ? "guide.pinkkicore.shaped" : "guide.pinkkicore.shapeless"), x, y);
            int columns = crafting instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            for (int i=0; i<9; i++) {
                int ix=i%3, iy=i/3, index=iy*columns+ix;
                boolean present = ix<columns && (!(crafting instanceof ShapedRecipe shaped) || iy<shaped.getHeight()) && index<crafting.getIngredients().size();
                slot(g, present ? sample(crafting.getIngredients().get(index)) : ItemStack.EMPTY, x+ix*22, y+16+iy*22, mx,my);
            }
            g.drawString(font, "→", x+75, y+43, 0x594B38, false);
            slot(g, crafting.getResultItem(minecraft.level.registryAccess()), x+96, y+38, mx,my);
        } else if (recipe instanceof GuabaConversionRecipe conversion) {
            label(g, Component.translatable("block.pinkkicore.guaba_workbench"), x, y);
            var input = sample(conversion.getIngredients().get(0)); input.setCount(conversion.inputCount());
            slot(g, new ItemStack(ModItems.GUABA.get(), Math.max(1, conversion.guabaCost())), x, y+17, mx,my);
            slot(g, input, x, y+40, mx,my); g.drawString(font, "→", x+37, y+45, 0x594B38, false);
            slot(g, conversion.getResultItem(minecraft.level.registryAccess()), x+62, y+40, mx,my);
            label(g, Component.translatable(conversion.guabaCost()==0 ? "guide.pinkkicore.catalyst" : "guide.pinkkicore.cost", conversion.guabaCost()), x, y+68);
        } else if (recipe instanceof io.github.vitech.pinkkicore.forge.charging.GuabaChargingRecipe charging) {
            label(g, Component.translatable("block.pinkkicore.guaba_charging_station"), x, y);
            slot(g, sample(charging.getIngredients().get(0)), x, y+40, mx,my);
            g.drawString(font, "→", x+37, y+45, 0x28687C, false);
            slot(g, charging.getResultItem(minecraft.level.registryAccess()), x+62, y+40, mx,my);
        } else label(g, Component.translatable("guide.pinkkicore.unavailable"), x, y);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (x >= left && x < bodyX && y >= top+54 && y < top+panelHeight) {
            listScroll -= (int)Math.signum(delta); refreshSidebar(); return true;
        }
        if (x >= bodyX && x < bodyX+bodyWidth && y >= bodyY && y < bodyY+bodyHeight) {
            scroll -= (int)(delta*26); clamp(); return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button==0 && x>=bodyX && x<bodyX+bodyWidth && y>=bodyY && y<bodyY+bodyHeight) {
            for (var row : rows) if (row.text != null && y >= bodyY+row.y-scroll && y < bodyY+row.y-scroll+13) {
                Style style = font.getSplitter().componentStyleAtWidth(row.text, (int)x-bodyX);
                if (style != null && style.getClickEvent() != null) {
                    navigate(style.getClickEvent().getValue()); return true;
                }
            }
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!search.isFocused()) {
            if (key==264 || key==267) { scroll += key==267 ? bodyHeight-20 : 26; clamp(); return true; }
            if (key==265 || key==266) { scroll -= key==266 ? bodyHeight-20 : 26; clamp(); return true; }
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { if (!loadFailed) { lastPage=page; lastScroll=scroll; } }
}
