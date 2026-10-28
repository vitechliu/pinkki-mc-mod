package io.github.vitech.pinkkicore.forge.workbench;

public enum SideMode {
    // Append new modes: existing ordinal IDs 0..3 are saved in worlds.
    DISABLED(false, false), INPUT(true, false), OUTPUT(false, true), BOTH(true, true), GUABA_INPUT(false, false);
    public final boolean input;
    public final boolean output;
    SideMode(boolean input, boolean output) { this.input = input; this.output = output; }
    public boolean canInsertSlot(int slot) {
        return input && (slot == GuabaWorkbenchEntity.CATALYST || slot == GuabaWorkbenchEntity.INPUT)
            || this == GUABA_INPUT && slot == GuabaWorkbenchEntity.CATALYST;
    }
    public SideMode next() { return values()[(ordinal()+1)%values().length]; }
    public String key() { return "gui.pinkkicore.side." + name().toLowerCase(java.util.Locale.ROOT); }
    public static SideMode fromId(int id) { return id >= 0 && id < values().length ? values()[id] : DISABLED; }
}
