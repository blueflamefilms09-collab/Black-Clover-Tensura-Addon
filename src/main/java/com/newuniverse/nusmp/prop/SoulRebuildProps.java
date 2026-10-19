package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.SongRebuildKit;
import com.newuniverse.nusmp.book.SoulRules;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Soul Magic: the server behaviours of the two props and the Soul Harvest hook (the casts are in book.SoulBook; the models are tools/gen_soul_rebuild_geo.py, drawn by
 * client.prop.SoulRebuildPropPainter).
 * SOUL_1, a soul wisp (param low byte = its place 0..2, bit 8 = attacking): it floats on a ring of radius 2.2 round its caster; every 28 ticks, when a foe is within 10 blocks,
 * it lunges at it (6 ticks), bites on the last tick (raw 5 through the book plus Spiritual Damage, SoulRules.spirit) and glides back; the painter shows the attack clip while
 * bit 8 is set. param bit 9 = it is part of Soul Ascension (it ends in a burst of its own).
 * SOUL_2, the soul-flame orb: the flying model of the Soul Flame Bolt (the hitbox is the SpellRuntime bolt SongRebuildKit.projectile fires with it).
 * Soul Harvest: while a player's persistent 'nusmp_soul_harvest_until' has not run out, every foe that dies within 16 blocks of the player returns magicules and health to
 * the player (SoulRules, the first 6 souls of a harvest), with a rising soul on the spot.
 */
public final class SoulRebuildProps {
    private SoulRebuildProps() {}

    public static final int ATTACK = 0x100, ASCEND = 0x200, LUNGE_EVERY = 28, LUNGE_TICKS = 6;
    public static final String HARVEST_UNTIL = "nusmp_soul_harvest_until", HARVEST_COUNT = "nusmp_soul_harvest_n";
    private static final int TEAL = 0xFF72D9CC;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SOUL_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.8f * e.scale(), 2.2f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { wisp(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                VfxSpawn.send(sl, VfxShape.SOUL_FX3, e.position(), e.position().add(0, 1, 0), TEAL, 22, (e.param() & ASCEND) != 0 ? 1.0f : 0.6f);
                SongRebuildKit.unbind(e);
            }
        });
        MagicProps.register(PropKind.SOUL_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { SongRebuildKit.FLY.init(e, sl); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { SongRebuildKit.FLY.tick(e, sl); }
        });
        AttributeEvents.death(ev -> {
            LivingEntity dead = ev.getEntity();
            if (dead.level().isClientSide || !(dead.level() instanceof ServerLevel sl)) return;
            long now = sl.getGameTime();
            for (ServerPlayer p : sl.players()) {
                CompoundTag d = p.getPersistentData();
                if (d.getLong(HARVEST_UNTIL) <= now || p.distanceToSqr(dead) > 16 * 16 || dead == p || dead.isAlliedTo(p)) continue;
                int n = d.getInt(HARVEST_COUNT);
                if (!SoulRules.pays(n)) continue;
                d.putInt(HARVEST_COUNT, n + 1);
                var ex = TensuraStorages.getExistenceFrom(p);
                if (ex != null) {
                    double max = EnergyHelper.getMaxMagicule(p);
                    ex.setMagicule(Math.min(max, ex.getMagicule() + SoulRules.harvestMana(max)));
                    ex.markDirty();
                }
                BalanceLaw.heal(p, SoulRules.harvestHealth(p.getMaxHealth()));
                VfxSpawn.send(sl, VfxShape.SOUL_FX3, dead.position(), p.position().add(0, 1, 0), TEAL, 18, 0.45f);
            }
        });
    }

    private static void wisp(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !SongRebuildKit.bound(e)) { e.expire(); return; }
        int role = e.param() & 0xFF, t = e.life();
        int phase = (t + role * 9) % LUNGE_EVERY;
        double a = role * (Math.PI * 2 / 3.0) + t * 0.03;
        Vec3 home = new Vec3(p.getX() + Math.cos(a) * 2.2, p.getY() + 0.35 + 0.2 * Math.sin(t * 0.09 + role), p.getZ() + Math.sin(a) * 2.2);
        LivingEntity foe = phase < LUNGE_TICKS + 2 ? SongRebuildKit.nearestFoe(p, p.position(), 10) : null;
        Vec3 pos = home;
        if (foe != null && phase < LUNGE_TICKS) {                                       // the lunge: from the ring to the foe
            double k = (phase + 1) / (double) LUNGE_TICKS;
            Vec3 target = new Vec3(foe.getX(), foe.getY() + 0.2, foe.getZ());
            pos = home.add(target.subtract(home).scale(k));
            e.setYRot(SongRebuildKit.yawOf(target.x - home.x, target.z - home.z));
            if ((e.param() & ATTACK) == 0) e.setParam(e.param() | ATTACK);
            if (phase == LUNGE_TICKS - 1) bite(e, sl, p, foe);
        } else {
            if ((e.param() & ATTACK) != 0) e.setParam(e.param() & ~ATTACK);
            e.setYRot(p.getYRot());
        }
        e.setPos(pos.x, pos.y, pos.z);
        e.setDeltaMovement(Vec3.ZERO);
    }

    private static void bite(MagicPropEntity e, ServerLevel sl, ServerPlayer p, LivingEntity foe) {
        SongRebuildKit.Ctx ctx = SongRebuildKit.ctx(e);
        if (ctx == null) return;
        ctx.book().hurt(ctx.inst(), p, foe, ctx.mode(), SongRebuildKit.safe(foe, 5f));
        EnergyBridge.spirit(foe, SoulRules.spirit(BalanceLaw.epScale(p), 1.5f, BalanceLaw.isBoss(foe)));
        VfxSpawn.send(sl, VfxShape.SOUL_FX1, e.position().add(0, 1, 0), foe.getBoundingBox().getCenter(), TEAL, 8, 0.4f);
    }
}
