package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.WaterProps;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Water Magic, rebuilt after Noelle Silva (the Sea Dragon's magic and the Valkyrie Dress). The six pages that existed keep their ids, order and cost tier (saves and mastery depend on
 * them) and are re-implemented on the new bodies: the Sea Dragon is a real model that flies ({@link WaterProps#dragon}), the javelin and the tide lance are models, every page draws the
 * rebuilt Water VFX ({@code WATERFX_*}), and every successful cast puts the Water armour on the caster for 5 s ({@link WaterArmour}). Four pages are appended. Damage always goes
 * through {@code GrimoireBook.hurt} (the balance law: EP scale, boss caps), ranges and counts scale with {@code GrimoireBook.size} (the caster's EP and the grimoire), the knockbacks
 * are plain pushes and no hard control is used (the slows are Slowness, which a boss shrugs off as in the rest of the mod).
 * <pre>
 *   id                 name                       what it does
 *   waterball          Sea Dragon's Waterball     (kept) a Tensura water ball; the armour
 *   roar               Sea Dragon's Roar          a sea dragon flies to the target (or the aim point), 20 damage and a knock in 2.5 blocks where it crashes
 *   cradle             Sea Dragon's Cradle        a whirling sphere for 8 s: projectiles crawl, allies inside regenerate
 *   aqua_javelin       Aqua Javelin               a pressure lance through everything in 26 blocks
 *   sea_dragons_nest   Sea Dragon's Nest          a dome of water for 10 s: enemy spells swallowed, foes slowed and battered, allies healed
 *   valkyrie_dress     Valkyrie Dress             30 s of the Valkyrie armour (the full set, style 1) and its buffs
 *   sea_dragons_wrath  Sea Dragon's Wrath         (new) three sea dragons in a fan
 *   tide_lance         Valkyrie Tide Lance        (new) a great lance of water, 22 damage through everything it meets, pushes them back
 *   scale_mail         Sea Dragon's Scale Mail    (new) absorption hearts and resistance for 15 s, allies near you are healed
 *   torrent            Sea Dragon's Torrent       (new) a cone of rushing water: damage, a hard push and Slowness
 * </pre>
 */
public final class WaterArts {
    private WaterArts() {}

    static final int STYLE_VALKYRIE = 1;

    private static float epMod(ServerPlayer p) { return 1f + EnergyBridge.modifier(p) * 0.1f; }

    // ---------------------------------------------------------------- the six that existed
    /** Sea Dragon's Roar: the dragon is a prop flying along the line; the bolt that does the hitting flies with it. */
    static boolean roar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        LivingEntity target = GrimoireBook.target(p, 32);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.4f);
        Vec3 end = target != null ? target.position().add(0, target.getBbHeight() / 2, 0) : GrimoireBook.aim(p, 32);
        int flight = Math.max(10, Math.min(70, (int) (start.distanceTo(end) / 0.9) + 6));
        WaterProps.dragon(p.serverLevel(), p, start.add(dir.scale(1.2)).add(0, -0.3, 0), end, flight, 1.1f * s);
        b.vfx(p, VfxShape.WATERFX_CAST, start, end, flight, 1.2f * s);
        SpellRuntime.bolt(p, start, dir.scale(0.9), 1.4 * s, 70, false, target, (bolt, t) -> { }, (bolt, at) -> {
            for (LivingEntity t : GrimoireBook.around(p, at, 2.5 * s)) {
                b.hurt(i, p, t, mode, 20f);
                t.clearFire();
                Vec3 away = t.position().subtract(at).normalize();
                t.knockback(2.5, -away.x, -away.z);
            }
            b.vfx(p, VfxShape.WATERFX_BURST, at, at, 26, 1.4f * s);
        });
        return true;
    }

    /** Sea Dragon's Cradle: a whirling water sphere round you for 8 s; enemy projectiles crawl through it, you and your allies regenerate. */
    static boolean cradle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 4 * GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WATERFX_ZONE, p, p.position(), b.color, 160, r);
        SpellRuntime.zone(p.serverLevel(), 160, 1, age -> {
            Vec3 c = p.position();
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r))) {
                if (pr.getOwner() != p) { pr.setDeltaMovement(pr.getDeltaMovement().scale(0.6)); pr.hurtMarked = true; }
            }
            if (age % 20 == 0) {
                for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r))) {
                    if (ally == p || ally.isAlliedTo(p)) BalanceLaw.heal(ally, 2f);
                }
            }
        });
        return true;
    }

    /** Aqua Javelin: the lance is a model flying 26 blocks in 10 ticks; everything on its line is pierced. */
    static boolean aquaJavelin(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.8f);
        WaterProps.javelin(p.serverLevel(), p, start.add(dir.scale(1.0)).add(0, -0.2, 0), dir, 2.6, 10, 1.0f * s, 0);
        b.vfx(p, VfxShape.WATERFX_CAST, start, start.add(dir.scale(26)), 12, 0.9f * s);
        SpellRuntime.bolt(p, start, dir.scale(2.6), 0.6 * s, 10, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 12f);
            t.clearFire();
            t.knockback(0.7, -dir.x, -dir.z);
            b.vfx(p, VfxShape.WATERFX_BURST, t.position().add(0, t.getBbHeight() / 2, 0), t.position(), 18, 0.5f);
        }, null);
        return true;
    }

    /** Sea Dragon's Nest: a dome of water over you for 10 s. */
    static boolean nest(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 6 * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.WATERFX_ZONE, c, c.add(0, 1, 0), 200, r);
        SpellRuntime.zone(p.serverLevel(), 200, 5, age -> {
            AABB box = new AABB(c, c).inflate(r);
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                if (pr.position().distanceToSqr(c) <= r * r) pr.discard();
            }
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                if (age % 20 == 0) b.hurt(i, p, t, mode, 3f);
            }
            if (age % 20 == 0) {
                for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, box)) {
                    if ((ally == p || ally.isAlliedTo(p)) && ally.position().distanceToSqr(c) <= r * r) BalanceLaw.heal(ally, 1.5f);
                }
            }
        });
        return true;
    }

    /** Valkyrie Dress: the full Valkyrie armour (style 1) and the buffs of the old page, 30 s. */
    static boolean valkyrieDress(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.WATERFX_BURST, p.position(), p.position().add(0, 1, 0), 26, 1.6f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        p.clearFire();
        return true;
    }

    // ---------------------------------------------------------------- appended
    /** Sea Dragon's Wrath: three sea dragons fan out from you; each hits what it reaches for 14 and throws it back. */
    static boolean wrath(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 1.5f);
        for (int k = -1; k <= 1; k++) {
            double a = Math.toRadians(k * 18.0);
            Vec3 dir = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y, look.x * Math.sin(a) + look.z * Math.cos(a)).normalize();
            int delay = Math.abs(k) * 3;
            SpellRuntime.later(p.serverLevel(), delay, () -> {
                Vec3 end = start.add(dir.scale(22));
                WaterProps.dragon(p.serverLevel(), p, start.add(dir.scale(1.2)).add(0, -0.3, 0), end, 24, 0.9f * s);
                b.vfx(p, VfxShape.WATERFX_CAST, start, end, 24, 1.0f * s);
                SpellRuntime.bolt(p, start, dir.scale(0.95), 1.1 * s, 24, true, null, (bolt, t) -> {
                    b.hurt(i, p, t, mode, 14f);
                    t.clearFire();
                    t.knockback(1.6, -dir.x, -dir.z);
                }, (bolt, at) -> b.vfx(p, VfxShape.WATERFX_BURST, at, at, 24, 1.1f * s));
            });
        }
        return true;
    }

    /** Valkyrie Tide Lance: the big lance (the javelin model at 1.4x): 22 damage through everything on its line, pushing it back. */
    static boolean tideLance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.2f);
        WaterProps.javelin(p.serverLevel(), p, start.add(dir.scale(1.2)).add(0, -0.2, 0), dir, 2.4, 14, 1.0f * s, 1);
        b.vfx(p, VfxShape.WATERFX_CAST, start, start.add(dir.scale(33)), 16, 1.3f * s);
        SpellRuntime.bolt(p, start, dir.scale(2.4), 0.9 * s, 14, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 22f);
            t.clearFire();
            t.knockback(1.8, -dir.x, -dir.z);
            b.vfx(p, VfxShape.WATERFX_BURST, t.position().add(0, t.getBbHeight() / 2, 0), t.position(), 20, 0.8f);
        }, (bolt, at) -> b.vfx(p, VfxShape.WATERFX_BURST, at, at, 24, 1.3f * s));
        return true;
    }

    /** Sea Dragon's Scale Mail: scales of water close over you: absorption hearts and resistance for 15 s; allies within 6 blocks are healed. */
    static boolean scaleMail(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 1.1f);
        int amp = 1 + Math.min(3, EnergyBridge.modifier(p));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, amp));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1));
        p.clearFire();
        for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6 * s))) {
            if (ally != p && ally.isAlliedTo(p)) BalanceLaw.heal(ally, 3f * epMod(p));
        }
        b.vfx(p, VfxShape.WATERFX_ZONE, p.position(), p.position().add(0, 1, 0), 60, 2.4f);
        return true;
    }

    /** Sea Dragon's Torrent: a cone of rushing water 9 blocks long: 10 damage, a hard push and Slowness 3 s. */
    static boolean torrent(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), flat = new Vec3(look.x, 0, look.z).normalize();
        b.castCircle(p, 1.3f);
        for (int k = 1; k <= 3; k++) {
            Vec3 at = p.position().add(flat.scale(2.5 * k));
            int delay = k * 3;
            SpellRuntime.later(p.serverLevel(), delay, () -> b.vfx(p, VfxShape.WATERFX_BURST, at, at, 22, 1.0f * s));
        }
        b.vfx(p, VfxShape.WATERFX_CAST, eye, eye.add(look.scale(9 * s)), 16, 1.1f * s);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 9 * s)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.35) continue;
            b.hurt(i, p, t, mode, 10f);
            t.clearFire();
            t.knockback(2.2, -flat.x, -flat.z);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }
        return true;
    }
}
