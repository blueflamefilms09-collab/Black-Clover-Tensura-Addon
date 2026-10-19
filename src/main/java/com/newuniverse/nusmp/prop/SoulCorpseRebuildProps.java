package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SongRebuildKit;
import com.newuniverse.nusmp.book.SoulCorpseRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Soul Corpse Magic: the server behaviours of the two props (the casts are in book.SoulCorpseBook; the models are tools/gen_soulcorpse_rebuild_geo.py, drawn by
 * client.prop.SoulCorpseRebuildPropPainter; the numbers are book.SoulCorpseRules).
 * SOUL_CORPSE_1, param low byte 0 = a marionette: a stitched puppet that walks (0.2 blocks per tick, it jumps a one-block step) to the nearest foe within 14 blocks of its caster
 * and strikes it every 20 ticks in reach (raw 6 through the book and Weakness), or follows its caster when nothing is near; bit 8 = striking (the attack clip), bit 10 = collapsing
 * (the last 20 ticks of its life). 1 = a decoy: it stands where it was put and bursts when its life runs out (raw 8 on foes within 3 blocks and a shove). 2 = a thread nail, the
 * flying model of the Marionette Thread bolt (the hitbox is the SpellRuntime bolt SongRebuildKit.projectile fires with it).
 * SOUL_CORPSE_2, a grasping hand: it rises out of the ground where it was put and closes on everything hostile within 1.8 blocks every 24 ticks (raw 3.5, Slowness III for 2 s);
 * bit 8 = closing (the grasp clip). Every end is a burst of bone and violet light; all of them end when the caster dies or logs out.
 */
public final class SoulCorpseRebuildProps {
    private SoulCorpseRebuildProps() {}

    public static final int STRIKING = 0x100, COLLAPSING = 0x400, CLOSING = 0x100, DECOY = 1, NAIL = 2;
    private static final int VIOLET = 0xFF6C7899;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SOUL_CORPSE_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & 0xFF) == NAIL) SongRebuildKit.FLY.init(e, sl);
                else { e.setSize(0.7f * e.scale(), 2.0f * e.scale()); e.setGravity(true); }
            }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) {
                int kind = e.param() & 0xFF;
                if (kind == NAIL) SongRebuildKit.FLY.tick(e, sl);
                else puppet(e, sl, kind == DECOY);
            }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                int kind = e.param() & 0xFF;
                if (kind == DECOY) burst(e, sl);
                if (kind != NAIL) VfxSpawn.send(sl, VfxShape.SOUL_CORPSE_FX3, e.position(), e.position().add(0, 1, 0), VIOLET, 18, 0.6f);
                SongRebuildKit.unbind(e);
            }
        });
        MagicProps.register(PropKind.SOUL_CORPSE_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.9f * e.scale(), 2.4f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { hand(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                VfxSpawn.send(sl, VfxShape.SOUL_CORPSE_FX3, e.position(), e.position().add(0, 1, 0), VIOLET, 16, 0.5f);
                SongRebuildKit.unbind(e);
            }
        });
    }

    // ================================================================ the marionette and the decoy
    private static void puppet(MagicPropEntity e, ServerLevel sl, boolean decoy) {
        if (!(e.owner() instanceof ServerPlayer p) || !SongRebuildKit.bound(e)) { e.expire(); return; }
        int t = e.life(), left = e.maxLife() - t;
        boolean collapse = left <= 20;
        if (collapse && (e.param() & COLLAPSING) == 0) e.setParam(e.param() | COLLAPSING);
        CompoundTag d = e.getPersistentData();
        double vy = d.getDouble("vy");
        double dx = 0, dz = 0;
        boolean striking = false;
        if (!decoy && !collapse) {
            LivingEntity foe = SongRebuildKit.nearestFoe(p, p.position(), 14);
            Vec3 goal = foe != null ? foe.position() : p.position();
            double stop = foe != null ? SoulCorpseRules.ATTACK_RANGE - 0.4 : 3.0;
            if (SoulCorpseRules.beyondLeash(e.distanceTo(p))) { goal = p.position(); stop = 2.0; }
            double[] st = SoulCorpseRules.step(e.getX(), e.getZ(), goal.x, goal.z, SoulCorpseRules.PUPPET_SPEED, stop);
            dx = st[0]; dz = st[1];
            if (dx != 0 || dz != 0) e.setYRot(SongRebuildKit.yawOf(dx, dz));
            if (foe != null) {
                double dist = e.distanceTo(foe);
                if (SoulCorpseRules.strikes(t, dist)) {
                    SongRebuildKit.Ctx ctx = SongRebuildKit.ctx(e);
                    if (ctx != null) {
                        ctx.book().hurt(ctx.inst(), p, foe, ctx.mode(), SongRebuildKit.safe(foe, 6f));
                        foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, true));
                    }
                    striking = true;
                    e.setYRot(SongRebuildKit.yawOf(foe.getX() - e.getX(), foe.getZ() - e.getZ()));
                }
            }
        }
        boolean showStrike = striking || ((e.param() & STRIKING) != 0 && t % SoulCorpseRules.ATTACK_EVERY < 8);
        e.setParam(showStrike ? e.param() | STRIKING : e.param() & ~STRIKING);
        if (e.onGround()) vy = e.horizontalCollision && (dx != 0 || dz != 0) ? 0.42 : 0.0; else vy = Math.max(-0.9, vy - 0.08);
        e.move(MoverType.SELF, new Vec3(dx, vy < 0 || vy > 0 ? vy : -0.1, dz));
        d.putDouble("vy", vy);
    }

    private static void burst(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p)) return;
        SongRebuildKit.Ctx ctx = SongRebuildKit.ctx(e);
        if (ctx == null) return;
        for (LivingEntity t : SongRebuildKit.foes(p, e.position(), 3.0)) {
            ctx.book().hurt(ctx.inst(), p, t, ctx.mode(), SongRebuildKit.safe(t, 8f));
            Vec3 away = t.position().subtract(e.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
            t.knockback(0.8, -away.x, -away.z);
        }
        VfxSpawn.send(sl, VfxShape.SOUL_CORPSE_FX3, e.position(), e.position().add(0, 1, 0), VIOLET, 22, 1.0f);
    }

    // ================================================================ the grasping hand
    private static void hand(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !SongRebuildKit.bound(e)) { e.expire(); return; }
        int t = e.life();
        e.setDeltaMovement(Vec3.ZERO);
        boolean closing = SoulCorpseRules.handClosing(t) && t >= SoulCorpseRules.HAND_EVERY;
        e.setParam(closing ? e.param() | CLOSING : e.param() & ~CLOSING);
        if (!SoulCorpseRules.handCloses(t)) return;
        SongRebuildKit.Ctx ctx = SongRebuildKit.ctx(e);
        if (ctx == null) return;
        int n = 0;
        for (LivingEntity f : SongRebuildKit.foes(p, e.position(), SoulCorpseRules.HAND_REACH)) {
            if (n++ >= 4) break;
            ctx.book().hurt(ctx.inst(), p, f, ctx.mode(), SongRebuildKit.safe(f, 3.5f));
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, BalanceLaw.isBoss(f) ? 1 : 2, false, true));
        }
    }
}
