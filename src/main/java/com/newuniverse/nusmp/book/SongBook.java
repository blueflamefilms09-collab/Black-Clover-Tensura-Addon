package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ext.ExclusiveExt;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
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
 * 1.93.0 Song Magic, rebuilt on the Black Clover look: notes, staff lines, choirs and brass. The five generic pages keep their ids and their order (attribute_bolt,
 * attribute_cut, attribute_field, attribute_domain, attribute_guard), Kahono's five song pages (SongExt) follow as before, and four pages are appended (song_choir,
 * song_metronome, song_harmony, song_finale): 14 family pages of the 24 allowed. Every successful cast puts the 5 second Concert Regalia on the caster (Aura.SONG,
 * client.aura.SongAura); every page that summons uses a real model (song_note, song_choir, song_metronome: client.prop.SongRebuildPropPainter).
 * <pre>
 *   attribute_bolt    Staccato Bolt        three short note bolts, 4 ticks apart, each a flying note (SONG_1 param 0): raw 4.2 and Weakness, bursting at the end
 *   attribute_cut     Glissando            a staff of five lines drawn through every foe on a 18 block line: raw 8, Slowness
 *   attribute_field   Choir Field          a field of radius 5 for 5 s with three choir spirits standing in it: every second foes take raw 4.5 and slow, allies heal 2 percent
 *   attribute_domain  Grand Concerto       a great burst of radius 7: raw 15, knock-back, allies gain Absorption
 *   attribute_guard   Aria Armour          Regeneration, melee bonus and thorns for 9 s, and the armour for as long
 *   song_choir        Choir of Spirits     three choir spirits (SONG_2) circle the caster for 20 s and sing a verse every 1.5 s (heal allies, hurt the nearest foe)
 *   song_metronome    Metronome            a brass metronome (SONG_1 param 1) on the aimed ground for 10 s: allies in 6 blocks are hastened, foes slowed
 *   song_harmony      Harmony              up to four allies within 12 blocks share Resistance, Speed and Regeneration for 10 s
 *   song_finale       Grand Finale         everything the song has built released at once: raw 20 on every foe within 14 and half of it as spiritual damage,
 *                                          allies heal 25 percent (never bosses); the choir sings its loudest verse
 * </pre>
 * Damage is the book's {@code hurt} (EP law); bosses take half of the raw value; no page controls hard.
 */
public final class SongBook extends ElementBook {
    private static final int ROSE = 0xFFFFB4E8;

    public SongBook() {
        super(MagicType.SONG, WikiBooks.color(MagicType.SONG), TensuraDamageTypes.MAGIC_GENERIC, familyList());
    }

    public static List<BookPage> familyList() {
        List<BookPage> l = new ArrayList<>(List.of(
                starter("attribute_bolt", "Song: Staccato Bolt", SongBook::staccato),
                mid("attribute_cut", "Song: Glissando", SongBook::glissando),
                zone("attribute_field", "Song: Choir Field", SongBook::choirField),
                signature("attribute_domain", "Song: Grand Concerto", SongBook::concerto).withCooldown(900),
                mid("attribute_guard", "Song: Aria Armour", SongBook::aria)));
        l.addAll(ExclusiveExt.pages(MagicType.SONG));
        l.add(mid("song_choir", "Song: Choir of Spirits", SongBook::choir).withCooldown(300));
        l.add(mid("song_metronome", "Song: Metronome", SongBook::metronome).withCooldown(240));
        l.add(mid("song_harmony", "Song: Harmony", SongBook::harmony).withCooldown(400));
        l.add(signature("song_finale", "Song: Grand Finale", SongBook::finale).withCooldown(1800).withAnim("signature"));
        return List.copyOf(l);
    }

