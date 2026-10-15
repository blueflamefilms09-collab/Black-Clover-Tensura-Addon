package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the head relics of wave 26 (the masks, the helms, the goggles, the lens and the Mind stone) turn a handful of tabletop rules into
 * Minecraft numbers, and those numbers and small state machines are easier to trust when they can be tested without a game: how a "disadvantage"
 * becomes a damage factor, how Frank's commands and failures count, which Voodoo spell a d4 picks, how a saving throw becomes an existence
 * (EP) comparison, and what the Lens of Straight Creepin' remembers. Pure Java (no Minecraft types). One long rest is a Minecraft day (24000
 * ticks), one tabletop hour is twenty minutes (24000 ticks), one round is 6 seconds (120 ticks), 5 ft is 1.5 blocks.
 */
public final class Wave26Rules {
    private Wave26Rules() {}

    public static final int DAY = 24000, HOUR = 24000, MINUTE = 1200, ROUND = 120;

    /** The one flag every relic of the wave carries (a single cheap lookup in the handlers), then one flag per relic that needs a handler. */
    public static final String F_ANY = "w26_any", F_FRANK = "w26_frank", F_GOGGLES = "w26_goggles", F_HELM = "w26_helm", F_BUTT = "w26_butt",
            F_JELLY = "w26_jelly", F_LENS = "w26_lens", F_DIFF = "w26_diff", F_INDIFF = "w26_indiff", F_WITCH = "w26_witch", F_MIND = "w26_mind";

    /** The Minecraft day a world time falls in. */
    public static long dayOf(long dayTime) { return Math.floorDiv(dayTime, (long) DAY); }

    /** A d{@code sides} (1 to sides) from a bounded random source. */
    public static int die(java.util.function.IntUnaryOperator rng, int sides) { return 1 + Math.floorMod(rng.applyAsInt(Math.max(1, sides)), Math.max(1, sides)); }

    /** Did this saving throw succeed? d20 plus a bonus against a difficulty class; a natural 20 always does, a natural 1 never. */
    public static boolean saves(int d20, int bonus, int dc) {
        if (d20 >= 20) return true;
        if (d20 <= 1) return false;
        return d20 + bonus >= dc;
    }

    /** The saving throw as Tensura existence: a creature whose maximum EP is more than three times the caster's shrugs the spell off. */
    public static boolean epSaves(double targetMaxEp, double casterMaxEp) { return targetMaxEp > Math.max(1.0, casterMaxEp) * 3.0; }

    // ================================================================ Frank
    /** Frank's Foresight lasts one tabletop hour (twenty minutes) and may be used once a day. */
    public static final int FORESIGHT_TICKS = HOUR;
    /** "Advantage" on the wearer's blows and "disadvantage" on everyone else's against the wearer, as damage factors. */
    public static final float FORESIGHT_OUT = 1.20f, FORESIGHT_IN = 0.80f;
    /** The curse: without the mask on, the carrier's blows are weaker (disadvantage on attack rolls). */
    public static final float UNMASKED_OUT = 0.70f;
    /** Failing Frank this many times earns the nighttime visit. */
    public static final int FAILS_FOR_VISIT = 5;
    /** How long the wearer has to carry out a command, and how long Frank waits before the next one. */
    public static final int COMMAND_WINDOW = 3 * MINUTE, COMMAND_GAP = 2 * MINUTE;
    /** The countdown of the final curse, in Minecraft days (the card's 28 days). */
    public static final int COUNTDOWN_DAYS = 28;
    /** A golden apple is the nearest "remove curse" (lifts the unmasked disadvantage for a day); an enchanted one is the nearest "wish". */
    public static final int REMOVE_CURSE_TICKS = DAY;

    /** What Frank asks of the wearer: twisting a moral compass into mischief a Minecraft player can actually perform. */
    public enum Command {
        STRIKE_PEACEFUL("Strike a peaceful creature"), THROW_AWAY("Throw something away"), HOLD_STILL("Hold still, crouched, for ten seconds");
        public final String text;
        Command(String text) { this.text = text; }
    }

    /** The command a given round of Foresight asks for (cycling). */
    public static Command commandFor(int round) { return Command.values()[Math.floorMod(round, Command.values().length)]; }

    /** Crouched and still for this many ticks counts as holding still. */
    public static final int STILL_TICKS = 200;

