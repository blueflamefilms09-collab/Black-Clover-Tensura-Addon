package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ext.ExclusiveExt;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.prop.SoulCorpseRebuildProps;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * 1.93.0 Soul Corpse Magic, rebuilt as a puppeteer's craft: threads, stitched puppets, hands out of the grave. The five generic pages keep their ids and their order
 * (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard), Rades's five pages (SoulCorpseExt: Reanimation, Jimmy, Alfredo, Carl, True Soul Corpse
 * Magic, with their lock lines in book.CharacterSpells) follow as before, and three pages are appended (corpse_marionette, corpse_swap, corpse_legion): 13 family pages of the
 * 24 allowed. Every successful cast puts the 5 second Marionette Shell on the caster (Aura.SOUL_CORPSE, client.aura.SoulCorpseAura). The puppets and the hands are props with real
 * models (client.prop.SoulCorpseRebuildPropPainter); the existing Soul Corpses of the corpse package (Reanimation, the named puppets, the pacts) are untouched.
 * <pre>
 *   attribute_bolt    Marionette Thread  a coffin nail on a thread (SOUL_CORPSE_1 param 2): raw 7 on the first foe it meets, Slowness II and Weakness for 3 s, and a pull of 1 block toward you
 *   attribute_cut     Puppet Strings     a line of 14 blocks: raw 5 on everything along it, Slowness, and every foe hit is pulled 1.5 blocks toward you
 *   attribute_field   Grave Field        a field of radius 5 for 6 s: five hands (SOUL_CORPSE_2) rise in it and close on foes within 1.8 blocks every 1.2 s (raw 3.5, Slowness III)
 *   attribute_domain  Necropolis         a burst of radius 8: raw 11 and Wither for 4 s, and two marionettes rise to fight for 10 s
 *   attribute_guard   Corpse Plate       Resistance, Absorption, a melee bonus and thorns for 9 s; the armour for as long
 *   corpse_marionette Marionette         a stitched puppet (SOUL_CORPSE_1) fights for you for 25 s: it walks to the nearest foe and strikes every second for raw 6 (cap 4 puppets)
 *   corpse_swap       Corpse Swap        a decoy puppet takes your place and you step 6 blocks back; mobs that hunted you lose their target; it bursts after 3 s (raw 8, shove)
 *   corpse_legion     Corpse Legion      four marionettes rise round you for 20 s and the hands of the grave close under every foe within 8 blocks (raw 3.5, Slowness)
 * </pre>
 * No page controls hard (no beginControl); bosses take half of the raw value and only Slowness I.
 */
public final class SoulCorpseBook extends ElementBook {
    private static final int VIOLET = 0xFF6C7899;

    public SoulCorpseBook() {
        super(MagicType.SOUL_CORPSE, WikiBooks.color(MagicType.SOUL_CORPSE), TensuraDamageTypes.MAGIC_GENERIC, familyList());
    }

