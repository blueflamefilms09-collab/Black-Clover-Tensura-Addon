package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.anim.CastHold;
import com.newuniverse.nusmp.anim.CastHoldView;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxAnim;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The grimoire circle of a HELD cast key: one continuous "active casting" circle instead of a new circle every few ticks.
 *
 * <p>The server ({@link CastHold}, glued in {@code book.CastHoldFx}) sends MAGIC_CIRCLE_HOLD when the charge starts, again when the
 * charge reaches the next step and as a keep-alive, and MAGIC_CIRCLE_HOLD_END on release. Every message is only a sign of life: the
 * state of the circle (when it opened, how big it is, whether it is fading) lives here, per followed entity, on the game clock, so a
 * new message never restarts the pop-open, the spin or the fade. The newest instance of an entity is the one that draws; older ones
 * stay silent until they expire. A circle that hears nothing for its duration (a lost release, a death, leaving the range) simply
 * fades over its last {@value CastHold#END_TAIL} ticks.
 *
 * <p>The circle itself is the standard grimoire circle ({@link MagicCircleLayer#drawCircle}), so a held circle looks like every other.
 */
public final class CastHoldLayer extends AbstractVfxLayer {
    private static final Set<VfxShape> SHAPES = EnumSet.of(VfxShape.MAGIC_CIRCLE_HOLD, VfxShape.MAGIC_CIRCLE_HOLD_END);
    private static final float OPEN_TICKS = 24f;      // MagicCircleLayer's circleOpen() opens within a quarter of this

    /** One entity's hold circle: its look on the game clock ({@link CastHoldView}) and the newest instance, which is the one that draws. */
    private static final class Hold {
        VfxInstance owner;
        CastHoldView view;
        int color;
    }

    private static final Map<Integer, Hold> HOLDS = new HashMap<>();

    @Override public Set<VfxShape> shapes() { return SHAPES; }
    @Override public int defaultDuration(VfxShape shape) { return shape == VfxShape.MAGIC_CIRCLE_HOLD ? CastHold.LIFE : CastHold.END_TAIL + 6; }
    @Override public int defaultColor(VfxShape shape) { return 0xFFFFD86B; }

    @Override
    public void onSpawn(VfxInstance inst) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        for (Iterator<Hold> it = HOLDS.values().iterator(); it.hasNext(); ) {
            Hold h = it.next();
            if (h.owner == null || h.owner.done()) it.remove();
        }
        int key = inst.payload.followEntity();
        Hold h = HOLDS.get(key);
        if (inst.shape == VfxShape.MAGIC_CIRCLE_HOLD_END) {
            if (h == null) return;                                   // nothing was showing: nothing to fade
            h.owner = inst;
            h.view.end(now, inst.duration);
            return;
        }
        if (h == null) {
            h = new Hold();
            h.view = CastHoldView.open(now, inst.power);
            HOLDS.put(key, h);
        } else {
            h.view.renew(now, inst.power);                           // a sign of life: never restarts the pop-open, the spin or the fade
        }
        h.owner = inst;
        h.color = inst.color;
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        Hold h = HOLDS.get(inst.payload.followEntity());
        if (h == null || h.owner != inst) return;
        float t = ctx.level.getGameTime() + ctx.partialTick;
        float a = h.view.alpha(t, inst.duration - inst.ageTicks(ctx.partialTick));
        if (a <= 0.003f) return;
        float age = h.view.age(t);
        float p = h.view.power(t);
        float charge = Mth.clamp((p - 0.45f) / 0.7f, 0f, 1f);
        if (charge > 0.99f) a *= 0.92f + 0.08f * Mth.sin(t * 0.45f);          // a full charge breathes
        float scale = VfxAnim.circleOpen(Math.min(1f, age / OPEN_TICKS));
        int col = VfxVertexBuffer.whiten(h.color, 0.3f * charge);
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vec3 dir = to.subtract(from);
        Vector3f facing = dir.lengthSqr() < 0.01 ? new Vector3f(0, 1, 0) : new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        VfxPose pose = VfxPose.facing(ctx.rel(from), facing);
        MagicCircleLayer.drawCircle(ctx, buf, pose, age, a, scale, 1.6f * p, col, 1f);
    }
}
