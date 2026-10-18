package com.newuniverse.nusmp.elite.skills.rules;

import com.newuniverse.nusmp.skill.rules.SkillMath;

/**
 * Why: the five Extra skills (Ancient Soul, Iron Resolve, Meditation, Runesmith's Insight, Scavenger) are mostly passives that react to what happens to the wearer. Every
 * threshold they use is decided here, without Minecraft types, so the "when does it trigger and how much" part can be unit tested. Numbers are designed for this mod
 * (the original addon's source was not available) and sit before the config multipliers.
 */
public final class ExtraRules {
    private ExtraRules() {}

    // ======================================================================================= Ancient Soul
    /** Kills feed fragments of old souls; fragments are spent to mend the body. Passive: a soul that has lived before shrugs off part of any magic. */
    public static final class AncientSoul {
        private AncientSoul() {}

        public static final int CAP = 100;
        public static final int SPEND = 20;
        public static final double COST = 250.0;
        public static final int COOLDOWN = 200;

        public static int gainForKill(boolean boss) { return boss ? 5 : 1; }

        public static int add(int have, int gain) { return SkillMath.clamp(have + Math.max(0, gain), 0, CAP); }

        public static boolean canSpend(int have) { return have >= SPEND; }

        public static int spend(int have) { return canSpend(have) ? have - SPEND : have; }

        /** Share of max health restored by one Echo of Ages. */
        public static double healShare(boolean mastered) { return mastered ? 0.45 : 0.30; }

        /** Share of magic / spirit damage that never lands. */
        public static double magicReduction(boolean mastered) { return mastered ? 0.20 : 0.12; }
    }

    // ======================================================================================= Iron Resolve
    /** The lower the health, the harder to move: every hit taken while wounded stacks Resolve, and Brace holds the line for a moment. */
    public static final class IronResolve {
        private IronResolve() {}

        public static final double WOUNDED_BELOW = 0.5;
        public static final int WINDOW_TICKS = 160;
        public static final double PER_STACK = 0.05;
        public static final double MAX_REDUCTION = 0.45;
        public static final double BRACE_REDUCTION = 0.5;
        public static final int BRACE_TICKS = 60;
        public static final double BRACE_COST = 300.0;
        public static final int BRACE_COOLDOWN = 400;

        public static int maxStacks(boolean mastered) { return mastered ? 7 : 5; }

        /** The stack count after a hit taken at the given health share (above the wounded line nothing stacks, and the count is kept). */
        public static int stackAfterHit(int stacks, double healthShare, boolean mastered) {
            if (healthShare > WOUNDED_BELOW) return stacks;
            return Math.min(maxStacks(mastered), stacks + 1);
        }

        /** Share of incoming damage that is ignored at this stack count, while the stacks last. */
        public static double reduction(int stacks, boolean braced) {
            double r = Math.min(MAX_REDUCTION, PER_STACK * Math.max(0, stacks));
            return braced ? Math.max(r, BRACE_REDUCTION) : r;
        }

        public static boolean knockbackImmune(int stacks, boolean braced) { return braced || stacks >= 3; }

        /** Stacks expire together once this many ticks passed since the last stacking hit. */
        public static boolean expired(long now, long lastHit) { return now - lastHit > WINDOW_TICKS; }
    }

    // ======================================================================================= Meditation
    /** Standing still and unhurt builds Focus; Focus returns magicules and, deeper in, health. A toggle that breaks when you move or are hit. */
    public static final class Meditation {
        private Meditation() {}

        public static final int MAX_FOCUS = 5;
        public static final int SECONDS_PER_FOCUS = 6;
        public static final int COMBAT_LOCK_TICKS = 100;
        public static final double MOVE_EPSILON = 0.0025;   // squared distance per second

        public static int focus(int stillSeconds) {
            return SkillMath.clamp(Math.max(0, stillSeconds) / SECONDS_PER_FOCUS, 0, MAX_FOCUS);
        }

        /** Share of MAX magicules returned per second at this Focus. */
        public static double regenShare(int focus, boolean mastered) {
            if (focus <= 0) return 0.0;
            return (0.003 + 0.0035 * focus) * (mastered ? 1.4 : 1.0);
        }

        /** Hit points per second from Focus 3 on. */
        public static double healPerSecond(int focus) { return focus < 3 ? 0.0 : 0.5 * (focus - 2); }

        /** True if the wearer counts as keeping still between two samples one second apart. */
        public static boolean still(double movedSquared) { return movedSquared <= MOVE_EPSILON; }

        /** A hit taken this recently stops the meditation from building. */
        public static boolean inCombat(long now, long lastHurt) { return now - lastHurt < COMBAT_LOCK_TICKS; }
    }

    // ======================================================================================= Runesmith's Insight
    /** Read an item's runes, or inscribe one of four runes on the weapon in hand for a while. */
    public static final class Runesmith {
        private Runesmith() {}

        public enum Rune { FIRE, FROST, THUNDER, VITALITY }

        public static final double INSCRIBE_COST = 400.0;
        public static final int INSCRIBE_COOLDOWN = 100;
        public static final double READ_COST = 0.0;

        public static int durationTicks(boolean mastered) { return mastered ? 12000 : 6000; }

        /** Mode 0 reads, modes 1..4 inscribe FIRE, FROST, THUNDER, VITALITY. */
        public static Rune runeForMode(int mode) {
            Rune[] all = Rune.values();
            return mode >= 1 && mode <= all.length ? all[mode - 1] : null;
        }

        public static int igniteSeconds(boolean mastered) { return mastered ? 6 : 4; }

        public static int frostSlowTicks(boolean mastered) { return mastered ? 90 : 60; }

        /** Extra raw damage of a THUNDER rune on a hit of this size. */
        public static double thunderBonus(double hit, boolean mastered) { return hit * (mastered ? 0.30 : 0.20); }

        /** Health restored by a VITALITY rune on a hit of this size. */
        public static double vitalityHeal(double hit, boolean mastered) { return hit * (mastered ? 0.08 : 0.05); }

        public static boolean active(long now, long until) { return now < until; }
    }

    // ======================================================================================= Scavenger
    /** Kills sometimes leave more behind; the wearer can sniff out dropped items. */
    public static final class Scavenger {
        private Scavenger() {}

        public static final double SNIFF_COST = 150.0;
        public static final int SNIFF_COOLDOWN = 300;
        public static final int SNIFF_TICKS = 400;
        public static final int SNIFF_MAX_ITEMS = 32;

        public static double dupChance(boolean mastered) { return mastered ? 0.35 : 0.20; }

        /** True if the extra stack drops; {@code roll} in [0,1). */
        public static boolean duplicates(double roll, boolean mastered) { return roll < dupChance(mastered); }

        /** At most one extra stack per kill, whatever the drop list looks like. */
        public static int extraStacks(boolean duplicates, int dropCount) { return duplicates && dropCount > 0 ? 1 : 0; }

        public static double sniffRadius(boolean mastered) { return mastered ? 48.0 : 32.0; }
    }
}
