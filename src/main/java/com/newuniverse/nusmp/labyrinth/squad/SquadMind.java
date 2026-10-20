package com.newuniverse.nusmp.labyrinth.squad;

import java.util.List;
import java.util.Locale;

/**
 * Why: three judgements of a squad are pure comparisons, and a judgement nobody can run in a game must at least be a function a test can run: WHO is the ward the
 * tank stands in front of (the softest member: a healer first, then a shooter, then whoever is lowest on health), WHICH threat the whole squad hits (focus fire, with
 * the taunt rule that whoever just hurt a squad member comes first), and WHEN morale breaks and what the survivors do (they flee, they enrage, or by role). The
 * squad's tick feeds these small records from live entities and applies the answer; nothing here knows a mob.
 */
public final class SquadMind {
    private SquadMind() {}

    // ---------------------------------------------------------------- the ward
    /** A member as the ward choice sees it: its index in the squad, its role and its health as a fraction of its maximum. */
    public record WardView(int index, SquadRole role, double hpFrac) {}

    /**
     * The member a tank covers: the softest of the others. A support is softer than a ranged damage dealer, which is softer than a melee kind; inside a class the one
     * lowest on health wins, then the lowest index. Tanks and leaders are never wards. Returns -1 when nobody needs covering (a squad of tanks).
     */
    public static int chooseWard(List<WardView> members, int selfIndex) {
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (WardView m : members) {
            if (m.index() == selfIndex || m.role().isFrontLine()) continue;
            double classScore = switch (m.role()) {
                case SUPPORT -> 0.0;
                case DPS_RANGED -> 1.0;
                default -> 2.0;
            };
            double score = classScore + Math.max(0.0, Math.min(1.0, m.hpFrac()));
            if (score < bestScore - 1e-9) {
                bestScore = score;
                best = m.index();
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- the focus target
    /**
     * A possible target as the focus choice sees it. {@code attackedAlly} is true when it hurt a squad member a moment ago (the taunt: it is the squad's business
     * first); {@code distToWard} and {@code distToCentre} are blocks; {@code hpFrac} its health as a fraction (a hurt target is finished before a fresh one is started).
     */
    public record ThreatView(int id, double distToCentre, double distToWard, double hpFrac, boolean attackedAlly, boolean player) {}

    /** The index (in the list) of the threat the squad hits, or -1 for none: the lowest score wins, ties go to the lowest id, so every member chooses the same one. */
    public static int focusTarget(List<ThreatView> threats) {
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < threats.size(); i++) {
            ThreatView t = threats.get(i);
            double score = t.distToCentre() * 0.4 + t.distToWard() * 0.6 + Math.max(0.0, Math.min(1.0, t.hpFrac())) * 3.0 - (t.attackedAlly() ? 30.0 : 0.0) - (t.player() ? 4.0 : 0.0);
            if (best < 0 || score < bestScore - 1e-9 || (Math.abs(score - bestScore) <= 1e-9 && t.id() < threats.get(best).id())) {
                best = i;
                bestScore = score;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- morale
    /** How a squad member reacts to a broken morale. */
    public enum Reaction { HOLD, FLEE, ENRAGE }

    /** The three modes of the config key {@code squads.moraleBreak}. */
    public enum Mode {
        FLEE, ENRAGE, MIXED;

        public static Mode of(String s) {
            if (s == null) return MIXED;
            return switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "flee" -> FLEE;
                case "enrage" -> ENRAGE;
                default -> MIXED;
            };
        }
    }

    /** Why morale broke, or NONE. */
    public enum Cause { NONE, LEADER_DOWN, LOSSES, LAST_STANDING }

    /**
     * Has the squad's nerve gone? Yes when the leader fell, when only one member is left of a squad of more, or when fewer than {@code fraction} of the squad stand.
     * The leader rule goes first (it is the clearest story to tell the player).
     */
    public static Cause check(int size, int alive, boolean leaderKnown, boolean leaderAlive, double fraction) {
        if (size <= 1 || alive <= 0) return Cause.NONE;
        if (leaderKnown && !leaderAlive) return Cause.LEADER_DOWN;
        if (alive <= 1) return Cause.LAST_STANDING;
        if (alive < Math.ceil(size * Math.max(0.0, Math.min(1.0, fraction)))) return Cause.LOSSES;
        return Cause.NONE;
    }

    /** What one survivor does: in {@code mixed} mode the front-line and melee kinds enrage and the shooters and healers flee; the last one standing always flees. */
    public static Reaction react(Mode mode, SquadRole role, int alive) {
        if (alive <= 1) return Reaction.FLEE;
        return switch (mode) {
            case FLEE -> Reaction.FLEE;
            case ENRAGE -> Reaction.ENRAGE;
            case MIXED -> role.isRear() ? Reaction.FLEE : Reaction.ENRAGE;
        };
    }
}
