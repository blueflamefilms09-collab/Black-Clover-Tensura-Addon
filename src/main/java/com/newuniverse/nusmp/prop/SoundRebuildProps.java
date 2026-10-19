package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.book.SongRebuildKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Sound Magic: the server behaviours of the two props (the casts are in book.SoundBook; the models are tools/gen_sound_rebuild_geo.py, drawn by
 * client.prop.SoundRebuildPropPainter).
 * SOUND_1, a speaker stack planted on the ground. Param low byte 0 = a stack the Speaker Stack page planted: every 30 ticks it sends a sonic pulse at the nearest foe within
 * 12 blocks (raw 4.5 through the book, and a short Magic Jamming: the foe's Ultimate skills are caught with chance 0.25, Nullification.interfere); 1 = a stack of the Resonance
 * Field: it only plays (the field's own zone does the damage). Param bit 8 is set for 8 ticks around each pulse (the painter then uses the pulse clip: the woofer thumps).
 * SOUND_2, the tuning-fork bolt: the flying model of the Sonic Bolt (the hitbox is the SpellRuntime bolt SongRebuildKit.projectile fires with it, this only moves the model).
 * Both end when the caster dies or logs out; a stack's end is a burst of sound.
 */
public final class SoundRebuildProps {
    private SoundRebuildProps() {}

    public static final int PULSE = 0x100, PULSE_TICKS = 30, PULSE_SHOW = 8;
    private static final int CYAN = 0xFF70C8F0;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SOUND_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(1.0f * e.scale(), 2.7f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { speaker(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                VfxSpawn.send(sl, VfxShape.SOUND_FX3, e.position(), e.position().add(0, 1, 0), CYAN, 18, 0.7f);
                SongRebuildKit.unbind(e);
            }
        });
        MagicProps.register(PropKind.SOUND_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { SongRebuildKit.FLY.init(e, sl); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { SongRebuildKit.FLY.tick(e, sl); }
        });
    }

    private static void speaker(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !SongRebuildKit.bound(e)) { e.expire(); return; }
        e.setDeltaMovement(Vec3.ZERO);
        int t = e.life();
        boolean show = (e.param() & PULSE) != 0;
        if (show && t % PULSE_TICKS == PULSE_SHOW) e.setParam(e.param() & ~PULSE);
        if (t % PULSE_TICKS != 0) return;
        e.setParam(e.param() | PULSE);
        Vec3 c = e.position().add(0, 1.4 * e.scale(), 0);
        if ((e.param() & 0xFF) == 1) {                                                   // a field's stack only plays
            VfxSpawn.send(sl, VfxShape.SOUND_FX1, c, c, CYAN, 10, 0.5f);
            return;
        }
        SongRebuildKit.Ctx ctx = SongRebuildKit.ctx(e);
        LivingEntity foe = SongRebuildKit.nearestFoe(p, c, 12);
        if (foe == null || ctx == null) return;
        ctx.book().hurt(ctx.inst(), p, foe, ctx.mode(), SongRebuildKit.safe(foe, 4.5f));
        Nullification.interfere(foe, 2, 0.25f, 0.15f);
        VfxSpawn.send(sl, VfxShape.SOUND_FX1, c, foe.getBoundingBox().getCenter(), CYAN, 10, 0.55f);
    }
}
