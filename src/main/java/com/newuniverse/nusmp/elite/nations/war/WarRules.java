package com.newuniverse.nusmp.elite.nations.war;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the war state machine and the war score handler ("War Score Handler"), pure. {@link #advance} moves a war through its phases by the clock and by the score; {@link #scoreKill} and {@link #scoreSiege}
 * award points (with anti-farming and the siege/capture rule); {@link #canDeclare} lists every reason a declaration is refused; {@link #proposeCeasefire}/{@link #acceptCeasefire}/{@link #surrender}/{@link #cancel}
 * end a war by agreement. Nothing here knows Minecraft; the service feeds it the clock and the facts.
 */
public final class WarRules {
    private WarRules() {}

    /** Durations in game ticks and the score rules. */
    public record Timings(long declareTicks, long prepTicks, long activeTicks, long ceasefireTicks, int targetScore) {
        public static Timings defaults() { return new Timings(20L * 60, 20L * 60 * 10, 20L * 60 * 60, 20L * 60 * 30, 200); }
    }

    public enum Transition { TO_PREPARATION, TO_ACTIVE, ACTIVE_ENDED, TO_ENDED }

    public enum Refusal { NONE, SELF, UNKNOWN_NATION, ALLIED, ALREADY_AT_WAR, TRUCE, TOO_MANY_WARS, LEVEL_TOO_LOW, DECLARER_COOLDOWN }

    /** Everything the declare rule needs, as plain values. */
    public record DeclareContext(boolean bothExist, boolean allied, boolean openWarBetween, boolean truceBetween, int declarerOpenWars, int maxWarsPerNation,
                                 int declarerLevel, int minLevel, boolean declarerCooldown) {}

    public static Refusal canDeclare(String a, String b, DeclareContext c) {
        if (a == null || b == null || a.equals(b)) return Refusal.SELF;
        if (!c.bothExist()) return Refusal.UNKNOWN_NATION;
        if (c.allied()) return Refusal.ALLIED;
        if (c.openWarBetween()) return Refusal.ALREADY_AT_WAR;
        if (c.truceBetween()) return Refusal.TRUCE;
        if (c.declarerLevel() < c.minLevel()) return Refusal.LEVEL_TOO_LOW;
        if (c.declarerCooldown()) return Refusal.DECLARER_COOLDOWN;
        if (c.maxWarsPerNation() > 0 && c.declarerOpenWars() >= c.maxWarsPerNation()) return Refusal.TOO_MANY_WARS;
        return Refusal.NONE;
    }

    public static War declare(long id, String attacker, String defender, long now, Timings t, String reason) {
        War w = new War(id, attacker, defender);
        w.declaredAt = now;
        w.phaseEnds = now + t.declareTicks();
        w.reason = reason == null ? "" : reason;
        return w;
    }

    /** Moves the war as far as the clock and score allow (more than one step if a long time passed). Returns the transitions made, in order. */
    public static List<Transition> advance(War w, long now, Timings t) {
        List<Transition> out = new ArrayList<>();
        for (int guard = 0; guard < 6; guard++) {
            switch (w.phase) {
                case DECLARED -> {
                    if (now < w.phaseEnds) return out;
                    w.phase = WarPhase.PREPARATION;
                    w.phaseEnds = w.phaseEnds + t.prepTicks();
                    out.add(Transition.TO_PREPARATION);
                }
                case PREPARATION -> {
                    if (now < w.phaseEnds) return out;
                    w.phase = WarPhase.ACTIVE;
                    w.activeSince = w.phaseEnds;
                    w.phaseEnds = w.phaseEnds + t.activeTicks();
                    out.add(Transition.TO_ACTIVE);
                }
                case ACTIVE -> {
                    boolean reached = t.targetScore() > 0 && (w.scoreAttacker >= t.targetScore() || w.scoreDefender >= t.targetScore());
                    if (!reached && now < w.phaseEnds) return out;
                    finishByScore(w, now, t);
                    out.add(Transition.ACTIVE_ENDED);
                }
                case CEASEFIRE -> {
                    if (now < w.phaseEnds) return out;
                    w.phase = WarPhase.ENDED;
                    out.add(Transition.TO_ENDED);
                    return out;
                }
                case ENDED -> { return out; }
            }
        }
        return out;
    }

    private static void finishByScore(War w, long now, Timings t) {
        if (w.scoreAttacker == w.scoreDefender) {
            w.outcome = War.Outcome.DRAW;
            w.winner = "";
        } else {
            w.outcome = War.Outcome.VICTORY;
            w.winner = w.scoreAttacker > w.scoreDefender ? w.attacker : w.defender;
        }
        w.phase = WarPhase.CEASEFIRE;
        w.phaseEnds = now + t.ceasefireTicks();
        w.ceasefireFrom = "";
    }

    // ---------------------------------------------------------------- ending by agreement
    /** A side asks for a ceasefire during the ACTIVE phase. False if not possible. */
    public static boolean proposeCeasefire(War w, String by) {
        if (w.phase != WarPhase.ACTIVE || !w.involves(by) || !w.ceasefireFrom.isEmpty()) return false;
        w.ceasefireFrom = by;
        return true;
    }

    /** The other side accepts: the war ends in a CEASEFIRE (no winner, no spoils). */
    public static boolean acceptCeasefire(War w, String by, long now, Timings t) {
        if (w.phase != WarPhase.ACTIVE || !w.involves(by) || w.ceasefireFrom.isEmpty() || w.ceasefireFrom.equals(by)) return false;
        w.outcome = War.Outcome.CEASEFIRE;
        w.winner = "";
        w.phase = WarPhase.CEASEFIRE;
        w.phaseEnds = now + t.ceasefireTicks();
        w.ceasefireFrom = "";
        return true;
    }

    public static boolean declineCeasefire(War w, String by) {
        if (w.ceasefireFrom.isEmpty() || w.ceasefireFrom.equals(by) || !w.involves(by)) return false;
        w.ceasefireFrom = "";
        return true;
    }

    /** A side gives up during ACTIVE: the other side wins. */
    public static boolean surrender(War w, String by, long now, Timings t) {
        if (w.phase != WarPhase.ACTIVE || !w.involves(by)) return false;
        w.outcome = War.Outcome.SURRENDER;
        w.winner = w.other(by);
        w.phase = WarPhase.CEASEFIRE;
        w.phaseEnds = now + t.ceasefireTicks();
        w.ceasefireFrom = "";
        return true;
    }

    /** The attacker withdraws before the fighting starts. Straight to ENDED. */
    public static boolean cancel(War w, String by) {
        if (!(w.phase == WarPhase.DECLARED || w.phase == WarPhase.PREPARATION) || !w.attacker.equals(by)) return false;
        w.outcome = War.Outcome.CANCELLED;
        w.winner = "";
        w.phase = WarPhase.ENDED;
        return true;
    }

    /** Admin: end any open war now with no winner. */
    public static void force(War w, long now, Timings t) {
        if (w.phase == WarPhase.ENDED) return;
        w.outcome = War.Outcome.FORCED;
        w.winner = "";
        w.phase = WarPhase.CEASEFIRE;
        w.phaseEnds = now + Math.min(t.ceasefireTicks(), 20L * 60);
        w.ceasefireFrom = "";
    }

    // ---------------------------------------------------------------- score
    /**
     * A member of {@code killerNation} killed {@code victimId} (a member of the other side). Points only in ACTIVE, and the same victim only scores again after {@code cooldownTicks}. Returns the points
     * added (0 if none).
     */
    public static int scoreKill(War w, String killerNation, String victimId, long now, int points, long cooldownTicks) {
        if (w.phase != WarPhase.ACTIVE || !w.involves(killerNation) || victimId == null || points <= 0) return 0;
        Long last = w.lastKill.get(victimId);
        if (last != null && now >= last && now - last < cooldownTicks) return 0;
        w.lastKill.put(victimId, now);
        if (w.attacker.equals(killerNation)) { w.scoreAttacker += points; w.killsAttacker++; }
        else { w.scoreDefender += points; w.killsDefender++; }
        return points;
    }

    public record SiegeResult(int points, boolean captured) {}

    /**
     * One hold interval at a capital. {@code besiegerIsAttacker}: the attacker is standing in the DEFENDER's capital area (else the defender in the attacker's). {@code besiegers}/{@code defenders}: how many
     * members of each side are inside right now. The besiegers hold the area when they outnumber the defenders; each held interval gives {@code holdPoints}, and {@code captureIntervals} consecutive held
     * intervals capture the capital (one {@code capturePoints} award, the counter restarts). If the besiegers do not hold, the counter falls by one.
     */
    public static SiegeResult scoreSiege(War w, boolean besiegerIsAttacker, int besiegers, int defenders, int holdPoints, int capturePoints, int captureIntervals) {
        if (w.phase != WarPhase.ACTIVE) return new SiegeResult(0, false);
        int progress = besiegerIsAttacker ? w.siegeAttacker : w.siegeDefender;
        int points = 0;
        boolean captured = false;
        if (besiegers > 0 && besiegers > defenders) {
            progress++;
            points += Math.max(0, holdPoints);
            if (captureIntervals > 0 && progress >= captureIntervals) {
                progress = 0;
                captured = true;
                points += Math.max(0, capturePoints);
            }
        } else {
            progress = Math.max(0, progress - 1);
        }
        if (besiegerIsAttacker) {
            w.siegeAttacker = progress;
            w.scoreAttacker += points;
            if (captured) w.capturesAttacker++;
        } else {
            w.siegeDefender = progress;
            w.scoreDefender += points;
            if (captured) w.capturesDefender++;
        }
        return new SiegeResult(points, captured);
    }

    /** The coins a victory takes from the loser's treasury: a percent of its balance, never more than the cap. */
    public static long spoilsCoins(long loserBalance, int percent, long cap) {
        if (loserBalance <= 0 || percent <= 0) return 0L;
        long raw = loserBalance * Math.min(100, percent) / 100;
        return Math.max(0L, Math.min(raw, cap <= 0 ? Long.MAX_VALUE : cap));
    }
}
