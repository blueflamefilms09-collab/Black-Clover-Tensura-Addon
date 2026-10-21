package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: every number of the anti-speedrun layer is a taste the owner will want to tune after the first play: which share of the halls lock, how deep into the room a
 * player must stand before it closes, how fast is "too fast", how long the dungeon waits between two ambushes. So none is in code:
 * {@code world/serverconfig/nusmp-labyrinth-mega-lockdown-server.toml}, sections {@code [lockdown]} and {@code [ambush]}. Registered by the class itself from
 * {@link LockdownRegistry#init} through the mod container that is active while the mod is being constructed (the pattern of the escape rules' config), so the mod class
 * gains no line. The two switches are independent: {@code lockdown.enabled = false} and {@code ambush.enabled = false} each return the dungeon to how it was. Plain
 * {@code define} / {@code defineInRange}, never {@code defineInList}; every reader answers its default while the spec is not loaded yet.
 */
public final class LockdownConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, FOR_CREATIVE, ROOM_DATA, AMBUSH_ENABLED, AMBUSH_LEFT, AMBUSH_SPEED, AMBUSH_TELEPORT, AMBUSH_ESCAPE;
    public static final ModConfigSpec.IntValue ENTRY_INSET, COOLDOWN_SECONDS, WALL_DEPTH, OPEN_HEIGHT, MAX_CELLS, MAX_PER_OPENING, MAX_OPENINGS, WAVE_PAUSE_TICKS,
            AMBUSH_COOLDOWN_SECONDS, AMBUSH_BUDGET, AMBUSH_BUDGET_MINUTES, AMBUSH_CROWD, AMBUSH_MIN_LIVE, AMBUSH_WINDOW_TICKS;
    public static final ModConfigSpec.DoubleValue AMBUSH_THRESHOLD, AMBUSH_DECAY, AMBUSH_SPEED_LIMIT, AMBUSH_TELEPORT_BLOCKS, AMBUSH_MIN_DIST, AMBUSH_MAX_DIST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("lockdown");
        ENABLED = b.comment("Room lockdown: a key challenge room closes its doorways with unbreakable barriers when a party is well inside, spawns its wave, and opens when the wave is dead. "
                        + "false = rooms never lock (the trial chambers, which are templates of their own, are not affected).")
                .define("enabled", true);
        FOR_CREATIVE = b.comment("true = players in Creative mode also lock a room (a tester wants this). false = Creative and Spectator players never do.")
                .define("forCreative", false);
        ROOM_DATA = b.comment("Which rooms lock is read from data/nusmp/mega/lockdown/rooms.json. false = only the built-in minimum applies: treasure and set-piece rooms lock, nothing else.")
                .define("roomData", true);
        ENTRY_INSET = b.comment("How many blocks inside the room's box a player must stand before it closes (the wall layers are two thick, so 4 is two blocks into the room proper).")
                .defineInRange("entryInset", 4, 2, 12);
        COOLDOWN_SECONDS = b.comment("Seconds before a room that was opened by the escape rules (not cleared) may lock again.")
                .defineInRange("cooldownSeconds", 120, 10, 3600);
        WALL_DEPTH = b.comment("Which layer of the wall is closed, counted from the outside of the room's box (0 = the outermost). The rooms of the dungeon have two wall layers, so 1 is the one next to the inside.")
                .defineInRange("wallDepth", 1, 0, 4);
        OPEN_HEIGHT = b.comment("How many blocks above the floor a doorway is looked for. A door of the clearance rule is 7 high; the extra blocks catch an arch.")
                .defineInRange("openHeight", 9, 4, 14);
        MAX_CELLS = b.comment("A room whose openings add up to more than this many wall cells is an open-plan room and does not lock.")
                .defineInRange("maxCells", 320, 16, 2000);
        MAX_PER_OPENING = b.comment("An opening bigger than this many cells is not a doorway; the room does not lock.")
                .defineInRange("maxPerOpening", 96, 8, 500);
        MAX_OPENINGS = b.comment("A room with more openings than this does not lock.")
                .defineInRange("maxOpenings", 12, 1, 40);
        WAVE_PAUSE_TICKS = b.comment("Ticks between the last monster of a wave dying and the next wave appearing.")
                .defineInRange("wavePauseTicks", 60, 0, 600);
        b.pop();
        b.push("ambush");
        AMBUSH_ENABLED = b.comment("The dynamic ambush: a player who bypasses a room (leaves it with its wave alive, runs through the corridors too fast, teleports past, or escapes a lockdown) "
                        + "finds a squad waiting in the way ahead. false = no ambush.")
                .define("enabled", true);
        AMBUSH_LEFT = b.comment("Count leaving a room with its monsters still alive as a bypass.").define("leftLiveRoom", true);
        AMBUSH_SPEED = b.comment("Count running through the corridors faster than speedLimit as a bypass.").define("fastTravel", true);
        AMBUSH_TELEPORT = b.comment("Count a teleport of teleportBlocks or more as a bypass.").define("teleport", true);
        AMBUSH_ESCAPE = b.comment("Count escaping a lockdown by the release rune or the timers as a bypass.").define("escapedLockdown", true);
        AMBUSH_THRESHOLD = b.comment("Heat an ambush needs (a room left with live monsters is worth about 4, a second of fast travel 1.5, a teleport 4, an escaped lockdown 5).")
                .defineInRange("heatThreshold", 6.0, 1.0, 100.0);
        AMBUSH_DECAY = b.comment("Heat lost per second. 0.25 = a full threshold cools in 24 seconds.")
                .defineInRange("heatDecayPerSecond", 0.25, 0.0, 20.0);
        AMBUSH_SPEED_LIMIT = b.comment("Blocks per second, sustained over three seconds, above which travel counts as running past (a sprint is about 5.6, a sprint jump 7, Speed II about 8).")
                .defineInRange("speedLimit", 8.5, 6.0, 60.0);
        AMBUSH_TELEPORT_BLOCKS = b.comment("A jump of this many blocks between two samples (half a second apart) is a teleport.")
                .defineInRange("teleportBlocks", 24.0, 8.0, 512.0);
        AMBUSH_MIN_LIVE = b.comment("A room counts as left with its wave alive when at least this many of the Dungeon Master's or the lockdown's monsters are alive in it.")
                .defineInRange("minLiveMobs", 2, 1, 64);
        AMBUSH_COOLDOWN_SECONDS = b.comment("Seconds a player is left alone after an ambush fired for them.")
                .defineInRange("cooldownSeconds", 90, 5, 3600);
        AMBUSH_BUDGET = b.comment("At most this many ambushes per player in the budget window.")
                .defineInRange("budget", 3, 1, 20);
        AMBUSH_BUDGET_MINUTES = b.comment("The budget window in minutes.")
                .defineInRange("budgetMinutes", 10, 1, 200);
        AMBUSH_CROWD = b.comment("No ambush while this many of the Dungeon Master's monsters already live within 64 blocks of the party.")
                .defineInRange("crowdCap", 8, 1, 64);
        AMBUSH_MIN_DIST = b.comment("The nearest, in blocks, that the squad may stand ahead of the player.")
                .defineInRange("minDistance", 14.0, 8.0, 64.0);
        AMBUSH_MAX_DIST = b.comment("The farthest, in blocks, that the squad may stand ahead of the player.")
                .defineInRange("maxDistance", 40.0, 16.0, 128.0);
        AMBUSH_WINDOW_TICKS = b.comment("The window, in ticks, over which the speed is measured.")
                .defineInRange("speedWindowTicks", 60, 20, 400);
        b.pop();
        SPEC = b.build();
    }

    private LockdownConfig() {}

    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean ambushEnabled() { return get(AMBUSH_ENABLED); }

    public static OpeningScan.Limits limits() { return new OpeningScan.Limits(get(MAX_CELLS), get(MAX_PER_OPENING), get(MAX_OPENINGS)); }

    public static AmbushRules.Config ambush() {
        AmbushRules.Config d = AmbushRules.Config.DEFAULT;
        return new AmbushRules.Config(get(AMBUSH_THRESHOLD), get(AMBUSH_DECAY), d.leftBase(), d.leftPerMob(), d.fastPerSecond(), d.teleport(), d.escaped(), get(AMBUSH_SPEED_LIMIT),
                get(AMBUSH_WINDOW_TICKS), get(AMBUSH_TELEPORT_BLOCKS), get(AMBUSH_MIN_LIVE), get(AMBUSH_COOLDOWN_SECONDS) * 20L, get(AMBUSH_BUDGET),
                get(AMBUSH_BUDGET_MINUTES) * 1200L, get(AMBUSH_CROWD), get(AMBUSH_BUDGET_MINUTES) * 1200L).clamped();
    }

    /** Registers the spec; called once from {@link LockdownRegistry#init} while the mod is being constructed. Never throws: a failure leaves the defaults in force. */
    static void register() {
        try {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            container.registerConfig(ModConfig.Type.SERVER, SPEC, "nusmp-labyrinth-mega-lockdown-server.toml");
        } catch (RuntimeException | LinkageError e) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The lockdown config could not be registered ({}); the defaults apply", e.toString());
        }
    }
}
