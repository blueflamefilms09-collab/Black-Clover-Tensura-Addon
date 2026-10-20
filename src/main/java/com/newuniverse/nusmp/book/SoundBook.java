package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ext.ExclusiveExt;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * 1.93.0 Sound Magic, rebuilt as vibration made visible: shock fronts, speakers, tuning forks, resonance. The five generic pages keep their ids and their order
 * (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard); four pages are appended (sound_speaker, sound_silence, sound_resonance, sound_boom):
 * 9 family pages of the 24 allowed. Every successful cast puts the 5 second Resonance Plate on the caster (Aura.SOUND, client.aura.SoundAura). Damage is wind elemental.
 * Every sonic hit leaves a RESONANCE mark (stacks to 5 for 10 s, see {@link SoundResonance}); Resonant Shatter rings them all.
 * <pre>
 *   attribute_bolt    Sonic Bolt         a tuning-fork bolt (SOUND_2): raw 9 on the first foe or wall it meets, a burst of radius 1.6, a resonance mark and a short Magic Jamming
 *   attribute_cut     Sound Wave         a waveform line of 20 blocks: raw 8 on everything along it, knock-back, a mark
 *   attribute_field   Resonance Field    a field of radius 5 for 5 s with two speaker stacks flanking it: every second raw 4 on foes, Slowness, a mark and barriers shattered
 *   attribute_domain  Sonic Boom         a burst of radius 8: raw 14, big knock-back, the foes' Ultimate skills jammed for 3 s (Nullification.interfere) and a mark
 *   attribute_guard   Echo Plate         Speed, a melee bonus and thorns for 9 s; the armour for as long
 *   sound_speaker     Speaker Stack      two stacks (SOUND_1) planted at the caster's sides for 15 s; each sends a pulse at the nearest foe every 1.5 s (raw 4.5, jamming)
 *   sound_silence     Silence            Magic Jamming over radius 9: every active skill of every foe is put on a cooldown of 6 s (Nullification.jamAll), bosses 2 s; no damage
 *   sound_resonance   Resonant Shatter   rings every mark within 24 blocks: raw 5 per stack, barriers shattered round each foe
 *   sound_boom        Mach Step          a dash of 9 blocks that hits everything on the way for raw 12 and leaves a shock ring
 * </pre>
 * No page controls hard; bosses take half of the raw value and shorter jamming.
 */
public final class SoundBook extends ElementBook {
    private static final int CYAN = 0xFF70C8F0;
    private static final String RES = "nusmp_sound_res", RES_AT = "nusmp_sound_res_at";

    public SoundBook() {
        super(MagicType.SOUND, WikiBooks.color(MagicType.SOUND), TensuraDamageTypes.WIND_ELEMENTAL, familyList());
    }

