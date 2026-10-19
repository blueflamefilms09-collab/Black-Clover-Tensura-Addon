package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SongRebuildKit;
import com.newuniverse.nusmp.book.SongRules;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.WikiBooks;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Song Magic: the server behaviours of the two props (the casts are in book.SongBook; the models are tools/gen_song_rebuild_geo.py, drawn by
 * client.prop.SongRebuildPropPainter).
 * SONG_1, param (low byte) 0 = a note bolt: the flying body of the Staccato notes (the hitbox is the SpellRuntime bolt that SongRebuildKit.projectile fires with it, this
 * only moves the model); 1 = the metronome: a brass pyramid planted on the ground whose pendulum keeps the tempo of a field of radius 6 x scale. Every second it
 * hastens the allies in it (Speed, Haste) and slows the foes (Slowness); its tick is the pendulum's beat (the model swings once a second).
 * SONG_2, a choir spirit: param low byte = its place in the choir (0..2), bit 8 = singing (set for 12 ticks around each verse, the painter then uses the sing clip), bit 9 = stays where it stands (the Choir Field's spirits).
 * It floats on a ring of radius 2.4 round its caster, turning slowly, facing where the caster looks; every 30 ticks it sings a verse: allies within 7 blocks of it
 * heal 1.5 percent of their health (BalanceLaw.heal, never on bosses) and the nearest foe within 10 gets a small blow through the book (raw 3.5) and Weakness.
 * Both end when the caster dies or logs out, and a choir spirit's end is a burst of notes.
 */
public final class SongRebuildProps {
    private SongRebuildProps() {}

    public static final int SING = 0x100, STAY = 0x200, VERSE_TICKS = SongRules.VERSE_TICKS, METRONOME_RADIUS = SongRules.METRONOME_RADIUS;
    private static final int ROSE = 0xFFFFB4E8;

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.SongRebuildSelfTests.register();      // 1.93.0: the CI server smoke test runs the rebuild's self-tests (magic.song_rebuild_books / _props)
        MagicProps.register(PropKind.SONG_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & 0xFF) == 1) e.setSize(1.0f * e.scale(), 2.0f * e.scale());
                else SongRebuildKit.FLY.init(e, sl);
            }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & 0xFF) == 0) { SongRebuildKit.FLY.tick(e, sl); return; }
                metronome(e, sl);
            }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & 0xFF) == 1) VfxSpawn.send(sl, VfxShape.SONG_FX3, e.position(), e.position().add(0, 1, 0), ROSE, 20, 0.8f);
                SongRebuildKit.unbind(e);
            }
        });
        MagicProps.register(PropKind.SONG_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.8f * e.scale(), 2.0f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { choir(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                VfxSpawn.send(sl, VfxShape.SONG_FX3, e.position(), e.position().add(0, 1, 0), ROSE, 20, 0.7f);
                SongRebuildKit.unbind(e);
            }
        });
    }

    // ================================================================ the metronome
    private static void metronome(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !SongRebuildKit.bound(e)) { e.expire(); return; }
        if (!SongRules.beats(e.life())) return;
        double r = METRONOME_RADIUS * e.scale();
        Vec3 c = e.position();
        int n = 0;
        for (LivingEntity a : SongRebuildKit.allies(p, c, r)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 0, false, true));
            a.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 50, 0, false, true));
        }
        n = 0;
        for (LivingEntity t : SongRebuildKit.foes(p, c, r)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, BalanceLaw.isBoss(t) ? 0 : 1, false, true));
        }
        VfxSpawn.send(sl, VfxShape.SONG_RING, c, c, ROSE, 16, (float) r);
    }

    // ================================================================ the choir spirit
    private static void choir(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !SongRebuildKit.bound(e)) { e.expire(); return; }
        int role = e.param() & 0xFF, t = e.life();
        if ((e.param() & STAY) == 0) {                                          // the caster's choir rides a ring round the caster; a field's choir stays where it stands
            double a = SongRules.ringAngle(role, t);
            Vec3 pos = new Vec3(p.getX() + Math.cos(a) * 2.4, p.getY() + 0.25 + 0.15 * Math.sin(t * 0.1 + role), p.getZ() + Math.sin(a) * 2.4);
            e.setPos(pos.x, pos.y, pos.z);
            e.setYRot(p.getYRot());
        }
        e.setDeltaMovement(Vec3.ZERO);
        boolean singing = (e.param() & SING) != 0;
        int phase = Math.floorMod(t + role * SongRules.VERSE_STAGGER, SongRules.VERSE_TICKS);
        if (phase == 0) {
            e.setParam(e.param() | SING);
            verse(e, sl, p);
        } else if (singing && phase == SongRules.SING_TICKS) {
            e.setParam(e.param() & ~SING);
        }
    }

    private static void verse(MagicPropEntity e, ServerLevel sl, ServerPlayer p) {
        SongRebuildKit.Ctx ctx = SongRebuildKit.ctx(e);
        Vec3 c = e.position().add(0, 1.2, 0);
        int n = 0;
        for (LivingEntity a : SongRebuildKit.allies(p, c, 7)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            if (!BalanceLaw.isBoss(a)) BalanceLaw.heal(a, a.getMaxHealth() * 0.015f);
        }
        LivingEntity foe = SongRebuildKit.nearestFoe(p, c, 10);
        if (foe != null && ctx != null) {
            ctx.book().hurt(ctx.inst(), p, foe, ctx.mode(), SongRebuildKit.safe(foe, 3.5f));
            foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true));
            VfxSpawn.send(sl, VfxShape.SONG_FX1, c, foe.getBoundingBox().getCenter(), ROSE, 10, 0.5f);
        }
        VfxSpawn.sendFollowing(sl, VfxShape.SONG_NOTES, e, e.position().add(0, 1.8, 0), WikiBooks.color(MagicType.SONG), 30, 0.7f);
    }
}
