package com.newuniverse.nusmp.sinweapons;

import java.util.Set;

/**
 * Why: the numbers and decisions of the five Berserk of Gluttony weapons added next to the Black Sword Greed (the Weapons of Mortal Sin that are not Greed, and the holy sword Seifort), in one pure class
 * (no game types) so a test can pin every cap. Names come from the series wiki; the abilities are this mod's own design (the wiki lists names, not mechanics). Every weapon is an honest melee weapon first; its
 * right-click technique is bounded: one cooldown per player (the game's own item cooldown, nothing stored), at most {@link Kind#cap} targets, one box query, a handful of particle packets. All damage of the
 * techniques goes through the balance law ({@code relics.Fx.damage}). Settings: server {@code nusmp-sinweapons-server.toml}.
 */
public final class SinRules {
    private SinRules() {}

    public static final String SLOTH_ID = "black_axe_of_sloth", ENVY_ID = "black_gunblade_of_envy", VANITY_ID = "black_spear_of_vanity", CHAKRAMS_ID = "black_chakrams", HOLY_ID = "seifort_holy_sword";

    /** The five weapons. Order is the registration order and may only be appended to. {@code base} is the attack damage modifier on top of netherite's 4 (and the wielder's own 1). */
    public enum Kind {
        //     id           english                       technique            base  speed   cooldown  tech dmg  reach  cap  rgb
        SLOTH(SLOTH_ID, "Black Axe of Sloth", "Dead Weight", 5.0f, -3.2f, 160, 0f, 6.0, 8, 0x7A3FD1),
        ENVY(ENVY_ID, "Black Gunblade of Envy", "Green-Eyed Shot", 3.0f, -2.4f, 60, 14f, 28.0, 1, 0x2FBF5A),
        VANITY(VANITY_ID, "Black Spear of Vanity", "Vain Thrust", 3.0f, -2.6f, 80, 12f, 8.0, 4, 0xD64FB0),
        CHAKRAMS(CHAKRAMS_ID, "Black Chakrams", "Spinning Ring", 1.0f, -1.6f, 70, 9f, 3.5, 6, 0xE23B4B),
        HOLY(HOLY_ID, "Seifort Holy Sword", "Grand Cross", 4.0f, -2.4f, 100, 16f, 12.0, 8, 0xE8B98A);

        public final String id, english, technique;
        public final float base, speed, techDamage;
        public final int cooldown, cap, rgb;
        public final double reach;

        Kind(String id, String english, String technique, float base, float speed, int cooldown, float techDamage, double reach, int cap, int rgb) {
            this.id = id; this.english = english; this.technique = technique; this.base = base; this.speed = speed; this.cooldown = cooldown;
            this.techDamage = techDamage; this.reach = reach; this.cap = cap; this.rgb = rgb;
        }

        public String techniqueKey() { return "sinweapons.technique." + id; }
    }

    public static final Set<String> IDS = Set.of(SLOTH_ID, ENVY_ID, VANITY_ID, CHAKRAMS_ID, HOLY_ID);

    /** Slowness / Mining Fatigue of Dead Weight and of a plain Sloth hit, in ticks and amplifier. */
    public static final int WEIGHT_TICKS = 80, WEIGHT_SLOW_AMP = 2, WEIGHT_FATIGUE_AMP = 0, HIT_SLOW_TICKS = 40, HIT_SLOW_AMP = 0;
    /** Covet (Envy): the most a stolen effect lasts on the wielder, and the least ticks between two thefts of one wielder. */
    public static final int COVET_MAX_TICKS = 200, COVET_GAP_TICKS = 40;
    /** Vain Thrust: the line's thickness. Grand Cross: the stem's thickness, where the crossbar crosses it, its half-width and half-height. */
    public static final double THRUST_RADIUS = 1.0, STEM_RADIUS = 1.0, BAR_AT = 8.0, BAR_HALF_WIDTH = 5.0, BAR_HALF_HEIGHT = 2.0;
    /** The Holy Sword's damage factor against the undead. */
    public static final float UNDEAD_FACTOR = 1.5f;
    /** A hit of the Chakrams is followed by the twin blow of the Black Sword Greed's twin forms (60%, 12 ticks), see {@code GreedForms#offBlow}. */
    public static final int PARTICLES_PER_TARGET = 4, TRAIL_STEP = 2;

    /** Is a creature at {@code along} blocks forward and {@code off} blocks from the look line inside a line of this length and thickness (its own half-width counts)? */
    public static boolean onLine(double along, double off, double range, double radius, double halfWidth) {
        return along >= 0.0 && along <= range + 0.5 && off >= 0.0 && off <= radius + halfWidth;
    }

    /** Grand Cross: the stem (a line of {@link Kind#reach}) or the crossbar (across the stem at {@link #BAR_AT}). {@code lateral} and {@code vertical} are from the look line. */
    public static boolean onCross(double along, double lateral, double vertical, double halfWidth) {
        double off = Math.sqrt(lateral * lateral + vertical * vertical);
        if (onLine(along, off, Kind.HOLY.reach, STEM_RADIUS, halfWidth)) return true;
        return Math.abs(along - BAR_AT) <= STEM_RADIUS + halfWidth && Math.abs(lateral) <= BAR_HALF_WIDTH + halfWidth && Math.abs(vertical) <= BAR_HALF_HEIGHT + halfWidth;
    }

    /** The damage of a technique: the listed damage scaled by the server setting (percent), zero for a bad number, never above the balance law's own 400. */
    public static float techniqueDamage(float listed, double percent, boolean undeadAndHoly) {
        double d = listed * percent / 100.0 * (undeadAndHoly ? UNDEAD_FACTOR : 1.0);
        if (Double.isNaN(d) || d <= 0.0) return 0f;
        return (float) Math.min(d, 400.0);
    }

    /** May Envy steal an effect now? (a clock set back far does not lock the wielder out for ever) */
    public static boolean covetReady(long now, long next) { return now >= next || next - now > COVET_GAP_TICKS; }

    /** The stolen effect's length on the wielder: the victim's remaining time, at most {@link #COVET_MAX_TICKS}, at least 1. */
    public static int covetTicks(int remaining) { return Math.max(1, Math.min(remaining, COVET_MAX_TICKS)); }

    /** The most creatures one use can hurt: the cap of its kind, never above the 24 the foe query returns. */
    public static int targetCap(Kind k) { return Math.min(k.cap, 24); }

    /** Worst-case work of one use: targets hurt, particle packets (a spray per target and one per trail step), and the cooldown that limits how often. */
    public static int worstPacketsPerUse(Kind k) {
        int trail = switch (k) { case ENVY -> (int) (k.reach / TRAIL_STEP); case VANITY -> (int) (k.reach / TRAIL_STEP); case HOLY -> (int) (k.reach / TRAIL_STEP) + 6; default -> 6; };
        return trail + targetCap(k);
    }
}
