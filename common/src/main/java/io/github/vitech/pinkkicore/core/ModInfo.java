package io.github.vitech.pinkkicore.core;

/** Shared Java 17 code: keep Minecraft, Forge and client-only imports out of this layer. */
public final class ModInfo {
    public static final String ID = "pinkkicore";

    private ModInfo() {}

    public static String startupMessage() {
        return "[pinkkicore] Mod loaded successfully.";
    }
}
