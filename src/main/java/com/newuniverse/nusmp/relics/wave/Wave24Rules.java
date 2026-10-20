package com.newuniverse.nusmp.relics.wave;

import java.util.Arrays;
import java.util.Locale;

/**
 * Why: Relics of Many Worlds part 4, wave 24 (twelve small wondrous items and rings): every number and table the relics use lives here with no
 * Minecraft imports, so the rules can be tested alone. The tabletop's dice, rests and saving throws become the ticks, caps and chances below.
 * <p>
 * Tabletop becomes Minecraft: a long rest is a Minecraft day (24000 ticks), a short rest a few minutes, a round six seconds, five feet one block,
 * a d20 save is a roll plus a bonus against a difficulty class. The callers pass the game's own random source in as a {@link Dice}.
 */
public final class Wave24Rules {
    private Wave24Rules() {}

    public static final long DAY = 24000L, WEEK = 7 * DAY;

    /** The game's random source, as {@code nextInt(bound)}. */
    public interface Dice { int next(int bound); }

    public static int d(Dice r, int sides) { return 1 + Math.floorMod(r.next(Math.max(1, sides)), Math.max(1, sides)); }

    public static int roll(Dice r, int n, int sides) {
        int sum = 0;
        for (int i = 0; i < Math.max(0, Math.min(n, 40)); i++) sum += d(r, sides);
        return sum;
    }

    /** The Minecraft day a world time falls on. */
    public static long day(long gameTime) { return Math.floorDiv(Math.max(0L, gameTime), DAY); }

    /** A d20 saving throw: a natural 20 always holds, a natural 1 always fails, otherwise roll plus bonus must reach the difficulty class. */
    public static boolean saves(int d20, int bonus, int dc) {
        if (d20 >= 20) return true;
        if (d20 <= 1) return false;
        return d20 + bonus >= dc;
    }

    /** The saving bonus a creature of this size has: a point per ten health, at most five. */
    public static int saveBonus(double maxHealth) { return (int) Math.max(0, Math.min(5, Math.floor(maxHealth / 10.0))); }

    /** A creature with at least this much health counts as a boss and shrugs off the long effects by half. */
    public static final double BOSS_HEALTH = 100.0;

    public static boolean isBoss(double maxHealth) { return maxHealth >= BOSS_HEALTH; }

    /** How long an effect lasts on a creature that saved (half) or not (all); a boss halves it again. */
    public static int afterSave(int base, boolean saved, double victimMaxHealth) {
        int t = saved ? base / 2 : base;
        if (isBoss(victimMaxHealth)) t /= 2;
        return Math.max(0, t);
    }

    // ================================================================ Instant Cooking Pot
    public static final int COOK_MAX_ITEMS = 16, COOK_TICKS_PER_DIE = 100;

    private static final String[][] COOKING = {
            {"minecraft:beef", "minecraft:cooked_beef"}, {"minecraft:porkchop", "minecraft:cooked_porkchop"},
            {"minecraft:chicken", "minecraft:cooked_chicken"}, {"minecraft:mutton", "minecraft:cooked_mutton"},
            {"minecraft:rabbit", "minecraft:cooked_rabbit"}, {"minecraft:cod", "minecraft:cooked_cod"},
            {"minecraft:salmon", "minecraft:cooked_salmon"}, {"minecraft:potato", "minecraft:baked_potato"},
            {"minecraft:kelp", "minecraft:dried_kelp"}, {"minecraft:chorus_fruit", "minecraft:popped_chorus_fruit"}};

    /** The cooked item id for a raw food id, or null when the pot has nothing to do with it. */
    public static String cooked(String rawId) {
        if (rawId == null) return null;
        for (String[] row : COOKING) if (row[0].equals(rawId)) return row[1];
        return null;
    }

    /** The microwave time: a d4 of five-second steps (five to twenty seconds). */
    public static int cookTicks(int d4) { return Math.max(1, Math.min(4, d4)) * COOK_TICKS_PER_DIE; }

