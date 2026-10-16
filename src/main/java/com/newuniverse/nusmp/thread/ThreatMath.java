package com.newuniverse.nusmp.thread;

/**
 * Why: Rouge has to be standing next to the right ally before the blow lands, which means ranking the allies by how close they are
 * to harm and choosing how to cross the distance. Both are plain arithmetic, kept apart from the entity so the ranking can be tested
 * and tuned without a running game. A score of 1.0 or more means "go now".
 */
public final class ThreatMath {
    private ThreatMath() {}

    public static final double RUN_THRESHOLD = 1.0;
    /** A projectile counts as "about to hit" when it arrives within this many ticks. */
    public static final int PROJECTILE_HORIZON = 12;
    public static final double LOW_HEALTH = 0.4;

    /** What the watcher saw about one protected ally. projectileEta is -1 when nothing is flying at them. */
    public record Threat(double healthFraction, boolean hostileNear, boolean recentlyHurt, int projectileEta, boolean attackerAdjacent, boolean bossAttacker) {}

    public static double score(Threat t) {
        double s = 0;
        if (t.healthFraction() < LOW_HEALTH) {
            s += (t.hostileNear() || t.recentlyHurt()) ? 1.0 + (LOW_HEALTH - t.healthFraction()) * 2.0 : 0.25;
        }
        if (t.projectileEta() >= 0 && t.projectileEta() <= PROJECTILE_HORIZON) s += 2.0 - t.projectileEta() * 0.05;
        if (t.attackerAdjacent() && t.healthFraction() < 0.6) s += 0.6;
        if (t.bossAttacker() && (t.hostileNear() || t.attackerAdjacent())) s += 0.5;
        return s;
    }

    /** Ticks until a projectile at 'distance' blocks, closing at 'closingSpeed' blocks per tick, arrives; -1 if it is not closing. */
    public static int projectileEta(double distance, double closingSpeed) {
        if (closingSpeed <= 0.05) return -1;
        return (int) Math.ceil(Math.max(0.0, distance) / closingSpeed);
    }

    public enum Approach { ARRIVED, SPRINT, HOP }

    public static final double ARRIVED_DISTANCE = 1.8, HOP_DISTANCE = 8.0;

    /** How Rouge crosses 'distance' blocks to the endangered ally: already there, sprint-and-leap, or hop through thread. */
    public static Approach approach(double distance) {
        if (distance <= ARRIVED_DISTANCE) return Approach.ARRIVED;
        return distance <= HOP_DISTANCE ? Approach.SPRINT : Approach.HOP;
    }

    /** How far from the caster Rouge may wander before she hops back (blocks). */
    public static final double LEASH = 16.0;

    public static boolean tooFarFromCaster(double distance) { return distance > LEASH; }
}
