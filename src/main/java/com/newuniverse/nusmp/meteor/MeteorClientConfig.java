package com.newuniverse.nusmp.meteor;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config of the meteor's look: {@code nusmp-meteor-client.toml} (docs/meteor_fall.md). Only what the trail looks like; nothing here changes the game. The particles go through
 * the client particle budget ({@code clientParticlesPerTick}) and only appear within {@link MeteorRules#TRAIL_DISTANCE} blocks of the player. Reads fall back to HIGH before the config is loaded.
 */
public final class MeteorClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Trail> TRAIL;

    /** How many trail particles one meteor makes a tick (the rock, its tail model and the glow are always drawn). */
    public enum Trail {
        OFF(0), LOW(2), HIGH(5);

        public final int perTick;

        Trail(int perTick) { this.perTick = perTick; }
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("meteor");
        TRAIL = b.comment("The particle trail behind a falling meteor: OFF (none), LOW (2 particles a tick), HIGH (5 a tick, default). The meteor model and its tail are drawn either way.").defineEnum("trailQuality", Trail.HIGH);
        b.pop();
        SPEC = b.build();
    }

    private MeteorClientConfig() {}

    public static int particlesPerTick() {
        try { return TRAIL.get().perTick; } catch (IllegalStateException unloaded) { return Trail.HIGH.perTick; }
    }
}
