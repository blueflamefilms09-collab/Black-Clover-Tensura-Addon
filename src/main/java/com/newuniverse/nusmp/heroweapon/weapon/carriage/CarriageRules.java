package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Geometry;

/**
 * Why: the carriage is a vehicle, and the rules that make it feel right and keep it safe are small pieces of arithmetic that decide who may sit where and how
 * fast the wheels turn: how quickly it can swing towards where the driver looks (faster when it is slow, slower at a gallop), how a boost and a charge multiply
 * its speed, which people may board and when, and how long the owner may stay away before the carriage goes. They are kept here without any game class so a test
 * can pin each of them; {@code CarriageEntity} only feeds them numbers.
 */
public final class CarriageRules {
    private CarriageRules() {}

    /** The most the carriage turns in one tick, in degrees, at a speed of {@code blocksPerTick}: 9 degrees standing still, 3 at a gallop. */
    public static double turnRate(double blocksPerTick) { return Geometry.clamp(9.0 - 12.0 * Math.max(0.0, blocksPerTick), 3.0, 9.0); }

    /** The yaw one tick later: the carriage swings towards {@code targetYaw} by at most the turn rate of its speed. */
    public static double steer(double yaw, double targetYaw, double blocksPerTick) { return Geometry.approachAngle(yaw, targetYaw, turnRate(blocksPerTick)); }

    /** The multiplier on the carriage's movement speed: a charge outruns a boost, which outruns a walk. */
    public static double speedFactor(boolean boosting, boolean charging) {
        if (charging) return CarriageNumbers.CHARGE_FACTOR;
        return boosting ? CarriageNumbers.BOOST_FACTOR : 1.0;
    }

    /** The same with Haikuikku running: the surge multiplies whatever the carriage is doing. */
    public static double speedFactor(boolean boosting, boolean charging, boolean hasted) {
        return speedFactor(boosting, charging) * (hasted ? CarriageNumbers.HASTE_FACTOR : 1.0);
    }

    /** The forward input the carriage takes from the driver: full ahead, and a reverse that is only a fraction of it. */
    public static double forwardInput(double zza) {
        double z = Geometry.clamp(zza, -1.0, 1.0);
        return z < 0.0 ? z * CarriageNumbers.REVERSE_FACTOR : z;
    }

    /**
     * May this person get on? The owner may whenever there is a seat. Anybody else (an ally, a teammate, a pet) may only when there is a seat AND the owner is
     * already aboard, so the owner always has the driver's seat and a stranger cannot sit on an empty carriage and be carried off by it.
     */
    public static boolean mayBoard(boolean isOwner, boolean ownerAboard, boolean isAlly, int aboard, int seats) {
        if (aboard >= seats) return false;
        if (isOwner) return true;
        return ownerAboard && isAlly;
    }

    /**
     * Why the Portal Gate cannot open, or null when it can: no waypoint is marked, the waypoint is in another dimension, it is nearer than the gate's minimum
     * or farther than its maximum. The text is what the wielder reads on the action bar.
     */
    public static String portalProblem(boolean marked, boolean sameDimension, double distance) {
        if (!marked) return "No waypoint is marked yet";
        if (!sameDimension) return "The waypoint is in another world";
        if (distance < CarriageNumbers.PORTAL_MIN) return "The waypoint is too near: the gate needs at least " + (int) CarriageNumbers.PORTAL_MIN + " blocks";
        if (distance > CarriageNumbers.PORTAL_MAX) return "The waypoint is too far: the gate reaches " + (int) CarriageNumbers.PORTAL_MAX + " blocks";
        return null;
    }

    /** Counts how long the owner has been off the carriage. */
    public static final class Absence {
        private int ticks;

        /** Call once a tick with whether the owner is aboard. */
        public void tick(boolean ownerAboard) { ticks = ownerAboard ? 0 : ticks + 1; }

        /** True once the owner has been away for more than {@code limit} ticks. */
        public boolean expired(int limit) { return ticks > limit; }

        public int ticks() { return ticks; }
    }
}
