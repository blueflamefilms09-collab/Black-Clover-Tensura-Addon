package com.newuniverse.nusmp.book;

/**
 * The spin of the Valkyrie drill lance, pure: it turns about its own axis, an idle {@value #IDLE_DEG_PER_SEC} degrees a second, ramping up to {@value #MAX_DEG_PER_SEC} while it is charged or swung, and back to idle in
 * {@value #RAMP_DOWN_TICKS} ticks. Two forms: the armour layer ({@code client/mode/ModeArmorValkyrieV2}) integrates the speed per wearer ({@link #step}, a few floats of state in a small table); the weapon item
 * ({@code client/WeaponRenderer}) has no state and no packet: its angle is a pure function of the client game time and of the holder's charge ({@link #itemAngle}).
 */
public final class ModeArmorValkyrieSpin {
    private ModeArmorValkyrieSpin() {}

    public static final float IDLE_DEG_PER_SEC = 60f, MAX_DEG_PER_SEC = 720f;
    public static final float RAMP_UP_TICKS = 6f, RAMP_DOWN_TICKS = 10f;

    /** The speed (degrees per tick) after {@code dt} ticks: it moves towards the maximum while {@code active}, towards the idle speed otherwise, at the rates of the ramps. */
    public static float step(float speedPerTick, boolean active, float dt) {
        float idle = IDLE_DEG_PER_SEC / 20f, max = MAX_DEG_PER_SEC / 20f;
        if (active) return Math.min(max, speedPerTick + (max - idle) / RAMP_UP_TICKS * dt);
        return Math.max(idle, speedPerTick - (max - idle) / RAMP_DOWN_TICKS * dt);
    }

    public static float idleSpeedPerTick() { return IDLE_DEG_PER_SEC / 20f; }

    /**
     * The stateless angle of the weapon item (degrees): the idle turn of {@code timeTicks} plus what the charge adds, where {@code chargeTicks} is how long the holder has been charging or swinging
     * (0 when not); the speed ramps linearly from idle to the maximum over {@link #RAMP_UP_TICKS} and stays there, so the extra angle is the integral of that ramp. Continuous in both arguments.
     */
    public static float itemAngle(float timeTicks, float chargeTicks) {
        float idle = IDLE_DEG_PER_SEC / 20f, max = MAX_DEG_PER_SEC / 20f;
        float c = Math.max(0f, chargeTicks), r = RAMP_UP_TICKS;
        float extra = c < r ? (max - idle) * c * c / (2f * r) : (max - idle) * (r / 2f + (c - r));
        return idle * timeTicks + extra;
    }
}
