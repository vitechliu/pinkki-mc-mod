package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.forge.item.ModItems;
import io.github.vitech.pinkkicore.forge.niumo.NiumoTableMenu;
import io.github.vitech.pinkkicore.forge.workbench.SideMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class NiumoTableScreen extends AbstractContainerScreen<NiumoTableMenu> {
    private final List<Button> sideButtons = new ArrayList<>();
    private boolean sidesOpen;

    public NiumoTableScreen(NiumoTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 183;
        inventoryLabelY = 90;
    }

    @Override protected void init() {
        super.init();
        var craft = new CraftButton(leftPos + 77, topPos + 65);
        craft.setTooltip(Tooltip.create(craft.getMessage()));
        addRenderableWidget(craft);
        addRenderableWidget(Button.builder(Component.literal("≡"), button -> sidesOpen = !sidesOpen)
            .bounds(leftPos + 178, topPos + 8, 22, 22)
            .tooltip(Tooltip.create(Component.translatable("gui.pinkkicore.sides"))).build());
        sideButtons.clear();
        for (Direction direction : Direction.values()) {
            int index = direction.ordinal();
            sideButtons.add(addRenderableWidget(Button.builder(Component.empty(),
                button -> send(NiumoTableMenu.FIRST_SIDE + index))
                .bounds(leftPos + 204, topPos + 34 + index * 19, 102, 18).build()));
        }
        updateSideButtons();
    }

    private final class CraftButton extends Button {
        private final ItemStack icon = new ItemStack(ModItems.NIUMO.get());

        CraftButton(int x, int y) {
            super(x, y, 22, 22, Component.translatable("gui.pinkkicore.niumo_craft"),
                button -> send(NiumoTableMenu.CRAFT), DEFAULT_NARRATION);
        }

        @Override public void playDownSound(SoundManager soundManager) {}

        @Override protected void renderWidget(GuiGraphics graphics, int x, int y, float tick) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF382823);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                isHoveredOrFocused() ? 0xFFF3D5A1 : 0xFFD4AE7E);
            graphics.renderItem(icon, getX() + 3, getY() + 3);
        }
    }

    private void send(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private void updateSideButtons() {
        for (Direction direction : Direction.values()) {
            Button button = sideButtons.get(direction.ordinal());
            button.visible = sidesOpen;
            button.active = sidesOpen;
            button.setMessage(Component.translatable("gui.pinkkicore.side.label",
                Component.translatable("gui.pinkkicore.direction." + direction.getName()),
                Component.translatable(SideMode.fromId(menu.sideMode(direction)).key())));
        }
    }

    public List<Rect2i> getExtraGuiAreas() {
        var tab = new Rect2i(leftPos + 178, topPos + 8, 22, 22);
        return sidesOpen ? List.of(tab, new Rect2i(leftPos + 201, topPos + 8, 108, 144)) : List.of(tab);
    }

    @Override protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button)
            && getExtraGuiAreas().stream().noneMatch(area -> area.contains((int) mouseX, (int) mouseY));
    }

    private void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF604032);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFCBA67A);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFE5CBA6);
    }

    private void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF715849);
        graphics.fill(x, y, x + 16, y + 16, 0xFFB5957B);
    }

    @Override protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        panel(graphics, x, y, imageWidth, imageHeight);
        graphics.fill(x + 6, y + 18, x + 170, y + 89, 0xFFD6B58E);
        graphics.drawString(font, Component.translatable("gui.pinkkicore.input"), x + 44, y + 31, 0x483427, false);
        graphics.drawString(font, Component.translatable("gui.pinkkicore.output"), x + 116, y + 31, 0x483427, false);
        slot(graphics, x + 44, y + 43);
        slot(graphics, x + 116, y + 43);
        graphics.drawCenteredString(font, "→", x + 88, y + 47, 0x604032);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slot(graphics, x + 8 + col * 18, y + 101 + row * 18);
        }
        for (int col = 0; col < 9; col++) slot(graphics, x + 8 + col * 18, y + 159);
        if (sidesOpen) {
            panel(graphics, x + 201, y + 8, 108, 144);
            graphics.drawString(font, Component.translatable("gui.pinkkicore.sides"),
                x + 206, y + 19, 0x483427, false);
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        updateSideButtons();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, tick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
