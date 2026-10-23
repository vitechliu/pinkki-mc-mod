package io.github.vitech.pinkkicore.forge.workbench;

public enum SideMode {
    DISABLED(false, false), INPUT(true, false), OUTPUT(false, true), BOTH(true, true);
    public final boolean input;
    public final boolean output;
    SideMode(boolean input, boolean output) { this.input = input; this.output = output; }
    public String key() { return "gui.pinkkicore.side." + name().toLowerCase(java.util.Locale.ROOT); }
    public static SideMode fromId(int id) { return id >= 0 && id < values().length ? values()[id] : DISABLED; }
}
