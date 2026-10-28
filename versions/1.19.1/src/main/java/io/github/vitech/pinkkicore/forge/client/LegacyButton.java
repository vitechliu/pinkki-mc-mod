package io.github.vitech.pinkkicore.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Button construction and dynamic hover hints for the legacy GUI. */
public final class LegacyButton extends Button {
    private Component tooltip;
    private LegacyButton(int x, int y, int width, int height, Component text, OnPress press) {
        super(x, y, width, height, text, press);
    }
    public void setTooltip(Component tooltip) { this.tooltip = tooltip; }
    @Override public void renderToolTip(PoseStack pose, int x, int y) {
        var screen = Minecraft.getInstance().screen;
        if (tooltip != null && screen != null) screen.renderTooltip(pose, tooltip, x, y);
    }
    public static Builder builder(Component text, OnPress press) { return new Builder(text, press); }
    public static final class Builder {
        private final Component text;
        private final OnPress press;
        private int x, y, width, height;
        private Component tooltip;
        private Builder(Component text, OnPress press) { this.text=text; this.press=press; }
        public Builder bounds(int x, int y, int width, int height) { this.x=x; this.y=y; this.width=width; this.height=height; return this; }
        public Builder tooltip(Component tooltip) { this.tooltip=tooltip; return this; }
        public LegacyButton build() { var button=new LegacyButton(x,y,width,height,text,press); button.setTooltip(tooltip); return button; }
    }
}
