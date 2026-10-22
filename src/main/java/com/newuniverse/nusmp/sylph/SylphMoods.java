package com.newuniverse.nusmp.sylph;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the server half of her moods (docs/magic/sylph.md, "Poses, faces and voices"): it turns what happens to the owner into a {@link SylphMood.Event}, asks the pure rules what she does
 * ({@link SylphMood#decide}), writes the POSE into the prop word (four bits, synced with the prop as before: no new packet) and lets her SPEAK ({@link SylphVoice}). The clients only read the pose
 * code and play that clip; the face is part of the clip. Everything is per owner ({@link SylphCasting.State#mood}), checked only for players that have a Sylph out ({@code SylphCasting.LIVE}),
 * bounded by the rules' cooldowns, and gated by the server config {@code magic.moods.poses} / {@code magic.moods.voices} (off = the old behaviour: the clip follows the clock and the cast, she says nothing).
 */
final class SylphMoods {
    private SylphMoods() {}

    /** Decides and applies one event for a linked owner. Never throws; does nothing when the poses and voices are both off. */
    static void fire(ServerPlayer p, SylphCasting.State s, SylphMood.Event e) {
        if (!SylphConfig.poses() && !SylphConfig.voices()) return;
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        int rnd = sl.getRandom().nextInt() & 0x7FFFFFFF;
        SylphMood.Decision d = SylphMood.decide(s.mood, e, now, rnd);
        if (e == SylphMood.Event.LOW_HEALTH && !d.any()) d = SylphMood.decide(s.mood, SylphMood.Event.HURT, now, rnd);     // already worried: an ordinary flinch
        apply(p, s, sl, d, rnd);
    }

    private static void apply(ServerPlayer p, SylphCasting.State s, ServerLevel sl, SylphMood.Decision d, int rnd) {
        MagicPropEntity small = SylphCasting.prop(sl, s.spirit), powered = SylphCasting.prop(sl, s.powered);
        if (SylphConfig.poses()) {
            if (d.pose() != 0 && small != null) small.setParam(SylphRules.withPose(small.param(), d.pose()));
            if (d.poweredPose() != 0 && powered != null) powered.setParam(SylphRules.withPose(powered.param(), d.poweredPose()));
        }
        if (d.voice() != null) {
            MagicPropEntity at = powered != null ? powered : small;
            double x = at != null ? at.getX() : p.getX(), y = at != null ? at.getY() : p.getY() + 1.6, z = at != null ? at.getZ() : p.getZ();
            SylphVoice.say(sl, x, y, z, d.voice(), s.mood, rnd >>> 3);
        }
    }

    /** The owner took damage (a server event; the owner has a Sylph out): a flinch, a startle for a heavy hit, worry when it leaves them low. */
    static void onHurt(ServerPlayer p, float damage) {
        SylphCasting.State s = SylphCasting.STATES.get(p.getUUID());
        if (s == null) return;
        fire(p, s, SylphMood.hurtEvent(damage, p.getHealth(), p.getMaxHealth()));
    }

    /** The owner killed a monster: a cheer, a victory for a boss-sized one. */
    static void onKill(ServerPlayer p, LivingEntity victim) {
        SylphCasting.State s = SylphCasting.STATES.get(p.getUUID());
        if (s == null) return;
        fire(p, s, SylphMood.killEvent(victim.getMaxHealth()));
    }

    /** Every few ticks for a linked owner: moving ends an idle pose and starts the idle clock over; standing still long enough gives her something to do. */
    static void idleTick(ServerPlayer p, SylphCasting.State s, ServerLevel sl, long now, MagicPropEntity small) {
        if (!SylphConfig.poses() && !SylphConfig.voices()) return;
        double dx = p.getX() - s.mood.lastX, dz = p.getZ() - s.mood.lastZ;
        s.mood.lastX = p.getX();
        s.mood.lastZ = p.getZ();
        if (SylphMood.moved(dx, dz)) {
            s.mood.active(now);
            if (s.mood.smallPose(now) != 0 && s.mood.priority <= SylphMood.spec(SylphMood.Event.IDLE).priority()) s.mood.poseUntil = now;      // she gets up when they move
            return;
        }
        if (now - s.mood.lastActivity < SylphMood.IDLE_TICKS) return;
        if (small == null || SylphRules.hidden(small.param()) || now - s.mood.lastIdle < SylphMood.IDLE_GAP) return;
        fire(p, s, SylphMood.Event.IDLE);
    }

    /** The clip bits of a prop when its mood is over: keeps the pose bits in step with the state (called by the prop's own tick). */
    static int syncPose(int param, SylphCasting.State s, long now, boolean powered) {
        int shown = SylphRules.pose(param);
        if (shown == 0) return param;
        int want = powered ? s.mood.poweredPose(now) : s.mood.smallPose(now);
        return want == shown ? param : SylphRules.withPose(param, want);
    }
}
