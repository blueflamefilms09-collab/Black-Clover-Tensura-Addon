package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ext.ExclusiveExt;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.prop.SoulRebuildProps;
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
 * 1.93.0 Soul Magic, rebuilt on the Black Clover look: pale soul fire, hollow-eyed spirits, the soul as something that can be struck, drained and harvested. The five generic
 * pages keep their ids and their order (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard), Lucius's two Paladin pages (SoulExt) follow as
 * before, and four pages are appended (soul_wisps, soul_bind, soul_harvest, soul_ascension): 11 family pages of the 24 allowed. Every successful cast puts the 5 second Spirit
 * Raiment on the caster (Aura.SOUL, client.aura.SoulAura). Every page pairs a body blow with Spiritual Damage (EnergyBridge.spirit, see {@link SoulRules}); at the spiritual
 * floor that becomes the mod's Soul Annihilation rule (book.KotodamaWords.spirit), nothing new here.
 * <pre>
 *   attribute_bolt    Soul Flame Bolt   a soul-flame orb (SOUL_2): raw 7 and spiritual 2 on the first foe it meets, a burst of radius 1.6, 1.5 percent of the foe's magicules drained to you
 *   attribute_cut     Soul Sever        a line of 14 blocks: raw 6 and spiritual 2.5 on everything along it, 3 percent of their magicules drained to you
 *   attribute_field   Soul Vortex       a field of radius 5.5 for 5 s: every second raw 3, spiritual 1.2 and 1.5 percent drained to you; Slowness on the foes
 *   attribute_domain  Soul Release      a burst of radius 8: raw 12 and spiritual 4, allies heal 10 percent, foes weakened; souls rise out of everything it touches
 *   attribute_guard   Spirit Mantle     Absorption, Regeneration, a melee bonus and thorns for 9 s; the armour for as long
 *   soul_wisps        Soul Wisps        three soul spirits (SOUL_1) circle you for 20 s and lunge at foes within 10 blocks every 1.4 s (raw 5 and spiritual 1.5 each bite)
 *   soul_bind         Soul Bind         the foe in your sights: three pulses a second apart of raw 3 and spiritual 2, Weakness and Glowing for 6 s, 2 percent of your health back per pulse
 *   soul_harvest      Soul Harvest      for 10 s every foe that dies within 16 blocks returns 6 percent of your magicules and 8 percent of your health (the first 6 souls)
 *   soul_ascension    Soul Ascension    three wisps rise round you and detonate after 2 s: raw 10 and spiritual 3 on every foe within 6 blocks of each, 15 percent of your health back
 * </pre>
 * No page controls hard; bosses take half of the raw value and half of the spiritual damage.
 */
public final class SoulBook extends ElementBook {
    private static final int TEAL = 0xFF72D9CC;

    public SoulBook() {
        super(MagicType.SOUL, WikiBooks.color(MagicType.SOUL), TensuraDamageTypes.MAGIC_GENERIC, familyList());
    }

