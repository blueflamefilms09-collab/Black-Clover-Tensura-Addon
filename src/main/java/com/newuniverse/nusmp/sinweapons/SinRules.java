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

    // ================================================================ the extra abilities (docs/sin_weapons_abilities.md): appended, nothing above changes
    /** The shape a technique hits: round the wielder, a line ahead, a line ahead and behind, a cone ahead. */
    public enum Shape { AREA, LINE, BOTH_WAYS, CONE }

    /**
     * The two extra techniques of each weapon, run by the skill's modes 3 and 4 (slot 1 and 2; the right click stays the old technique). Order may only be appended to. {@code reach} is the radius (AREA, CONE) or the
     * length (LINE, BOTH_WAYS), {@code thick} the line's thickness, {@code cooldown} the ticks before THIS technique can be used again (its own clock in the wielder's persistent data), {@code emptyCosts} whether
     * a use that finds nobody still costs its cooldown (a self effect or a shot at nothing does; a slam with no one near does not).
     */
    public enum Ability {
        //                 weapon          slot id                  english            cooldown dmg    reach  cap shape             thick emptyCosts
        SLOTH_STILLNESS(Kind.SLOTH, 1, "stillness", "Stillness", 280, 0f, 5.0, 8, Shape.AREA, 0.0, false),
        SLOTH_GRAVITY(Kind.SLOTH, 2, "crushing_gravity", "Crushing Gravity", 200, 10f, 4.5, 8, Shape.AREA, 0.0, false),
        ENVY_EYES(Kind.ENVY, 1, "green_eyes", "Green Eyes", 240, 0f, 20.0, 4, Shape.LINE, 1.5, true),
        ENVY_LIFE(Kind.ENVY, 2, "stolen_life", "Stolen Life", 240, 7f, 5.0, 4, Shape.AREA, 0.0, false),
        VANITY_REFLECT(Kind.VANITY, 1, "reflected_thrust", "Reflected Thrust", 100, 9f, 8.0, 6, Shape.BOTH_WAYS, 1.0, true),
        VANITY_NARCISSUS(Kind.VANITY, 2, "narcissus", "Narcissus", 260, 0f, 8.0, 6, Shape.CONE, 0.0, true),
        CHAKRAMS_THROW(Kind.CHAKRAMS, 1, "thrown_rings", "Thrown Rings", 100, 10f, 14.0, 4, Shape.LINE, 1.2, true),
        CHAKRAMS_BIND(Kind.CHAKRAMS, 2, "binding_embrace", "Binding Embrace", 200, 0f, 4.5, 6, Shape.AREA, 0.0, false),
        HOLY_PURGE(Kind.HOLY, 1, "purge", "Purge", 240, 10f, 6.0, 8, Shape.AREA, 0.0, true),
        HOLY_GROUND(Kind.HOLY, 2, "holy_ground", "Holy Ground", 300, 8f, 5.0, 8, Shape.AREA, 0.0, true);

        public final Kind kind;
        public final int slot, cooldown, cap;
        public final String id, english;
        public final float damage;
        public final double reach, thick;
        public final Shape shape;
        public final boolean emptyCosts;

        Ability(Kind kind, int slot, String id, String english, int cooldown, float damage, double reach, int cap, Shape shape, double thick, boolean emptyCosts) {
            this.kind = kind; this.slot = slot; this.id = id; this.english = english; this.cooldown = cooldown; this.damage = damage; this.reach = reach; this.cap = cap; this.shape = shape;
            this.thick = thick; this.emptyCosts = emptyCosts;
        }

        /** The lang key of the technique's name (also the mode name in the skill). */
        public String key() { return "sinweapons.ability." + kind.id + "." + id; }

        /** The lang key of the technique's plain-words description. */
        public String descKey() { return key() + ".desc"; }
    }

    /** How many extra techniques each weapon has (skill modes 3 and 4). */
    public static final int ABILITIES_PER_KIND = 2;

    /** The extra technique of a weapon in slot 1 or 2, or null. */
    public static Ability ability(Kind k, int slot) {
        for (Ability a : Ability.values()) if (a.kind == k && a.slot == slot) return a;
        return null;
    }

    /** The name of the weapon's passive (one per weapon, works on a plain hit of the owner's weapon). */
    public static String passiveName(Kind k) {
        return switch (k) { case SLOTH -> "Sinking Weight"; case ENVY -> "Glaring Eyes"; case VANITY -> "Vain Shine"; case CHAKRAMS -> "Hungry Rings"; case HOLY -> "Smiting Light"; };
    }

    /** The lang key of the passive's name; the description is this key plus ".desc". */
    public static String passiveKey(Kind k) { return "sinweapons.passive." + k.id; }

    /** Stillness: Slowness VII (nothing moves) for 2 s. Green Eyes: glowing and weakness ticks, and how many beneficial effects one use may take in all. */
    public static final int STILL_TICKS = 40, STILL_AMP = 6, EYES_TICKS = 120, EYES_STEAL_MAX = 3;
    /** Crushing Gravity's downward shove, Binding Embrace's pull towards the wielder, Holy Ground's knock-back, and the most any shove may be. */
    public static final double GRAVITY_DOWN = 1.0, BIND_PULL = 0.6, GROUND_PUSH = 0.9, MAX_PUSH = 1.5;
    /** Stolen Life: the health per foe hurt and the most one use heals. */
    public static final float LIFE_HEAL_PER_FOE = 1.5f, LIFE_HEAL_MAX = 6f;
    /** Narcissus: weakness ticks, slowness ticks and level, and how narrow the cone is (cosine of its half angle). */
    public static final int NARCISSUS_WEAK_TICKS = 100, NARCISSUS_SLOW_TICKS = 60, NARCISSUS_SLOW_AMP = 2;
    public static final double NARCISSUS_MIN_COS = 0.5;
    /** Binding Embrace: slowness and weakness ticks. Purge: glowing ticks, harmful effects cleansed at most. Holy Ground: health given and regeneration ticks. */
    public static final int BIND_TICKS = 80, PURGE_GLOW_TICKS = 100, CLEANSE_MAX = 8, GROUND_REGEN_TICKS = 100;
    public static final float GROUND_HEAL = 6f;
    /** The passives: Sinking Weight (Sloth, a hit on an already slowed creature), Glaring Eyes (Envy), Vain Shine (Vanity, absorption), Hungry Rings (Chakrams, health), Smiting Light (Holy, fire on the undead). */
    public static final int SINK_TICKS = 60, SINK_AMP = 1, GLARE_TICKS = 60, SHINE_TICKS = 80, SMITE_FIRE_TICKS = 80;
    public static final float SHINE_ABSORB = 2f, HUNGRY_HEAL = 0.5f;
    /** The effect packets of one themed use: one cast picture and at most this many small hit flashes. */
    public static final int HIT_FX_MAX = 4;

    /** Is a creature inside an extra technique? {@code along}, {@code off} (from the look line), {@code dist} (from the eyes) and {@code hw} (its half-width) are in blocks. */
    public static boolean inAbility(Ability a, double along, double off, double dist, double hw) {
        return switch (a.shape) {
            case AREA -> dist <= a.reach + hw;
            case LINE -> onLine(along, off, a.reach, a.thick, hw);
            case BOTH_WAYS -> onLine(Math.abs(along), off, a.reach, a.thick, hw);
            case CONE -> dist <= a.reach + hw && inCone(along, dist, NARCISSUS_MIN_COS);
        };
    }

    /** Is a point {@code dist} away, {@code along} blocks forward, inside a cone of this half angle cosine (the wielder's own spot counts as inside)? */
    public static boolean inCone(double along, double dist, double minCos) { return dist < 1.0e-6 || along / dist >= minCos; }

    /** A clock in the wielder's persistent data: ready when its time is over; a clock set back further than {@code maxSpan} does not lock the wielder out. */
    public static boolean clockReady(long now, long readyAt, int maxSpan) { return now >= readyAt || readyAt - now > maxSpan; }

    /** Ticks left on a clock (0 = ready), never above the technique's own cooldown. */
    public static int ticksLeft(long now, long readyAt, int cooldown) { return clockReady(now, readyAt, cooldown) ? 0 : (int) (readyAt - now); }

    /** Stolen Life's health for the foes it hurt: {@link #LIFE_HEAL_PER_FOE} each, at most {@link #LIFE_HEAL_MAX}. */
    public static float lifeHeal(int foesHurt) { return Math.max(0f, Math.min(foesHurt * LIFE_HEAL_PER_FOE, LIFE_HEAL_MAX)); }

    /** A shove, never above {@link #MAX_PUSH} and never negative or not a number. */
    public static double push(double strength) { return Double.isNaN(strength) ? 0.0 : Math.max(0.0, Math.min(strength, MAX_PUSH)); }

    /** Sinking Weight: a plain hit slows by one level, two when the creature is slowed already (and longer). */
    public static int hitSlowAmp(boolean alreadySlowed) { return alreadySlowed ? SINK_AMP : HIT_SLOW_AMP; }

    public static int hitSlowTicks(boolean alreadySlowed) { return alreadySlowed ? SINK_TICKS : HIT_SLOW_TICKS; }

    /** The least ticks between two procs of a weapon's passive (Vain Shine, Hungry Rings; the others work on every hit and cost nothing). */
    public static int passiveGap(Kind k) { return switch (k) { case VANITY -> 60; case CHAKRAMS -> 30; default -> 0; }; }

    /** Worst-case effect packets of one themed use of a technique: the cast picture, up to {@link #HIT_FX_MAX} hit flashes and one small classic puff for clients that hide the picture. */
    public static int themedPacketsPerUse() { return 1 + HIT_FX_MAX + 1; }

    /** The same for an extra technique (its cap counts only up to {@link #HIT_FX_MAX}). */
    public static int worstPacketsPerAbility(Ability a) { return 1 + Math.min(Math.min(a.cap, 24), HIT_FX_MAX) + 1; }

    /** The damage of an extra technique against one creature (the listed damage scaled by the server setting; the undead take {@link #UNDEAD_FACTOR} from the holy ones). */
    public static float abilityDamage(Ability a, double percent, boolean undeadAndHoly) { return techniqueDamage(a.damage, percent, undeadAndHoly); }
}
