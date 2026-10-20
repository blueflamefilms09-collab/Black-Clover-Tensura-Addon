package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.LightningProps;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.93.0 Lightning Magic, rebuilt after Luck Voltia (Raging Lightning, Thunder Fiend, Pulsaranta, God of Lightning Rising Salim, Thunderbird Cavalry, Black Lightning Battle Fiend). The
 * eight pages that existed keep their ids, order and cost tier and are re-implemented with their numbers kept (WikiSpells and the old static methods are untouched) on the new bodies:
 * the thunderbird and the thunder lance are models ({@link LightningProps}), every page draws the rebuilt Lightning VFX ({@code LIGHTNINGFX_*}, bolts re-rolled every two ticks), and every
 * successful cast puts the Lightning armour on the caster for 5 s ({@link WaterArmour}); the Fiend pages keep it for their 30 s (Black Lightning wears the black variant, style 1).
 * The Thunder Fiend / Black Lightning arcs and Thunder Boots' stored charge stay in LightningBook.onDamageEntity (FiendUntil, FiendBlack, ChargeUntil). Three pages are appended.
 * <pre>
 *   id                           what it does (numbers as before)
 *   chain                        a bolt to the target for 9, then 6 to two more within 5 blocks of the last
 *   thunder_boots                dash up to 6 blocks, your next blow in 5 s carries a stored charge (+8, scaled)
 *   thunder_lance                a lance of thunder (model), 13 damage, a short push
 *   thunder_fiend                30 s of lightning on your body: Speed III, Haste II, blows arc to a second foe
 *   pulsaranta                   three balls of lightning that hunt the target, 7 each in 2 blocks
 *   rising_salim                 a pillar of lightning where you aim: 22 damage in 4.5 blocks and a throw upward
 *   thunderbird_cavalry          eight thunderbirds fly from you in a fan, 5 damage each where they strike
 *   black_lightning_battle_fiend 30 s, Speed IV, Strength II, Haste III, blows arc to two more foes; the black armour
 *   raging_lightning             (new) Raging Lightning: 30 s of the armour, Speed III and Strength II, a bolt strikes the nearest foe within 8 blocks every two seconds
 *   thunder_drum                 (new) Raijin's drums: for 5 s bolts strike up to three foes in the zone every half second (6 each), 25 percent paralyse (shared control gate, never a boss)
 *   lightning_surge              (new) a chain dash through up to four foes within 12 blocks, 9 each
 * </pre>
 */
public final class LightningArts {
    private LightningArts() {}

    private static boolean ally(ServerPlayer p, LivingEntity t) { return t == p || t.isAlliedTo(p); }

