package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.MemoryRewind;
import com.newuniverse.nusmp.book.SongRebuildKit;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 1.93.0 Memory Magic: the server behaviours of the two props and the three hooks (the casts are in book.MemoryBook; the models are tools/gen_memory_rebuild_geo.py, drawn by
 * client.prop.MemoryRebuildPropPainter; the history ring and the Archive tally are book.MemoryRewind).
 * MEMORY_1, the memory echo (param bit 8 = striking): an afterimage of the caster that does not walk but blinks. Every 30 ticks, when a foe is within 12 blocks of the caster, it
 * appears 1.6 blocks from the foe on the line towards the caster, strikes (raw 5 through the book) and REPLAYS the same blow 8 ticks later (raw 3), then blinks back to its place
 * beside the caster 14 ticks after the first blow. With no foe it stands one block behind the caster's shoulder, facing where the caster looks.
 * MEMORY_2, the recollection orb: the flying model of the Recollection Bolt (the hitbox is the SpellRuntime bolt SongRebuildKit.projectile fires with it).
 * Hooks: (1) playerTick records the caster's history (a sample per 20 ticks: Recall goes back to one); (2) incoming: while 'nusmp_mem_veil_until' has not run out, the first hit a
 * player takes is forgotten (cancelled; a boss's hit is only cut by 70 percent), once; (3) damaged: every blow a foe takes while it is archived is added to its tally (Archive).
 * Logout clears the history. Everything is keyed by UUID in small static maps that the hooks clean.
 */
public final class MemoryRebuildProps {
    private MemoryRebuildProps() {}

    public static final int STRIKING = 0x100, BLINK_EVERY = 30, REPLAY_AFTER = 8, BACK_AFTER = 14;
    public static final String VEIL_UNTIL = "nusmp_mem_veil_until";
    private static final int LAV = 0xFFBCA8FF;

    private static final Map<UUID, MemoryRewind.Ring> RINGS = new HashMap<>();
    /** Archived foes: foe uuid -> {until game time, recorded damage}. */
    private static final Map<UUID, double[]> ARCHIVE = new HashMap<>();

    public static MemoryRewind.Ring ring(ServerPlayer p) { return RINGS.computeIfAbsent(p.getUUID(), k -> new MemoryRewind.Ring()); }

    public static void archive(LivingEntity t, long until) { ARCHIVE.put(t.getUUID(), new double[]{until, 0}); }

    /** Ends the archive of a foe and returns what was recorded (0 when it was not archived). */
    public static double takeArchive(LivingEntity t) {
        double[] a = ARCHIVE.remove(t.getUUID());
        return a == null ? 0 : a[1];
    }

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.MEMORY_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.8f * e.scale(), 2.0f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { echo(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                VfxSpawn.send(sl, VfxShape.MEMORY_FX3, e.position(), e.position().add(0, 1, 0), LAV, 18, 0.6f);
                SongRebuildKit.unbind(e);
            }
        });
        MagicProps.register(PropKind.MEMORY_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { SongRebuildKit.FLY.init(e, sl); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { SongRebuildKit.FLY.tick(e, sl); }
        });
        AttributeEvents.playerTick(p -> {
            long now = p.level().getGameTime();
            if (now % 5 != 0) return;
            if (!p.isAlive()) return;
            ring(p).offer(new MemoryRewind.Sample(now, p.getX(), p.getY(), p.getZ(), p.getHealth(), p.level().dimension().location().toString()));
        });
        AttributeEvents.logout(p -> RINGS.remove(p.getUUID()));
        AttributeEvents.incoming(ev -> {
            if (!(ev.getEntity() instanceof ServerPlayer p)) return;
            long until = p.getPersistentData().getLong(VEIL_UNTIL);
            if (until <= p.level().getGameTime()) return;
            p.getPersistentData().putLong(VEIL_UNTIL, 0L);
            boolean boss = ev.getSource().getEntity() instanceof LivingEntity att && BalanceLaw.isBoss(att);
            if (boss) ev.setAmount(ev.getAmount() * 0.3f); else ev.setCanceled(true);
            if (p.level() instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.MEMORY_FX3, p.position(), p.position().add(0, 1, 0), LAV, 16, 0.5f);
        });
        AttributeEvents.damaged(ev -> {
            if (ARCHIVE.isEmpty()) return;
            LivingEntity v = ev.getEntity();
            double[] a = ARCHIVE.get(v.getUUID());
            if (a == null) return;
            if (v.level().getGameTime() > a[0]) { ARCHIVE.remove(v.getUUID()); return; }
            a[1] += ev.getNewDamage();
        });
    }

    // ================================================================ the echo
    private static void echo(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !SongRebuildKit.bound(e)) { e.expire(); return; }
        int t = e.life(), phase = t % BLINK_EVERY;
        e.setDeltaMovement(Vec3.ZERO);
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        Vec3 home = p.position().add(side.scale(-1.3)).add(look.scale(-0.8));
        if (phase == 0) {                                                                   // the blink to the foe and the first blow
            LivingEntity foe = SongRebuildKit.nearestFoe(p, p.position(), 12);
            if (foe != null) {
                Vec3 toP = p.position().subtract(foe.position()).multiply(1, 0, 1);
                toP = toP.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : toP.normalize();
                Vec3 at = foe.position().add(toP.scale(1.6));
                e.setPos(at.x, foe.getY(), at.z);
                e.setYRot(SongRebuildKit.yawOf(foe.getX() - at.x, foe.getZ() - at.z));
                e.getPersistentData().putInt("foe", foe.getId());
                blow(e, sl, p, foe, 5f);
                e.setParam(e.param() | STRIKING);
                return;
            }
        }
        if (phase == REPLAY_AFTER && e.level().getEntity(e.getPersistentData().getInt("foe")) instanceof LivingEntity foe && foe.isAlive() && e.distanceTo(foe) < 4) {
            blow(e, sl, p, foe, 3f);                                                         // the same blow, replayed
        }
        if (phase == BACK_AFTER) {                                                          // back beside the caster
            e.getPersistentData().putInt("foe", -1);
            e.setParam(e.param() & ~STRIKING);
            e.setPos(home.x, p.getY(), home.z);
            e.setYRot(p.getYRot());
        } else if (phase > BACK_AFTER && SongRebuildKit.ctx(e) != null) {                    // idling beside the caster
            e.setPos(home.x, p.getY() + 0.1 * Math.sin(t * 0.1), home.z);
            e.setYRot(p.getYRot());
        }
    }

    private static void blow(MagicPropEntity e, ServerLevel sl, ServerPlayer p, LivingEntity foe, float raw) {
        SongRebuildKit.Ctx ctx = SongRebuildKit.ctx(e);
        if (ctx == null) return;
        ctx.book().hurt(ctx.inst(), p, foe, ctx.mode(), SongRebuildKit.safe(foe, raw));
        VfxSpawn.send(sl, VfxShape.MEMORY_FX1, e.position().add(0, 1.2, 0), foe.getBoundingBox().getCenter(), LAV, 8, 0.4f);
    }
}
