package io.github.vitech.pinkkicore.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** Drawing adapter for the 1.19.1 PoseStack GUI API. Client-only. */
public final class LegacyGraphics  {
    private final PoseStack pose;
    private final Minecraft minecraft = Minecraft.getInstance();
    public LegacyGraphics(PoseStack pose) { this.pose = pose; }
    public void fill(int x1, int y1, int x2, int y2, int color) { GuiComponent.fill(pose, x1, y1, x2, y2, color); }
    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) font.drawShadow(pose, text, x, y, color); else font.draw(pose, text, x, y, color);
    }
    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        drawString(font, text.getVisualOrderText(), x, y, color, shadow);
    }
    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if (shadow) font.drawShadow(pose, text, x, y, color); else font.draw(pose, text, x, y, color);
    }
    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        drawString(font, text, x-font.width(text)/2, y, color, true);
    }
    public void drawWordWrap(Font font, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, width)) { drawString(font, line, x, y, color, false); y += font.lineHeight; }
    }
    public void renderItem(ItemStack stack, int x, int y) { minecraft.getItemRenderer().renderAndDecorateItem(stack, x, y); }
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) { minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x, y); }
    public void renderTooltip(Font font, ItemStack stack, int x, int y) {
        if (minecraft.screen != null) minecraft.screen.renderTooltip(pose, minecraft.screen.getTooltipFromItem(stack), java.util.Optional.empty(), x, y);
    }
    public void renderTooltip(Font font, Component text, int x, int y) {
        if (minecraft.screen != null) minecraft.screen.renderTooltip(pose, text, x, y);
    }
    public void enableScissor(int x1, int y1, int x2, int y2) {
        double scale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor((int)(x1*scale), (int)(minecraft.getWindow().getHeight()-y2*scale),
            (int)((x2-x1)*scale), (int)((y2-y1)*scale));
    }
    public void disableScissor() { RenderSystem.disableScissor(); }
}
