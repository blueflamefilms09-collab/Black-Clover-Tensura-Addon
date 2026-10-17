package com.newuniverse.nusmp.client.zogratis;

/**
 * Which animation clip a Zogratis body plays, and at what second of it (pure: no game classes, unit tested). One instance remembers one
 * entity; the renderer fills a {@link Frame} from the entity every frame and calls {@link #update}. The priority is: dying, the Gates'
 * transformation, a cast (an ultimate's clip is paced to the wind-up so its slam lands on the release, and settles after it), a swing,
 * a flinch, walking or running (paced by the ground covered, so the feet do not slide), idle.
 */
public final class ZogratisPose {
    public static final String IDLE = "idle", WALK = "walk", RUN = "run", ATTACK = "attack", CAST = "cast", CAST_BIG = "cast_big",
            HURT = "hurt", TRANSFORM = "transform", DEATH = "death", SPAWN = "spawn",
            /** Lucius only: played once when the chest gem opens (a one-shot clip; a model without it simply skips it). */
            REVEAL = "chest_gem_reveal";

    /** The slam of a one-shot cast_big clip lies at this fraction of its length: the wind-up ends there, the rest is the settling. */
    public static final float CAST_BIG_RELEASE = 0.8f;
    /** A swing starts this many seconds into the attack clip: the arm is already up, so the blow lands in the first ticks. */
    public static final float ATTACK_LEAD = 0.2f;
    /** Horizontal blocks per tick: above RUN_ON it runs until it falls under RUN_OFF; under WALK_ON it stands. */
    public static final float WALK_ON = 0.02f, RUN_ON = 0.22f, RUN_OFF = 0.17f;
    /** A step of more blocks than this in one tick is a portal, a blink or a teleport, not a stride: it is not walked (no run clip over a warp). */
    public static final float TELEPORT = 1.6f;
    /** The clock of a new entity that took the id of an old one starts again when time runs this many ticks backwards. */
    private static final float REWIND = 2f;
    /** A cast that ended with at least this much of its wind-up seen was released (not cancelled): the settling plays. */
    private static final float RELEASED_FROM = 0.8f;

    /** The lengths (seconds) and looping of the clips of the model that is drawn. */
    public interface Clips {
        /** Length in seconds of a clip; 0 or less when the model has no such clip. */
        float length(String clip);
        boolean loops(String clip);
    }

    /** Everything one frame tells the clock. */
    public static final class Frame {
        /** Client ticks plus the partial tick: grows steadily. */
        public float age;
        /** Whole client tick count of the entity (the speed is sampled once per tick). */
        public int tick;
        public boolean dead;
        /** Ticks since the death began, and how many ticks the death lasts before the body is gone. */
        public float deathTicks, deathSpan = 20f;
        public boolean transforming;
        /** 0..1 of the part of the transformation this model plays. */
        public float transformProgress;
        /** 0 none, 1 a cast, 2 an ultimate's cast. */
        public int cast;
        /** 0..1 of the wind-up. */
        public float castProgress;
        public boolean swinging;
        /** True while the chest gem is out (Lucius): its rising edge starts the {@link #REVEAL} clip. */
        public boolean gemOut;
        /** Ticks since the last hit landed, negative when there is none lately. */
        public float hurtTicks = -1f;
        /** Horizontal blocks the entity moved in its current tick (the clock adds them up into the ground covered). */
        public float rawSpeed;
        /** Blocks of ground that one full cycle of the walk and of the run clip covers. */
        public float walkCycle = 1.6f, runCycle = 2.6f;
    }

    // ---- what the last update decided
    public String clip = IDLE;
    public float seconds;

    // ---- memory
    private float lastAge = Float.NaN;
    private int lastTick = Integer.MIN_VALUE;
    private float speed, prevRaw, travelBase, travel;
    private boolean running, wasSwinging, wasGemOut;
    private float attackStart = -1e9f, castStart = -1e9f, releaseAge = -1e9f, revealStart = -1e9f, lastCastProgress;
    private int lastCast;
    private float scale = Float.NaN, lastScaleAge, yaw = Float.NaN, lastYawAge;

    /** Reads one frame: sets {@link #clip} and {@link #seconds}. */
    public void update(Frame f, Clips clips) {
        if (!Float.isNaN(lastAge) && f.age < lastAge - REWIND) reset();   // a new body with the id of a forgotten one: a clean clock
        float raw = f.rawSpeed >= 0f && f.rawSpeed <= TELEPORT ? f.rawSpeed : 0f;     // a warp (or a NaN) covers no ground
        if (f.tick != lastTick) {
            if (lastTick != Integer.MIN_VALUE) travelBase += prevRaw;     // the tick that just ended is part of the ground covered
            lastTick = f.tick;
            speed += (raw - speed) * 0.4f;
            if (running) { if (speed < RUN_OFF) running = false; }
            else if (speed > RUN_ON) running = true;
        }
        prevRaw = raw;
        travel = travelBase + raw * clamp01(f.age - f.tick);
        if (f.swinging && !wasSwinging) attackStart = f.age;
        wasSwinging = f.swinging;
        if (f.gemOut && !wasGemOut) revealStart = f.age;
        wasGemOut = f.gemOut;
        if (f.cast > 0) {
            if (lastCast == 0) castStart = f.age;
            lastCastProgress = f.castProgress;
        } else if (lastCast == 2 && lastCastProgress >= RELEASED_FROM) {
            releaseAge = f.age;
        }
        lastCast = f.cast;
        lastAge = f.age;
        choose(f, clips);
    }

