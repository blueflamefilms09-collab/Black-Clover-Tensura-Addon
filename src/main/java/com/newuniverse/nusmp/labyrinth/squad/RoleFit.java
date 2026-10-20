package com.newuniverse.nusmp.labyrinth.squad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Why: the plan says WHICH roles a squad needs; the Dungeon Master has already chosen WHICH monsters come (the encounter table and the faction decide that, and a squad
 * must not change an entity type). So the roles are handed to the monsters that fit them best: the tankiest body is the tank, a mob that casts or supports is the
 * healer, a mob with a ranged attack is the shooter, the quickest light body flanks, the hardest hitter is the leader's second, and whoever is left fights in the front.
 * A Mob Foundry mob says what it is (its archetype), any other mob is read from its attributes. The scoring is a pure function over a few numbers so it is tested, and
 * the assignment is greedy in role order (tanks, leader, supports, shooters, flankers, melee) with ties to the lowest index, so the same mobs always get the same roles.
 */
public final class RoleFit {
    private RoleFit() {}

    /** What the fit reads of a mob: its health, armour, speed and damage, whether it has a ranged attack, whether it casts, and its Mob Foundry archetype ("" for none). */
    public record Traits(double maxHealth, double armor, double speed, double damage, boolean ranged, boolean caster, String archetype) {
        public String arch() { return archetype == null ? "" : archetype.toLowerCase(Locale.ROOT); }
    }

    /** How well this mob suits this role (higher is better); roles are scored on comparable scales only against other mobs for the SAME role. */
    public static double score(SquadRole role, Traits t) {
        String a = t.arch();
        double body = t.maxHealth() + t.armor() * 4.0;
        double bias = switch (role) {
            case TANK -> a.equals("tank") ? 60 : a.equals("brute") || a.equals("charger") ? 25 : 0;
            case LEADER -> a.equals("brute") || a.equals("tank") ? 20 : a.equals("charger") ? 10 : 0;
            case SUPPORT -> a.equals("support") || a.equals("summoner") ? 80 : a.equals("caster") ? 40 : t.caster() ? 50 : 0;
            case DPS_RANGED -> a.equals("archer") ? 80 : a.equals("caster") ? 60 : t.ranged() ? 70 : t.caster() ? 40 : 0;
            case FLANKER -> a.equals("assassin") || a.equals("skirmisher") ? 50 : a.equals("flier") ? 40 : 0;
            case DPS_MELEE -> a.equals("brute") || a.equals("charger") ? 15 : 0;
        };
        return switch (role) {
            case TANK -> body * (t.ranged() || t.caster() ? 0.4 : 1.0) + bias;
            case LEADER -> (t.maxHealth() * 0.5 + t.damage() * 6.0) * (t.ranged() ? 0.5 : 1.0) + bias;
            case SUPPORT -> bias + (t.ranged() ? 10 : 0) + 200.0 / Math.max(1.0, t.maxHealth()) - t.damage() * 0.2;
            case DPS_RANGED -> bias + t.damage() * 2.0 - t.maxHealth() * 0.05;
            case FLANKER -> t.speed() * 120.0 + bias - body * 0.05 - (t.ranged() ? 20 : 0);
            case DPS_MELEE -> t.damage() * 5.0 + bias + body * 0.02 - (t.ranged() ? 15 : 0);
        };
    }

    /**
     * The role of each mob: roles are given out in the order tank, leader, support, ranged, flanker, melee, each to the unassigned mob that scores highest for it
     * (the lowest index on a tie). If there are more mobs than roles the extra mobs fight as melee damage; if fewer, the last roles go unfilled. The result has one
     * entry per mob.
     */
    public static SquadRole[] assign(List<SquadRole> wanted, List<Traits> mobs) {
        int n = mobs.size();
        SquadRole[] out = new SquadRole[n];
        boolean[] taken = new boolean[n];
        List<SquadRole> order = new ArrayList<>(wanted);
        order.sort((x, y) -> Integer.compare(priority(x), priority(y)));
        for (SquadRole role : order) {
            int best = -1;
            double bestScore = -Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (taken[i]) continue;
                double s = score(role, mobs.get(i));
                if (s > bestScore + 1e-9) {
                    bestScore = s;
                    best = i;
                }
            }
            if (best < 0) break;
            out[best] = role;
            taken[best] = true;
        }
        for (int i = 0; i < n; i++) if (out[i] == null) out[i] = SquadRole.DPS_MELEE;
        return out;
    }

    private static int priority(SquadRole r) {
        return switch (r) {
            case TANK -> 0;
            case LEADER -> 1;
            case SUPPORT -> 2;
            case DPS_RANGED -> 3;
            case FLANKER -> 4;
            case DPS_MELEE -> 5;
        };
    }

    /** The roles an array holds, as a list (for the System line). */
    public static List<SquadRole> asList(SquadRole[] roles) { return new ArrayList<>(Arrays.asList(roles)); }
}