    // ---------------------------------------------------------------- the effects of a cast (FX1 = cast, FX3 = impact)
    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SONG_FX1, p, p.getEyePosition(), ROSE, 12, 0.6f * power);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) {
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_FX3, at, at.add(0, 1, 0), ROSE, 20, power);
    }

    // ---------------------------------------------------------------- the five generic pages
    private static boolean staccato(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1f);
        for (int k = 0; k < 3; k++) {
            final int n = k;
            SpellRuntime.later(level, k * 4, () -> {
                if (!p.isAlive()) return;
                Vec3 start = p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.25 + 0.1 * (n - 1), 0);
                SongRebuildKit.projectile(p, PropKind.SONG_1, 0, start, p.getViewVector(1f), 1.7, 0.5 * s, 22, false,
                        (bolt, t) -> { },
                        (bolt, at) -> {
                            for (LivingEntity t : GrimoireBook.around(p, at, 1.8 * s)) {
                                b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 4.2f));
                                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, true));
                            }
                            b.impact(p, at, 0.6f);
                        }, 0.9f * s);
            });
        }
        return true;
    }

    private static boolean glissando(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition().add(0, -0.2, 0), dir = p.getViewVector(1f), end = a.add(dir.scale(18 * s));
        int hit = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.1)) {
            if (hit++ >= SongRebuildKit.MAX_TARGETS) break;
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 8f));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, true));
        }
        b.castCircle(p, 0.8f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_FX1, a, end, ROSE, 18, 1.4f);
        return true;
    }

    private static boolean choirField(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 5 * s;
        int ticks = 100;
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.SONG_FX2, c, c.add(0, 1, 0), ROSE, ticks, (float) r);
        for (int k = 0; k < 3; k++) {                                           // three spirits stand in the field
            double an = k * Math.PI * 2 / 3 + 0.5;
            Vec3 at = c.add(Math.cos(an) * r * 0.62, 0, Math.sin(an) * r * 0.62);
            MagicPropEntity e = SongRebuildKit.spawn(p, PropKind.SONG_2, at, (float) Math.toDegrees(an) + 90f, 0.9f, ticks, k | com.newuniverse.nusmp.prop.SongRebuildProps.STAY, b, i, mode);
            if (e != null) e.setPos(at.x, at.y, at.z);
        }
        SpellRuntime.zone(level, ticks, 20, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 4.5f));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, true));
            }
            for (LivingEntity a : SongRebuildKit.allies(p, c, r)) if (!BalanceLaw.isBoss(a)) BalanceLaw.heal(a, a.getMaxHealth() * 0.02f);
        });
        return true;
    }

    private static boolean concerto(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        double r = 7 * s;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 15f));
            Vec3 d = t.position().subtract(c).multiply(1, 0, 1);
            d = d.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : d.normalize();
            t.knockback(1.0, -d.x, -d.z);
        }
        for (LivingEntity a : SongRebuildKit.allies(p, c, r)) a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 0, false, true));
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_FX3, c, c.add(0, 1, 0), ROSE, 28, (float) r / 3f * 1.4f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_RING, c, c, ROSE, 26, (float) r);
        return true;
    }

    private static boolean aria(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        long now = p.level().getGameTime();
        tag.putLong("EmpowerUntil", now + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SONG_NOTES, p, p.position().add(0, 1.6, 0), ROSE, 60, 1.2f);
        return true;
    }

    // ---------------------------------------------------------------- the appended pages
    private static boolean choir(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        SongRebuildKit.dismiss(p, PropKind.SONG_2, -1, 0);
        int life = SongRebuildKit.span(400, p);
        float s = 0.9f * Math.min(1.3f, SongRebuildKit.size(p));
        for (int k = 0; k < 3; k++) {
            double an = k * Math.PI * 2 / 3;
            Vec3 at = p.position().add(Math.cos(an) * 2.4, 0.25, Math.sin(an) * 2.4);
            SongRebuildKit.spawn(p, PropKind.SONG_2, at, p.getYRot(), s, life, k, b, i, mode);
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_FX2, p.position(), p.position().add(0, 1, 0), ROSE, 30, 3.5f);
        return true;
    }

    private static boolean metronome(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        SongRebuildKit.dismiss(p, PropKind.SONG_1, 1, 0);
        Vec3 at = GrimoireBook.aim(p, 16);
        int life = SongRebuildKit.span(200, p);
        MagicPropEntity e = SongRebuildKit.spawn(p, PropKind.SONG_1, at, p.getYRot() + 180f, 1.0f * Math.min(1.3f, SongRebuildKit.size(p)), life, 1, b, i, mode);
        if (e == null) { GrimoireBook.fail(p, "The metronome would not stand."); return false; }
        b.castCircle(p, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_FX2, at, at.add(0, 1, 0), ROSE, life, 6f);
        return true;
    }

    private static boolean harmony(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<LivingEntity> allies = SongRebuildKit.allies(p, p.position(), 12);
        int n = 0;
        for (LivingEntity a : allies) {
            if (n >= 4) break;
            if (!(a instanceof ServerPlayer)) continue;
            n++;
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true));
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, false, true));
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, true));
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SONG_NOTES, a, a.position().add(0, 1.6, 0), ROSE, 50, 0.9f);
        }
        if (n == 0) { p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true)); }
        b.castCircle(p, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_RING, p.position(), p.position(), ROSE, 24, 12f);
        return true;
    }

    private static boolean finale(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        double r = 14 * s;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 20f));
            EnergyBridge.spirit(t, SongRebuildKit.safe(t, 10f));
        }
        for (LivingEntity a : SongRebuildKit.allies(p, c, r)) if (!BalanceLaw.isBoss(a)) BalanceLaw.heal(a, a.getMaxHealth() * 0.25f);
        for (MagicPropEntity e : SongRebuildKit.mine(p, PropKind.SONG_2)) e.setParam(e.param() | com.newuniverse.nusmp.prop.SongRebuildProps.SING);
        b.castCircle(p, 1.5f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_FX3, c, c.add(0, 1, 0), ROSE, 40, (float) r / 3f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SONG_RING, c, c, ROSE, 36, (float) r);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SONG_NOTES, p, p.position().add(0, 1.6, 0), ROSE, 80, 1.8f);
        return true;
    }
}
