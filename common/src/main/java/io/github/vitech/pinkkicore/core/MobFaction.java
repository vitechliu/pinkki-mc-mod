package io.github.vitech.pinkkicore.core;

/** Baseline attitude to players. Special rivalries are separate from faction identity. */
public enum MobFaction {
    HOSTILE("hostile"),
    NEUTRAL("neutral"),
    SPECIAL("special");

    private final String id;
    MobFaction(String id) { this.id = id; }
    public String id() { return id; }
}