    public static int cookCount(int held) { return Math.max(0, Math.min(COOK_MAX_ITEMS, held)); }

    // ================================================================ Lucky Egg
    public static final int EGG_ARMED_TICKS = 600, EGG_USES_PER_DAY = 2;
    /** A hit this big (health points) is worth a reroll. */
    public static final float EGG_MIN_HIT = 2.0f;

    /** The damage after the egg forces the die to be rolled again: a fresh roll between 30% and 80% of the old one, so it is never worse. {@code r} is 0 up to 1. */
    public static float rerolled(float damage, double r) {
        double x = Math.max(0.0, Math.min(0.999999, r));
        return (float) (damage * (0.30 + 0.50 * x));
    }

    // ================================================================ Lumpy
    public static final int LUMPY_QUIET_TICKS = 1200, LUMPY_SCAN_RADIUS = 16, LUMPY_PULL_RADIUS = 10, LUMPY_MESSAGE_RADIUS = 64, LUMPY_THOUGHTS = 6;
    public static final double LUMPY_BLOCK_REACH = 8.0;

    public enum Mood { HURT, KILL, LOW, DEATH }

    private static final String[][] LUMPY = {
            {"Ooooh, did the big mean monster hit you? Maybe stand somewhere it can't reach, silly.",
                    "Ouch! Again? Have you considered not being in the way?",
                    "That looked painful. For you. Hee hee!",
                    "I would help, but I am a sock."},
            {"Yay, you hit it! With the pointy end, even! Clever you, for once.",
                    "One less monster! Your technique was dreadful, but it worked.",
                    "Bravo! A toddler could have done that. Probably faster.",
                    "Splat! Do tell the story, I am sure it was longer than the fight."},
            {"Sweetie, you are almost dead. That is a bad thing. Try not being almost dead.",
                    "Little tip from Lumpy: the red hearts are meant to stay full.",
                    "Run away, run away! Or lie down. Both work. One is slower.",
                    "You are looking pale. Paler. Oh, that is the colour of a corpse, how nice."},
            {"Oh dear. And I thought you might finally get good at this.",
                    "Night night! We will practise tomorrow, shall we?"}};

    public static String lumpyLine(Mood m, int pick) {
        String[] lines = LUMPY[Math.max(0, Math.min(Mood.values().length - 1, m.ordinal()))];
        return lines[Math.floorMod(pick, lines.length)];
    }

    /** Whether Lumpy may speak again: quiet for a minute after every remark. */
    public static boolean lumpyMayTalk(long now, long lastSpoke) { return lastSpoke <= 0 || now - lastSpoke >= LUMPY_QUIET_TICKS || now < lastSpoke; }

    /** Low health for the puppet's taste: a quarter of the maximum or less. */
    public static boolean lowHealth(float health, float max) { return max > 0f && health > 0f && health <= max * 0.25f; }

    // ================================================================ Monster Compass
    public static final int COMPASS_RANGE = 64;

    /** A creature's threat from 0 (pale gold eye) to 4 (black eye): its health plus three times its attack damage, a boss always black. */
    public static int threat(double maxHealth, double attackDamage) {
        if (isBoss(maxHealth)) return 4;
        double score = Math.max(0.0, maxHealth) + 3.0 * Math.max(0.0, attackDamage);
        if (score < 30.0) return 0;
        if (score < 55.0) return 1;
        if (score < 90.0) return 2;
        if (score < 160.0) return 3;
        return 4;
    }

    private static final String[] EYES = {"pale gold", "amber", "ember red", "crimson", "black"};

    public static String eye(int threat) { return EYES[Math.max(0, Math.min(EYES.length - 1, threat))]; }

