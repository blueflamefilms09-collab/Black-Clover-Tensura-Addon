package com.newuniverse.nusmp.dungeon.nexus.zone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The hive mind of the Tempest Mirage (docs/living_dungeon.md section 5, zone 3), as pure maths: one blackboard, a role assigner and a
 * formation planner, with no Minecraft in it, so the whole of its intelligence is tested. {@link HiveDriver} feeds it what the swarm's
 * members see and suffer and carries out the orders it gives.
 *
 * <p><b>One picture.</b> The hive keeps a single note per foe (where it was last seen, how strong it looks), shared by every member: what one
 * sees all know. It also keeps a rally point, the shots it has taken from a distance, and how many of its own fell lately.
 *
 * <p><b>Six states.</b> SEARCH: no foe known; members wander near their homes. The first to spot a foe becomes the SCOUT and runs back to the
 * rally point; when it gets there (or after a few seconds, or when any member is hurt) the hive sounds the alarm: ASSEMBLE, everyone gathers
 * at the rally point; ENGAGE once enough have gathered, or the time is up; SURROUND when the hive is many times the party's strength; FALL_BACK
 * when it is losing; REFORM at the rally point, healing, until it is fit again (or the party comes to it); back to ASSEMBLE or SEARCH.
 *
 * <p><b>Six roles.</b> TANK (an orc: slow, holds the front), FLANKER (a hound or a lizardman: circles to the foe's sides and behind),
 * SKIRMISHER (hit and retreat, in turns), SLIME (gets round the foe and stays close), SCOUT, RETREATER (a wounded member falls back to the
 * rally point and returns when healed).
 *
 * <p><b>It learns.</b> If a foe keeps shooting from one place, the flankers (and half the skirmishers) go for that place. If the party is
 * clumped, the tanks and slimes split it: they push to its middle and the strikes are spread over everybody in it.
 *
 * <p><b>It is fair.</b> Members strike in turns: a member needs a token, tokens are few, each member has a cooldown, and one foe takes at
 * most one strike every {@link #FOE_GAP} ticks. Nothing here moves anything; the driver asks {@link #mayStrike} before a blow.
 */
public final class HiveTactics {
    // ---------------------------------------------------------------- vocabulary
    /** The kinds of member: how much it counts for, how far it reaches, how many ticks it waits between blows, and its speed class. */
    public enum Kind {
        ORC(1.0, 2.3, 34), LIZARDMAN(0.9, 2.3, 28), SLIME(0.5, 1.7, 40), GOBLIN(0.4, 1.9, 22), HOUND(0.6, 1.9, 26);

        public final double weight, reach;
        public final int gap;

        Kind(double weight, double reach, int gap) {
            this.weight = weight;
            this.reach = reach;
            this.gap = gap;
        }
    }

    public enum Role { SCOUT, TANK, FLANKER, SKIRMISHER, SLIME, RETREATER }

    public enum State { SEARCH, ASSEMBLE, ENGAGE, SURROUND, FALL_BACK, REFORM }

    /** What the driver does with a member: wander near home, walk to the goal, walk to the goal and strike when allowed, stand and face. */
    public enum Act { WANDER, MOVE, ENGAGE, HOLD }

    /** A member as the hive sees it: health is a fraction of its maximum; home is where it was born. */
    public record Unit(int id, Kind kind, double x, double z, double health, double homeX, double homeZ) {}

    /** A foe: its id, where it is, and how strong it looks (1 is an ordinary player). */
    public record Foe(int id, double x, double z, double power) {}

    /** What to do now. {@code target} is a foe id or -1; {@code strike} says the member holds a token; {@code heal} says it may mend. */
    public record Order(Role role, Act act, double goalX, double goalZ, int target, boolean strike, boolean heal, double speed) {}

    /** A change of state, for the driver to announce. */
    public record Cue(State from, State to, long tick) {}

    // ---------------------------------------------------------------- numbers
    /** Ticks between two thoughts of the hive (the driver thinks this often). */
    public static final int THINK = 10;
    /** A scout this close to the rally point raises the alarm; it raises it anyway after this many ticks. */
    public static final double ALERT_DIST = 6.0;
    public static final int ALERT_TIMEOUT = 120;
    /** Gathering: the radius round the rally point, the longest wait. */
    public static final double ASSEMBLE_RADIUS = 10.0;
    public static final int ASSEMBLE_MAX = 160;
    /** A foe seen this recently is "live"; one seen this recently is still hunted; one older is forgotten. */
    public static final int LIVE = 100, LOSE = 600, FORGET = 1200;
    /** How much hive strength one foe of power 1 is worth. */
    public static final double FOE_UNIT = 4.0;
    /** Surround: when the hive is this many times the party, with this many members near the foe; leaves under the lower ratio or after the time; the pause before it may be done again. */
    public static final double SURROUND_ON = 2.2, SURROUND_OFF = 1.6, SURROUND_NEAR_RANGE = 14.0;
    public static final int SURROUND_NEAR = 6, SURROUND_MAX = 200, SURROUND_COOL = 240;
    /** Falling back: under this ratio, or this mean health, or this many fallen (a share of the peak, at least the minimum) in the window; only after the dwell. */
    public static final double FALL_RATIO = 0.6, FALL_HEALTH = 0.4, FALL_LOSS_SHARE = 0.4;
    public static final int FALL_LOSS_MIN = 5, LOSS_WINDOW = 400, ENGAGE_DWELL = 60;
    public static final int FALL_MAX = 240, REFORM_MAX = 500, REFORM_MIN = 60;
    public static final double REFORM_HEAL = 0.75, RALLY_RADIUS = 9.0;
    /** A wounded member retreats under the first and returns over the second. */
    public static final double RETREAT_BELOW = 0.35, RETURN_ABOVE = 0.75;
    /** Foes within this of their middle are a clump. */
    public static final double CLUMP = 4.5;
    /** A hit from at least this far is a shot; this many from within the radius mark a place; a mark is kept this long after its last shot. */
    public static final double RANGED_DIST = 7.0, SPOT_JOIN = 6.0;
    public static final int RANGED_HITS = 3, SPOT_KEEP = 600;
    /** One foe takes one strike every FOE_GAP ticks; this many tokens for each foe, at most MAX_TOKENS in all. */
    public static final int FOE_GAP = 8, TOKENS_PER_FOE = 2, MAX_TOKENS = 5;
    /** A skirmisher draws back this long after a blow. */
    public static final int SKIRM_RETREAT = 30;
    /** A slime may slow a foe once in this many ticks. */
    public static final int SLOW_GAP = 120;
    /** Flankers are only sent when this many members are engaged. */
    public static final int FLANK_MIN = 6;
    private static final double TAU = Math.PI * 2;

    // ---------------------------------------------------------------- the blackboard
    private static final class Note {
        double x, z, power;
        long seen;
    }

    private static final class Mark {
        double x, z;
        int hits;
        long last;
    }

    private final double rallyX, rallyZ, arenaX, arenaZ, arenaA, arenaB;
    private State state = State.SEARCH;
    private long since;
    private final Map<Integer, Note> known = new TreeMap<>();
    private final Map<Integer, Role> roles = new TreeMap<>();
    private final Map<Integer, Long> nextStrike = new HashMap<>(), drawnBack = new HashMap<>(), foeNext = new HashMap<>(), slowedAt = new HashMap<>();
    private final Set<Integer> tokens = new HashSet<>();
    private final List<Mark> marks = new ArrayList<>();
    private final List<Long> deaths = new ArrayList<>();
    private int scout = -1;
    private long scoutSince;
    private boolean alarm;
    private int peak;
    private long noSurroundUntil;
    private Cue cue;

    /** A hive with its rally point and the ellipse (centre, half-widths) of the ground it may use. */
    public HiveTactics(double rallyX, double rallyZ, double arenaX, double arenaZ, double arenaA, double arenaB) {
        this.rallyX = rallyX;
        this.rallyZ = rallyZ;
        this.arenaX = arenaX;
        this.arenaZ = arenaZ;
        this.arenaA = Math.max(1.0, arenaA);
        this.arenaB = Math.max(1.0, arenaB);
    }

    public State state() { return state; }

    public long stateSince() { return since; }

    public Role roleOf(int unit) { return roles.get(unit); }

    public int scoutId() { return scout; }

    /** The change of state since the last call, once; null if there was none. */
    public Cue takeCue() {
        Cue c = cue;
        cue = null;
        return c;
    }

    // ---------------------------------------------------------------- what the members report
    /** A member sees a foe: the picture is shared, and in SEARCH the first to see becomes the scout. */
    public void sight(int unit, Foe foe, long tick) {
        note(foe.id(), foe.x(), foe.z(), foe.power(), tick);
        if (state == State.SEARCH && scout < 0) {
            scout = unit;
            scoutSince = tick;
        }
    }

    /** A member was struck by a foe (dist: how far the foe stood). Pain is shared: the hive is alarmed, and a blow from afar is remembered as a shot. */
    public void hurt(int unit, int foeId, double foeX, double foeZ, double dist, long tick) {
        Note n = known.get(foeId);
        note(foeId, foeX, foeZ, n == null ? 1.0 : n.power, tick);
        if (state == State.SEARCH) alarm = true;
        if (dist >= RANGED_DIST) shot(foeX, foeZ, tick);
    }

    /** A member fell. */
    public void died(int unit, long tick) {
        deaths.add(tick);
        if (unit == scout) scout = -1;
        roles.remove(unit);
        tokens.remove(unit);
    }

    /** A trap was sprung on a foe: the hive is on it at once. */
    public void ambush(Foe foe, long tick) {
        note(foe.id(), foe.x(), foe.z(), foe.power(), tick);
        alarm = true;
        if (state == State.SEARCH || state == State.ASSEMBLE || state == State.REFORM) go(State.ENGAGE, tick);
    }

    private void note(int id, double x, double z, double power, long tick) {
        Note n = known.computeIfAbsent(id, k -> new Note());
        n.x = x;
        n.z = z;
        n.power = Math.max(0.2, power);
        n.seen = tick;
    }

    private void shot(double x, double z, long tick) {
        for (Mark m : marks) {
            if ((m.x - x) * (m.x - x) + (m.z - z) * (m.z - z) <= SPOT_JOIN * SPOT_JOIN) {
                m.x = (m.x * m.hits + x) / (m.hits + 1);
                m.z = (m.z * m.hits + z) / (m.hits + 1);
                m.hits++;
                m.last = tick;
                return;
            }
        }
        Mark m = new Mark();
        m.x = x;
        m.z = z;
        m.hits = 1;
        m.last = tick;
        marks.add(m);
        if (marks.size() > 6) marks.remove(0);
    }

    /** The place the hive has learned a foe shoots from, {x, z}; null if it has learned none (or has forgotten it). */
    public double[] markedSpot(long tick) {
        Mark best = null;
        for (Mark m : marks) if (m.hits >= RANGED_HITS && tick - m.last <= SPOT_KEEP && (best == null || m.hits > best.hits)) best = m;
        return best == null ? null : new double[]{best.x, best.z};
    }

    // ---------------------------------------------------------------- turns
    /** May this member strike this foe now? It needs a token, its own cooldown over, and the foe's gap over. */
    public boolean mayStrike(int unit, int foeId, long tick) {
        return tokens.contains(unit) && tick >= nextStrike.getOrDefault(unit, 0L) && tick >= foeNext.getOrDefault(foeId, 0L);
    }

    /** The member struck: its token is spent, its cooldown and the foe's gap start, and a skirmisher draws back. */
    public void struck(int unit, Kind kind, int foeId, long tick) {
        tokens.remove(unit);
        nextStrike.put(unit, tick + kind.gap);
        foeNext.put(foeId, tick + FOE_GAP);
        if (roles.get(unit) == Role.SKIRMISHER) drawnBack.put(unit, tick + SKIRM_RETREAT);
    }

    /** May a slime slow this foe now (and if so, it is noted)? Once in {@link #SLOW_GAP} ticks. */
    public boolean mayRoot(int foeId, long tick) {
        Long last = slowedAt.get(foeId);
        if (last != null && tick - last < SLOW_GAP) return false;
        slowedAt.put(foeId, tick);
        return true;
    }

    // ---------------------------------------------------------------- thinking
    /** The hive thinks: updates its state and roles and returns one order for each unit, in the order given. */
    public Order[] think(long tick, List<Unit> units) {
        forget(tick);
        Order[] out = new Order[units.size()];
        if (units.isEmpty()) {
            tokens.clear();
            return out;
        }
        List<Unit> sorted = new ArrayList<>(units);
        sorted.sort(Comparator.comparingInt(Unit::id));
        List<Foe> live = foes(tick, LIVE), recent = foes(tick, LOSE);
        double party = 0;
        for (Foe f : live.isEmpty() ? recent : live) party += f.power() * FOE_UNIT;
        double power = 0, health = 0, cx = 0, cz = 0;
        for (Unit u : sorted) {
            power += u.kind().weight * Math.max(0.15, u.health());
            health += u.health();
            cx += u.x();
            cz += u.z();
        }
        int n = sorted.size();
        health /= n;
        cx /= n;
        cz /= n;
        double ratio = power / Math.max(1.0, party);
        transition(tick, sorted, recent, live, ratio, health, n);
        assign(sorted, tick);
        return orders(tick, units, sorted, live.isEmpty() ? recent : live, cx, cz, health);
    }

    private void forget(long tick) {
        known.values().removeIf(n -> tick - n.seen > FORGET);
        marks.removeIf(m -> tick - m.last > SPOT_KEEP * 2);
        deaths.removeIf(d -> tick - d > LOSS_WINDOW);
        if (known.isEmpty()) slowedAt.clear();
    }

    private List<Foe> foes(long tick, int window) {
        List<Foe> out = new ArrayList<>();
        for (Map.Entry<Integer, Note> e : known.entrySet()) if (tick - e.getValue().seen <= window) out.add(new Foe(e.getKey(), e.getValue().x, e.getValue().z, e.getValue().power));
        return out;
    }

    private void go(State to, long tick) {
        if (to == state) return;
        cue = new Cue(state, to, tick);
        state = to;
        since = tick;
        if (to == State.SEARCH) {
            scout = -1;
            alarm = false;
        }
    }

    private int near(List<Unit> units, double x, double z, double radius) {
        int c = 0;
        for (Unit u : units) if ((u.x() - x) * (u.x() - x) + (u.z() - z) * (u.z() - z) <= radius * radius) c++;
        return c;
    }

    private void transition(long tick, List<Unit> units, List<Foe> recent, List<Foe> live, double ratio, double health, int n) {
        long dwell = tick - since;
        switch (state) {
            case SEARCH -> {
                if (scout >= 0 && !alarm) {
                    Unit s = find(units, scout);
                    if (s == null) scout = -1;                                            // it fell before it got home: nobody knows
                    else if (dist(s.x(), s.z(), rallyX, rallyZ) <= ALERT_DIST || tick - scoutSince >= ALERT_TIMEOUT) alarm = true;
                }
                if (alarm) {
                    alarm = false;
                    scout = -1;
                    if (!recent.isEmpty()) go(State.ASSEMBLE, tick);
                }
            }
            case ASSEMBLE -> {
                if (recent.isEmpty()) {
                    go(State.SEARCH, tick);
                    return;
                }
                int gathered = near(units, rallyX, rallyZ, ASSEMBLE_RADIUS), need = Math.min(n, Math.max(3, (int) Math.ceil(0.55 * n)));
                if (gathered >= need || dwell >= ASSEMBLE_MAX) {
                    peak = n;
                    go(State.ENGAGE, tick);
                }
            }
            case ENGAGE -> {
                peak = Math.max(peak, n);
                if (recent.isEmpty()) {
                    go(State.SEARCH, tick);
                } else if (dwell >= ENGAGE_DWELL && losing(tick, ratio, health)) {
                    go(State.FALL_BACK, tick);
                } else if (tick >= noSurroundUntil && ratio >= SURROUND_ON && surroundable(units, live.isEmpty() ? recent : live)) {
                    go(State.SURROUND, tick);
                }
            }
            case SURROUND -> {
                peak = Math.max(peak, n);
                if (recent.isEmpty()) {
                    go(State.SEARCH, tick);
                } else if (losing(tick, ratio, health)) {
                    go(State.FALL_BACK, tick);
                } else if (ratio < SURROUND_OFF || dwell >= SURROUND_MAX) {
                    noSurroundUntil = tick + SURROUND_COOL;
                    go(State.ENGAGE, tick);
                }
            }
            case FALL_BACK -> {
                int home = near(units, rallyX, rallyZ, RALLY_RADIUS);
                if (home >= Math.ceil(0.6 * n) || dwell >= FALL_MAX) go(State.REFORM, tick);
            }
            case REFORM -> {
                boolean cornered = dwell >= REFORM_MIN && !live.isEmpty() && anyNear(live, rallyX, rallyZ, 10.0);
                if (cornered) {
                    peak = n;
                    go(State.ENGAGE, tick);
                } else if (health >= REFORM_HEAL || dwell >= REFORM_MAX) {
                    go(recent.isEmpty() ? State.SEARCH : State.ASSEMBLE, tick);
                }
            }
        }
    }

    private boolean losing(long tick, double ratio, double health) {
        int fallen = 0;
        for (long d : deaths) if (tick - d <= LOSS_WINDOW) fallen++;
        return ratio < FALL_RATIO || health < FALL_HEALTH || fallen >= Math.max(FALL_LOSS_MIN, (int) Math.ceil(FALL_LOSS_SHARE * peak));
    }

    /** Are enough members close to the foes to close a ring? */
    private boolean surroundable(List<Unit> units, List<Foe> foes) {
        if (foes.isEmpty()) return false;
        Foe f = foes.get(0);
        return near(units, f.x(), f.z(), SURROUND_NEAR_RANGE) >= SURROUND_NEAR;
    }

    private static boolean anyNear(List<Foe> foes, double x, double z, double radius) {
        for (Foe f : foes) if (dist(f.x(), f.z(), x, z) <= radius) return true;
        return false;
    }

    private static Unit find(List<Unit> units, int id) {
        for (Unit u : units) if (u.id() == id) return u;
        return null;
    }

    private static double dist(double ax, double az, double bx, double bz) { return Math.sqrt((ax - bx) * (ax - bx) + (az - bz) * (az - bz)); }

    // ---------------------------------------------------------------- roles
    private void assign(List<Unit> units, long tick) {
        roles.keySet().removeIf(id -> find(units, id) == null);
        int n = units.size();
        int tanksWanted = Math.max(1, Math.min(8, (int) Math.round(0.3 * n)));
        List<Unit> orcs = new ArrayList<>();
        for (Unit u : units) if (u.kind() == Kind.ORC && !retreating(u, roles.get(u.id()))) orcs.add(u);
        int tanks = 0;
        for (Unit u : orcs) if (roles.get(u.id()) == Role.TANK) tanks++;
        for (int i = orcs.size() - 1; i >= 0 && tanks > tanksWanted; i--) {                // too many: the highest ids give up the front
            if (roles.get(orcs.get(i).id()) == Role.TANK) {
                roles.put(orcs.get(i).id(), Role.SKIRMISHER);
                tanks--;
            }
        }
        for (int i = 0; i < orcs.size() && tanks < tanksWanted; i++) {                     // too few: the lowest ids take it
            if (roles.get(orcs.get(i).id()) != Role.TANK) {
                roles.put(orcs.get(i).id(), Role.TANK);
                tanks++;
            }
        }
        boolean flank = n >= FLANK_MIN;
        for (Unit u : units) {
            Role old = roles.get(u.id());
            if (retreating(u, old)) {
                roles.put(u.id(), Role.RETREATER);
                continue;
            }
            if (state == State.SEARCH && u.id() == scout) {
                roles.put(u.id(), Role.SCOUT);
                continue;
            }
            switch (u.kind()) {
                case ORC -> { if (old != Role.TANK) roles.put(u.id(), Role.SKIRMISHER); }
                case SLIME -> roles.put(u.id(), Role.SLIME);
                case HOUND -> roles.put(u.id(), flank ? Role.FLANKER : Role.SKIRMISHER);
                case LIZARDMAN -> roles.put(u.id(), flank && u.id() % 2 == 0 ? Role.FLANKER : Role.SKIRMISHER);
                default -> roles.put(u.id(), Role.SKIRMISHER);
            }
        }
    }

    /** Wounded members fall back and stay back until healed (the latch); in the fall-back states everyone is already going home. */
    private boolean retreating(Unit u, Role old) {
        if (state == State.SEARCH) return false;
        if (old == Role.RETREATER) return u.health() < RETURN_ABOVE;
        return u.health() < RETREAT_BELOW;
    }

    // ---------------------------------------------------------------- orders
    private Order[] orders(long tick, List<Unit> units, List<Unit> sorted, List<Foe> foes, double cx, double cz, double health) {
        Order[] out = new Order[units.size()];
        tokens.clear();
        boolean fight = state == State.ENGAGE || state == State.SURROUND;
        Foe primary = nearest(foes, cx, cz);
        double[] spot = markedSpot(tick);
        boolean clumped = clumped(foes);
        double fx = 0, fz = 0;
        for (Foe f : foes) {
            fx += f.x();
            fz += f.z();
        }
        if (!foes.isEmpty()) {
            fx /= foes.size();
            fz /= foes.size();
        }
        // slots round the ring, in the order the units are listed (tanks first), front first
        List<Unit> ring = new ArrayList<>();
        if (state == State.SURROUND) {
            for (Unit u : sorted) if (roles.get(u.id()) != Role.RETREATER) ring.add(u);
            ring.sort(Comparator.comparingInt((Unit u) -> roles.get(u.id()) == Role.TANK ? 0 : 1).thenComparingInt(Unit::id));
        }
        Map<Role, List<Unit>> byRole = new HashMap<>();
        for (Unit u : sorted) byRole.computeIfAbsent(roles.get(u.id()), k -> new ArrayList<>()).add(u);
        int rank = 0;
        Map<Integer, Order> made = new HashMap<>();
        for (Unit u : sorted) {
            Role role = roles.get(u.id());
            Foe target = fight ? targetFor(u, role, foes, clumped, rank++) : null;
            Order o = switch (state) {
                case SEARCH -> searchOrder(u, role, tick);
                case ASSEMBLE -> gatherOrder(u, role, tick, ASSEMBLE_RADIUS * 0.45, 1.15, false);
                case FALL_BACK -> gatherOrder(u, role, tick, 5.0, role == Role.RETREATER ? 1.35 : 1.25, false);
                case REFORM -> gatherOrder(u, role, tick, 6.0, 1.0, true);
                default -> fightOrder(u, role, target, foes, primary, spot, ring, byRole, cx, cz, fx, fz, clumped, tick);
            };
            made.put(u.id(), o);
        }
        if (fight) grantTokens(tick, sorted, made);
        for (int i = 0; i < units.size(); i++) {
            Order o = made.get(units.get(i).id());
            boolean token = tokens.contains(units.get(i).id());
            if (o.strike() != token) o = new Order(o.role(), o.act(), o.goalX(), o.goalZ(), o.target(), token, o.heal(), o.speed());
            out[i] = o;
        }
        return out;
    }

    private static Foe nearest(List<Foe> foes, double x, double z) {
        Foe best = null;
        double bd = Double.MAX_VALUE;
        for (Foe f : foes) {
            double d = (f.x() - x) * (f.x() - x) + (f.z() - z) * (f.z() - z);
            if (d < bd) {
                bd = d;
                best = f;
            }
        }
        return best;
    }

    private static boolean clumped(List<Foe> foes) {
        if (foes.size() < 2) return false;
        double fx = 0, fz = 0;
        for (Foe f : foes) {
            fx += f.x();
            fz += f.z();
        }
        fx /= foes.size();
        fz /= foes.size();
        for (Foe f : foes) if (dist(f.x(), f.z(), fx, fz) > CLUMP) return false;
        return true;
    }

    /** Who this member goes for: the nearest foe; in a clump the tanks and slimes take the foes in turn, so the strikes are spread. */
    private Foe targetFor(Unit u, Role role, List<Foe> foes, boolean clumped, int rank) {
        if (foes.isEmpty()) return null;
        if (clumped && (role == Role.TANK || role == Role.SLIME)) return foes.get(rank % foes.size());
        return nearest(foes, u.x(), u.z());
    }

    private Order searchOrder(Unit u, Role role, long tick) {
        if (role == Role.SCOUT) return new Order(role, Act.MOVE, rallyX, rallyZ, -1, false, false, 1.35);
        long slot = tick / 140;
        double a = TempestRng.hash(u.id() * 31L + slot, u.id(), (int) slot, 5) % 3600 / 3600.0 * TAU, r = 1.0 + TempestRng.hash(slot, u.id(), 9, 6) % 100 / 100.0 * 4.0;
        double[] g = clamp(u.homeX() + Math.cos(a) * r, u.homeZ() + Math.sin(a) * r);
        return new Order(role, Act.WANDER, g[0], g[1], -1, false, false, 0.8);
    }

    /** A place round the rally point for this member, kept to it (so the members spread and do not stack). */
    private Order gatherOrder(Unit u, Role role, long tick, double radius, double speed, boolean heal) {
        double a = (u.id() * 2.399963) % TAU, r = radius * (0.45 + 0.55 * ((u.id() * 7) % 10) / 10.0);
        double[] g = clamp(rallyX + Math.cos(a) * r, rallyZ + Math.sin(a) * r);
        boolean there = dist(u.x(), u.z(), g[0], g[1]) <= 1.6;
        return new Order(role, there ? Act.HOLD : Act.MOVE, g[0], g[1], -1, false, heal && dist(u.x(), u.z(), rallyX, rallyZ) <= RALLY_RADIUS && !healthy(u), there ? 1.0 : speed);
    }

    private static boolean healthy(Unit u) { return u.health() >= 0.995; }

    private Order fightOrder(Unit u, Role role, Foe target, List<Foe> foes, Foe primary, double[] spot, List<Unit> ring, Map<Role, List<Unit>> byRole, double cx, double cz,
                             double fx, double fz, boolean clumped, long tick) {
        if (role == Role.RETREATER || target == null) {
            double a = (u.id() * 2.399963) % TAU, r = 5.0 * (0.5 + 0.5 * ((u.id() * 7) % 10) / 10.0);
            double[] g = clamp(rallyX + Math.cos(a) * r, rallyZ + Math.sin(a) * r);
            boolean there = dist(u.x(), u.z(), g[0], g[1]) <= 1.6;
            boolean heal = role == Role.RETREATER && dist(u.x(), u.z(), rallyX, rallyZ) <= RALLY_RADIUS && !healthy(u) && (primary == null || dist(u.x(), u.z(), primary.x(), primary.z()) > 10.0);
            return new Order(role, there ? Act.HOLD : Act.MOVE, g[0], g[1], -1, false, heal, there ? 1.0 : 1.35);
        }
        boolean ringed = state == State.SURROUND && clumped;                                  // a clump is ringed as one
        double tx = ringed ? fx : target.x(), tz = ringed ? fz : target.z();
        double toward = Math.atan2(cz - tz, cx - tx);                                          // from the foe toward where the hive comes from
        if (dist(cx, cz, tx, tz) < 0.5) toward = Math.atan2(rallyZ - tz, rallyX - tx);
        double angle, radius, speed;
        List<Unit> same = byRole.getOrDefault(role, List.of());
        int idx = Math.max(0, indexOf(same, u.id())), cnt = Math.max(1, same.size());
        if (state == State.SURROUND) {
            int i = Math.max(0, indexOf(ring, u.id())), cntRing = Math.max(1, ring.size());
            int step = (i + 1) / 2;
            angle = toward + (i == 0 ? 0 : (i % 2 == 1 ? 1 : -1) * step * TAU / cntRing);
            radius = (role == Role.TANK ? 2.4 : Math.min(3.0, u.kind().reach + 0.7)) + (ringed ? CLUMP * 0.5 : 0.0);        // on the ring, but within reach of a blow
            speed = role == Role.TANK ? 0.95 : 1.15;
        } else if (role == Role.TANK) {
            angle = toward + (idx - (cnt - 1) / 2.0) * 0.45;
            radius = 2.3;
            speed = 0.9;
        } else if (role == Role.FLANKER) {
            double side = idx % 2 == 0 ? 1 : -1;
            angle = toward + (cnt >= 3 && idx == 2 ? Math.PI : side * (1.25 + 0.3 * (idx / 2)));
            radius = Math.min(3.2, u.kind().reach + 0.8);
            speed = 1.25;
        } else if (role == Role.SLIME) {
            angle = toward + TAU * (idx + 0.5) / cnt;
            radius = 2.2;
            speed = 1.0;
        } else {                                                                               // SKIRMISHER: in, strike, out
            boolean back = tick < drawnBack.getOrDefault(u.id(), 0L);
            angle = toward + (idx % 2 == 0 ? 1 : -1) * 0.6;
            radius = back ? 6.5 : 2.0;
            speed = back ? 1.4 : 1.1;
        }
        double gx = tx + Math.cos(angle) * radius, gz = tz + Math.sin(angle) * radius;
        Foe aim = target;
        if (state == State.ENGAGE && clumped && (role == Role.TANK || role == Role.SLIME)) {   // split the clump: push to its middle
            gx = fx + Math.cos(angle) * 0.8;
            gz = fz + Math.sin(angle) * 0.8;
        }
        if (state == State.ENGAGE && spot != null && (role == Role.FLANKER || (role == Role.SKIRMISHER && u.id() % 2 == 1))) {   // it has learned where the shots come from
            double a = (u.id() * 2.399963) % TAU;
            gx = spot[0] + Math.cos(a) * 2.0;
            gz = spot[1] + Math.sin(a) * 2.0;
            Foe shooter = nearest(foes, spot[0], spot[1]);                                     // whoever stands nearest the place is the shooter
            if (shooter != null) aim = shooter;
            speed = 1.3;
        }
        double[] g = clamp(gx, gz);
        boolean backing = role == Role.SKIRMISHER && tick < drawnBack.getOrDefault(u.id(), 0L);
        return new Order(role, backing ? Act.MOVE : Act.ENGAGE, g[0], g[1], aim.id(), false, false, speed);
    }

    private static int indexOf(List<Unit> list, int id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id() == id) return i;
        return -1;
    }

    /** A point kept inside the ground the hive may use (the arena ellipse, a tenth in from its edge). */
    public double[] clamp(double x, double z) {
        double u = (x - arenaX) / (arenaA * 0.92), v = (z - arenaZ) / (arenaB * 0.92), d = u * u + v * v;
        if (d <= 1.0) return new double[]{x, z};
        double s = 1.0 / Math.sqrt(d);
        return new double[]{arenaX + u * s * arenaA * 0.92, arenaZ + v * s * arenaB * 0.92};
    }

    // ---------------------------------------------------------------- tokens
    /** Hands out the few tokens to strike with: members within reach of their target, tanks first, then those who have waited longest. */
    private void grantTokens(long tick, List<Unit> units, Map<Integer, Order> made) {
        List<Unit> candidates = new ArrayList<>();
        Map<Integer, Unit> byId = new HashMap<>();
        for (Unit u : units) {
            Order o = made.get(u.id());
            if (o.act() != Act.ENGAGE || o.target() < 0) continue;
            Role role = o.role();
            if (role == Role.RETREATER || role == Role.SCOUT) continue;
            Note f = known.get(o.target());
            if (f == null) continue;
            if (dist(u.x(), u.z(), f.x, f.z) > u.kind().reach + 1.0) continue;
            if (tick < nextStrike.getOrDefault(u.id(), 0L)) continue;
            candidates.add(u);
            byId.put(u.id(), u);
        }
        candidates.sort(Comparator.<Unit>comparingInt(u -> priority(made.get(u.id()).role())).thenComparingLong(u -> nextStrike.getOrDefault(u.id(), 0L)).thenComparingInt(Unit::id));
        Map<Integer, Integer> perFoe = new HashMap<>();
        int total = Math.min(MAX_TOKENS, 2 + Math.max(1, known.size()));
        for (Unit u : candidates) {
            if (tokens.size() >= total) break;
            int foe = made.get(u.id()).target();
            int c = perFoe.getOrDefault(foe, 0);
            if (c >= TOKENS_PER_FOE) continue;
            perFoe.put(foe, c + 1);
            tokens.add(u.id());
        }
    }

    private static int priority(Role r) {
        return switch (r) { case TANK -> 0; case SLIME -> 1; case FLANKER -> 2; default -> 3; };
    }
}