    /** The outgoing factor of a blow: weaker while the carrier has the mask off and the curse is not lifted, stronger under Foresight. */
    public static float outFactor(boolean foresight, boolean unmaskedCursed) {
        float f = 1.0f;
        if (foresight) f *= FORESIGHT_OUT;
        if (unmaskedCursed) f *= UNMASKED_OUT;
        return f;
    }

    /** Failures after one more failure. */
    public static int afterFail(int fails) { return Math.max(0, fails) + 1; }

    /** Has Frank earned the nighttime visit? */
    public static boolean visitEarned(int fails) { return fails >= FAILS_FOR_VISIT; }

    /** The days left on the countdown that started on {@code startDay}; zero once the 28 days are spent. */
    public static int daysLeft(long startDay, long today) { return (int) Math.max(0, COUNTDOWN_DAYS - Math.max(0, today - startDay)); }

    // ================================================================ Goggles of Dwarvenkin
    /** How far (blocks) the goggles see hidden stonework and traps, and how many marks they show at most (60 ft is 18 blocks; kept tight). */
    public static final int GOGGLES_RADIUS = 14, GOGGLES_MARKS = 40, GOGGLES_COOLDOWN = 20 * 20;
    /** 20% chance a dawn grows a magical beard. */
    public static boolean growsBeard(int d100) { return d100 >= 1 && d100 <= 20; }
    /** The beard lasts until the next dawn; it stiffens the wearer: knockback resistance. */
    public static final double BEARD_KNOCKBACK = 0.2;

    // ================================================================ Helm of Whispers
    /** Dominate Person (3 a day): a minute under the helm's will. */
    public static final int DOMINATE_TICKS = MINUTE, DOMINATE_RANGE = 14, DOMINATE_MAX_HEALTH = 200;
    /** Psychic Scream (1 a day): radius, damage, fright. */
    public static final double SCREAM_RADIUS = 10.0;
    public static final float SCREAM_DAMAGE = 7.0f;
    public static final int SCREAM_FEAR_TICKS = 6 * 20, SCREAM_STUN_TICKS = 40;
    /** The curse of Voices: a Wisdom save against DC 16 each dawn (the helm's own +3 counted), a failure is a spell of madness. */
    public static final int VOICES_DC = 16, VOICES_BONUS = 3, MADNESS_TICKS = 90 * 20;
    /** Did the dawn's save fail? */
    public static boolean madnessStrikes(int d20) { return !saves(d20, VOICES_BONUS, VOICES_DC); }

    // ================================================================ Helmet of Headbutting
    public static final int HEADBUTT_COOLDOWN = 2 * 20 + 10, STUN_TICKS = 50;
    public static final double HEADBUTT_REACH = 2.8;
    /** 1d8 bludgeoning (a d8 from the dice source, clamped). */
    public static float headbuttDamage(int d8) { return Math.max(1, Math.min(8, d8)); }
    /** A "critical hit": the wearer was falling (a vanilla critical), or the d20 came up natural 20. */
    public static boolean headbuttCrit(boolean falling, int d20) { return falling || d20 >= 20; }
    /** The splitting headache: every this many ticks of wear, a bout (a few seconds of nausea). */
    public static final int HEADACHE_EVERY = 3 * MINUTE, HEADACHE_TICKS = 70;

    // ================================================================ Jellyfish Cap
    public static final double WRATH_RANGE = 9.0;
    public static final int WRATH_COOLDOWN = 6 * 20, WRATH_POISON_TICKS = 60;
    /** 1d6 poison damage, halved on a successful save. */
    public static float wrathDamage(int d6, boolean saved) {
        float d = Math.max(1, Math.min(6, d6));
        return saved ? d / 2f : d;
    }

    // ================================================================ Lens of Straight Creepin'
    public static final int LEDGER_CAP = 240, LEDGER_EVERY = 40;
    /** How old a track can be and still be seen (ten minutes), how far the lens looks, how many tracks it draws at most. */
    public static final long TRACK_AGE = 10L * MINUTE;
    public static final double LENS_RADIUS = 40.0;
    public static final int LENS_MARKS = 60;

    /** One footprint: where a creature stood, when, and what sort (0 monster, 1 animal, 2 person). */
    public record Mark(double x, double y, double z, long tick, int kind) {}

    /** The lens's memory of what moved past: a bounded ring (the oldest tracks are forgotten first), and a creature is noted again only after it has moved. */
    public static final class Ledger {
        private final List<Mark> marks = new ArrayList<>();
        public int size() { return marks.size(); }

