package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.anim.CastHold;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * The thin server glue of the held cast key: it keeps the {@link CastHold} state in the grimoire's skill tag (so no static map has to
 * be cleaned up on logout) and turns the state machine's decisions into the three messages the client understands
 * (VfxShape.MAGIC_CIRCLE_HOLD, MAGIC_CIRCLE_HOLD_END and, for a tap, the single MAGIC_CIRCLE the cast always had). It never decides
 * whether a spell is cast: {@link GrimoireBook} does that exactly as before.
 */
final class CastHoldFx {
    private CastHoldFx() {}

    private static final String LEVEL = "HoldLevel", SENT = "HoldSent", PRESS = "PressAt";

    private static CastHold.State read(CompoundTag tag) {
        return new CastHold.State(tag.getInt(LEVEL), tag.contains(SENT) ? tag.getLong(SENT) : CastHold.IDLE.sentAt());
    }

    private static void write(CompoundTag tag, CastHold.State s) {
        tag.putInt(LEVEL, s.level());
        tag.putLong(SENT, s.sentAt());
    }

    private static void send(ServerPlayer p, VfxShape shape, int color, int duration, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), shape, p, p.position().add(0, 0.08, 0), color, duration, power);
    }

    /** The key went down: forget the last hold and say whether the chant clip should play (a repeated press does not replay it). */
    static boolean pressed(ManasSkillInstance instance, long now) {
        CompoundTag tag = instance.getOrCreateTag();
        long last = tag.contains(PRESS) ? tag.getLong(PRESS) : CastHold.IDLE.sentAt();
        tag.putLong(PRESS, now);
        write(tag, CastHold.press());
        return CastHold.pressAccepted(last, now);
    }

    /** Every tick the key is held. {@code charge} is the held ticks up to the page's cast time, {@code allowed} whether the page could be cast now. */
    static void held(ManasSkillInstance instance, ServerPlayer p, int color, int charge, int castTicks, boolean allowed) {
        CompoundTag tag = instance.getOrCreateTag();
        CastHold.Step step = CastHold.held(read(tag), p.level().getGameTime(), charge, castTicks, allowed);
        write(tag, step.next());
        if (step.kind() == CastHold.Kind.START || step.kind() == CastHold.Kind.LEVEL || step.kind() == CastHold.Kind.RENEW) {
            send(p, VfxShape.MAGIC_CIRCLE_HOLD, color, CastHold.LIFE, CastHold.power(step.level()));
        } else if (step.kind() == CastHold.Kind.END) {
            send(p, VfxShape.MAGIC_CIRCLE_HOLD_END, color, CastHold.endDuration(0), CastHold.power(step.level()));
        }
    }

    /**
     * The key went up. A live hold circle holds through the wind-up and fades; a tap (no hold circle) gets the single release circle
     * of old when {@code tapAllowed} (the page can be cast and no earlier cast is winding up).
     */
    static void released(ManasSkillInstance instance, ServerPlayer p, int color, boolean tapAllowed, int windup, float chargePower) {
        CompoundTag tag = instance.getOrCreateTag();
        CastHold.Step step = CastHold.release(read(tag), p.level().getGameTime(), tapAllowed);
        write(tag, step.next());
        if (step.kind() == CastHold.Kind.END) {
            send(p, VfxShape.MAGIC_CIRCLE_HOLD_END, color, CastHold.endDuration(windup), CastHold.power(step.level()));
        } else if (step.kind() == CastHold.Kind.TAP) {
            send(p, VfxShape.MAGIC_CIRCLE, color, windup + 2, 0.48f + chargePower * 0.7f);
        }
    }
}
