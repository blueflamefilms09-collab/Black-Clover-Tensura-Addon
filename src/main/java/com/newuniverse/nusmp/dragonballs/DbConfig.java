package com.newuniverse.nusmp.dragonballs;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Server config of the Dragon Balls: {@code serverconfig/nusmp-dragonballs-server.toml}. Every setting changes gameplay, so every one is a SERVER setting. Reads go through the small accessors below,
 * which answer the default while the config is not loaded (a self-test or a unit test), so nothing ever throws. Ranges are the caps the rules allow ({@link DbRules}, {@link WishRules}).
 */
public final class DbConfig {
    private DbConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, AUTO_SCATTER, SAIYAN_WISH;
    private static final ModConfigSpec.IntValue COUNT, EARTH_WISHES, SUPER_WISHES, LOCK_MINUTES, SESSION_SECONDS, GATHER_RADIUS, SCATTER_MIN, SCATTER_MAX, SCATTER_SEP, RADAR_RANGE, POWER_CAP, REVIVE_MINUTES, WINDOW_SECONDS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EARTH_PHRASES, SUPER_PHRASES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("dragonBalls");
        ENABLED = b.comment("The Dragon Ball wish system (balls, summoning, wishes). false = the balls stay in the game but nothing can be summoned.").define("enabled", true);
        COUNT = b.comment("Balls per set. CANON IS 7 (Earth and Super Dragon Balls are both seven). Set 8 for the eight-ball version the owner asked for: the 8-star ball joins both sets, and a summoning then needs all eight. Changing it re-scatters nothing by itself: run /multiverse dragonballs scatter.")
                .defineInRange("count", DbRules.CANON_COUNT, DbRules.MIN_COUNT, DbRules.MAX_COUNT);
        EARTH_WISHES = b.comment("Wishes Shenron grants per summoning (the owner's brief: one).").defineInRange("earthWishes", BallSet.EARTH.defaultWishes(), 1, WishRules.MAX_WISHES);
        SUPER_WISHES = b.comment("Wishes Super Shenron grants per summoning (the owner's brief: two; canon Super Shenron grants one).").defineInRange("superWishes", BallSet.SUPER.defaultWishes(), 1, WishRules.MAX_WISHES);
        LOCK_MINUTES = b.comment("After a wish was granted the balls of that set turn to stone and cannot summon for this many REAL minutes (canon: a year; default one real hour). 0 = no lock.")
                .defineInRange("lockMinutes", 60, 0, (int) DbRules.MAX_LOCK_MINUTES);
        SESSION_SECONDS = b.comment("Shenron leaves, with a farewell, after this many seconds if the wishes are not all made (default 300 = five minutes).").defineInRange("sessionSeconds", 300, 60, 1800);
        GATHER_RADIUS = b.comment("Balls lying on the ground within this many blocks of the speaker count for the ritual, together with the ones in the inventory.").defineInRange("gatherRadius", 6, 2, 16);
        WINDOW_SECONDS = b.comment("The words may be spoken over several chat lines inside this many seconds.").defineInRange("phraseWindowSeconds", 10, 3, 30);
        AUTO_SCATTER = b.comment("Scatter the balls by themselves the first time the server runs with none placed. false = only /multiverse dragonballs scatter places them.").define("autoScatter", true);
        SCATTER_MIN = b.comment("Closest a ball may be placed to the world spawn, in blocks.").defineInRange("scatterMinRadius", 300, 0, 20000);
        SCATTER_MAX = b.comment("Farthest a ball may be placed from the world spawn, in blocks.").defineInRange("scatterMaxRadius", 3000, 100, 30000);
        SCATTER_SEP = b.comment("Balls are placed at least this far from each other (relaxed only if the ring is too small for it).").defineInRange("scatterMinSeparation", 500, 0, 5000);
        RADAR_RANGE = b.comment("The Dragon Radar finds the nearest ball within this many blocks.").defineInRange("radarRange", 2000, 100, 20000);
        POWER_CAP = b.comment("Lifetime ceiling of the permanent power wish, in levels (one level = +1 heart and +0.5 attack damage; Earth gives 1 level per wish, Super 2).").defineInRange("powerCapLevels", WishRules.POWER_CAP_DEFAULT, 1, WishRules.POWER_CAP_MAX);
        REVIVE_MINUTES = b.comment("The revive wish reaches a player who died at most this many minutes ago.").defineInRange("reviveMinutes", 60, 1, 1440);
        SAIYAN_WISH = b.comment("Allow the 'turn me into a Saiyan' wish (it calls the Saiyan race switch). false = the wish is refused.").define("saiyanWish", true);
        EARTH_PHRASES = b.comment("The words that call Shenron (whole words, case and punctuation ignored). Canon: the Namekian words are an assumption of the owner (the wiki gives the Porunga words 'Takkaraputo Popurunga Pupirittoparo'); the Japanese call is 'Ideyo Shenron'.")
                .defineListAllowEmpty("earthPhrases", BallSet.EARTH.defaultPhrases(), () -> "", o -> o instanceof String);
        SUPER_PHRASES = b.comment("The words that call Super Shenron. English wiki (Super Shenron): 'Come forth, Dragon of the Gods, and grant my wish pretty peas!' ('Divine Dragon' / 'peas and carrots' in the dub).")
                .defineListAllowEmpty("superPhrases", BallSet.SUPER.defaultPhrases(), () -> "", o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    public static boolean enabled() { try { return ENABLED.get(); } catch (Throwable t) { return true; } }

    public static int count() { try { return DbRules.clampCount(COUNT.get()); } catch (Throwable t) { return DbRules.CANON_COUNT; } }

    public static int wishes(BallSet s) {
        try { return WishRules.wishCount(s, s == BallSet.SUPER ? SUPER_WISHES.get() : EARTH_WISHES.get()); } catch (Throwable t) { return s.defaultWishes(); }
    }

    public static int lockMinutes() { try { return LOCK_MINUTES.get(); } catch (Throwable t) { return 60; } }

    public static int sessionSeconds() { try { return SESSION_SECONDS.get(); } catch (Throwable t) { return 300; } }

    public static int gatherRadius() { try { return GATHER_RADIUS.get(); } catch (Throwable t) { return 6; } }

    public static int windowSeconds() { try { return WINDOW_SECONDS.get(); } catch (Throwable t) { return 10; } }

    public static boolean autoScatter() { try { return AUTO_SCATTER.get(); } catch (Throwable t) { return true; } }

    public static int scatterMin() { try { return SCATTER_MIN.get(); } catch (Throwable t) { return 300; } }

    public static int scatterMax() { try { return Math.max(SCATTER_MAX.get(), SCATTER_MIN.get() + 100); } catch (Throwable t) { return 3000; } }

    public static int scatterSeparation() { try { return SCATTER_SEP.get(); } catch (Throwable t) { return 500; } }

    public static int radarRange() { try { return RADAR_RANGE.get(); } catch (Throwable t) { return 2000; } }

    public static int powerCap() { try { return POWER_CAP.get(); } catch (Throwable t) { return WishRules.POWER_CAP_DEFAULT; } }

    public static int reviveMinutes() { try { return REVIVE_MINUTES.get(); } catch (Throwable t) { return 60; } }

    public static boolean saiyanWish() { try { return SAIYAN_WISH.get(); } catch (Throwable t) { return true; } }

    public static List<? extends String> phrases(BallSet s) {
        try { return s == BallSet.SUPER ? SUPER_PHRASES.get() : EARTH_PHRASES.get(); } catch (Throwable t) { return s.defaultPhrases(); }
    }
}