    /** The compass point of a target relative to where the wearer faces: 0 ahead, 1 ahead-right, 2 right ... 7 ahead-left. Yaw is Minecraft's (0 faces south, 90 west). */
    public static int bearing(double dx, double dz, double facingYawDeg) {
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = ((target - facingYawDeg) % 360.0 + 360.0) % 360.0;
        return (int) Math.round(rel / 45.0) % 8;
    }

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    public static String arrow(int bearing) { return ARROWS[Math.floorMod(bearing, 8)]; }

    // ================================================================ Official-Looking Badge
    public static final double BADGE_RADIUS = 8.0, BADGE_MAX_HEALTH = 60.0;
    public static final int BADGE_FEAR_TICKS = 100, BADGE_HALT_TICKS = 200;

    /** The badge works on ordinary creatures: not on a boss and not on anything with more than 60 health. */
    public static boolean respects(double victimMaxHealth) { return victimMaxHealth < BADGE_MAX_HEALTH && !isBoss(victimMaxHealth); }

    // ================================================================ Orb of Plagues
    public static final int ORB_MAX_CHARGES = 10, COST_SICKNESS = 1, COST_ENFEEBLE = 2, COST_CLOUD = 3, COST_BLIGHT = 4, COST_CONTAGION = 5;
    public static final double PLAGUE_RADIUS = 7.0, CLOUD_RADIUS = 4.0, ORB_RANGE = 20.0, DETECT_RADIUS = 24.0;
    public static final int DISEASE_TICKS = 600;

    /** Charges the orb has after a dawn: it regains 1d6+3 once for every new Minecraft day, up to ten. */
    public static int regained(int charges, long lastDay, long today, int d6) {
        int c = Math.max(0, Math.min(ORB_MAX_CHARGES, charges));
        if (today <= lastDay) return c;
        return Math.min(ORB_MAX_CHARGES, c + Math.max(1, Math.min(6, d6)) + 3);
    }

    /** An empty orb shatters on a natural 1 of a d20. */
    public static boolean shatters(int d20) { return d20 <= 1; }

    /** The difficulty class of the Plague Cloud: 10 plus the charges left. */
    public static int plagueDc(int chargesLeft) { return 10 + Math.max(0, Math.min(ORB_MAX_CHARGES, chargesLeft)); }

    /** The six diseases of Contagion as effect lists ("id:amplifier"); the ids a world lacks are skipped by the caller. */
    public enum Disease {
        BLINDING_SICKNESS("Blinding Sickness", "minecraft:blindness:0", "minecraft:nausea:0"),
        FILTH_FEVER("Filth Fever", "minecraft:weakness:1", "minecraft:mining_fatigue:1"),       // 1.99.0, the hunger ban: fatigue where the Hunger effect was
        FLESH_ROT("Flesh Rot", "minecraft:wither:0", "tensura:fragility:0"),
        MINDFIRE("Mindfire", "minecraft:nausea:0", "tensura:confusion:0", "minecraft:slowness:0"),
        SEIZURE("Seizure", "minecraft:slowness:1", "minecraft:mining_fatigue:1"),
        SLIMY_DOOM("Slimy Doom", "minecraft:poison:1", "minecraft:weakness:0");

        public final String label;
        public final String[] effects;

        Disease(String label, String... effects) {
            this.label = label;
            this.effects = effects;
        }
    }

    public static Disease disease(int d6) { return Disease.values()[Math.floorMod(d6 - 1, Disease.values().length)]; }

    /** Splits "minecraft:wither:0" into id and amplifier. */
    public static String effectId(String spec) { return spec.substring(0, spec.lastIndexOf(':')); }

    public static int effectAmp(String spec) {
        try {
            return Integer.parseInt(spec.substring(spec.lastIndexOf(':') + 1));
        } catch (NumberFormatException bad) {
            return 0;
        }
    }

    // ================================================================ Orb of the Dryad
    public static final double DRYAD_RANGE = 20.0, DRYAD_LINE_WIDTH = 1.4;
    public static final int DRYAD_PULSES = 4, DRYAD_PULSE_TICKS = 120, DRYAD_LIGHT_TICKS = 260;