        public void add(double x, double y, double z, long tick, int kind) {
            if (!marks.isEmpty()) {                                               // standing still leaves no new print on top of the last one
                Mark last = marks.get(marks.size() - 1);
                double dx = last.x() - x, dz = last.z() - z;
                if (last.kind() == kind && dx * dx + dz * dz < 0.25 && Math.abs(last.y() - y) < 1.5 && tick - last.tick() < LEDGER_EVERY) return;
            }
            marks.add(new Mark(x, y, z, tick, kind));
            while (marks.size() > LEDGER_CAP) marks.remove(0);
        }

        /** Tracks no older than {@code maxAge} ticks and within {@code radius} of the point, newest first, at most {@code limit}. */
        public List<Mark> recent(long now, long maxAge, double cx, double cz, double radius, int limit) {
            List<Mark> out = new ArrayList<>();
            for (int i = marks.size() - 1; i >= 0 && out.size() < limit; i--) {
                Mark m = marks.get(i);
                if (now - m.tick() > maxAge || now < m.tick()) continue;
                double dx = m.x() - cx, dz = m.z() - cz;
                if (dx * dx + dz * dz <= radius * radius) out.add(m);
            }
            return out;
        }

        public void clear() { marks.clear(); }
    }

    // ================================================================ Mask of Difference and Indifference
    public static final int FRIENDS_TICKS = MINUTE, COMMAND_HALT_TICKS = 3 * 20, SUGGEST_TICKS = 40 * 20;
    public static final double HALT_RADIUS = 9.0;
    /** The masks chatter every few minutes; Difference sulks when the wearer stands about for twenty minutes (her "an hour"). */
    public static final int CHATTER_EVERY = 4 * MINUTE;
    public static final long BORED_AFTER = HOUR;
    public static boolean bored(long idleTicks) { return idleTicks >= BORED_AFTER; }

    // ================================================================ Mask of Mind Melting
    public static final int MELT_COOLDOWN = 2 * MINUTE;
    public static final double MELT_REACH = 3.5;
    /** The memory is experience: twice what the mind was worth, 10 to 120 points. */
    public static int memoryXp(int base) { return Math.max(10, Math.min(120, Math.max(0, base) * 2)); }
    /** 30% chance the mask takes one of the wearer's own memories in return. */
    public static boolean stealsMemory(int d100) { return d100 >= 1 && d100 <= 30; }
    /** The memory it takes: 1 to 3 levels of experience, never more than the wearer has. */
    public static int levelsLost(int level, int d3) { return Math.max(0, Math.min(Math.max(0, level), Math.max(1, Math.min(3, d3)))); }
    /** Out cold: asleep, put under by a spell, or beaten to a sliver (15% health). */
    public static boolean unconscious(boolean sleeping, boolean sedated, float health, float max) { return sleeping || sedated || (max > 0 && health <= max * 0.15f); }

    // ================================================================ Mask of the Raven Queen's Disciple
    /** Spare the Dying: anyone at 30% health or 6 points (whichever is higher) or less counts as dying; the cantrip lifts them to at least 6 health. */
    public static boolean dying(float health, float max) { return health <= Math.max(6f, max * 0.30f); }
    public static float sparedTo(float health, float max) { return Math.min(max, Math.max(health, 6f)); }

    // ================================================================ Mask of the Witchdoctor
    /** The four Voodoo spells a d4 picks. */
    public enum Voodoo { WITCH_BOLT, HEX, FROG, HYPNOTIC_PATTERN }

    public static Voodoo voodoo(int d4) { return Voodoo.values()[Math.max(1, Math.min(4, d4)) - 1]; }

    public static final double VOODOO_RANGE = 18.0;
    public static final float BOLT_DAMAGE = 7.0f;
    public static final int HEX_TICKS = 20 * 20, FROG_TICKS = 15 * 20, HYPNOSIS_TICKS = 8 * 20;

    // ================================================================ Mind stone
    public static final int MASS_SUGGESTION_TICKS = 3 * MINUTE;
    public static final double MASS_SUGGESTION_RADIUS = 12.0;

    /** Charm strength for a suggestion: the full time for a creature that does not save, nothing for one that does. */
    public static int charmTicks(int ticks, boolean saved) { return saved ? 0 : Math.max(0, ticks); }
}