    public static List<BookPage> familyList() {
        List<BookPage> l = new ArrayList<>(List.of(
                starter("attribute_bolt", "Sound: Sonic Bolt", SoundBook::sonicBolt),
                mid("attribute_cut", "Sound: Sound Wave", SoundBook::soundWave),
                zone("attribute_field", "Sound: Resonance Field", SoundBook::resonanceField),
                signature("attribute_domain", "Sound: Sonic Boom", SoundBook::sonicBoom).withCooldown(900),
                mid("attribute_guard", "Sound: Echo Plate", SoundBook::echoPlate)));
        l.addAll(ExclusiveExt.pages(MagicType.SOUND));
        l.add(mid("sound_speaker", "Sound: Speaker Stack", SoundBook::speakers).withCooldown(300));
        l.add(mid("sound_silence", "Sound: Silence", SoundBook::silence).withCooldown(400));
        l.add(zone("sound_resonance", "Sound: Resonant Shatter", SoundBook::shatter).withCooldown(320));
        l.add(mid("sound_boom", "Sound: Mach Step", SoundBook::machStep).withCooldown(260));
        return List.copyOf(l);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SOUND_FX1, p, p.getEyePosition(), CYAN, 12, 0.6f * power);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) {
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUND_FX3, at, at.add(0, 1, 0), CYAN, 20, power);
    }

    // ---------------------------------------------------------------- resonance marks
    static void mark(LivingEntity t) {
        CompoundTag d = t.getPersistentData();
        long now = t.level().getGameTime();
        d.putInt(RES, SoundResonance.next(d.getInt(RES), d.getLong(RES_AT), now));
        d.putLong(RES_AT, now);
    }

    static int marks(LivingEntity t) {
        CompoundTag d = t.getPersistentData();
        return SoundResonance.alive(d.getInt(RES), d.getLong(RES_AT), t.level().getGameTime());
    }

    static void clearMarks(LivingEntity t) { t.getPersistentData().putInt(RES, 0); }

    // ---------------------------------------------------------------- the five generic pages
    private static boolean sonicBolt(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.25, 0);
        b.castCircle(p, 1f);
        SongRebuildKit.projectile(p, PropKind.SOUND_2, 0, start, p.getViewVector(1f), 2.4, 0.55 * s, 20, false,
                (bolt, t) -> { },
                (bolt, at) -> {
                    for (LivingEntity t : GrimoireBook.around(p, at, 1.6 * s)) {
                        b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 9f));
                        mark(t);
                        Nullification.interfere(t, 2, 0.3f, 0.2f);
                    }
                    b.impact(p, at, 0.7f);
                }, 0.85f * s);
        return true;
    }

    private static boolean soundWave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition().add(0, -0.2, 0), dir = p.getViewVector(1f), end = a.add(dir.scale(20 * s));
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.2)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 8f));
            mark(t);
            Vec3 d = new Vec3(dir.x, 0, dir.z);
            d = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
            t.knockback(0.6, -d.x, -d.z);
        }
        b.castCircle(p, 0.8f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUND_FX1, a, end, CYAN, 16, 1.5f);
        return true;
    }

    private static boolean resonanceField(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 5 * s;
        int ticks = 100;
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.SOUND_FX2, c, c.add(0, 1, 0), CYAN, ticks, (float) r);
        for (int k = 0; k < 2; k++) {                                                     // two speaker stacks flank the field
            Vec3 at = c.add(k == 0 ? r * 0.9 : -r * 0.9, 0, 0);
            SongRebuildKit.spawn(p, PropKind.SOUND_1, at, k == 0 ? 90f : -90f, 0.9f, ticks, 1, b, i, mode);
        }
        SpellRuntime.zone(level, ticks, 20, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 4f));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, true));
                mark(t);
            }
            Nullification.shatterBarriers(level, c, r, p);
        });
        return true;
    }

    private static boolean sonicBoom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        double r = 8 * s;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 14f));
            Vec3 d = t.position().subtract(c).multiply(1, 0, 1);
            d = d.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : d.normalize();
            t.knockback(1.4, -d.x, -d.z);
            Nullification.interfere(t, 3, 0.6f, 0.4f);
            mark(t);
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUND_FX3, c, c.add(0, 1, 0), CYAN, 26, (float) r / 3f * 1.3f);
        return true;
    }

    private static boolean echoPlate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        long now = p.level().getGameTime();
        tag.putLong("EmpowerUntil", now + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0, false, true));
        b.castCircle(p, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUND_FX2, p.position(), p.position().add(0, 1, 0), CYAN, 24, 1.6f);
        return true;
    }

    // ---------------------------------------------------------------- the appended pages
    private static boolean speakers(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        SongRebuildKit.dismiss(p, PropKind.SOUND_1, 0, 0);
        int life = SongRebuildKit.span(300, p);
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        float yaw = SongRebuildKit.yawOf(look.x, look.z);
        for (int k = -1; k <= 1; k += 2) {
            Vec3 at = p.position().add(side.scale(2.3 * k)).add(look.scale(0.6));
            SongRebuildKit.spawn(p, PropKind.SOUND_1, at, yaw, 0.95f * Math.min(1.3f, SongRebuildKit.size(p)), life, 0, b, i, mode);
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUND_FX3, p.position(), p.position().add(0, 1, 0), CYAN, 20, 0.9f);
        return true;
    }

    private static boolean silence(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position();
        double r = 9 * GrimoireBook.size(i, p);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            Nullification.jamAll(t, BalanceLaw.isBoss(t) ? 2 : 6, 0.5f);
            EnergyBridge.effect(t, "silence", BalanceLaw.controlTicks(t, 100), 0);
        }
        b.castCircle(p, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUND_FX2, c, c.add(0, 1, 0), CYAN, 40, (float) r);
        return true;
    }

    private static boolean shatter(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        int rung = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, 24 * s)) {
            int m = marks(t);
            if (m <= 0) continue;
            if (rung++ >= SongRebuildKit.MAX_TARGETS) break;
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, SoundResonance.ring(m, 5f)));
            clearMarks(t);
            Nullification.shatterBarriers(p.serverLevel(), t.position(), 3.0, p);
            VfxSpawn.send(p.serverLevel(), VfxShape.SOUND_FX3, t.position(), t.position().add(0, 1, 0), CYAN, 18, 0.5f + 0.15f * m);
        }
        if (rung == 0) { GrimoireBook.fail(p, "No foe is ringing."); return false; }
        b.castCircle(p, 1f);
        return true;
    }

    private static boolean machStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        BookPage.Cast dash = ElementBook.dash(12f, 9, false, VfxShape.SOUND_FX1, (t, pl) -> mark(t));
        boolean ok = dash.cast(b, i, p, mode);
        if (ok) VfxSpawn.send(p.serverLevel(), VfxShape.SOUND_FX3, p.position(), p.position().add(0, 1, 0), CYAN, 22, 1.1f);
        return ok;
    }
}
