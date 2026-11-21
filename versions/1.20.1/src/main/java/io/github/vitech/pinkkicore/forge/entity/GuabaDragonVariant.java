package io.github.vitech.pinkkicore.forge.entity;

import net.minecraft.network.chat.Component;

public enum GuabaDragonVariant {
    NORMAL("guaba_dragon"),
    PINK("pink_guaba_dragon"),
    THUNDER("thunder_guaba_dragon"),
    CLOUD("cloud_guaba_dragon"),
    NETHER("nether_guaba_dragon");

    private final String id;

    GuabaDragonVariant(String id) { this.id = id; }

    public String id() { return id; }
    public String eggId() { return id + "_egg"; }
    public Component displayName() { return Component.translatable("entity.pinkkicore.pet_" + id); }

    static GuabaDragonVariant byId(String id) {
        for (GuabaDragonVariant variant : values()) {
            if (variant.id.equals(id)) return variant;
        }
        return NORMAL;
    }
}
