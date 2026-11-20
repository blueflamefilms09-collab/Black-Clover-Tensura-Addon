package com.newuniverse.nusmp.arc.realm;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the settings of the boss realm, {@code nusmp-boss-realm-server.toml}, section {@code [bossRealm]}. They all change gameplay, so they are SERVER settings (rule 9), every one is a plain
 * {@code define} / {@code defineInRange} (a list-validated value throws while the server is loading) with a hard range, and every reader goes through {@link #get}, which answers the default while
 * the spec is not loaded yet. {@code enabled = false} restores the old behaviour exactly: a boss is made where the door is (egg, command) and the story arenas keep their old shapes.
 */
public final class BossRealmConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue PARTY_CAP, MAX_ACTIVE_SITES, EMPTY_GRACE_SECONDS, RETURN_SECONDS, FIGHT_TIMEOUT_MINUTES, DOOR_COOLDOWN_SECONDS;
    public static final ModConfigSpec.DoubleValue PARTY_RADIUS;

    public static final int DEFAULT_PARTY_CAP = 6, DEFAULT_MAX_SITES = 4, DEFAULT_GRACE = 20, DEFAULT_RETURN = 45, DEFAULT_TIMEOUT = 30, DEFAULT_DOOR_COOLDOWN = 5;
    public static final double DEFAULT_PARTY_RADIUS = 16.0;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("bossRealm");
        ENABLED = b.comment("Bosses that are not dungeon bosses (Goku, Sinbad) are fought in their own arena of the story-arena dimension: the spawn egg and the summon commands carry the party there. False: the old behaviour, the boss is made where the door is.")
                .define("enabled", true);
        PARTY_CAP = b.comment("Most players carried in one door (the one who uses it and the companions nearby).")
                .defineInRange("partyCap", DEFAULT_PARTY_CAP, 1, 16);
        PARTY_RADIUS = b.comment("Blocks round the one who uses the door within which companions are carried along.")
                .defineInRange("partyRadius", DEFAULT_PARTY_RADIUS, 1.0, 64.0);
        MAX_ACTIVE_SITES = b.comment("Most realm arenas that may hold people at the same time (a hard cap on the fights and on the arena builds).")
                .defineInRange("maxActiveSites", DEFAULT_MAX_SITES, 1, 16);
        EMPTY_GRACE_SECONDS = b.comment("Seconds an arena with a fight may stand empty (everyone dead, gone or disconnected) before the boss withdraws. The story arenas use 2 seconds; a realm waits longer.")
                .defineInRange("emptyGraceSeconds", DEFAULT_GRACE, 0, 120);
        RETURN_SECONDS = b.comment("Seconds after a fight ends (won, withdrawn or timed out) before the people still in the arena are carried home. 0 = at once.")
                .defineInRange("returnSeconds", DEFAULT_RETURN, 0, 600);
        FIGHT_TIMEOUT_MINUTES = b.comment("Longest a realm fight may last; then the boss withdraws and everyone is carried home. 0 = no limit.")
                .defineInRange("fightTimeoutMinutes", DEFAULT_TIMEOUT, 0, 240);
        DOOR_COOLDOWN_SECONDS = b.comment("Seconds a player must wait between two uses of a door (stops a spam of travels).")
                .defineInRange("doorCooldownSeconds", DEFAULT_DOOR_COOLDOWN, 0, 120);
        b.pop();
        SPEC = b.build();
    }

    private BossRealmConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static int partyCap() { return get(PARTY_CAP); }

    public static double partyRadius() { return get(PARTY_RADIUS); }

    public static int maxActiveSites() { return get(MAX_ACTIVE_SITES); }

    public static long graceTicks() { return BossRealmRules.ticks(get(EMPTY_GRACE_SECONDS), 20); }

    public static long returnTicks() { return BossRealmRules.ticks(get(RETURN_SECONDS), 20); }

    public static long timeoutTicks() { return BossRealmRules.ticks(get(FIGHT_TIMEOUT_MINUTES), 1200); }

    public static long doorCooldownTicks() { return BossRealmRules.ticks(get(DOOR_COOLDOWN_SECONDS), 20); }
}
