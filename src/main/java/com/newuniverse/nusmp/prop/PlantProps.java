package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.TreeKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Plant Magic: the server behaviours of PropKind.PLANT_1 (variant = low byte of the param; painter client.prop.PlantPropPainter):
 * <ul>
 *   <li>variant 0, the Flower Sprite (Plant Magic: Flower Sprite): a familiar that circles the caster at 1.8 blocks and 1.7 high for its 30 s. Every 40 ticks it mends the most hurt friend
 *       within 12 blocks of the caster that is below 85 percent health (5 health, healing decay applies; param phase 2 = healing), otherwise it spits a petal at the nearest foe within 12 blocks
 *       (4 damage, phase 1 = strike, a petal line effect). It can be hit (20 health).</li>
 *   <li>variant 1, the Healing Bloom (Plant Magic: Healing Bloom): a ground flower; every 20 ticks everyone friendly within 5 blocks (times its scale) mends 4 percent of the maximum health, foes
 *       there are slowed. Phase 2 while it pulses.</li>
 * </ul>
 */
public final class PlantProps {
    private PlantProps() {}

    public static void init() {
        MagicProps.register(PropKind.PLANT_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                float s = e.scale();
                if ((e.param() & TreeProps.VARIANT) == 0) { e.setSize(0.8f * s, 1.2f * s); e.setHittable(20); } else e.setSize(1.4f * s, 2.0f * s);
            }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { if ((e.param() & TreeProps.VARIANT) == 0) sprite(e, sl); else bloom(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                TreeProps.burst(e, sl, VfxShape.PLANT_FX3, (e.param() & TreeProps.VARIANT) == 0 ? 0.6f : 1.0f);
                TreeKit.unbind(e);
            }
        });
    }

    private static void sprite(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        CompoundTag d = e.getPersistentData();
        int t = e.life();
        double a = t * 0.07;
        Vec3 home = p.position().add(Math.cos(a) * 1.8, 1.7 + 0.2 * Math.sin(t * 0.11), Math.sin(a) * 1.8);
        Vec3 pos = e.position();
        if (pos.distanceToSqr(home) > 30 * 30) pos = home;
        Vec3 np = pos.add(home.subtract(pos).scale(0.25));
        e.setPos(np.x, np.y, np.z);
        e.setDeltaMovement(Vec3.ZERO);
        e.setYRot((float) Math.toDegrees(-a) - 90f);
        int until = d.getInt("anim");
        if (t < until) { TreeProps.phase(e, d.getInt("kind"), t - (until - 12)); return; }
        TreeProps.phase(e, 0);
        if (t % 40 != 0) return;
        LivingEntity hurt = null;
        float lowest = 0.85f;
        for (LivingEntity f : TreeKit.friends(p, p.position(), 12)) {
            float frac = f.getHealth() / f.getMaxHealth();
            if (frac < lowest) { lowest = frac; hurt = f; }
        }
        if (hurt != null) {
            BalanceLaw.heal(hurt, 5f);
            VfxSpawn.send(sl, VfxShape.PLANT_FX1, e.position(), hurt.getBoundingBox().getCenter(), TreeProps.colorOf(e), 14, 0.5f);
            VfxSpawn.send(sl, VfxShape.PLANT_FX3, hurt.position(), hurt.position().add(0, 1, 0), TreeProps.colorOf(e), 22, 0.4f);
            TreeProps.phase(e, 2, 0);
            d.putInt("kind", 2);
            d.putInt("anim", t + 12);
            return;
        }
        LivingEntity foe = TreeKit.nearest(p, p.position(), 12);
        if (foe != null) {
            TreeKit.hurt(e, foe, 4f);
            VfxSpawn.send(sl, VfxShape.PLANT_FX1, e.position(), foe.getBoundingBox().getCenter(), TreeProps.colorOf(e), 12, 0.5f);
            TreeProps.phase(e, 1, 0);
            d.putInt("kind", 1);
            d.putInt("anim", t + 12);
        }
    }

    private static void bloom(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        if (e.life() % 20 != 0) { if (e.life() % 20 == 6) TreeProps.phase(e, 0); return; }
        double r = 5 * e.scale();
        Vec3 c = e.position();
        for (LivingEntity f : TreeKit.friends(p, c, r)) BalanceLaw.heal(f, f.getMaxHealth() * 0.04f);
        for (LivingEntity v : TreeKit.foes(p, c, r)) v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        VfxSpawn.send(sl, VfxShape.PLANT_FX3, c, c.add(0, 1, 0), TreeProps.colorOf(e), 20, 0.5f);
        TreeProps.phase(e, 2);
    }
}
