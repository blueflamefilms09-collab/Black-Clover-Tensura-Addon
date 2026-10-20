package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.SquadGeometry.V2;

import java.util.Optional;

/**
 * Why: the four movement goals (intercept, flank, retreat, follow) and the driver that steers the squads of mobs without a goal list all have to agree on what a
 * role DOES in a given moment, so that decision is one pure function: from a snapshot of the mob, its target, its ward and its squad it answers "walk here, at this
 * speed, for this reason", "stand still and wait", or nothing (let the mob's own attack goals act). The goals are thin: they build the snapshot from live entities
 * every few ticks, ask here, and walk to the answer. Because the answer is a value, every rule of the brief is a unit test: a tank steps to the point 1.5 blocks from
 * the ward on the segment to the target and then holds; a flanker waits until a tank has engaged, then goes to a point 110 to 150 degrees off the target's facing,
 * its start staggered by its slot so two flankers do not clump, and gives up when the target looks at it from close by or when it is hurt; a healer keeps 8 to 12
 * blocks from the nearest threat and runs to the squad's centre; a follower keeps its formation slot behind the leader while nothing is in sight.
 */
public final class RoleMotion {
    private RoleMotion() {}

    /** What the move is for; each goal answers to one kind. */
    public enum Kind { INTERCEPT, FLANK, RETREAT, FORM, HOLD, ABORT }

    /** A step to take: the kind, the place (null for HOLD and ABORT) and the speed modifier for the navigation. */
    public record Move(Kind kind, V2 to, double speed) {
        public static Move hold() { return new Move(Kind.HOLD, null, 1.0); }

        public static Move abort() { return new Move(Kind.ABORT, null, 1.0); }
    }

    /**
     * The facts one decision reads. {@code target}, {@code ward}, {@code threat} and {@code leader} are null when there is none. {@code targetYaw} is where the
     * target looks. {@code slot} is the member's index in the squad (it staggers flankers); {@code slotOffset} is its formation slot; {@code sinceEngaged} counts ticks
     * since a tank reached the target (0 when none has); {@code waited} counts ticks the member has waited for that; {@code flip} turns a flanker to the other flank.
     */
    public record Ctx(SquadRole role, int slot, V2 self, double hpFrac, V2 target, double targetYaw, V2 ward, V2 centre, double squadYaw, V2 leader,
                      SquadFormation.Offset slotOffset, boolean tankEngaged, int sinceEngaged, int waited, V2 threat, boolean flip, double reach) {}

    /** Speed modifiers handed to the navigation. */
    public static final double SPEED_INTERCEPT = 1.2, SPEED_FLANK = 1.1, SPEED_RETREAT = 1.15, SPEED_FORM = 1.0;
    /** The tolerances: how close to the segment counts as covering, how far from its slot a follower may drift, how long a flanker waits, and its stagger per slot (ticks). */
    public static final double COVER_TOLERANCE = 1.6, SLOT_TOLERANCE = 2.5, SEEN_CONE = 40.0, SEEN_RANGE = 8.0, LOW_HEALTH = 0.35, ARRIVE = 1.2;
    public static final int MAX_WAIT = 120, STAGGER = 10;
    /** The ward-to-tank gap, and the bands the rear roles keep from a threat (blocks). */
    public static final double AHEAD = 1.5, SUPPORT_MIN = 8.0, SUPPORT_MAX = 12.0, RANGED_MIN = 6.0, RANGED_MAX = 14.0, HOLD_BACK = 9.0;

    /** Which melee damage dealers flank: all flankers, and every second melee damage dealer (by slot parity), so a squad always keeps some at the front. */
    public static boolean flanks(SquadRole role, int slot) {
        return role == SquadRole.FLANKER || (role == SquadRole.DPS_MELEE && slot % 2 == 1);
    }

    /** The decision for one member; empty means "nothing special: let the mob's own goals act". Never throws on a partial snapshot. */
    public static Optional<Move> decide(Ctx c) {
        if (c.target() == null) return follow(c);
        return switch (c.role()) {
            case TANK -> tank(c);
            case LEADER -> c.ward() == null ? Optional.empty() : tank(c);
            case SUPPORT -> support(c);
            case DPS_RANGED -> ranged(c);
            case FLANKER, DPS_MELEE -> flanks(c.role(), c.slot()) ? flank(c) : Optional.empty();
        };
    }

    // ---------------------------------------------------------------- no target: stay in formation
    private static Optional<Move> follow(Ctx c) {
        if (c.role() == SquadRole.LEADER) return Optional.empty();
        V2 anchor = c.leader() != null ? c.leader() : c.centre();
        SquadFormation.Offset o = c.slotOffset();
        V2 slot = o == null ? anchor : SquadGeometry.slot(anchor, c.squadYaw(), o.forward(), o.lateral());
        if (c.self().dist(slot) <= SLOT_TOLERANCE) return Optional.of(Move.hold());
        return Optional.of(new Move(Kind.FORM, slot, SPEED_FORM));
    }

    // ---------------------------------------------------------------- the tank
    private static Optional<Move> tank(Ctx c) {
        if (c.ward() == null) return Optional.empty();
        if (SquadGeometry.covers(c.self(), c.ward(), c.target(), COVER_TOLERANCE)) return Optional.of(Move.hold());
        V2 p = SquadGeometry.interceptPoint(c.ward(), c.target(), AHEAD);
        return Optional.of(new Move(Kind.INTERCEPT, p, SPEED_INTERCEPT));
    }

    // ---------------------------------------------------------------- the flanker
    private static Optional<Move> flank(Ctx c) {
        double dist = c.self().dist(c.target());
        if (c.hpFrac() < LOW_HEALTH) return Optional.of(Move.abort());
        if (SquadGeometry.seenBy(c.target(), c.targetYaw(), c.self(), SEEN_CONE, SEEN_RANGE) && dist <= SEEN_RANGE) return Optional.of(Move.abort());
        boolean committed = c.tankEngaged() && c.sinceEngaged() >= c.slot() * STAGGER;
        if (!committed && c.waited() < MAX_WAIT) {
            if (dist < HOLD_BACK) return Optional.of(Move.hold());
            return Optional.empty();                         // far away: close in the usual way until the tank has engaged
        }
        if (dist <= c.reach() + 0.6) return Optional.empty();                // near enough to strike: the mob's own attack goal takes over
        V2 p = SquadGeometry.flankPoint(c.target(), c.targetYaw(), SquadGeometry.flankAngle(c.slot(), c.flip()), Math.max(1.6, c.reach() * 0.8));
        return Optional.of(new Move(Kind.FLANK, p, SPEED_FLANK));
    }

    // ---------------------------------------------------------------- the rear
    private static Optional<Move> support(Ctx c) {
        V2 threat = c.threat() != null ? c.threat() : c.target();
        if (threat == null) return follow(c);
        V2 p = SquadGeometry.retreatPoint(c.self(), threat, c.centre(), SUPPORT_MIN, SUPPORT_MAX);
        if (p == null) return Optional.of(Move.hold());
        return Optional.of(new Move(Kind.RETREAT, p, SPEED_RETREAT));
    }

    private static Optional<Move> ranged(Ctx c) {
        if (!c.tankEngaged()) return Optional.empty();                       // the line is not formed yet: shoot as the mob likes
        V2 p = SquadGeometry.retreatPoint(c.self(), c.target(), c.centre(), RANGED_MIN, RANGED_MAX);
        if (p == null) return Optional.empty();
        return Optional.of(new Move(Kind.RETREAT, p, SPEED_RETREAT));
    }
}