    /** Health points one touch of the orb heals: 1d4+1. */
    public static float dryadHeal(int d4) { return 1 + Math.max(1, Math.min(4, d4)); }

    /** The orb is ethereal: it flies through at most one solid block on its way (a foot of stone), a thick wall stops it. */
    public static boolean dryadBlocked(int solidBlocksOnTheWay) { return solidBlocksOnTheWay > 1; }

    /** The orb takes about a block a tick to reach its target. */
    public static int flightTicks(double distance) { return (int) Math.max(1, Math.min(60, Math.ceil(distance))); }

    // ================================================================ Pocket Time
    public static final long CLOCK_STEP = 1000L;

    /** The hour a world time shows on the clock (0 to 23; tick 0 is 6:00). */
    public static int clockHour(long dayTime) { return (int) (Math.floorMod(dayTime, DAY) / CLOCK_STEP + 6) % 24; }

    /**
     * Ticks to skip forward so that the clock next reads {@code hour} o'clock on a twelve-hour face (the hour or the one twelve hours later,
     * whichever comes first, never zero): at most twelve hours.
     */
    public static long ticksForward(long dayTime, int d12) {
        int h = Math.max(1, Math.min(12, d12));
        long tod = Math.floorMod(dayTime, DAY);
        long a = Math.floorMod((h - 6) * CLOCK_STEP - tod, DAY), b = Math.floorMod((h + 6) * CLOCK_STEP - tod, DAY);
        if (a == 0) a = DAY;
        if (b == 0) b = DAY;
        return Math.min(a, b);
    }

    public static String clockText(long dayTime) {
        long tod = Math.floorMod(dayTime, DAY);
        int h = clockHour(dayTime), m = (int) (tod % CLOCK_STEP * 60 / CLOCK_STEP);
        return String.format(Locale.ROOT, "%02d:%02d", h, m);
    }

    // ================================================================ Ring of Endless Rage
    public static final int RAGE_TICKS = 600;
    public static final int RAGE_SAVE_DC = 10;

    public enum Aftermath { STABLE_GOOD, STABLE, BARELY, EXHAUSTED }

    /** Three death saves (a d20 each, ten or more holds), rolled when the rage ends: the result decides what is left of you. */
    public static int deathSaves(Dice r) {
        int ok = 0;
        for (int i = 0; i < 3; i++) if (d(r, 20) >= RAGE_SAVE_DC) ok++;
        return ok;
    }

    public static Aftermath aftermath(int successes) {
        if (successes >= 3) return Aftermath.STABLE_GOOD;
        if (successes == 2) return Aftermath.STABLE;
        if (successes == 1) return Aftermath.BARELY;
        return Aftermath.EXHAUSTED;
    }

    /** The share of maximum health the bearer wakes with. Nothing kills outright: the worst result is a single half heart and heavy weakness. */
    public static double aftermathHealth(Aftermath a) {
        return switch (a) {
            case STABLE_GOOD -> 0.50;
            case STABLE -> 0.30;
            case BARELY -> 0.10;
            case EXHAUSTED -> 0.0;
        };
    }

    /** The damage a raging bearer still takes: never enough to bring health under one. */
    public static float rageGuard(float health, float damage) { return Math.max(0f, Math.min(damage, health - 1f)); }

    /** A hit that would end the bearer: health (with absorption) is not more than the damage. */
    public static boolean lethal(float health, float absorption, float damage) { return damage > 0f && damage >= health + Math.max(0f, absorption); }

    // ================================================================ Ring of Power
    public static final double POWER_RANGE = 30.0;
    public static final int DISINTEGRATE_DICE = 10;

    /** The extra force damage of a weapon attack (1d8); on a player it is held to a third of that player's cap-free share: at most 4. */
    public static float forceBonus(int d8, boolean victimIsPlayer) { return Math.max(1, Math.min(8, victimIsPlayer ? Math.min(d8, 4) : d8)); }

