package com.newuniverse.nusmp.fairytail;

/**
 * Why: each line of magic draws with the mod's existing effect trios (cast / zone / burst); the names are kept as text so this table is
 * Minecraft-free and testable, and {@link FtCast} resolves them against {@code VfxShape} (a name that no longer exists falls back to a
 * magic circle, so an effect can never break a cast).
 */
public enum FtElement {
    FIRE("FLAMEFX_CAST", "FLAMEFX_ZONE", "FLAMEFX_BURST", 0xFFFF7A2A),
    ICE("ICE_FX1", "ICE_FX2", "ICE_FX3", 0xFF9FE4FF),
    SKY("AIR_FX1", "AIR_FX2", "AIR_FX3", 0xFFBFF5E8),
    LIGHTNING("LIGHTNINGFX_CAST", "LIGHTNINGFX_ZONE", "LIGHTNINGFX_BURST", 0xFFFFE45A),
    IRON("IRON_FX1", "IRON_FX2", "IRON_FX3", 0xFFC9CED6),
    CRASH("GRAVITY_IMPACT", "GRAVITY_WELL", "SPACE_SHATTER", 0xFFB06CFF),
    WATER("WATERFX_CAST", "WATERFX_ZONE", "WATERFX_BURST", 0xFF4FA8FF);

    public final String cast, zone, burst;
    public final int argb;

    FtElement(String cast, String zone, String burst, int argb) {
        this.cast = cast; this.zone = zone; this.burst = burst; this.argb = argb;
    }
}
