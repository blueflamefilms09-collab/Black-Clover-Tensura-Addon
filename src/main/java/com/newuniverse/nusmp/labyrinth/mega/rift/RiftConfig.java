package com.newuniverse.nusmp.labyrinth.mega.rift;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the dimensional rifts and the waypoint (1.72.0), {@code world/serverconfig/nusmp-labyrinth-mega-rift-server.toml}, section {@code [rift]}. Its own file (the pattern of
 * {@code HubConfig}) so a server owner can switch the rifts off, make them rarer or lengthen the pockets without touching another system's file. Every value is a plain define /
 * defineInRange, and every reader goes through {@link #get}, which answers the default while the spec is not loaded.
 */
public final class RiftConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, POCKETS, WAYPOINT;
    public static final ModConfigSpec.DoubleValue SHORTCUT_CHANCE, POCKET_CHANCE, BOSS_SHARE;
    public static final ModConfigSpec.IntValue CHECK_SECONDS, RIFT_SECONDS, JUMP_MIN, JUMP_MAX, MAX_NEAR, POCKET_MINUTES, CRYSTAL_COOLDOWN, GATE_COOLDOWN, PORTAL_SECONDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("rift");
        ENABLED = b.comment("Rifts open by themselves on the floors of the Mega-Dungeon (shortcut and pocket rifts). false = none open on their own (admins can still open them with /multiverse mega rift).")
                .define("enabled", true);
        POCKETS = b.comment("Pocket rifts (an instanced hidden realm with a secret boss or a puzzle vault) may open. false = only shortcut rifts do.").define("pockets", true);
        WAYPOINT = b.comment("The last place a player stood in the dungeon is remembered, and the Return Crystal / Gate Key / Return Portal lead back to it.").define("waypoint", true);
        CHECK_SECONDS = b.comment("Seconds between two rolls for a new rift near a player on a floor.").defineInRange("checkSeconds", 90, 10, 3600);
        SHORTCUT_CHANCE = b.comment("Chance per roll that a shortcut rift opens near the player.").defineInRange("shortcutChance", 0.18, 0.0, 1.0);
        POCKET_CHANCE = b.comment("Chance per roll that a pocket rift opens near the player (rare).").defineInRange("pocketChance", 0.06, 0.0, 1.0);
        BOSS_SHARE = b.comment("Share of pockets that are a secret boss with a vault behind it (the rest are puzzle vaults).").defineInRange("bossShare", 0.5, 0.0, 1.0);
        RIFT_SECONDS = b.comment("Seconds an opened shortcut rift stays open (a pocket rift stays twice as long).").defineInRange("riftSeconds", 240, 30, 3600);
        JUMP_MIN = b.comment("The fewest floors a shortcut jumps.").defineInRange("jumpMin", 3, 1, 10);
        JUMP_MAX = b.comment("The most floors a shortcut jumps.").defineInRange("jumpMax", 5, 1, 10);
        MAX_NEAR = b.comment("Open rifts within 48 blocks of a player at which no further one opens.").defineInRange("maxNear", 1, 1, 6);
        POCKET_MINUTES = b.comment("Minutes before a pocket collapses and throws everyone inside back where they came in.").defineInRange("pocketMinutes", 12, 2, 120);
        CRYSTAL_COOLDOWN = b.comment("Seconds between two uses of the Return Crystal.").defineInRange("crystalCooldown", 300, 10, 7200);
        GATE_COOLDOWN = b.comment("Seconds between two uses of the Gate Key.").defineInRange("gateKeyCooldown", 600, 10, 7200);
        PORTAL_SECONDS = b.comment("Seconds a Return Portal stays open on Floor 0.").defineInRange("portalSeconds", 90, 10, 600);
        b.pop();
        SPEC = b.build();
    }

    private RiftConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
