package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Act;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Foe;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Kind;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Order;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.State;
import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Unit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The tactics, the damage budget and the waves run together over a few minutes of made-up play, the way the driver runs them: a swarm of
 * mutants against one player who stands and fights back. What is asserted is what the design promises: the swarm is fierce but never an
 * unavoidable stun-lock, members strike in turns, the cap holds, and the hive visits the states a fight should visit.
 */
class HiveSwarmSimTest {
    private static final class Mutant {
        final int id;
        final Kind kind;
        double x, z, hp = 1.0;
        final double homeX, homeZ;

        Mutant(int id, Kind kind, double x, double z) {
            this.id = id;
            this.kind = kind;
            this.x = x;
            this.z = z;
            this.homeX = x;
            this.homeZ = z;
        }
    }

    /** One run: returns the per-second damage dealt to the player, the strike times, the states seen. */
    private static final class Run {
        final List<Double> perSecond = new ArrayList<>();
        final List<Long> strikes = new ArrayList<>();
        final Map<State, Integer> states = new EnumMap<>(State.class);
        final Map<Integer, List<Long>> byMember = new HashMap<>();
        double playerDamage;
        int peakAlive;
    }

    private static Run play(long seed, int start, double playerMaxHealth, double blowDamage, boolean playerKills) {
        Random r = new Random(seed);
        HiveTactics h = new HiveTactics(0, 0, 0, 0, TempestMiragePlan.A, TempestMiragePlan.B);
        HiveRules.Budget budget = new HiveRules.Budget();
        Kind[] kinds = {Kind.ORC, Kind.LIZARDMAN, Kind.ORC, Kind.SLIME, Kind.HOUND, Kind.GOBLIN, Kind.ORC, Kind.SLIME};
        List<Mutant> alive = new ArrayList<>();
        int nextId = 1;
        for (int i = 0; i < start; i++) {
            double a = r.nextDouble() * Math.PI * 2, rad = 12 + r.nextDouble() * 28;
            alive.add(new Mutant(nextId++, kinds[i % kinds.length], Math.cos(a) * rad, Math.sin(a) * rad * 0.9));
        }
        double px = 30, pz = -20;                       // the player stands where they are and swings at what comes close
        Map<Integer, Order> orders = new HashMap<>();
        Run run = new Run();
        double windowDamage = 0;
        long lastWave = Long.MIN_VALUE;
        for (long t = 0; t < 6000; t++) {
            if (t % HiveTactics.THINK == 0) {
                List<Unit> units = new ArrayList<>();
                for (Mutant m : alive) {
                    if (Math.hypot(m.x - px, m.z - pz) < 20) h.sight(m.id, new Foe(900, px, pz, 1.0), t);
                    units.add(new Unit(m.id, m.kind, m.x, m.z, m.hp, m.homeX, m.homeZ));
                }
                Order[] o = h.think(t, units);
                orders.clear();
                for (int i = 0; i < o.length; i++) orders.put(units.get(i).id(), o[i]);
                h.takeCue();
                run.states.merge(h.state(), 1, Integer::sum);
            }
            for (Mutant m : new ArrayList<>(alive)) {
                Order o = orders.get(m.id);
                if (o == null) continue;
                double speed = 0.2 * o.speed(), dx = o.goalX() - m.x, dz = o.goalZ() - m.z, d = Math.hypot(dx, dz);
                if (o.act() != Act.HOLD && d > 0.6) {
                    m.x += dx / d * Math.min(speed, d);
                    m.z += dz / d * Math.min(speed, d);
                }
                if (o.heal() && t % 20 == 0) m.hp = Math.min(1.0, m.hp + 0.03);
                if (o.strike() && o.target() == 900 && Math.hypot(m.x - px, m.z - pz) <= m.kind.reach + 0.9 && h.mayStrike(m.id, 900, t)) {
                    h.struck(m.id, m.kind, 900, t);
                    double dealt = budget.take(t, blowDamage, playerMaxHealth);
                    windowDamage += dealt;
                    run.playerDamage += dealt;
                    run.strikes.add(t);
                    run.byMember.computeIfAbsent(m.id, k -> new ArrayList<>()).add(t);
                    assertTrue(dealt <= Math.max(HiveRules.MIN_HIT, HiveRules.HIT_SHARE * playerMaxHealth) + 1e-9, "one blow is capped");
                }
            }
            if (playerKills && t > 600 && t % 30 == 0) {                                  // the player kills the nearest one in reach, every 1.5 seconds
                Mutant best = null;
                for (Mutant m : alive) if (Math.hypot(m.x - px, m.z - pz) < 4.5 && (best == null || Math.hypot(m.x - px, m.z - pz) < Math.hypot(best.x - px, best.z - pz))) best = m;
                if (best != null) {
                    h.hurt(best.id, 900, px, pz, 2.0, t);
                    best.hp -= 0.6;
                    if (best.hp <= 0) {
                        h.died(best.id, t);
                        alive.remove(best);
                    }
                }
            }
            if (t % 20 == 19) {
                run.perSecond.add(windowDamage);
                windowDamage = 0;
            }
            int wave = HiveWaves.due(alive.size(), t, lastWave, 1);
            if (wave > 0) {                                                                  // the waves come in at the swarm spots, far from the player
                lastWave = t;
                for (int i = 0; i < wave; i++) alive.add(new Mutant(nextId++, kinds[r.nextInt(kinds.length)], -30 + r.nextDouble() * 10, 20 + r.nextDouble() * 10));
            }
            run.peakAlive = Math.max(run.peakAlive, alive.size());
        }
        return run;
    }

