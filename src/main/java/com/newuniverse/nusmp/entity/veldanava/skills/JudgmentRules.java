package com.newuniverse.nusmp.entity.veldanava.skills;

import java.util.List;

/**
 * Why: the numbers of the Veldanava ultimate skills (docs/veldanava_skills.md) in one place that names no Minecraft type, so the skill, the events, the unit tests and the CI self-test all read the
 * same table. Sandalphon, Lord of Judgment is defined here; the four skills that follow (Israfil, Jibril, Solomon, Hercules) add their own {@link Spec} in their own rules file (Israfil and Jibril are in) and reuse the helpers.
 *
 * <p>A skill has ten modes. Mode 0..3 are toggle passives, 4..8 are actives (a press, or a crouch + press), and 9 is the mastery active with a cooldown. Every active has a Magicule cost,
 * and a lower one once the skill is mastered; the server's {@code costScale} multiplies both.
 */
public final class JudgmentRules {
    private JudgmentRules() {}

    /** How a mode is used: a toggle that stays on, one press, a press while crouching, or a key held down. */
    public enum Kind { TOGGLE, PRESS, CROUCH_PRESS, HELD }

    /** One mode: its registry-safe id, its English name, how it is used, its cost (normal and mastered), its cooldown in seconds (0 = none) and whether a held cost is per second. */
    public record Mode(int index, String id, String name, Kind kind, int cost, int masteredCost, int cooldownSeconds, int masteredCooldownSeconds, boolean perSecond) {
        /** The form of 3.13.0 (one cooldown for both states); kept so older callers and tests compile unchanged. */
        public Mode(int index, String id, String name, Kind kind, int cost, int masteredCost, int cooldownSeconds, boolean perSecond) {
            this(index, id, name, kind, cost, masteredCost, cooldownSeconds, cooldownSeconds, perSecond);
        }

        public boolean toggle() { return kind == Kind.TOGGLE; }
        public boolean crouch() { return kind == Kind.CROUCH_PRESS; }
        /** The tenth mode of every skill answers only a mastered skill. */
        public boolean masteryOnly() { return index == MODES - 1; }
    }

    /** One skill of the reward: its id (the path of the skill registered in the mod), its title, and its ten modes. */
    public record Spec(String id, String title, String epithet, List<Mode> modes) {
        public Mode mode(int i) { return modes.get(Math.floorMod(i, modes.size())); }
        public int count() { return modes.size(); }
    }

    public static final int MODES = 10;

    /** The first four modes of every skill are the passives; with the master toggle (3.20.0) they leave the mode wheel. */
    public static final int TOGGLE_MODES = 4;

    /** The modes on the wheel of a skill with {@code count} modes: all of them, or without the four passives under the master toggle. */
    public static int wheelCount(boolean master, int count) { return master ? count - TOGGLE_MODES : count; }

    /** The mode of the table a wheel position stands for (a position saved by an older build wraps into the wheel). */
    public static int slot(boolean master, int count, int wheelMode) {
        int w = Math.floorMod(wheelMode, wheelCount(master, count));
        return master ? w + TOGGLE_MODES : w;
    }

    /** The cooldown slot of a mode of the table: the wheel position the game shows it at. */
    public static int cooldownSlot(boolean master, int modeIndex) { return master ? modeIndex - TOGGLE_MODES : modeIndex; }

    /** The toggle bits of the master toggle: all four passives, or none. */
    public static int masterBits(boolean on) { return on ? 15 : 0; }

    /** What an older save's toggle bits become under the master toggle: ON (all four) when any passive was on, else OFF. */
    public static boolean migrateToMaster(int oldBits) { return (oldBits & 15) != 0; }

