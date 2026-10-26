package com.newuniverse.nusmp.skilltree.item;

/**
 * Why: the client setting {@code legacyLook} (in {@code nusmp-skillbook-look-client.toml}, owned by the look builder) brings back the 1.82.0 grey covers and the old rule that every Skill Book glints. The glint
 * is decided in {@link SkillBookItem#isFoil}, which is COMMON code (it runs on a dedicated server too) and must never touch a client config or any client class. So the setting is handed over through this
 * tiny holder: a single volatile boolean that the client's config code sets when the file loads or changes, and that common code only reads. On a dedicated server nobody ever sets it, so it stays
 * false (the rarity glint rule: epic and mythic only), which is right: the glint is a draw-only matter and the server never draws. No Minecraft type, no config class, no per-frame cost beyond one volatile read.
 */
public final class LegacyLook {
    private LegacyLook() {}

    private static volatile boolean legacy;

    /** True when the client wants the old look: every Skill Book glints. Always false on a dedicated server. */
    public static boolean enabled() { return legacy; }

    /** Set by the client config code of the look (never by common code). */
    public static void set(boolean on) { legacy = on; }
}
