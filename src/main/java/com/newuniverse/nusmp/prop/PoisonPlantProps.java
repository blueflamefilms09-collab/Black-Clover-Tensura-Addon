package com.newuniverse.nusmp.prop;

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
 * 1.93.0 Poison Plant Magic: the server behaviours of PropKind.POISON_PLANT_1 (variant = low byte of the param; painter client.prop.PoisonPlantPropPainter):
 * <ul>
 *   <li>variant 0, the Miasma Bloom: a bulb that breathes poison gas; every 20 ticks every foe within 4.5 blocks (times its scale) takes 2.5 damage and poison, every 60 ticks also blindness,
 *       and a gas cloud puffs out (param phase 1 while it breathes: the petals open).</li>
 *   <li>variant 1, the Trap Maw: a carnivorous trap; armed, it snaps on the first foe within 2.4 blocks (the jaws close 6 ticks after it starts: 11 damage, strong poison, rooted 50 ticks) and
 *       re-arms after 50 ticks. Phase 1 while it snaps.</li>
 * </ul>
 */
public final class PoisonPlantProps {
    private PoisonPlantProps() {}

    public static void init() {
        MagicProps.register(PropKind.POISON_PLANT_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                float s = e.scale();
                if ((e.param() & TreeProps.VARIANT) == 0) { e.setSize(1.2f * s, 1.6f * s); e.setHittable(20); } else { e.setSize(1.4f * s, 1.8f * s); e.setHittable(25); }
            }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { if ((e.param() & TreeProps.VARIANT) == 0) bloom(e, sl); else maw(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                TreeProps.burst(e, sl, VfxShape.POISON_PLANT_FX3, 0.9f);
                TreeKit.unbind(e);
            }
        });
    }

    private static void bloom(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        int t = e.life();
        if (t % 20 == 12) TreeProps.phase(e, 0);
        if (t % 20 != 0) return;
        Vec3 c = e.position().add(0, 0.8, 0);
        double r = 4.5 * e.scale();
        for (LivingEntity v : TreeKit.foes(p, c, r)) {
            TreeKit.hurt(e, v, 2.5f);
            TreeKit.wound(v, 60, 1, 0.003);
            if (t % 60 == 0) v.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
        }
        TreeProps.phase(e, 1);
        if (t % 40 == 0) VfxSpawn.send(sl, VfxShape.POISON_PLANT_FX3, c, c.add(0, 1, 0), TreeProps.colorOf(e), 24, 0.7f);
    }

    private static void maw(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        CompoundTag d = e.getPersistentData();
        int t = e.life();
        int snap = d.getInt("snap");
        if (snap > 0) {
            int k = t - snap;
            if (k == 6) {
                for (LivingEntity v : TreeKit.foes(p, e.position().add(0, 0.8, 0), 2.4 * e.scale())) {
                    TreeKit.hurt(e, v, 11f);
                    TreeKit.wound(v, 120, 2, 0.005);
                    TreeKit.root(v, 50, 4);
                }
                TreeProps.burst(e, sl, VfxShape.POISON_PLANT_FX3, 0.6f);
            }
            if (k >= 16) { d.putInt("snap", 0); d.putInt("cd", t + 50); TreeProps.phase(e, 0); } else TreeProps.phase(e, 1, k);
            return;
        }
        if (t >= d.getInt("cd") && t % 4 == 0 && !TreeKit.foes(p, e.position().add(0, 0.8, 0), 2.4 * e.scale()).isEmpty()) { d.putInt("snap", t); TreeProps.phase(e, 1); }
    }
}