    // ---------------------------------------------------------------- the eight that existed
    static boolean chain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No target for the lightning."); return false; }
        Set<LivingEntity> hit = new HashSet<>();
        Vec3 from = p.getEyePosition().add(0, -0.3, 0);
        LivingEntity cur = t;
        b.castCircle(p, 0.8f);
        for (int jump = 0; jump < 3 && cur != null; jump++) {
            hit.add(cur);
            b.hurt(i, p, cur, mode, jump == 0 ? 9f : 6f);
            Vec3 to = cur.getBoundingBox().getCenter();
            int delay = jump * 2;
            Vec3 a = from, c = to;
            SpellRuntime.later(p.serverLevel(), delay, () -> {
                b.vfx(p, VfxShape.LIGHTNINGFX_CAST, a, c, 12, 0.7f);
                b.vfx(p, VfxShape.LIGHTNINGFX_BURST, c, c, 14, 0.5f);
            });
            from = to;
            LivingEntity next = null;
            for (LivingEntity e : GrimoireBook.around(p, to, 5)) if (!hit.contains(e) && (next == null || e.distanceToSqr(cur) < next.distanceToSqr(cur))) next = e;
            cur = next;
        }
        return true;
    }

    static boolean boots(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), dir = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 dest = null;
        for (double d = 6; d >= 1; d -= 0.5) {
            Vec3 c = from.add(dir.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to dash."); return false; }
        b.castCircle(p, 0.6f);
        p.teleportTo(dest.x, dest.y, dest.z);
        b.vfx(p, VfxShape.LIGHTNINGFX_CAST, from.add(0, 1, 0), dest.add(0, 1, 0), 10, 0.8f);
        b.vfx(p, VfxShape.LIGHTNINGFX_BURST, from.add(0, 0.2, 0), from.add(0, 0.2, 0), 14, 0.6f);
        i.getOrCreateTag().putLong("ChargeUntil", p.level().getGameTime() + 100);
        return true;
    }

    /** Thunder Lance: the lance is a model flying along the line; the bolt that hits is the old 13 damage and a short push. */
    static boolean thunderLance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1f);
        LightningProps.lance(p.serverLevel(), p, start.add(dir.scale(1.1)).add(0, -0.25, 0), dir, 2.4, 12, 0.9f * s, 0);
        b.vfx(p, VfxShape.LIGHTNINGFX_CAST, start, start.add(dir.scale(28)), 12, 0.8f * s);
        SpellRuntime.bolt(p, start, dir.scale(2.4), 0.7 * s, 12, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 13f);
            t.knockback(0.6, -dir.x, -dir.z);
        }, (bolt, at) -> b.vfx(p, VfxShape.LIGHTNINGFX_BURST, at, at, 18, 0.9f * s));
        return true;
    }

    static boolean fiend(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, boolean black) {
        b.castCircle(p, black ? 1.4f : 1f);
        b.vfx(p, VfxShape.LIGHTNINGFX_BURST, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 22, black ? 1.4f : 1.0f);
        i.getOrCreateTag().putLong("FiendUntil", p.level().getGameTime() + 600);
        i.getOrCreateTag().putBoolean("FiendBlack", black);
        i.markDirty();
        if (black) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 3));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 2));
        } else {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 1));
        }
        return true;
    }

    static boolean thunderFiend(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) { return fiend(b, i, p, false); }

    static boolean blackLightningFiend(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) { return fiend(b, i, p, true); }

    static boolean pulsaranta(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        b.castCircle(p, 0.8f);
        for (int k = 0; k < 3; k++) {
            SpellRuntime.later(p.serverLevel(), k * 4, () -> {
                Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
                SpellRuntime.bolt(p, start, dir.scale(1.3), 0.7, 30, false, t, (bolt, hit) -> { }, (bolt, at) -> {
                    for (LivingEntity e : GrimoireBook.around(p, at, 2)) b.hurt(i, p, e, mode, 7f);
                    b.vfx(p, VfxShape.LIGHTNINGFX_BURST, at, at, 16, 0.7f);
                });
                b.vfx(p, VfxShape.LIGHTNINGFX_CAST, start, t != null ? t.getBoundingBox().getCenter() : start.add(dir.scale(16)), 10, 0.5f);
            });
        }
        return true;
    }

    static boolean risingSalim(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 26);
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.LIGHTNINGFX_ZONE, c, c.add(0, 1, 0), 40, 4.5f * s);
        b.vfx(p, VfxShape.LIGHTNINGFX_CAST, c.add(0, 22, 0), c, 14, 2.0f * s);
        SpellRuntime.later(p.serverLevel(), 4, () -> {
            for (LivingEntity t : GrimoireBook.around(p, c, 4.5 * s)) {
                b.hurt(i, p, t, mode, 22f);
                t.setDeltaMovement(t.getDeltaMovement().add(0, 0.8, 0));
                t.hurtMarked = true;
            }
            b.vfx(p, VfxShape.LIGHTNINGFX_BURST, c, c, 26, 2.0f * s);
        });
        return true;
    }

    /** Thunderbird Cavalry: eight thunderbirds fly from you in a fan (toward the target when there is one), 5 damage each where they strike. */
    static boolean thunderbirdCavalry(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        LivingEntity target = GrimoireBook.target(p, 28);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        Vec3 aim = target != null ? target.getBoundingBox().getCenter().subtract(eye).normalize() : look;
        b.castCircle(p, 1.3f);
        for (int k = 0; k < 8; k++) {
            double spread = Math.toRadians((k - 3.5) * 5.0), lift = Math.toRadians(((k % 2) * 2 - 1) * 4.0);
            Vec3 dir = new Vec3(aim.x * Math.cos(spread) - aim.z * Math.sin(spread), aim.y + Math.sin(lift) * 0.5, aim.x * Math.sin(spread) + aim.z * Math.cos(spread)).normalize();
            SpellRuntime.later(p.serverLevel(), k * 2, () -> {
                Vec3 start = p.getEyePosition().add(dir.scale(0.8)).add(0, -0.3, 0);
                LightningProps.bird(p.serverLevel(), p, start, dir, 1.5, 26, 0.7f * s);
                b.vfx(p, VfxShape.LIGHTNINGFX_CAST, start, start.add(dir.scale(14)), 10, 0.5f);
                SpellRuntime.bolt(p, start, dir.scale(1.5), 0.9 * s, 26, false, null, (bolt, t) -> b.hurt(i, p, t, mode, 5f),
                        (bolt, at) -> b.vfx(p, VfxShape.LIGHTNINGFX_BURST, at, at, 14, 0.6f));
            });
        }
        return true;
    }

    // ---------------------------------------------------------------- appended
    /** Raging Lightning: 30 s of the armour; Speed III and Strength II; every two seconds a bolt strikes the nearest foe within 8 blocks (5 damage, scaled by EP). */
    static boolean ragingLightning(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.LIGHTNINGFX_BURST, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 22, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        SpellRuntime.zone(p.serverLevel(), 600, 40, age -> {
            if (!p.isAlive()) return;
            LivingEntity best = null;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 8)) {
                if (ally(p, t)) continue;
                if (best == null || t.distanceToSqr(p) < best.distanceToSqr(p)) best = t;
            }
            if (best == null) return;
            b.hurt(i, p, best, mode, 5f);
            Vec3 to = best.getBoundingBox().getCenter();
            b.vfx(p, VfxShape.LIGHTNINGFX_CAST, to.add(0, 7, 0), to, 10, 0.8f);
            b.vfx(p, VfxShape.LIGHTNINGFX_BURST, to, to, 14, 0.5f);
        });
        return true;
    }

    /** Raijin's Thunder Drums: for 5 s, every half second up to three foes in the zone are struck (6 damage); a quarter of the strikes paralyse for 1.5 s (never a boss). */
    static boolean thunderDrum(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 5 * GrimoireBook.size(i, p);
        boolean control = GrimoireBook.control(p);
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.LIGHTNINGFX_ZONE, c, c.add(0, 1, 0), 100, r);
        SpellRuntime.zone(p.serverLevel(), 100, 10, age -> {
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (ally(p, t) || n++ >= 3) continue;
                b.hurt(i, p, t, mode, 6f);
                Vec3 to = t.getBoundingBox().getCenter();
                b.vfx(p, VfxShape.LIGHTNINGFX_CAST, to.add(0, 8, 0), to, 8, 0.7f);
                if (control && !BalanceLaw.isBoss(t) && p.getRandom().nextFloat() < 0.25f) EnergyBridge.effect(t, "paralysis", BalanceLaw.controlTicks(t, 30), 0);
            }
        });
        return true;
    }

    /** Lightning Surge: a chain dash through up to four foes within 12 blocks (nearest first), 9 damage each, a hop every two ticks. */
    static boolean lightningSurge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<LivingEntity> near = GrimoireBook.around(p, p.position(), 12).stream().filter(t -> !ally(p, t)).sorted((x, y) -> Double.compare(x.distanceToSqr(p), y.distanceToSqr(p))).limit(4).toList();
        if (near.isEmpty()) { GrimoireBook.fail(p, "No one close enough to surge through."); return false; }
        b.castCircle(p, 1f);
        int k = 0;
        for (LivingEntity t : near) {
            SpellRuntime.later(p.serverLevel(), k * 2, () -> {
                if (!p.isAlive() || !t.isAlive()) return;
                Vec3 from = p.position().add(0, 1, 0), to = t.position();
                Vec3 behind = to.add(to.subtract(p.position()).normalize().scale(1.2));
                if (p.level().noCollision(p, p.getBoundingBox().move(behind.subtract(p.position())))) p.teleportTo(behind.x, behind.y, behind.z);
                b.hurt(i, p, t, mode, 9f);
                b.vfx(p, VfxShape.LIGHTNINGFX_CAST, from, t.getBoundingBox().getCenter(), 8, 0.7f);
                b.vfx(p, VfxShape.LIGHTNINGFX_BURST, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 12, 0.5f);
            });
            k++;
        }
        i.getOrCreateTag().putLong("ChargeUntil", p.level().getGameTime() + 100);
        return true;
    }
}
