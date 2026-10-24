package com.newuniverse.nusmp.star;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Star Magic nodes (docs/vfx_remake/star_ingame.md): the pure rules. No Minecraft class, so it runs in the unit tests, in the CI self-test on a dedicated server and in the headless preview.
 * Everything a server owner can tune is clamped here (the config class only reads the file and asks these functions), so a hand-edited toml can never break a bound.
 *
 * <p>The idea: a Star mage hangs small stars (nodes) in the world with {@code star_seed}; Conjunction teleports to beneath one (and consumes it), Quartile Scutum / Hasta / Flagellum use the four most
 * recent ones as corners (Scutum keeps them, Hasta consumes them), the Quintile pages use five. The whole Quartile family, Quintile and Black Neverland need the Grinberryall heir mark
 * ({@link #isHeir}); a mage who is not an heir keeps every older page (the Quartile Flagellum falls back to its old cone).
 */
public final class StarRules {
    private StarRules() {}

    // ---------------------------------------------------------------------------------------------- config defaults and clamps
    public static final String CONFIG_FILE = "nusmp-star-server.toml";
    public static final int DEFAULT_PER_PLAYER = 6, DEFAULT_HEIR_PER_PLAYER = 8, DEFAULT_SERVER = 128, DEFAULT_LIFE_TICKS = 800, DEFAULT_RANGE = 24, DEFAULT_SEED_COOLDOWN = 20, DEFAULT_ALLIES = 3;
    public static final int MIN_PER_PLAYER = 1, MAX_PER_PLAYER = 8, MIN_SERVER = 16, MAX_SERVER = 512, MIN_LIFE = 200, MAX_LIFE = 2400, MIN_RANGE = 8, MAX_RANGE = 32,
            MIN_SEED_COOLDOWN = 10, MAX_SEED_COOLDOWN = 80, MAX_ALLIES = 5;
    public static final double DEFAULT_HEIR_MASTERY = 1.0, MIN_HEIR_MASTERY = 0.5;
    public static final boolean DEFAULT_NODES = true, DEFAULT_AUTO_QUARTET = true, DEFAULT_FALLBACK = true;

    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    public static int perPlayerCap(boolean heir, int normal, int heirCap) {
        int n = clamp(normal, MIN_PER_PLAYER, MAX_PER_PLAYER), h = clamp(heirCap, MIN_PER_PLAYER, MAX_PER_PLAYER);
        return heir ? Math.max(n, h) : n;                      // an heir never has fewer than a normal mage
    }

    public static int serverCap(int v) { return clamp(v, MIN_SERVER, MAX_SERVER); }

    public static int lifeTicks(int v) { return clamp(v, MIN_LIFE, MAX_LIFE); }

    public static int placeRange(int v) { return clamp(v, MIN_RANGE, MAX_RANGE); }

    public static int seedCooldown(int v) { return clamp(v, MIN_SEED_COOLDOWN, MAX_SEED_COOLDOWN); }

    public static int maxAllies(int v) { return clamp(v, 0, MAX_ALLIES); }

    public static double heirMastery(double v) { return Math.max(MIN_HEIR_MASTERY, Math.min(1.0, v)); }

    // ---------------------------------------------------------------------------------------------- dual attribute users
    /** What happens to a questionnaire answer that ties two attributes when one of them is Star (server setting {@code star.dualMode}). */
    public enum DualMode {
        /** The existing behaviour (1.16.0): the two attributes fuse into ONE grimoire. Default. */
        FUSION,
        /** The mage gets two real grimoires, the other attribute's and a Star grimoire of their own (the mod's existing second-grimoire grant). */
        SECOND_GRIMOIRE
    }

    public static DualMode parseDual(String text) {
        if (text == null) return DualMode.FUSION;
        String t = text.strip().toUpperCase(Locale.ROOT);
        for (DualMode m : DualMode.values()) if (m.name().equals(t)) return m;
        return DualMode.FUSION;
    }

    /** The grimoires a tied pair becomes: {@code first} is the main grimoire, {@code extra} a second one (or null), {@code fuse} says whether the pair is written on the book as a fusion. */
    public record DualPlan(String first, String extra, boolean fuse) {}

    /**
     * Pure decision of the tower: {@code primary} is the attribute the fusion made, {@code parentA / parentB} the two tied ones (null when nothing tied). FUSION, or a pair without Star, returns the old
     * plan (the primary, fused). SECOND_GRIMOIRE with Star among the two (as a parent, or as the blend the pair made) returns two grimoires: the non-Star parent first, Star second.
     */
    public static DualPlan dualPlan(DualMode mode, String primary, String parentA, String parentB) {
        boolean tied = parentA != null && parentB != null;
        boolean star = tied && ("STAR".equals(parentA) || "STAR".equals(parentB) || "STAR".equals(primary));
        if (mode != DualMode.SECOND_GRIMOIRE || !star) return new DualPlan(primary, null, tied);
        String other = "STAR".equals(parentA) ? parentB : parentA;
        return new DualPlan(other, "STAR", false);
    }

    // ---------------------------------------------------------------------------------------------- the Grinberryall heir
    /**
     * The heir mark: the Star grimoire is a Spade grimoire, its mastery has reached {@code need} (the signature tier), and the Spade Kingdom Raid saga is cleared in the player's story (Dante, Vanica,
     * Zenon, Morris and Lucifero defeated). It is computed from those three facts each time it is asked (nothing is stored, so there is nothing to keep in sync).
     */
    public static boolean isHeir(boolean spadeStarGrimoire, double mastery, double need, boolean spadeRaidCleared) {
        return spadeStarGrimoire && spadeRaidCleared && mastery >= heirMastery(need) - 1e-9;
    }

    public static final String SCUTUM = "quartile_scutum", HASTA = "quartile_hasta", FLAGELLUM = "quartile_flagellum", SEED = "star_seed", CONJUNCTION = "star_conjunction",
            BLACK_NEVERLAND = "black_neverland", QUINTILE_SCUTUM = "quintile_atrum_scutum", QUINTILE_HASTA = "quintile_atrum_hasta", QUINTILE_FLAGELLUM = "quintile_atrum_flagellum";

    /** The new pages, in the order they are appended to the book. */
    public static final List<String> NEW_PAGES = List.of(SEED, SCUTUM, HASTA, BLACK_NEVERLAND, QUINTILE_SCUTUM, QUINTILE_HASTA, QUINTILE_FLAGELLUM);

    /** The NEW pages only an heir can open or cast. */
    public static final Set<String> HEIR_PAGES = Set.of(SCUTUM, HASTA, BLACK_NEVERLAND, QUINTILE_SCUTUM, QUINTILE_HASTA, QUINTILE_FLAGELLUM);

    /** Does casting this page need the heir mark? The Flagellum counts here: its whip is for the heir, a mage who is not one gets the old cone. */
    public static boolean needsHeir(String pageId) { return HEIR_PAGES.contains(pageId) || FLAGELLUM.equals(pageId); }

    /** The pages whose canon is not verified (one source only); the doc and the lang text say so. */
    public static boolean unverifiedCanon(String pageId) { return QUINTILE_SCUTUM.equals(pageId) || QUINTILE_HASTA.equals(pageId) || QUINTILE_FLAGELLUM.equals(pageId); }

    // ---------------------------------------------------------------------------------------------- what a cast consumes
    /** Conjunction consumes the star it goes to; Hasta (and Atrum Hasta) consume the whole quartet / quintet; Scutum, Flagellum and the rest keep their stars. */
    public static boolean consumesStars(String pageId) { return HASTA.equals(pageId) || QUINTILE_HASTA.equals(pageId) || CONJUNCTION.equals(pageId); }

    /** How many stars the page needs (0 = none, Conjunction needs one chosen star). */
    public static int starsNeeded(String pageId) {
        return switch (pageId) {
            case SCUTUM, HASTA, FLAGELLUM -> 4;
            case QUINTILE_SCUTUM, QUINTILE_HASTA, QUINTILE_FLAGELLUM -> 5;
            default -> 0;
        };
    }

    // ---------------------------------------------------------------------------------------------- allies brought by Conjunction
    /** Hard distance (blocks) from the caster within which an ally is brought along. */
    public static final double ALLY_RADIUS = 3.0;

    /** How many allies one Conjunction brings: the server setting (clamped 0..5) never above the tier's target cap minus the caster (Tier.MID: 6 targets, so 5). */
    public static int allyLimit(int configured, int tierMaxTargets) { return Math.max(0, Math.min(maxAllies(configured), tierMaxTargets - 1)); }

    /** The first {@code limit} entries of a list already sorted by distance (never more, never null). */
    public static <T> List<T> firstN(List<T> sortedByDistance, int limit) {
        int n = Math.max(0, Math.min(limit, sortedByDistance.size()));
        return new ArrayList<>(sortedByDistance.subList(0, n));
    }

    // ---------------------------------------------------------------------------------------------- Conjunction's choice and the damage numbers
    public static final double PICK_ANGLE_DEG = 20.0, PICK_RANGE = 48.0, QUARTET_RANGE = 48.0, DEST_SEARCH_DOWN = 16.0;
    /** Ticks of "no incoming damage" the caster gets at departure and arrival (the evade); a server setting may only lower it. */
    public static final int EVADE_TICKS = 4;

    /** Hasta's raw hit before the tier clamp: {@code tierHit * (1 + 0.04 * stars + 0.10 * min(1, mastery))}, times 1.15 for an heir. */
    public static float hastaRaw(float tierHit, int stars, double mastery, boolean heir) {
        float raw = tierHit * (1f + 0.04f * stars + 0.10f * (float) Math.min(1.0, Math.max(0.0, mastery)));
        return heir ? raw * 1.15f : raw;
    }

    /** Whip hit: 0.6 of the tier hit per pass, at most two passes on the same target (a full double hit is 1.2 hits, inside the 1.5 clamp). */
    public static float whipRaw(float tierHit) { return tierHit * 0.6f; }

    public static final int WHIP_MAX_HITS_PER_TARGET = 2, WHIP_STEPS = 3, HASTA_CHARGE_TICKS = 9, HASTA_MAX_TARGETS_BEAM = 6;

    /** The Scutum pane's health pool in raw hit points: three tier hits (four and a half for the Quintile pane). */
    public static float panePool(float tierHit, int stars) { return tierHit * (stars >= 5 ? 4.5f : 3f); }

    /** Share of a hit that counts against the pane: a hit tagged "devil" counts half (the canon barrier holds against devil magic). */
    public static float paneDamage(float amount, boolean devil) { return devil ? amount * 0.5f : amount; }

    public static final int PANE_LIFE_TICKS = 100, PANE_LIFE_TICKS_QUINTILE = 140, PANES_PER_PLAYER = 1, PANES_SERVER = 8;

    // ---------------------------------------------------------------------------------------------- the words a prop carries (synced param, read by the painter)
    /** STAR_NODE param: bits 0 to 3 the owner's placement index, bits 8 to 15 the drop to the ground below in quarter blocks (0 = unknown, the painter draws no ground sigil). */
    public static int packNode(int index, double dropBlocks) { return (index & 15) | (clamp((int) Math.round(dropBlocks * 4), 0, 255) << 8); }

    public static double nodeDrop(int param) { return ((param >> 8) & 255) / 4.0; }

    /** STAR_PANE param: bits 0 to 6 the share of the pool left (0..100), bits 8 to 15 the width in eighth blocks, bits 16 to 23 the height in eighth blocks. */
    public static int packPane(int percent, double width, double height) {
        return clamp(percent, 0, 100) | (clamp((int) Math.round(width * 8), 0, 255) << 8) | (clamp((int) Math.round(height * 8), 0, 255) << 16);
    }

    public static int panePercent(int param) { return param & 127; }

    public static double paneWidth(int param) { return ((param >> 8) & 255) / 8.0; }

    public static double paneHeight(int param) { return ((param >> 16) & 255) / 8.0; }

    /** The entity yaw (degrees) whose facing vector (-sin, 0, cos) is the pane's normal (cos t, 0, sin t). */
    public static float entityYawDeg(double normalYaw) { return (float) Math.toDegrees(Math.atan2(-Math.cos(normalYaw), Math.sin(normalYaw))); }
}
