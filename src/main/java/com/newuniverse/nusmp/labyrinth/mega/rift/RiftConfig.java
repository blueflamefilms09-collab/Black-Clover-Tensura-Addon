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
    /** 1.99.0 (the exclusive tombs): the mega-rare switches, the one-in-N, the pity, the depth and luck weights, the lifetimes, the cap, the build speed. */
    public static final ModConfigSpec.BooleanValue MEGA_ENABLED, MEGA_BROADCAST, KEEP_ITEMS, TRAPS_ON, GRAVITY_ON;
    public static final ModConfigSpec.DoubleValue MEGA_ONE_IN, DEPTH_BOOST, LUCK_STEP;
    public static final ModConfigSpec.IntValue PITY_START, PITY_HARD, MEGA_SECONDS, EXCLUSIVE_MINUTES, EXCLUSIVE_MAX, BUILD_PER_TICK;
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
        b.push("mega");
        MEGA_ENABLED = b.comment("Mega-rare rifts: a very rare rift leads to an EXCLUSIVE pocket dungeon (own biome, own monsters, a mega-boss, relics that drop nowhere else). false = none open on their own (admins can still make them).")
                .define("enabled", true);
        MEGA_ONE_IN = b.comment("On average one rift in this many is mega-rare (at floor 1, Luck 0, before the pity). The least is 20.").defineInRange("oneIn", 250.0, 20.0, 100000.0);
        PITY_START = b.comment("A player's pity counts the rifts that opened near them since their last mega-rare one; past this many the chance climbs.").defineInRange("pityStart", 150, 0, 100000);
        PITY_HARD = b.comment("At this many rifts without a mega-rare one the next is certain.").defineInRange("pityHard", 600, 1, 100000);
        DEPTH_BOOST = b.comment("Extra multiple of the chance at floor 1000 (1.0 = twice as likely; 0 = depth does not matter).").defineInRange("depthBoost", 1.0, 0.0, 10.0);
        LUCK_STEP = b.comment("Change of the chance per point of the Luck attribute (never below half or above three times).").defineInRange("luckStep", 0.08, 0.0, 0.5);
        MEGA_SECONDS = b.comment("Seconds an opened mega-rare rift stays open.").defineInRange("riftSeconds", 600, 60, 7200);
        EXCLUSIVE_MINUTES = b.comment("Minutes before an exclusive tomb collapses by itself and throws everyone inside back where they came in.").defineInRange("tombMinutes", 25, 5, 240);
        EXCLUSIVE_MAX = b.comment("Exclusive tombs that may stand at once on the server (each is a region of about 170 x 170 blocks that is built when someone enters and cleared when it collapses).").defineInRange("maxTombs", 4, 1, 32);
        BUILD_PER_TICK = b.comment("Blocks of a tomb the builder places per tick while it forms (lower = gentler on the server, slower to form).").defineInRange("buildPerTick", 2500, 200, 50000);
        MEGA_BROADCAST = b.comment("Announce to the whole server when a mega-rare rift opens (the kingdoms race for the tomb).").define("broadcast", true);
        KEEP_ITEMS = b.comment("A delver who dies inside a tomb keeps their items (they are given back when they return); the tomb is cleared whatever happens, so nothing they dropped could be fetched.").define("keepItemsOnDeath", true);
        TRAPS_ON = b.comment("The elemental traps of the tombs fire on contact. false = the tiles are only floor.").define("traps", true);
        GRAVITY_ON = b.comment("The gravity zones of the tombs (ceiling walkers, weightless rooms). false = normal gravity everywhere.").define("gravityZones", true);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private RiftConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
