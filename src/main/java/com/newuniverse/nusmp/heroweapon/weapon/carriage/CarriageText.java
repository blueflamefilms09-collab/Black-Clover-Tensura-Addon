package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Fmt;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.newuniverse.nusmp.heroweapon.weapon.carriage.CarriageNumbers.*;

/**
 * Why: the names and descriptions of the carriage's techniques and passives, built from {@link CarriageNumbers} so the numbers in the text are the numbers in
 * the table, free of game classes so a test can read them, and the source of the lang fragment the lead merges. Plain ASCII English.
 */
public final class CarriageText {
    private CarriageText() {}

    /** Skill id to {name, description}. */
    public static final Map<String, String[]> SKILLS = new LinkedHashMap<>();
    /** Passive id to {name, description}. */
    public static final Map<String, String[]> PASSIVES = new LinkedHashMap<>();

    static {
        SKILLS.put(CALL.id(), new String[]{"Call the Carriage",
                "Calls your carriage " + Fmt.num(CALL_AHEAD) + " blocks in front of you and seats you in it (if it is already out it is brought to you). Used while you drive it, "
                        + "the carriage is sent away. It also leaves when you put the weapon away or stay off it for " + Fmt.secs(ABSENT_LIMIT) + " s. Only one carriage at a time."});
        SKILLS.put(BOOST.id(), new String[]{"Gallop Boost",
                "While you drive: the carriage runs " + Fmt.pct(BOOST_FACTOR - 1.0) + " percent faster for " + Fmt.secs(BOOST_TICKS) + " s and the first blow on each foe it runs into deals "
                        + Fmt.num(BOOST.rawBase()) + " damage and a shove."});
        SKILLS.put(DRIFT_RAM.id(), new String[]{"Drift Ram",
                "While you drive: a ring shows round the carriage, then it sweeps everything within " + Fmt.num(DRIFT_RADIUS) + " blocks for " + Fmt.num(DRIFT_RAM.rawBase())
                        + " damage, a shove and a slow."});
        SKILLS.put(COMFORT.id(), new String[]{"Soothing Ride",
                "Heals everyone on the carriage, the carriage itself and every ally within " + Fmt.num(COMFORT_RADIUS) + " blocks of it (or of you), " + COMFORT.maxTargets()
                        + " at most, for " + Fmt.pct(COMFORT.heal()) + " percent of their health and washes away one harmful effect each."});
        SKILLS.put(CABIN_BARRIER.id(), new String[]{"Cabin Barrier",
                "For " + Fmt.secs(BARRIER_TICKS) + " s everyone aboard takes less damage (resistance) and the carriage itself cannot be hurt."});
        SKILLS.put(HAIKUIKKU.id(), new String[]{"Haikuikku",
                "A surge of speed for " + Fmt.secs(HAIKUIKKU_TICKS) + " s: the people aboard run faster and, while you drive, the carriage is " + Fmt.pct(HASTE_FACTOR - 1.0)
                        + " percent faster still. On foot it hastens you and the allies beside you."});
        SKILLS.put(WAYPOINT.id(), new String[]{"Mark Waypoint",
                "Saves the place you stand on as the waypoint of your carriage (one at a time; the Portal Gate goes there)."});
        SKILLS.put(PORTAL.id(), new String[]{"Portal Gate",
                "Opens a gate at the carriage and at the waypoint; after " + Fmt.secs(PORTAL_TELL) + " s the carriage and everyone aboard cross and sit down again. Same dimension, between "
                        + Fmt.num(PORTAL_MIN) + " and " + Fmt.num(PORTAL_MAX) + " blocks. Needs a clear place for the carriage at the waypoint."});
        SKILLS.put(CRASH_CHARGE.id(), new String[]{"Crash! Charge!",
                "While you drive: after " + Fmt.secs(CHARGE_WARNING) + " s of warning the carriage charges for " + Fmt.secs(CHARGE_TICKS) + " s at " + Fmt.num(CHARGE_FACTOR)
                        + " times its speed. Each foe it crashes into takes " + Fmt.num(CRASH_CHARGE.rawMax()) + " damage and a shove, the first is stunned for " + Fmt.secs(CHARGE_STUN_TICKS)
                        + " s, and a moment later every foe near it is stomped for " + Fmt.num(CRASH_CHARGE.rawBase()) + "."});

        PASSIVES.put("roomy_cabin", new String[]{"Roomy Cabin", "The carriage seats " + SEATS_ROOMY + " people instead of " + SEATS_PLAIN + "."});
        PASSIVES.put("swift_wheels", new String[]{"Swift Wheels", "The carriage is " + Fmt.pct(SWIFT_SPEED - 1.0) + " percent faster."});
        PASSIVES.put("armoured_frame", new String[]{"Armoured Frame", "The carriage has " + Fmt.num(ARMOURED_HEALTH) + " health instead of " + Fmt.num(BASE_HEALTH) + "."});
        PASSIVES.put("queens_pull", new String[]{"Queen's Pull", "The carriage is " + Fmt.pct(QUEEN_SPEED - 1.0) + " percent faster, as if the Filolial Queen drew it."});
    }

    public static String name(String skillId) { return SKILLS.get(skillId)[0]; }

    public static String desc(String skillId) { return SKILLS.get(skillId)[1]; }
}