    /** Disintegrate: 10d6 force damage. */
    public static float disintegrate(Dice r) { return roll(r, DISINTEGRATE_DICE, 6); }

    // ================================================================ Ring of Regeneration
    public static final int REGEN_EVERY_TICKS = 400, REGROW_EVERY_TICKS = 100;

    /** Health points the ring returns each time: 1d6. */
    public static float regenAmount(int d6) { return Math.max(1, Math.min(6, d6)); }

    /** A crippling effect the ring regrows: it is cut in half every pass, and ends outright below this many ticks. */
    public static final int REGROW_END_BELOW = 100;

    public static int regrown(int remainingTicks) { return remainingTicks <= REGROW_END_BELOW ? 0 : remainingTicks / 2; }

    public static final String[] CRIPPLING = {"minecraft:slowness", "minecraft:weakness", "minecraft:mining_fatigue", "minecraft:wither", "minecraft:poison",
            "minecraft:blindness", "minecraft:nausea", "tensura:fragility", "tensura:infection", "tensura:corrosion", "tensura:paralysis", "tensura:petrification"};

    // ================================================================ Ring of Memories
    public static final int TRAIL_STRIDE = 4, TRAIL_MAX = 150, TRAIL_EVERY_TICKS = 40, TRANCE_TICKS = 200, TRANCE_STEPS = 10;

    /** Adds a sample [time, x, y, z] to a trail (flat array, {@link #TRAIL_STRIDE} longs a sample), dropping the oldest beyond the maximum. */
    public static long[] push(long[] trail, long time, int x, int y, int z) {
        long[] t = trail == null ? new long[0] : trail;
        int n = t.length / TRAIL_STRIDE;
        int keep = Math.min(n, TRAIL_MAX - 1);
        long[] out = new long[(keep + 1) * TRAIL_STRIDE];
        System.arraycopy(t, (n - keep) * TRAIL_STRIDE, out, 0, keep * TRAIL_STRIDE);
        int o = keep * TRAIL_STRIDE;
        out[o] = time;
        out[o + 1] = x;
        out[o + 2] = y;
        out[o + 3] = z;
        return out;
    }

    /** A memory must have happened within the past week. */
    public static boolean storable(long now, long when) { return now >= when && now - when <= WEEK; }

    /** The samples of a trail that are still within the week. */
    public static long[] prune(long[] trail, long now) {
        if (trail == null) return new long[0];
        int n = trail.length / TRAIL_STRIDE, first = 0;
        while (first < n && !storable(now, trail[first * TRAIL_STRIDE])) first++;
        return Arrays.copyOfRange(trail, first * TRAIL_STRIDE, n * TRAIL_STRIDE);
    }

    public static int samples(long[] trail) { return trail == null ? 0 : trail.length / TRAIL_STRIDE; }

    /** The recorded span in game ticks (first to last sample), never more than an hour. */
    public static long span(long[] trail) {
        int n = samples(trail);
        if (n < 2) return 0L;
        return Math.min(72000L, trail[(n - 1) * TRAIL_STRIDE] - trail[0]);
    }

    /** Which samples a trance step shows: the range [from, to) of the memory, spread evenly over {@link #TRANCE_STEPS} seconds. */
    public static int[] tranceRange(int sampleCount, int step) {
        int n = Math.max(0, sampleCount), s = Math.max(0, Math.min(TRANCE_STEPS - 1, step));
        int from = n * s / TRANCE_STEPS, to = n * (s + 1) / TRANCE_STEPS;
        if (s == TRANCE_STEPS - 1) to = n;
        return new int[]{from, Math.max(from, to)};
    }

    /** A memory is only as clear as it is close: the farther the wearer is from where it happened, the sparser the replay (every k-th sample). */
    public static int clarityStride(double distanceBlocks) {
        if (distanceBlocks < 32) return 1;
        if (distanceBlocks < 128) return 2;
        if (distanceBlocks < 512) return 4;
        return 8;
    }
}
