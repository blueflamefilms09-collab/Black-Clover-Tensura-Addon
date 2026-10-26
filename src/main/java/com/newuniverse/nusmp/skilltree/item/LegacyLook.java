package com.newuniverse.nusmp.skilltree.item;

/**
 * Why: the client setting {@code legacyLook} (nusmp-skillbook-look-client.toml, default false) brings back the 1.82.0 grey covers, the old sprite and the old rule that every Skill Book glints. The setting
 * itself lives in a client class, but the answer has to be readable from common code too (the item's glint rule in {@link SkillBookItem#isFoil}, which also runs on a dedicated server), so it is mirrored
 * here: one volatile boolean, written by the client config's load / reload handler and nothing else, read for free. On a dedicated server nothing writes it, so it stays false (the new look and the rarity
 * glint rule: epic and mythic only), which is right because a server draws nothing. No Minecraft type, no client import.
 */
public final class LegacyLook {
    private LegacyLook() {}

    /** True when this client wants the old look. Written only by {@code client.skilltree.BookLookConfig} (through {@link #set}). */
    public static volatile boolean legacy;

    /** True when the client wants the old look: every Skill Book glints. Always false on a dedicated server. */
    public static boolean enabled() { return legacy; }

    public static boolean isLegacy() { return legacy; }

    /** Set by the client config code of the look (never by common code). */
    public static void set(boolean on) { legacy = on; }

    /** The value of the item property {@code nusmp:look}: 0 the new look, 1 the legacy look (the models of 1.82.0). */
    public static float lookValue() { return legacy ? 1f : 0f; }
}