    public static List<BookPage> familyList() {
        List<BookPage> l = new ArrayList<>(List.of(
                starter("attribute_bolt", "Soul: Soul Flame Bolt", SoulBook::flameBolt),
                mid("attribute_cut", "Soul: Soul Sever", SoulBook::sever),
                zone("attribute_field", "Soul: Soul Vortex", SoulBook::vortex),
                signature("attribute_domain", "Soul: Soul Release", SoulBook::release).withCooldown(900),
                mid("attribute_guard", "Soul: Spirit Mantle", SoulBook::mantle)));
        l.addAll(ExclusiveExt.pages(MagicType.SOUL));
        l.add(mid("soul_wisps", "Soul: Soul Wisps", SoulBook::wisps).withCooldown(300));
        l.add(mid("soul_bind", "Soul: Soul Bind", SoulBook::bind).withCooldown(260));
        l.add(zone("soul_harvest", "Soul: Soul Harvest", SoulBook::harvest).withCooldown(600));
        l.add(signature("soul_ascension", "Soul: Soul Ascension", SoulBook::ascension).withCooldown(1800).withAnim("signature"));
        return List.copyOf(l);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SOUL_FX1, p, p.getEyePosition(), TEAL, 12, 0.6f * power);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) {
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_FX3, at, at.add(0, 1, 0), TEAL, 22, power);
    }

    /** A body blow and a soul blow together. */
    private static void strike(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float raw, float spirit) {
        b.hurt(i, p, t, mode, SongRebuildKit.safe(t, raw));
        EnergyBridge.spirit(t, SoulRules.spirit(BalanceLaw.epScale(p), spirit, BalanceLaw.isBoss(t)));
    }

    // ---------------------------------------------------------------- the five generic pages
    private static boolean flameBolt(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.25, 0);
        b.castCircle(p, 1f);
        SongRebuildKit.projectile(p, PropKind.SOUL_2, 0, start, p.getViewVector(1f), 1.5, 0.6 * s, 26, false,
                (bolt, t) -> { },
                (bolt, at) -> {
                    for (LivingEntity t : GrimoireBook.around(p, at, 1.6 * s)) {
                        strike(b, i, p, t, mode, 7f, 2f);
                        EnergyBridge.drain(t, p, 0.015);
                    }
                    b.impact(p, at, 0.7f);
                }, 0.9f * s);
        return true;
    }

    private static boolean sever(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition().add(0, -0.2, 0), end = a.add(p.getViewVector(1f).scale(14 * s));
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.1)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            strike(b, i, p, t, mode, 6f, 2.5f);
            EnergyBridge.drain(t, p, 0.03);
        }
        b.castCircle(p, 0.8f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_FX1, a, end, TEAL, 16, 1.4f);
        return true;
    }

    private static boolean vortex(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 5.5 * s;
        int ticks = 100;
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.SOUL_FX2, c, c.add(0, 1, 0), TEAL, ticks, (float) r);
        SpellRuntime.zone(level, ticks, 20, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                strike(b, i, p, t, mode, 3f, 1.2f);
                EnergyBridge.drain(t, p, 0.015);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, true));
            }
        });
        return true;
    }

    private static boolean release(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        double r = 8 * s;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            strike(b, i, p, t, mode, 12f, 4f);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
            VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_FX3, t.position(), t.position().add(0, 1, 0), TEAL, 18, 0.4f);
        }
        for (LivingEntity a : SongRebuildKit.allies(p, c, r)) if (!BalanceLaw.isBoss(a)) BalanceLaw.heal(a, a.getMaxHealth() * 0.10f);
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_FX3, c, c.add(0, 1, 0), TEAL, 30, (float) r / 3f * 1.3f);
        return true;
    }

    private static boolean mantle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        long now = p.level().getGameTime();
        tag.putLong("EmpowerUntil", now + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true));
        b.castCircle(p, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_FX2, p.position(), p.position().add(0, 1, 0), TEAL, 24, 1.6f);
        return true;
    }

    // ---------------------------------------------------------------- the appended pages
    private static void spawnWisps(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, int life, int flags) {
        SongRebuildKit.dismiss(p, PropKind.SOUL_1, -1, 0);
        float s = 0.7f * Math.min(1.3f, SongRebuildKit.size(p));
        for (int k = 0; k < 3; k++) {
            double an = k * Math.PI * 2 / 3;
            Vec3 at = p.position().add(Math.cos(an) * 2.2, 0.35, Math.sin(an) * 2.2);
            SongRebuildKit.spawn(p, PropKind.SOUL_1, at, p.getYRot(), s, life, k | flags, b, i, mode);
        }
    }

    private static boolean wisps(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        spawnWisps(b, i, p, mode, SongRebuildKit.span(400, p), 0);
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_FX2, p.position(), p.position().add(0, 1, 0), TEAL, 30, 3.5f);
        return true;
    }

    private static boolean bind(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null || !SongRebuildKit.foes(p, t.position(), 1.5).contains(t)) { GrimoireBook.fail(p, "No foe in your sights."); return false; }
        ServerLevel level = p.serverLevel();
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, true));
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
        for (int k = 0; k < 3; k++) {
            SpellRuntime.later(level, 20 * k, () -> {
                if (!t.isAlive() || !p.isAlive()) return;
                strike(b, i, p, t, mode, 3f, 2f);
                BalanceLaw.heal(p, p.getMaxHealth() * 0.02f);
                VfxSpawn.send(level, VfxShape.SOUL_FX1, t.getBoundingBox().getCenter(), p.getEyePosition(), TEAL, 14, 0.7f);
            });
        }
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.SOUL_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), TEAL, 14, 1.0f);
        return true;
    }

    private static boolean harvest(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        long now = p.level().getGameTime();
        p.getPersistentData().putLong(SoulRebuildProps.HARVEST_UNTIL, now + 200);
        p.getPersistentData().putInt(SoulRebuildProps.HARVEST_COUNT, 0);
        b.castCircle(p, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_FX2, p.position(), p.position().add(0, 1, 0), TEAL, 200, 4.5f);
        return true;
    }

    private static boolean ascension(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        spawnWisps(b, i, p, mode, 60, SoulRebuildProps.ASCEND);
        b.castCircle(p, 1.4f);
        VfxSpawn.send(level, VfxShape.SOUL_FX2, p.position(), p.position().add(0, 1, 0), TEAL, 40, 4f);
        SpellRuntime.later(level, 40, () -> {
            if (!p.isAlive()) return;
            for (MagicPropEntity w : SongRebuildKit.mine(p, PropKind.SOUL_1)) {
                if ((w.param() & SoulRebuildProps.ASCEND) == 0) continue;
                for (LivingEntity t : GrimoireBook.around(p, w.position(), 6)) strike(b, i, p, t, mode, 10f, 3f);
                w.expire();
            }
            BalanceLaw.heal(p, p.getMaxHealth() * 0.15f);
        });
        return true;
    }
}