    /** Back to the state of a clock that has never seen a frame. */
    private void reset() {
        clip = IDLE;
        seconds = 0f;
        lastAge = Float.NaN;
        lastTick = Integer.MIN_VALUE;
        speed = 0f;
        prevRaw = 0f;
        travelBase = 0f;
        travel = 0f;
        running = false;
        wasSwinging = false;
        wasGemOut = false;
        revealStart = -1e9f;
        attackStart = -1e9f;
        castStart = -1e9f;
        releaseAge = -1e9f;
        lastCastProgress = 0f;
        lastCast = 0;
        scale = Float.NaN;
        lastScaleAge = 0f;
        yaw = Float.NaN;
        lastYawAge = 0f;
    }

    private void choose(Frame f, Clips c) {
        if (f.dead) {
            set(DEATH, clamp01(f.deathTicks / Math.max(1f, f.deathSpan)) * len(c, DEATH));
        } else if (f.transforming) {
            set(TRANSFORM, clamp01(f.transformProgress) * len(c, TRANSFORM));
        } else if (revealing(f.age, c)) {
            set(REVEAL, (f.age - revealStart) / 20f);
        } else if (f.cast == 2 && c.length(CAST_BIG) > 0f) {
            float l = len(c, CAST_BIG);
            set(CAST_BIG, c.loops(CAST_BIG) ? (f.age - castStart) / 20f : clamp01(f.castProgress) * CAST_BIG_RELEASE * l);
        } else if (f.cast > 0) {
            set(CAST, Math.max(0f, f.age - castStart) / 20f);
        } else if (settling(f.age, c)) {
            set(CAST_BIG, CAST_BIG_RELEASE * len(c, CAST_BIG) + (f.age - releaseAge) / 20f);
        } else if (attacking(f.age, c)) {
            set(ATTACK, ATTACK_LEAD + (f.age - attackStart) / 20f);
        } else if (f.hurtTicks >= 0f && f.hurtTicks / 20f < len(c, HURT)) {
            set(HURT, f.hurtTicks / 20f);
        } else if (speed > WALK_ON) {
            boolean run = running && len(c, RUN) > 0f;
            String name = run ? RUN : WALK;
            float cycle = Math.max(0.1f, run ? f.runCycle : f.walkCycle);
            float phase = travel / cycle;
            set(name, (phase - (float) Math.floor(phase)) * len(c, name));
        } else {
            set(IDLE, f.age / 20f);
        }
    }

    private boolean revealing(float age, Clips c) {
        float l = len(c, REVEAL);
        return l > 0f && age >= revealStart && (age - revealStart) / 20f < l;
    }

    private boolean attacking(float age, Clips c) {
        float l = len(c, ATTACK);
        return l > 0f && age >= attackStart && ATTACK_LEAD + (age - attackStart) / 20f < l;
    }

    private boolean settling(float age, Clips c) {
        float l = len(c, CAST_BIG);
        return l > 0f && !c.loops(CAST_BIG) && age >= releaseAge && CAST_BIG_RELEASE * l + (age - releaseAge) / 20f < l;
    }

    private void set(String name, float s) {
        clip = name;
        seconds = Math.max(0f, s);
    }

    private static float len(Clips c, String name) { return Math.max(0f, c.length(name)); }

    private static float clamp01(float v) { return v < 0f ? 0f : v > 1f ? 1f : v; }

    /** The smoothed horizontal speed in blocks per tick. */
    public float speed() { return speed; }

    /** The ground covered so far, in blocks (what the stride is paced by). */
    public float travel() { return travel; }

    /** True while the clock is in its running state. */
    public boolean running() { return running; }

    /** Eases a size toward 'target' (about 6 per second): the first call jumps to it. Uses the age of the last {@link #update}. */
    public float smoothScale(float target, float age) {
        if (Float.isNaN(scale)) { scale = target; lastScaleAge = age; return scale; }
        float dt = Math.max(0f, age - lastScaleAge);
        lastScaleAge = age;
        scale += (target - scale) * (1f - (float) Math.exp(-6f * dt / 20f));
        return scale;
    }

    /** Eases a facing in degrees toward 'target' the short way round (about 12 per second): the first call jumps to it. */
    public float smoothYaw(float target, float age) {
        if (Float.isNaN(yaw)) { yaw = target; lastYawAge = age; return yaw; }
        float dt = Math.max(0f, age - lastYawAge);
        lastYawAge = age;
        yaw += wrapDegrees(target - yaw) * (1f - (float) Math.exp(-12f * dt / 20f));
        return yaw;
    }

    /** An angle in degrees brought into [-180, 180). */
    public static float wrapDegrees(float d) {
        float r = d % 360f;
        if (r >= 180f) r -= 360f;
        if (r < -180f) r += 360f;
        return r;
    }

    /** The age the clock saw last (NaN before the first frame). */
    public float lastAge() { return lastAge; }
}
