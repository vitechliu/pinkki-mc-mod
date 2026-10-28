package io.github.vitech.pinkkicore.forge.workbench;

public enum RedstoneMode {
    IGNORE, HIGH, LOW, NEVER;
    public boolean allows(boolean powered) {
        return switch (this) { case IGNORE -> true; case HIGH -> powered; case LOW -> !powered; case NEVER -> false; };
    }
    public static RedstoneMode fromId(int id) { return id >= 0 && id < values().length ? values()[id] : IGNORE; }
}
