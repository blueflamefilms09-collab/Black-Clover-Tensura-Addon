package com.newuniverse.nusmp.relics.djinn;

import java.util.List;

/**
 * The numbers of Belial's mind magic (pure, so BelialMindRulesTest can check every bound): Belior Zakera (the memory and sense illusion, the scythe's third act) and the five
 * senses Belior Zauto devours. Owner notes: Life Magic lets Belial control living minds, alter the five senses and rewrite memories; a magician's defensive Borg blocks Zakera but the
 * defender still takes mental strain.
 *
 * <p><b>Zakera.</b> At most {@link #MAX_TARGETS} foes per cast. A mob forgets its target for 3 to 6 seconds ({@link #forgetTicks}) and cannot pick a new one for the first
 * {@link #RETARGET_BLOCK} ticks, except the foe it is turned on (half the time, another foe of the cone). A player gets distorted senses (Nausea and Darkness, a few seconds, then the
 * balance law's control-time cap and diminishing returns on top). A boss is never mind-controlled: only a short Darkness and a small strain that goes through the sever's boss caps. A
 * target that is blocking with a shield, or carries a barrier effect (the mod has no Borg model, this is the nearest thing it reads), is not affected but takes the mental strain and a short Nausea and Darkness (head trauma). Reach: 10 blocks in a Half Equip, 24 in a Full Equip.
 * <p><b>Senses.</b> Five vanilla effects stand for the five senses; they last seconds (the permanent loss of the story is a design cap here); a boss gets none (it only takes the
 * sever's own boss caps).
 * <p>The tables are bounded: {@link #MAX_MINDS} mobs on the server, {@link #MAX_PER_CASTER} per caster.
 */
public final class BelialMindRules {
    private BelialMindRules() {}

    public static final int MAX_TARGETS = 6;
    public static final int MAX_MINDS = 48, MAX_PER_CASTER = 12;
    public static final int FORGET_MIN = 60, FORGET_MAX = 120;
    /** A mob that forgot cannot pick a new target for this long (ticks), at most as long as the whole state. */
    public static final int RETARGET_BLOCK = 60;
    /** The share of mobs of a cast that are turned on another foe of the cone. */
    public static final double REDIRECT_CHANCE = 0.5;
    public static final int NAUSEA_MIN = 80, NAUSEA_MAX = 120, DARKNESS_PLAYER = 60, DARKNESS_BOSS = 40;
    /** The strain (a share of the act's power) a blocked target / a boss takes through the sever. */
    public static final double STRAIN_BLOCKED = 0.25, STRAIN_BOSS = 0.15;

    /** Reach of Belior Zakera in blocks: 10 through the Half Equip (the base Metal Vessel), 24 in the Full Equip (owner notes). */
    public static final double RANGE_HALF = 10.0, RANGE_FULL = 24.0;
    /** A blocked target (a raised shield, a barrier) still gets a short head trauma: Nausea and Darkness, before the control cap. */
    public static final int BLOCKED_NAUSEA = 40, BLOCKED_DARKNESS = 30;

    public static double zakeraRange(boolean full) { return full ? RANGE_FULL : RANGE_HALF; }

    // ---------------------------------------------------------------- Belior Goldoreza: the struck part goes numb ("dead weight")
    /**
     * The scythe leaves no wound: what it cuts has its Rukh and senses banished and goes numb and limp. Played as a bounded debuff on the victim (Slowness I and Weakness I) for 18 s
     * (a mob) or 8 s (a player), +2 s in a Full Equip, and a block on normal healing (regeneration, potions, food) for the same time. A boss gets none (it only takes the sever's boss caps).
     * At most {@link #DEAD_MAX} victims are tracked on the server.
     */
    public static final int DEAD_MOB = 360, DEAD_PLAYER = 160, DEAD_FULL = 40, DEAD_MAX_TICKS = 400, DEAD_MAX = 128;

    /** How long the dead weight lasts (0 for a boss): never more than {@link #DEAD_MAX_TICKS} (20 s). */
    public static int deadWeightTicks(BelialSeverRules.Kind kind, boolean full) {
        if (kind == BelialSeverRules.Kind.BOSS) return 0;
        return Math.min(DEAD_MAX_TICKS, (kind == BelialSeverRules.Kind.PLAYER ? DEAD_PLAYER : DEAD_MOB) + (full ? DEAD_FULL : 0));
    }

    /** What happens to one target of a Zakera. */
    public enum Fate { MIND, SENSES, BOSS, BLOCKED }

    /** The one rule: a defended target is blocked (whatever it is), then a boss, a player, a mob. */
    public static Fate fate(BelialSeverRules.Kind kind, boolean defended) {
        if (defended) return Fate.BLOCKED;
        if (kind == BelialSeverRules.Kind.BOSS) return Fate.BOSS;
        return kind == BelialSeverRules.Kind.PLAYER ? Fate.SENSES : Fate.MIND;
    }

    /** How long a mob forgets: 3 s, +1 s in a Full Equip, +0.5 s for a dark Djinn; always 3 to 6 s. */
    public static int forgetTicks(boolean full, boolean dark) {
        return Math.max(FORGET_MIN, Math.min(FORGET_MAX, FORGET_MIN + (full ? 20 : 0) + (dark ? 10 : 0)));
    }

    /** The Nausea of a player's distorted senses before the control cap: 4 s, +1 s in a Full Equip, +0.5 s dark; 4 to 6 s. */
    public static int nauseaTicks(boolean full, boolean dark) {
        return Math.max(NAUSEA_MIN, Math.min(NAUSEA_MAX, NAUSEA_MIN + (full ? 20 : 0) + (dark ? 10 : 0)));
    }

    /** The share of the act's power a defended target or a boss takes as mental strain (0 for the rest). */
    public static double strain(Fate f, double actPower) {
        double share = f == Fate.BLOCKED ? STRAIN_BLOCKED : f == Fate.BOSS ? STRAIN_BOSS : 0.0;
        return Math.max(0.0, actPower) * share;
    }

    public static boolean shouldRedirect(double roll) { return roll < REDIRECT_CHANCE; }

    /** May a new mind be taken (the tables are bounded)? A mob that already forgot only has its time renewed. */
    public static boolean accept(boolean already, int casterCount, int globalCount) {
        return already || (casterCount < MAX_PER_CASTER && globalCount < MAX_MINDS);
    }

    // ---------------------------------------------------------------- the five senses of Belior Zauto
    /** One sense: the vanilla effect that stands for it, its level, and whether it is a control effect (it then goes through the balance law's control time). */
    public record Sense(String name, String effectId, int amp, boolean control) {}

    public static final List<Sense> SENSES = List.of(
            new Sense("sight", "minecraft:blindness", 0, true),
            new Sense("hearing", "minecraft:nausea", 0, true),
            new Sense("touch", "minecraft:slowness", 1, true),
            new Sense("taste", "minecraft:weakness", 0, false),
            new Sense("smell", "minecraft:mining_fatigue", 0, false));

    public static final int SENSE_MOB = 100, SENSE_PLAYER = 60, SENSE_DARK = 20, SENSE_MAX = 140;

    /** How long the senses are gone (0 for a boss): 5 s for a mob, 3 s for a player, +1 s for a dark Djinn; never more than {@link #SENSE_MAX}. */
    public static int senseTicks(BelialSeverRules.Kind kind, boolean dark) {
        if (kind == BelialSeverRules.Kind.BOSS) return 0;
        int base = kind == BelialSeverRules.Kind.PLAYER ? SENSE_PLAYER : SENSE_MOB;
        return Math.min(SENSE_MAX, base + (dark ? SENSE_DARK : 0));
    }
}
