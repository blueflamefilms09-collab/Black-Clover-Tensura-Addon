package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.Heard;
import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.Sighting;
import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.State;
import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.Tuning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the thinking of one wandering boss, with no Minecraft in it. The game looks at the world every half second and hands this class what the
 * boss can perceive (the players it can see, the loudest thing it heard, where it stands, whether its walk has ended); the mind answers with ONE
 * order: where to walk and how fast, and whom to hunt. Keeping the decision apart from the body is what makes the behaviour testable ("a
 * crouching player in the dark is not chased", "a chase that loses its target ends in a search, then a patrol") and what lets the same mind
 * drive an entity with goals, a Tensura brain or a boss of our own. The cycle is PATROL (walk the room graph) to INVESTIGATE (a sound) or CHASE
 * (a sighting) to SEARCH (it lost you; look where you were, then nearby rooms) and back to PATROL. A chase is followed only while the target is
 * perceived; once it is hidden the boss walks to where it LAST saw the target and no further, so it never tracks a player through walls. A chase
 * ends by loss (nobody seen for the lose time), by refuge (the target reached a safe room: the boss does not follow), or because the target is
 * gone (dead, left the sector, logged out); the first two are the "survived" moments that quests count. The mind keeps its own stuck detection
 * (no progress for four seconds, or the walk ended short of the goal) and remembers the nodes it failed to reach for a minute, so a blocked door
 * does not trap it.
 */
public final class WandererMind {
    /** The horizontal distance at which a goal counts as reached. */
    public static final double REACH = 3.0;
    public static final int STUCK_TICKS = 80, RECENT = 6, BLOCKED_TICKS = 1200;
    public static final double PATROL_SPEED = 0.6, INVESTIGATE_SPEED = 0.85, CHASE_SPEED = 1.1, SEARCH_SPEED = 0.65;

    /** What happened on this pass, for the game to announce, count or reward. */
    public enum Event { NONE, SPOTTED, HEARD, LOST, REFUGE, GAVE_UP }

    /**
     * One look at the world. {@code seen} holds every player of the boss's sector the game considers (the chase target is always included
     * while it is alive and in the sector, even when far; absence means it is gone). {@code navDone} is whether the walk the boss was sent on
     * has ended. {@code heard} is the best sound it could hear, or null. {@code hurtBy} is the player that struck it since the last pass, or null:
     * a boss that is hit knows where the blow came from even when it cannot see the player (unless the player stands in a safe room).
     */
    public record Input(long now, double x, double y, double z, boolean navDone, List<Sighting> seen, Heard heard, UUID hurtBy) {}

    /**
     * The answer. {@code move} says whether to walk to ({@code gx}, {@code gy}, {@code gz}); {@code chase} is the player to HUNT right now (set
     * only while that player is perceived) or null (the boss must have no target); {@code event} and {@code eventPlayer} describe what changed;
     * {@code chaseTicks} is how long the chase that just ended had lasted (0 for other events).
     */
    public record Order(State state, boolean move, double gx, double gy, double gz, UUID chase, double speed, Event event, UUID eventPlayer, long chaseTicks) {}

    private State state = State.PATROL;
    private UUID target;
    private long chaseSince, lastSeen = Long.MIN_VALUE, searchUntil, investigateUntil, goalSetAt = Long.MIN_VALUE, progressAt = Long.MIN_VALUE;
    private double lastX, lastY, lastZ, px, pz;
    private boolean hasGoal;
    private double gx, gy, gz;
    private int goalNode = -1, currentNode = -1;
    private double heardLoudness;
    private final List<Integer> recent = new ArrayList<>();
    private final Map<Integer, Long> blocked = new HashMap<>();

    public State state() { return state; }

    public UUID target() { return target; }

    public int currentNode() { return currentNode; }

    public int goalNode() { return goalNode; }

    public boolean hasGoal() { return hasGoal; }

    /** The target the boss may hunt right now, or null: the game's target-change guard lets a target through only when it is this one. */
    public UUID approvedTarget() { return state == State.CHASE ? target : null; }

    // ---------------------------------------------------------------- one pass
    public Order step(Input in, Tuning t, PatrolGraph g, Random r) {
        long now = in.now();
        Event event = Event.NONE;
        UUID who = null;
        long chased = 0;
        blocked.values().removeIf(until -> until <= now);

        Sighting best = null, mine = null, struck = null;
        for (Sighting s : in.seen()) {
            if (s.id().equals(target)) mine = s;
            if (s.id().equals(in.hurtBy()) && !s.safe()) struck = s;
            if (WandererRules.notices(t, s) && (best == null || s.distance() < best.distance())) best = s;
        }
        if (struck != null) best = struck;

        if (state == State.CHASE) {
            if (mine == null) {
                who = target;
                chased = now - chaseSince;
                event = Event.GAVE_UP;
                beginSearch(now, t, g);
            } else if (mine.safe()) {
                remember(mine, now);
                who = target;
                chased = now - chaseSince;
                event = Event.REFUGE;
                beginSearch(now, t, g);
            } else if (WandererRules.notices(t, mine)) {
                remember(mine, now);
            } else if (now - lastSeen > t.loseTicks()) {
                who = target;
                chased = now - chaseSince;
                event = Event.LOST;
                beginSearch(now, t, g);
            } else if (best != null && best != mine) {
                beginChase(best, now);
                event = Event.SPOTTED;
                who = best.id();
            }
        } else if (best != null) {
            beginChase(best, now);
            event = Event.SPOTTED;
            who = best.id();
        } else if (in.heard() != null && (state != State.INVESTIGATE || in.heard().loudness() > heardLoudness + 1.0)) {
            beginInvestigate(in.heard(), now);
            event = Event.HEARD;
        }

        if (state == State.CHASE) {
            boolean seeing = false;                                  // is the (possibly new) target perceived right now?
            for (Sighting s : in.seen()) if (s.id().equals(target) && WandererRules.notices(t, s)) seeing = true;
            return new Order(state, true, lastX, lastY, lastZ, seeing ? target : null, CHASE_SPEED, event, who, chased);
        }

        // PATROL, INVESTIGATE and SEARCH walk to a goal
        boolean arrived = hasGoal && horizontal(in.x(), in.z(), gx, gz) <= REACH && Math.abs(in.y() - gy) <= 6.0;
        boolean stuck = stuck(in, arrived);
        switch (state) {
            case INVESTIGATE -> {
                if (arrived || stuck || now >= investigateUntil) {
                    if (arrived) {
                        lastX = gx;
                        lastY = gy;
                        lastZ = gz;
                    }
                    beginSearch(now, t, g);
                    arrived = false;
                    stuck = false;
                }
            }
            case SEARCH -> {
                if (now >= searchUntil) {
                    state = State.PATROL;
                    hasGoal = false;
                    currentNode = g.nearest(in.x(), in.z());
                    if (currentNode >= 0) push(currentNode);
                }
            }
            default -> { }
        }
        if (state == State.PATROL || state == State.SEARCH) advance(in, g, r, arrived, stuck);
        double speed = switch (state) {
            case INVESTIGATE -> INVESTIGATE_SPEED;
            case SEARCH -> SEARCH_SPEED;
            default -> PATROL_SPEED;
        };
        return new Order(state, hasGoal, gx, gy, gz, null, speed, event, who, chased);
    }

    // ---------------------------------------------------------------- transitions
    private void beginChase(Sighting s, long now) {
        state = State.CHASE;
        target = s.id();
        chaseSince = now;
        hasGoal = false;
        remember(s, now);
    }

    private void remember(Sighting s, long now) {
        lastSeen = now;
        lastX = s.x();
        lastY = s.y();
        lastZ = s.z();
    }

    private void beginInvestigate(Heard h, long now) {
        state = State.INVESTIGATE;
        investigateUntil = now + 400;
        heardLoudness = h.loudness();
        gx = h.x();
        gy = h.y();
        gz = h.z();
        hasGoal = true;
        goalNode = -1;
        goalSetAt = now;
        progressAt = Long.MIN_VALUE;
    }

    /** Look round where the target (or the noise) was: go to the nearest room and then to the rooms beside it, for the search time. */
    private void beginSearch(long now, Tuning t, PatrolGraph g) {
        state = State.SEARCH;
        target = null;
        searchUntil = now + t.searchTicks();
        int n = g.nearest(lastX, lastZ);
        currentNode = n;
        if (n >= 0) {
            PatrolGraph.Node node = g.node(n);
            setGoal(node.id(), node.x(), node.y(), node.z(), now);
            push(n);
        } else {
            hasGoal = false;
        }
    }

    /** Picks the next node of the walk when the goal was reached (or failed), and sets it as the goal. */
    private void advance(Input in, PatrolGraph g, Random r, boolean arrived, boolean stuck) {
        long now = in.now();
        if (hasGoal && !arrived && !stuck) return;
        if (stuck && goalNode >= 0) blocked.put(goalNode, now + BLOCKED_TICKS);
        if (arrived && goalNode >= 0) {
            currentNode = goalNode;
            push(goalNode);
        }
        if (currentNode < 0 || !g.has(currentNode)) {
            currentNode = g.nearest(in.x(), in.z());
            if (currentNode >= 0) push(currentNode);
        }
        if (currentNode < 0) {
            hasGoal = false;
            return;
        }
        Set<Integer> avoid = blocked.isEmpty() ? Set.of() : new HashSet<>(blocked.keySet());
        int next = g.next(currentNode, recent, avoid, r);
        if (next < 0) {
            hasGoal = false;
            return;
        }
        PatrolGraph.Node n = g.node(next);
        setGoal(n.id(), n.x(), n.y(), n.z(), now);
    }

    private void setGoal(int node, double x, double y, double z, long now) {
        hasGoal = true;
        goalNode = node;
        gx = x;
        gy = y;
        gz = z;
        goalSetAt = now;
        progressAt = Long.MIN_VALUE;
    }

    private void push(int node) {
        recent.add(node);
        while (recent.size() > RECENT) recent.remove(0);
    }

    /** No progress for four seconds, or the walk has ended well short of the goal. */
    private boolean stuck(Input in, boolean arrived) {
        if (!hasGoal || arrived) {
            progressAt = Long.MIN_VALUE;
            return false;
        }
        long now = in.now();
        if (progressAt == Long.MIN_VALUE) {
            progressAt = now;
            px = in.x();
            pz = in.z();
        }
        if (in.navDone() && now - goalSetAt > 40) return true;
        if (now - progressAt >= STUCK_TICKS) {
            boolean still = horizontal(px, pz, in.x(), in.z()) < 1.5;
            progressAt = now;
            px = in.x();
            pz = in.z();
            return still;
        }
        return false;
    }

    private static double horizontal(double ax, double az, double bx, double bz) {
        double dx = ax - bx, dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