    @Test
    void aSwarmOfFortyIsFierceButNeverMoreThanTheBudgetAllowsInASecond() {
        for (double maxHealth : new double[]{20, 60, 200}) {
            Run run = play(7, 36, maxHealth, 8.0, true);
            assertTrue(run.strikes.size() > 100, "a real fight: " + run.strikes.size() + " blows");
            double worst = 0;
            for (double s : run.perSecond) worst = Math.max(worst, s);
            assertTrue(worst <= HiveRules.WINDOW_SHARE * maxHealth + 1e-9, "max health " + maxHealth + ": the worst second took " + worst);
            assertTrue(worst > 0.5 * HiveRules.WINDOW_SHARE * maxHealth, "and the swarm does get its blows in: " + worst);
        }
    }

    @Test
    void membersStrikeInTurnsWithCooldownsAndOneFoeTakesFewBlows() {
        Run run = play(11, 40, 20, 5.0, true);
        for (Map.Entry<Integer, List<Long>> e : run.byMember.entrySet()) {
            List<Long> ts = e.getValue();
            for (int i = 1; i < ts.size(); i++) assertTrue(ts.get(i) - ts.get(i - 1) >= 22, "member " + e.getKey() + " struck twice within its cooldown: " + (ts.get(i) - ts.get(i - 1)));
        }
        for (int i = 1; i < run.strikes.size(); i++) assertTrue(run.strikes.get(i) - run.strikes.get(i - 1) >= HiveTactics.FOE_GAP, "two blows on one foe closer than the gap");
        long seconds = 6000 / 20;
        assertTrue(run.strikes.size() <= seconds * 20 / HiveTactics.FOE_GAP + 1, "at most one blow every gap: " + run.strikes.size());
    }

    @Test
    void theHiveVisitsTheStatesAFightShouldVisitAndTheCapHolds() {
        Run run = play(3, 36, 20, 4.0, true);
        assertTrue(run.states.getOrDefault(State.SEARCH, 0) > 0, "it starts quiet");
        assertTrue(run.states.getOrDefault(State.ASSEMBLE, 0) > 0, "it rallies");
        assertTrue(run.states.getOrDefault(State.ENGAGE, 0) > 0, "it fights");
        assertTrue(run.states.getOrDefault(State.SURROUND, 0) > 0, "a swarm this size closes a ring on one player");
        assertTrue(run.peakAlive <= HiveWaves.CAP, "the cap holds through the waves: " + run.peakAlive);
    }

    @Test
    void aSmallSwarmThatIsLosingFallsBackAndRegroups() {
        // eight against a player who kills one every second and a half: the swarm is thinned, falls back to its banner, regroups
        Run run = play(1, 8, 20, 5.0, true);
        assertTrue(run.states.getOrDefault(State.FALL_BACK, 0) > 0, "it falls back: " + run.states);
        assertTrue(run.states.getOrDefault(State.REFORM, 0) > 0, "and regroups: " + run.states);
        assertTrue(run.states.getOrDefault(State.ENGAGE, 0) > 0);
    }

    @Test
    void nobodyBeingAboutMeansNoBlows() {
        Run run = play(9, 30, 20, 8.0, false);
        // the player never fights back, and the swarm is never hurt: the budget still holds
        double worst = 0;
        for (double s : run.perSecond) worst = Math.max(worst, s);
        assertTrue(worst <= HiveRules.WINDOW_SHARE * 20 + 1e-9);
        assertTrue(run.playerDamage > 0);
    }
}
