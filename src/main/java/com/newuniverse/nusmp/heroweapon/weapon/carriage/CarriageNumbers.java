package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;

import java.util.List;

/**
 * Why: every number of the Vassal Carriage (its nine techniques and the vehicle they call) in one place without game classes, so a test can judge the whole
 * table against the design's budget and pin the vehicle's rules (the chassis a form's passives give, the seats, the speeds). The strongest value of each
 * technique already counts everything that can raise it, and the Crash! Charge! row says both of its blows (the crash and the stomp) so that one foe's total
 * stays inside an ultimate's 32 raw. The carriage deals its damage only while a technique is running (a boost, a drift, a charge): driving it never hurts anyone.
 */
public final class CarriageNumbers {
    private CarriageNumbers() {}

    // ---------------------------------------------------------------- the techniques (the budget table)
    /** Calls the carriage, boards it, or sends it away. */
    public static final Budget.Spec CALL = Budget.utility("carriage.call", 100, 0.01, 1);
    /** A gallop: the carriage runs faster for a while and the first blow on each foe it meets is 8 raw with a shove. */
    public static final Budget.Spec BOOST = Budget.damage("carriage.boost", 160, 0.015, 8.0, 8.0, 1, 12);
    /** A sweep round the carriage: 7 raw, a shove and a slow for every foe near it. */
    public static final Budget.Spec DRIFT_RAM = Budget.damage("carriage.drift_ram", 140, 0.02, 7.0, 7.0, 1, 12);
    /** Heals the people on the carriage and beside it by 12 percent of their health and washes away one harmful effect each. */
    public static final Budget.Spec COMFORT = Budget.utility("carriage.comfort", 400, 0.03, 6).withHeal(0.12);
    /** A cabin barrier: the people aboard take less damage and the carriage cannot be hurt for five seconds. */
    public static final Budget.Spec CABIN_BARRIER = Budget.utility("carriage.cabin_barrier", 360, 0.025, 4);
    /** Haikuikku: a surge of speed for the people aboard. */
    public static final Budget.Spec HAIKUIKKU = Budget.utility("carriage.haikuikku", 300, 0.02, 4);
    /** Saves the place the wielder stands on as the carriage's waypoint. */
    public static final Budget.Spec WAYPOINT = Budget.utility("carriage.waypoint", 100, 0.01, 1);
    /** Opens a gate to the waypoint and carries the carriage and its riders through. */
    public static final Budget.Spec PORTAL = Budget.utility("carriage.portal", 1200, 0.03, 4);
    /** The ultimate: a charge that crashes into the first foes for 14 raw and stomps them for 8 more. */
    public static final Budget.Spec CRASH_CHARGE = Budget.ultimate("carriage.crash_charge", 900, 0.06, 8.0, 14.0, 2, 12);

    /** Every technique of the carriage, in the order the logic lists them. */
    public static final List<Budget.Spec> ALL = List.of(CALL, BOOST, DRIFT_RAM, COMFORT, CABIN_BARRIER, HAIKUIKKU, WAYPOINT, PORTAL, CRASH_CHARGE);

    // ---------------------------------------------------------------- the vehicle
    /** The movement speed attribute of the plainest carriage (a horse is about 0.225), and what a boost and a charge multiply it by. */
    public static final double BASE_SPEED = 0.22, BOOST_FACTOR = 1.8, CHARGE_FACTOR = 2.3, REVERSE_FACTOR = 0.3;
    /** Health of the plain and of the armoured frame. */
    public static final double BASE_HEALTH = 40.0, ARMOURED_HEALTH = 90.0;
    /** Speed multipliers of the Swift Wheels and the Queen's Pull passives. */
    public static final double SWIFT_SPEED = 1.15, QUEEN_SPEED = 1.3;
    /** Seats of the plain and of the roomy cabin (the driver's seat is the first). */
    public static final int SEATS_PLAIN = 2, SEATS_ROOMY = 3;
    /**
     * Where the riders sit, in the carriage's own frame (x to the left, y up, z forward, in blocks from the middle of the whole model, which is the middle of its
     * length including the draught pole): the point of the carriage that a rider's seat point (0.6 above their feet) is put on. It is the BENCH SURFACE, a little
     * sunk into the cushion so that a seated player's head clears the canopy: the art's bench surface is 0.17 of the model's length (0.544 blocks) above the
     * ground and the points are at 0.50. The driver sits 0.17 of the length ahead of the middle of the BODY (the body is 0.70 of the length, tail to dash, so its
     * middle is 0.15 of the length behind the middle of the model: +0.06 here) and the two passengers 0.19 behind it and 0.14 to either side (-1.09 and +-0.45).
     */
    public static final double[][] SEATS = {{0.0, 0.50, 0.06}, {0.45, 0.50, -1.09}, {-0.45, 0.50, -1.09}};
    /** Hitbox of the carriage in blocks (width, height); the model is drawn 3.2 blocks long. */
    public static final float WIDTH = 2.2f, HEIGHT = 1.7f;
    public static final double MODEL_LENGTH = 3.2;
    /** Ticks the owner may stay off the carriage before it goes away (ten seconds). */
    public static final int ABSENT_LIMIT = 200;

    // ---------------------------------------------------------------- the techniques' shapes
    public static final double CALL_AHEAD = 3.2;

    public static final int BOOST_TICKS = 50;
    public static final double RAM_REACH = 1.4, RAM_PUSH = 1.0;

    public static final double DRIFT_RADIUS = 4.0, DRIFT_PUSH = 1.1;
    public static final int DRIFT_DELAY = 8, DRIFT_SLOW_TICKS = 30;

    public static final double COMFORT_RADIUS = 6.0;

    public static final int BARRIER_TICKS = 100, BARRIER_AMP = 1;

    public static final int HAIKUIKKU_TICKS = 160, HAIKUIKKU_AMP = 1;
    /** What Haikuikku multiplies the carriage's own speed by (the people aboard also get the speed effect). */
    public static final double HASTE_FACTOR = 1.25;

    public static final int PORTAL_TELL = 60;
    public static final double PORTAL_MIN = 16.0, PORTAL_MAX = 1000.0;

    public static final int CHARGE_WARNING = 16, CHARGE_TICKS = 44, CHARGE_STUN_TICKS = 20, STOMP_DELAY = 10;
    public static final double CHARGE_PUSH = 1.6, STOMP_RADIUS = 4.0;
    /** How far ahead of the carriage the charge's warning line is drawn, in blocks. */
    public static final double CHARGE_LANE = 20.0;

    /** What a form's passives make of the carriage. */
    public record Chassis(double health, double speed, int seats) {}

    /**
     * The chassis the passives give: Roomy Cabin adds the third seat, Armoured Frame the larger health, Swift Wheels a faster carriage and the Queen's Pull a
     * faster one still (they do not add up; the greater counts).
     */
    public static Chassis chassis(boolean roomy, boolean swift, boolean armoured, boolean queens) {
        double speed = BASE_SPEED * (queens ? QUEEN_SPEED : swift ? SWIFT_SPEED : 1.0);
        return new Chassis(armoured ? ARMOURED_HEALTH : BASE_HEALTH, speed, roomy ? SEATS_ROOMY : SEATS_PLAIN);
    }
}
