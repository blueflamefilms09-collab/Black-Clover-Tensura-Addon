package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.TreeKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Mucus Magic: the server behaviours of PropKind.MUCUS_1 (variant = low byte of the param; painter client.prop.MucusPropPainter):
 * <ul>
 *   <li>variant 0, the Mucus Construct (Mucus Magic: Mucus Construct): a mound of mucus of 2.4 blocks that rises in its first 20 ticks and follows the caster slowly; every 5 ticks it swallows every
 *       projectile within 3.5 blocks, every 20 ticks everything hostile within 3.2 blocks is hurt (3), slowed hard and drained of 1 percent of its magicules (param phase 1 = absorbing while
 *       something is inside). It can be hit (60 health).</li>
 *   <li>variant 1, the Mucus Tendril: a pseudopod that rises where the line of Mucus Tendril ends and lashes once (lives 20 ticks; the spell did the damage); purely visual.</li>
 * </ul>
 */
public final class MucusProps {
    private MucusProps() {}

    public static void init() {
        MagicProps.register(PropKind.MUCUS_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                float s = e.scale();
                if ((e.param() & TreeProps.VARIANT) == 0) { e.setSize(2.0f * s, 2.4f * s); e.setHittable(60); } else e.setSize(1.0f * s, 2.4f * s);
            }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { if ((e.param() & TreeProps.VARIANT) == 0) construct(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & TreeProps.VARIANT) == 0) TreeProps.burst(e, sl, VfxShape.MUCUS_FX3, 1.0f);
                TreeKit.unbind(e);
            }
        });
    }

    private static void construct(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        int t = e.life();
        TreeProps.keepNear(e, p);
        if (t < 20) return;
        TreeProps.walk(e, sl, p.position().add(Math.cos(t * 0.015) * 3.0, 0, Math.sin(t * 0.015) * 3.0), 0.08, 1.5);
        Vec3 c = e.position().add(0, 1.0, 0);
        if (t % 5 == 0) SpellRuntime.dissolveBolts(sl, c, 3.5 * e.scale(), p);
        if (t % 20 != 0) { if (t % 20 == 10) TreeProps.phase(e, 0); return; }
        boolean any = false;
        for (LivingEntity v : TreeKit.foes(p, c, 3.2 * e.scale())) {
            any = true;
            TreeKit.hurt(e, v, 3f);
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            EnergyBridge.drain(v, p, 0.01);
        }
        TreeProps.phase(e, any ? 1 : 0);
    }
}
