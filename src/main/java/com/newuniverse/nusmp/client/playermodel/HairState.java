package com.newuniverse.nusmp.client.playermodel;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: the hair of one entity remembers how it was moving, so it can lag, stream, swing and whip: the springs of every chain of the style it wears, the speed it had on the previous tick (the
 * acceleration is the difference), whether it stood on the ground (a landing is a kick), and the time of the last update (the springs are integrated by real elapsed time, so a frame rate of 20 or
 * 200 looks the same). {@link #update} turns the entity's motion into the numbers of {@link HairPhysics}, hands them to the {@link HairSpring} of every chain and leaves the angles to read.
 * One state per entity, in a weak map: an entity that goes away takes its hair with it; a style change rebuilds the springs.
 */
public final class HairState {
    private static final Map<LivingEntity, HairState> STATES = new WeakHashMap<>();

    /** The state of an entity (made when first asked for). */
    public static HairState of(LivingEntity e) { return STATES.computeIfAbsent(e, k -> new HairState()); }

    private String style = "";
    private final Map<String, HairSpring> springs = new HashMap<>();
    private float lastAge = Float.NaN;
    private int lastTick = Integer.MIN_VALUE;
    private double lastVx, lastVy, lastVz, accX, accZ;
    private boolean wasOnGround = true;
    private final float[] out = new float[2];

    /** The spring of a chain (made on first use). */
    public HairSpring spring(HairStyleData.Chain c) {
        return springs.computeIfAbsent(c.name(), k -> new HairSpring(Math.min(4, c.segments().size()), paramsOf(c.params())));
    }

    static HairSpring.Params paramsOf(String word) {
        return switch (word == null ? "default" : word) {
            case "stiff" -> HairSpring.Params.STIFF;
            case "heavy" -> HairSpring.Params.HEAVY;
            default -> HairSpring.Params.DEFAULT;
        };
    }

    /**
     * Moves the hair of {@code e} on to this frame.
     *
     * @param styleData the style worn now (a change of style resets the springs)
     * @param age       {@code tickCount + partial tick}
     * @param headPitch the head's pitch in radians (positive: looking down)
     * @param bodyPitch the lean of the body in radians (positive: forward)
     */
    public void update(LivingEntity e, HairStyleData.Style styleData, float age, float headPitch, float headRoll, float bodyPitch) {
        if (!styleData.id().equals(style)) {
            style = styleData.id();
            springs.clear();
            lastAge = Float.NaN;
        }
        float dt = Float.isNaN(lastAge) ? 0f : Mth.clamp(age - lastAge, 0f, 4f);
        lastAge = age;
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        boolean onGround = e.onGround();
        if (e.tickCount != lastTick) {
            if (lastTick != Integer.MIN_VALUE) {
                accX = vx - lastVx;
                accZ = vz - lastVz;
                float kick = 0f;
                if (onGround && !wasOnGround) kick = HairPhysics.landingKick((float) lastVy);
                else if (!onGround && wasOnGround && vy > 0.3) kick = HairPhysics.jumpKick((float) vy);
                if (kick > 0f) for (HairSpring s : springs.values()) s.kick(kick, 0.4f);
            }
            lastTick = e.tickCount;
            lastVx = vx;
            lastVy = vy;
            lastVz = vz;
            wasOnGround = onGround;
        }
        float yaw = e.yBodyRot * Mth.DEG_TO_RAD;
        float fx = -Mth.sin(yaw), fz = Mth.cos(yaw), lx = Mth.cos(yaw), lz = Mth.sin(yaw);
        float speedF = (float) (vx * fx + vz * fz);
        float accF = (float) (accX * fx + accZ * fz), accL = (float) (accX * lx + accZ * lz);
        float turn = -Mth.wrapDegrees(e.yBodyRot - e.yBodyRotO) * Mth.DEG_TO_RAD;       // positive = turning to the entity's left
        HairPhysics.Input in = new HairPhysics.Input(headPitch, headRoll, bodyPitch, speedF, accF, accL, turn, age);
        for (HairStyleData.Chain c : styleData.chains()) {
            HairSpring sp = spring(c);
            HairPhysics.Tuning head = HairPhysics.Tuning.named(c.tuning(), c.phase());
            HairPhysics.Tuning body = head.asBody();
            float[] tp = new float[sp.segments()], tr = new float[sp.segments()];
            HairPhysics.target(in, head, out);
            float hp = out[0], hr = out[1];
            HairPhysics.target(in, body, out);
            float bp = out[0], br = out[1];
            for (int i = 0; i < sp.segments(); i++) {
                boolean onBody = c.segments().get(i).part() == HairStyleData.Part.BODY;
                tp[i] = onBody ? bp : hp;
                tr[i] = onBody ? br : hr;
            }
            sp.step(dt, tp, tr);
        }
    }

    /** A state for a drawing that has no entity (the creation screen's preview): the hair sways on a clock and nothing else moves it. */
    public static void idle(HairStyleData.Style styleData, HairState into, float age) {
        if (!styleData.id().equals(into.style)) {
            into.style = styleData.id();
            into.springs.clear();
            into.lastAge = Float.NaN;
        }
        float dt = Float.isNaN(into.lastAge) ? 0f : Mth.clamp(age - into.lastAge, 0f, 4f);
        into.lastAge = age;
        HairPhysics.Input in = new HairPhysics.Input(0f, 0f, 0f, 0f, 0f, 0f, 0f, age);
        for (HairStyleData.Chain c : styleData.chains()) {
            HairSpring sp = into.spring(c);
            float[] o = new float[2];
            HairPhysics.target(in, HairPhysics.Tuning.named(c.tuning(), c.phase()), o);
            sp.step(dt, o[0], o[1]);
        }
    }

    /** A free-standing state (a preview keeps its own). */
    public static HairState create() { return new HairState(); }
}
