package io.github.vitech.pinkkicore.forge.client;

import io.github.vitech.pinkkicore.forge.charging.GuabaChargingMenu;
import io.github.vitech.pinkkicore.forge.workbench.SideMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The machine UI is entirely client-side; configuration changes use vanilla menu buttons. */
public final class GuabaChargingScreen extends AbstractContainerScreen<GuabaChargingMenu> {
    private boolean sidesOpen;
    private Button redstone;
    private final List<Button> sideButtons = new ArrayList<>();
    public GuabaChargingScreen(GuabaChargingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 173;
        inventoryLabelY = 80;
    }
    @Override protected void init() {
        super.init();
        // Keep AbstractContainerScreen's centering of the main panel. JEI uses
        // getExtraGuiAreas() to avoid the tabs/drawer without shifting the machine.
        sideButtons.clear();
        redstone = addRenderableWidget(Button.builder(Component.literal("R"), b -> send(0))
            .bounds(leftPos+178, topPos+8, 22, 22).build());
        addRenderableWidget(Button.builder(Component.literal("≡"), b -> sidesOpen = !sidesOpen)
            .bounds(leftPos+178, topPos+32, 22, 22)
            .tooltip(Tooltip.create(Component.translatable("gui.pinkkicore.sides"))).build());
        for (Direction direction : Direction.values()) {
            int index = direction.ordinal();
            sideButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> send(10+index))
                .bounds(leftPos+204, topPos+52+index*19, 102, 18).build()));
        }
        updateButtons();
    }
    private void send(int button) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }
    /** Screen coordinates for optional recipe-viewer overlays; contains no JEI dependency. */
    public List<Rect2i> getExtraGuiAreas() {
        Rect2i tabs = new Rect2i(leftPos+178, topPos+8, 22, 46);
        return sidesOpen ? List.of(tabs, new Rect2i(leftPos+201, topPos+32, 108, 137)) : List.of(tabs);
    }
    @Override protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        boolean main = mouseX >= leftPos && mouseX < leftPos+imageWidth && mouseY >= topPos && mouseY < topPos+imageHeight;
        boolean tabs = mouseX >= leftPos+178 && mouseX < leftPos+200 && mouseY >= topPos+8 && mouseY < topPos+54;
        boolean drawer = sidesOpen && mouseX >= leftPos+201 && mouseX < leftPos+309 && mouseY >= topPos+32 && mouseY < topPos+169;
        return !(main || tabs || drawer);
    }
    private void updateButtons() {
        redstone.setTooltip(Tooltip.create(Component.translatable("gui.pinkkicore.redstone.tooltip",
            Component.translatable("gui.pinkkicore.redstone."+menu.redstoneMode()),
            Component.translatable(menu.powered() ? "gui.pinkkicore.signal.on" : "gui.pinkkicore.signal.off"))));
        for (Direction direction : Direction.values()) {
            Button button = sideButtons.get(direction.ordinal());
            button.visible = sidesOpen;
            button.active = sidesOpen;
            button.setMessage(Component.translatable("gui.pinkkicore.side.label",
                Component.translatable("gui.pinkkicore.direction."+direction.getName()),
                Component.translatable(SideMode.fromId(menu.sideMode(direction.ordinal())).key())));
            button.setTooltip(Tooltip.create(Component.translatable("gui.pinkkicore.sides.tooltip")));
        }
    }
    private void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x+w, y+h, 0xFF654735);
        graphics.fill(x+1, y+1, x+w-1, y+h-1, 0xFFD5C0A2);
        graphics.fill(x+2, y+2, x+w-2, y+h-2, 0xFFE7D8BE);
    }
    private void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x-1, y-1, x+17, y+17, 0xFF6E5746);
        graphics.fill(x, y, x+17, y+17, 0xFFF8EAD1);
        graphics.fill(x, y, x+16, y+16, 0xFFB6A18B);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        panel(graphics, x, y, imageWidth, imageHeight);
        graphics.fill(x+6, y+17, x+170, y+77, 0xFFC5AC8C);
        slot(graphics, x+44, y+53); slot(graphics, x+116, y+53);
        graphics.drawCenteredString(font, Component.translatable("gui.pinkkicore.charging"), x+88, y+24, 0xFF28687C);
        graphics.drawString(font, Component.translatable("gui.pinkkicore.input"), x+44, y+42, 0xFF483427, false);
        graphics.drawString(font, Component.translatable("gui.pinkkicore.output"), x+116, y+42, 0xFF483427, false);
        // A solid arrow fills over twenty server ticks.
        graphics.fill(x+77, y+59, x+99, y+64, 0xFF806954);
        graphics.fill(x+95, y+55, x+99, y+68, 0xFF806954);
        graphics.fill(x+99, y+58, x+102, y+65, 0xFF806954);
        int progress = Math.min(25, menu.progress()*25/20);
        graphics.fill(x+77, y+59, x+77+progress, y+64, 0xFF52C8F2);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) slot(graphics, x+8+18*col, y+91+18*row);
        for (int col = 0; col < 9; col++) slot(graphics, x+8+18*col, y+149);
        if (sidesOpen) {
            panel(graphics, x+201, y+32, 108, 137);
            graphics.drawString(font, Component.translatable("gui.pinkkicore.sides"), x+206, y+39, 0xFF483427, false);
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.renderItem(new ItemStack(Items.REDSTONE), leftPos+181, topPos+11);
        if (isHovering(76, 52, 28, 20, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.pinkkicore.status."+menu.status()), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }
}
