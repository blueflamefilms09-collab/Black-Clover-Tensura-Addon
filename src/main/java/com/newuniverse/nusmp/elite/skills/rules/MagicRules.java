package com.newuniverse.nusmp.elite.skills.rules;

import com.newuniverse.nusmp.skill.rules.SkillMath;

/**
 * Why: the three Aspectual magics (Kamehameha, Gravity Well, Static Chain) were designed here from the names in the owner's inventory (the original addon's source was not
 * available), so every number lives in one Minecraft-free class that JUnit can check: how long a charge takes, what a level costs, how far and wide a beam goes, how a well
 * pulls, how a chain picks its next victim. The skills only translate these answers into game calls. All numbers are BEFORE the config multipliers
 * (nusmp-elite-skills-server.toml) and before {@code BalanceLaw} (EP and mastery scaling, the PvP hit cap).
 */
public final class MagicRules {
    private MagicRules() {}

    // ======================================================================================= Kamehameha
    /** A held charge, then one piercing beam. The longer the hold, the higher the level (0..5); a hold below level 1 fizzles and costs nothing. */
    public static final class Kamehameha {
        private Kamehameha() {}

        public static final int MAX_LEVEL = 5;

        public static int ticksPerLevel(boolean mastered) { return mastered ? 14 : 20; }

        /** Level reached after {@code heldTicks} of holding. */
        public static int level(int heldTicks, boolean mastered) {
            return SkillMath.clamp(Math.max(0, heldTicks) / ticksPerLevel(mastered), 0, MAX_LEVEL);
        }

        /** Ticks of holding to the next level (0 at the top). */
        public static int ticksToNext(int heldTicks, boolean mastered) {
            int lv = level(heldTicks, mastered);
            return lv >= MAX_LEVEL ? 0 : (lv + 1) * ticksPerLevel(mastered) - Math.max(0, heldTicks);
        }

        public static boolean fires(int level) { return level >= 1; }

        /** Magicules, paid on release for the level reached. */
        public static double cost(int level) { return level < 1 ? 0.0 : 350.0 + 300.0 * level; }

        public static double range(int level) { return level < 1 ? 0.0 : 18.0 + 8.0 * level; }

        public static double width(int level) { return level < 1 ? 0.0 : 0.8 + 0.35 * level; }

        /** Raw damage to every foe on the beam (BalanceLaw scales it). */
        public static double damage(int level, boolean mastered) {
            return level < 1 ? 0.0 : (7.0 + 6.5 * level) * (mastered ? 1.15 : 1.0);
        }

        public static double knockback(int level) { return level < 1 ? 0.0 : 0.5 + 0.3 * level; }

        public static int cooldownTicks(int level) { return level < 1 ? 0 : 100 + 50 * level; }

        /** The firing stance: Slowness for this long, so the max beam is a commitment. */
        public static int recoilTicks(int level) { return level <= 1 ? 0 : 20 * (level - 1); }

        /** Whether the beam is wide enough to be drawn as the big nova at its end. */
        public static boolean finale(int level) { return level >= MAX_LEVEL; }
    }

    // ======================================================================================= Gravity Well
    /** A pull-and-crush zone at the look point, with an early Collapse mode. */
    public static final class GravityWell {
        private GravityWell() {}

        public static final int PULSE_INTERVAL = 10;
        public static final double CAST_RANGE = 24.0;
        public static final double COST = 600.0;
        public static final int COOLDOWN = 240;
        public static final double MAX_PULL = 0.7;

        public static double radius(boolean mastered) { return mastered ? 7.0 : 5.0; }

        public static int durationTicks(boolean mastered) { return mastered ? 160 : 120; }

        public static int pulses(boolean mastered) { return durationTicks(mastered) / PULSE_INTERVAL; }

        /** Share of the zone's strength at distance d from the centre: 1 at the centre, 0.4 at the rim, 0 outside. */
        public static double falloff(double d, double radius) {
            if (radius <= 0 || d > radius) return 0.0;
            return 0.4 + 0.6 * (1.0 - SkillMath.clamp(d / radius, 0.0, 1.0));
        }

        /** Velocity added toward the centre per pulse (blocks per tick), capped. */
        public static double pull(double d, double radius, boolean boss) {
            double f = falloff(d, radius);
            if (f <= 0) return 0.0;
            double p = SkillMath.clamp(0.12 + 0.5 * f, 0.0, MAX_PULL);
            return boss ? p * 0.4 : p;
        }

        /** Raw damage of one pulse at distance d. */
        public static double pulseDamage(double d, double radius, boolean mastered) {
            return (mastered ? 3.0 : 2.0) * falloff(d, radius);
        }

        /** The Collapse (end of the well or the early mode): this many pulses of damage at once. */
        public static double collapseDamage(double d, double radius, boolean mastered) {
            return pulseDamage(d, radius, mastered) * 4.0 + (falloff(d, radius) > 0.9 ? 6.0 : 0.0);
        }

        public static int slowTicks() { return 30; }

        /** The Collapse mode refunds nothing but costs only a tenth. */
        public static double collapseCost() { return COST * 0.1; }
    }

    // ======================================================================================= Static Chain
    /** Chained lightning that jumps from foe to foe, then tethers them together. */
    public static final class StaticChain {
        private StaticChain() {}

        public static final double CAST_RANGE = 20.0;
        public static final double JUMP_RANGE = 7.0;
        public static final double COST = 700.0;
        public static final int COOLDOWN = 160;
        public static final double FALLOFF = 0.85;
        public static final double TETHER_RADIUS = 5.0;

        public static int maxJumps(boolean mastered) { return mastered ? 6 : 4; }

        public static double baseDamage(boolean mastered) { return mastered ? 14.0 : 12.0; }

        /** Damage of the n-th link (0 = the first victim). */
        public static double linkDamage(int link, boolean mastered) {
            return baseDamage(mastered) * Math.pow(FALLOFF, Math.max(0, link));
        }

        public static int bindTicks(boolean mastered) { return mastered ? 80 : 60; }

        /**
         * The order the chain visits: start at {@code start}, then always the nearest unvisited point within {@code range} of the last one, up to {@code maxJumps}
         * further links. {@code pos[i]} is {x,y,z}. Returns the visited indices in order (start first).
         */
        public static int[] order(double[][] pos, int start, int maxJumps, double range) {
            if (pos == null || start < 0 || start >= pos.length) return new int[0];
            boolean[] seen = new boolean[pos.length];
            int[] out = new int[Math.min(pos.length, Math.max(0, maxJumps) + 1)];
            int n = 0, cur = start;
            out[n++] = cur;
            seen[cur] = true;
            while (n < out.length) {
                int best = -1;
                double bd = range * range;
                for (int i = 0; i < pos.length; i++) {
                    if (seen[i]) continue;
                    double dx = pos[i][0] - pos[cur][0], dy = pos[i][1] - pos[cur][1], dz = pos[i][2] - pos[cur][2];
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 <= bd) { bd = d2; best = i; }
                }
                if (best < 0) break;
                seen[best] = true;
                out[n++] = best;
                cur = best;
            }
            return java.util.Arrays.copyOf(out, n);
        }

        /** True if a tethered victim at distance d from the tether anchor must be pulled back. */
        public static boolean yank(double d) { return d > TETHER_RADIUS; }
    }
}