    public static List<BookPage> familyList() {
        List<BookPage> l = new ArrayList<>(List.of(
                starter("attribute_bolt", "Soul Corpse: Marionette Thread", SoulCorpseBook::thread),
                mid("attribute_cut", "Soul Corpse: Puppet Strings", SoulCorpseBook::strings),
                zone("attribute_field", "Soul Corpse: Grave Field", SoulCorpseBook::grave),
                signature("attribute_domain", "Soul Corpse: Necropolis", SoulCorpseBook::necropolis).withCooldown(900),
                mid("attribute_guard", "Soul Corpse: Corpse Plate", SoulCorpseBook::plate)));
        l.addAll(ExclusiveExt.pages(MagicType.SOUL_CORPSE));
        l.add(mid("corpse_marionette", "Soul Corpse: Marionette", SoulCorpseBook::marionette).withCooldown(260));
        l.add(mid("corpse_swap", "Soul Corpse: Corpse Swap", SoulCorpseBook::swap).withCooldown(300));
        l.add(signature("corpse_legion", "Soul Corpse: Corpse Legion", SoulCorpseBook::legion).withCooldown(1800).withAnim("signature"));
        l.addAll(com.newuniverse.nusmp.book.ext.SoulCorpseExt.latePages());      // Rades's No.3 David and No.0 Michael Caesal: appended last so no mode number (and no unlock bit) of an older page moves
        return List.copyOf(l);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SOUL_CORPSE_FX1, p, p.getEyePosition(), VIOLET, 12, 0.6f * power);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) {
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_CORPSE_FX3, at, at.add(0, 1, 0), VIOLET, 20, power);
    }

    private static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, BalanceLaw.isBoss(t) ? 0 : amp, false, true));
    }

    /** Pulls a foe {@code blocks} toward the caster on the ground plane (knockback takes the direction it pushes AGAINST). */
    private static void pull(ServerPlayer p, LivingEntity t, double blocks) {
        Vec3 d = p.position().subtract(t.position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) return;
        d = d.normalize();
        t.knockback(blocks * 0.55, d.x, d.z);
        t.hurtMarked = true;
    }

    // ---------------------------------------------------------------- the five generic pages
    private static boolean thread(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.25, 0);
        b.castCircle(p, 1f);
        SongRebuildKit.projectile(p, PropKind.SOUL_CORPSE_1, SoulCorpseRebuildProps.NAIL, start, p.getViewVector(1f), 1.9, 0.5 * s, 22, false,
                (bolt, t) -> { },
                (bolt, at) -> {
                    for (LivingEntity t : GrimoireBook.around(p, at, 1.4 * s)) {
                        b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 7f));
                        slow(t, 60, 1);
                        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
                        pull(p, t, 1.0);
                    }
                    b.impact(p, at, 0.6f);
                }, 0.9f * s);
        return true;
    }

    private static boolean strings(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition().add(0, -0.2, 0), end = a.add(p.getViewVector(1f).scale(14 * s));
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.1)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 5f));
            slow(t, 50, 1);
            pull(p, t, 1.5);
        }
        b.castCircle(p, 0.8f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_CORPSE_FX1, a, end, VIOLET, 16, 1.4f);
        return true;
    }

    private static boolean grave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 5 * s;
        int ticks = 120;
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.SOUL_CORPSE_FX2, c, c.add(0, 1, 0), VIOLET, ticks, (float) r);
        for (int k = 0; k < 5; k++) {
            double an = k * Math.PI * 2 / 5 + 0.3, rr = r * (0.35 + 0.5 * ((k * 37 % 10) / 10.0));
            Vec3 at = c.add(Math.cos(an) * rr, 0, Math.sin(an) * rr);
            SongRebuildKit.spawn(p, PropKind.SOUL_CORPSE_2, at, (float) (k * 70), 0.9f, ticks, 0, b, i, mode);
        }
        return true;
    }

    private static boolean necropolis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = p.position();
        double r = 8 * s;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            b.hurt(i, p, t, mode, SongRebuildKit.safe(t, 11f));
            t.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, true));
        }
        raise(b, i, p, mode, 2, 200);
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_CORPSE_FX3, c, c.add(0, 1, 0), VIOLET, 28, (float) r / 3f * 1.3f);
        return true;
    }

    private static boolean plate(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        long now = p.level().getGameTime();
        tag.putLong("EmpowerUntil", now + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 0, false, true));
        b.castCircle(p, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_CORPSE_FX2, p.position(), p.position().add(0, 1, 0), VIOLET, 24, 1.6f);
        return true;
    }

    // ---------------------------------------------------------------- the appended pages
    /** Raises {@code n} marionettes round the caster, keeping at most {@link SoulCorpseRules#PUPPET_CAP} puppets (the oldest end). */
    private static int raise(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, int n, int life) {
        // the owner's call: Soul Corpse Magic summons real zombies (owned, no sun burn, no drops; cap corpse.zombieCap). corpse.zombiesInsteadOfPuppets = false brings the old puppet props back
        if (com.newuniverse.nusmp.corpse.CorpseConfig.zombiesInsteadOfPuppets())
            return com.newuniverse.nusmp.corpse.SoulZombies.summon(p, n, life, SongRebuildKit.size(p), null).size();
        SongRebuildKit.dismiss(p, PropKind.SOUL_CORPSE_1, 0, Math.max(0, SoulCorpseRules.PUPPET_CAP - n));
        float s = 0.9f * Math.min(1.3f, SongRebuildKit.size(p));
        int made = 0;
        for (int k = 0; k < n; k++) {
            double an = k * Math.PI * 2 / n + p.getYRot() * 0.0174;
            Vec3 at = p.position().add(Math.cos(an) * 2.0, 0.1, Math.sin(an) * 2.0);
            if (SongRebuildKit.spawn(p, PropKind.SOUL_CORPSE_1, at, p.getYRot(), s, life, 0, b, i, mode) != null) made++;
        }
        return made;
    }

    private static boolean marionette(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        raise(b, i, p, mode, 1, SongRebuildKit.span(500, p));
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_CORPSE_FX3, p.position(), p.position().add(0, 1, 0), VIOLET, 20, 0.8f);
        return true;
    }

    private static boolean swap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.position();
        Vec3 back = p.getViewVector(1f).multiply(1, 0, 1);
        back = back.lengthSqr() < 1e-4 ? new Vec3(0, 0, -1) : back.normalize().scale(-1);
        Vec3 dest = null;
        for (double d = 6; d >= 2 && dest == null; d -= 1) {
            Vec3 c = from.add(back.scale(d));
            if (level.noCollision(p, p.getBoundingBox().move(c.subtract(from)))) dest = c;
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to step back."); return false; }
        SongRebuildKit.dismiss(p, PropKind.SOUL_CORPSE_1, SoulCorpseRebuildProps.DECOY, 0);
        SongRebuildKit.spawn(p, PropKind.SOUL_CORPSE_1, from, p.getYRot(), 0.95f, 60, SoulCorpseRebuildProps.DECOY, b, i, mode);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) if (m.getTarget() == p) m.setTarget(null);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.SOUL_CORPSE_FX3, from, from.add(0, 1, 0), VIOLET, 20, 0.9f);
        return true;
    }

    private static boolean legion(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        raise(b, i, p, mode, SoulCorpseRules.PUPPET_CAP, SongRebuildKit.span(400, p));
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 8 * GrimoireBook.size(i, p))) {
            if (n++ >= 6) break;
            SongRebuildKit.spawn(p, PropKind.SOUL_CORPSE_2, t.position(), p.getYRot(), 0.9f, 60, 0, b, i, mode);
        }
        b.castCircle(p, 1.5f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_CORPSE_FX3, p.position(), p.position().add(0, 1, 0), VIOLET, 36, 1.6f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SOUL_CORPSE_FX2, p.position(), p.position().add(0, 1, 0), VIOLET, 60, 8f);
        return true;
    }
}