    /** Sandalphon, Lord of Judgment (the Archangel of the Executioner): see the skill drop file of the owner. */
    public static final Spec SANDALPHON = new Spec("sandalphon", "Sandalphon", "Lord of Judgment", List.of(
            new Mode(0, "executioners_authority", "Executioner's Authority", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(1, "absolute_severance", "Absolute Severance", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(2, "inevitable_demise", "Inevitable Demise", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(3, "judgment_gaze", "Judgment Gaze", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(4, "judgment_bullet", "Judgment Bullet", Kind.PRESS, 2500, 1800, 6, 3, false),
            new Mode(5, "condemnation", "Condemnation", Kind.CROUCH_PRESS, 4200, 3100, 20, 12, false),
            new Mode(6, "supreme_edict", "Supreme Edict", Kind.PRESS, 5500, 4000, 30, 20, false),
            new Mode(7, "guillotine_cleave", "Guillotine Cleave", Kind.CROUCH_PRESS, 3800, 2500, 8, 4, false),
            new Mode(8, "verdict_of_ruin", "Verdict of Ruin", Kind.PRESS, 8000, 5500, 40, 25, false),
            new Mode(9, "final_judgment", "Final Judgment", Kind.HELD, 45000, 45000, 120, false)));

    /** The skills this build has, in the order of the owner's file. Releases append one line. */
    public static final List<Spec> SPECS = List.of(SANDALPHON, IsrafilRules.ISRAFIL, JibrilRules.JIBRIL, SolomonRules.SOLOMON, HerculesRules.HERCULES);

    /** The spec of a skill id, or null. */
    public static Spec spec(String id) {
        if (id == null) return null;
        for (Spec sp : SPECS) if (sp.id().equals(id)) return sp;
        return null;
    }

    /**
     * The code of a skill in the player's mask (bits 5..7 of {@link JudgmentState#mask}): 1 = Sandalphon, 2 = Israfil, 3 = Jibril, 4 = Solomon, 5 = Hercules. 0 = none. A passive reads the mask
     * only for its own code, so a creature that somehow holds two of the skills never has one skill's passives switch on with the other's toggles.
     */
    public static int code(String id) {
        if (id == null) return 0;
        for (int i = 0; i < CARDS.size(); i++) if (CARDS.get(i).id().equals(id)) return i + 1;
        return 0;
    }

    // ---------------------------------------------------------------- the five reward cards
    /** One card of the reward screen: the skill id, the title, the epithet, and whether this build can already give it. */
    public record Card(String id, String title, String epithet, boolean available) {}

    /**
     * The five cards of the Will of the Creator, in the order of the skill drop file. Sandalphon is the first release; the other four are listed (so the screen has its final shape) but sealed until
     * their own release, and a claim for one of them is refused by the server. Entries are only ever appended or switched to available, never removed.
     */
    public static final List<Card> CARDS = List.of(
            new Card("sandalphon", "Sandalphon", "Lord of Judgment", true),
            new Card("israfil", "Israfil", "Lord of Trials", true),
            new Card("jibril", "Jibril", "Lord of Rigor", true),
            new Card("solomon", "Solomon", "Lord of Sorcery", true),
            new Card("hercules", "Hercules", "Lord of Martial Prowess", true));

    public static Card card(String id) {
        if (id == null) return null;
        for (Card c : CARDS) if (c.id().equals(id)) return c;
        return null;
    }

    /** True when the id names a card this build can give. The only gate the claim packet and the command pass through. */
    public static boolean claimable(String id) {
        Card c = card(id);
        return c != null && c.available();
    }

    // ---------------------------------------------------------------- cost
    /** The Magicule a mode costs: the mastered price once mastered, times the server's scale, never negative. */
    public static double cost(Mode m, boolean mastered, double scale) {
        return Math.max(0.0, (mastered ? m.masteredCost() : m.cost()) * Math.max(0.0, scale));
    }

    /** The cooldown of a mode in seconds: the mastered one once mastered, times the server's {@code cooldownScale}, rounded, never negative (0 = none). */
    public static int cooldownSeconds(Mode m, boolean mastered, double scale) {
        double base = mastered ? m.masteredCooldownSeconds() : m.cooldownSeconds();
        return (int) Math.max(0L, Math.round(base * Math.max(0.0, scale)));
    }

    // ---------------------------------------------------------------- numbers of Sandalphon
    /** Critical strike chance of Executioner's Authority and the damage of a critical strike. */
    public static final double CRIT_CHANCE = 0.20, CRIT_MULTIPLIER = 1.5;
    /** The share of the target's armour that Executioner's Authority adds as bonus true damage (armour points x this), and its most. */
    public static final double ARMOUR_PIERCE_PER_POINT = 0.35, ARMOUR_PIERCE_MAX = 12.0;
    /** Condemnation executes at or below this share of the target's health; Inevitable Demise (mastered) marks a target below this share. */
    public static final double EXECUTE_BELOW = 0.20, DEMISE_BELOW = 0.15;
    /** How long a Judgment Sigil lasts (ticks), how long a target cannot heal after a Severance hit, after a Bullet. */
    public static final int SIGIL_TICKS = 600, NO_HEAL_HIT_TICKS = 60, NO_HEAL_BULLET_TICKS = 100;
    /** The Temporal Debt a hit adds (Executioner's Authority), Judgment Gaze adds per second, Condemnation stores, a Bullet applies. */
    public static final int DEBT_PER_HIT = 1, DEBT_PER_GAZE = 2, DEBT_CONDEMN = 25, DEBT_BULLET = 10, DEBT_CLEAVE = 5;
    /** Reach and size of the actives. */
    public static final double BULLET_RANGE = 64.0, BULLET_WIDTH = 0.9, CONDEMN_RANGE = 48.0, EDICT_RADIUS = 30.0, CLEAVE_REACH = 5.5, CLEAVE_HALF_ANGLE_DEG = 65.0,
            VERDICT_RANGE = 40.0, VERDICT_RADIUS = 8.0, GAZE_RANGE = 24.0, GAZE_COSINE = 0.985, PRESENCE_RANGE = 24.0, FINAL_RANGE = 80.0;
    /** The most entities one press can touch (a Bullet pierces at most this many, an Edict strips this many, a Cleave hits this many). */
    public static final int MAX_BULLET_HITS = 8, MAX_EDICT_TARGETS = 32, MAX_CLEAVE_TARGETS = 12, MAX_GAZE_TARGETS = 16, MAX_PRESENCE_TARGETS = 12, MAX_FIELD_TARGETS = 24;
    /** Verdict of Ruin: how long the field lasts, how often it pulses (ticks), and the share of the victim's max health one pulse takes. */
    public static final int VERDICT_TICKS = 240, FIELD_PULSE_TICKS = 10;
    public static final double VERDICT_PULSE_SHARE = 0.015;
    /** Final Judgment: the channel in ticks, the multiplier on the debt, the lowest hit, and the effect lengths while frozen. */
    public static final int FINAL_CHANNEL_TICKS = 60, FINAL_MAX_HELD = 120;
    public static final double FINAL_DEBT_MULTIPLIER = 5.0, FINAL_MIN_DAMAGE = 200.0;

    /** The least gap between two actives of one player (ticks), the base damage of a Bullet and of a Cleave, the most good effects one Bullet or Edict strips from one creature. */
    public static final int ACTIVE_GAP_TICKS = 8, STRIP_MAX = 16;
    public static final double BULLET_DAMAGE = 80.0, CLEAVE_DAMAGE = 90.0;
    /** Lengths (ticks) of the conditions the actives apply. Hard control goes through the Balance Law's control limits. */
    public static final int STUTTER_BULLET_TICKS = 40, GAZE_LOCK_TICKS = 30, REJECTION_TICKS = 60, SEAL_ON_HIT_TICKS = 40, CHANNEL_LOCK_TICKS = 20, GAZE_PERIOD_TICKS = 20;

    /** Inevitable Demise: the damage multiplier from the target's remaining health share (x1 at full health, x2 at none). */
    public static double demiseMultiplier(double healthShare) {
        double s = Math.max(0.0, Math.min(1.0, healthShare));
        return 1.0 + (1.0 - s);
    }

    /** Temporal Debt makes a target frailer to magic: +1% per stack, at most +50%. */
    public static double magicVulnerability(int debt) {
        return 1.0 + Math.min(0.5, Math.max(0, debt) * 0.01);
    }

    /** The bonus damage Executioner's Authority adds from the armour points of the target (never negative, never above the cap). */
    public static double armourPierce(double armourPoints) {
        return Math.max(0.0, Math.min(ARMOUR_PIERCE_MAX, armourPoints * ARMOUR_PIERCE_PER_POINT));
    }

    /** The damage of detonating a debt: stacks x per-stack x multiplier, at least {@code floor} when there is any debt, never above {@code cap}. */
    public static double detonation(int debt, double perStack, double multiplier, double floor, double cap) {
        if (debt <= 0) return Math.min(cap, floor);
        return Math.min(cap, Math.max(floor, debt * perStack * multiplier));
    }

    /** True when health is at or below the share (and the share is a real fraction of a positive maximum). */
    public static boolean atOrBelow(double health, double max, double share) {
        return max > 0.0 && health / max <= share;
    }

    /** True when the look direction of a watcher (unit vectors) points at the watched: the dot of the watcher's view with the line to the watched is at least {@code cosine}. */
    public static boolean looksAt(double vx, double vy, double vz, double dx, double dy, double dz, double cosine) {
        double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (dl < 1.0E-6 || vl < 1.0E-6) return false;
        return (vx * dx + vy * dy + vz * dz) / (vl * dl) >= cosine;
    }

    /** True when a point is inside the cone of a sweep: within {@code reach} of the origin and within {@code halfAngleDeg} of the facing (the facing is a unit vector on the ground plane). */
    public static boolean inCone(double fx, double fz, double dx, double dz, double reach, double halfAngleDeg) {
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > reach) return false;
        if (dist < 1.0E-6) return true;
        double fl = Math.sqrt(fx * fx + fz * fz);
        if (fl < 1.0E-6) return false;
        double cos = (fx * dx + fz * dz) / (fl * dist);
        return cos >= Math.cos(Math.toRadians(halfAngleDeg));
    }

    // ---------------------------------------------------------------- toggle bits
    /** Bit of a toggle mode in the mask the skill keeps on the player (modes 0..3). */
    public static int bit(int mode) { return mode >= 0 && mode < 4 ? 1 << mode : 0; }

    public static boolean on(int mask, int mode) { return (mask & bit(mode)) != 0; }

    public static int flip(int mask, int mode) { return mask ^ bit(mode); }
}
