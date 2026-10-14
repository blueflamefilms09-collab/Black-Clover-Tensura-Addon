package com.newuniverse.nusmp.client.relics.fx;

import java.util.Locale;

/**
 * The client option {@code relicfx.quality} (config/nusmp-relicfx-client.toml): how much the worn relics add on top of their models. Pure (no Minecraft types),
 * so the budgets are tested. {@code vertexCap} is the most vertices the worn models and their effects may put on screen for one player in one frame
 * (the owner's worst case is about 6000); {@code particleRate} scales how often the ambient particles spawn; {@code particlesPerTick} is the cap on
 * particles all wearers together may spawn in one client tick.
 */
public enum FxQuality {
    LOW(2400, 0.4f, 6),
    MEDIUM(4200, 1.0f, 16),
    HIGH(6000, 1.6f, 32);

    public final int vertexCap;
    public final float particleRate;
    public final int particlesPerTick;

    FxQuality(int vertexCap, float particleRate, int particlesPerTick) {
        this.vertexCap = vertexCap;
        this.particleRate = particleRate;
        this.particlesPerTick = particlesPerTick;
    }

    /** "low", "Medium", "HIGH" in any case; the fallback for anything else. */
    public static FxQuality byName(String name, FxQuality fallback) {
        if (name == null) return fallback;
        String s = name.trim().toUpperCase(Locale.ROOT);
        for (FxQuality q : values()) if (q.name().equals(s)) return q;
        return fallback;
    }
}
