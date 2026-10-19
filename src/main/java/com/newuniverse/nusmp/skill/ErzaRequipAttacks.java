package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.requip.Armour;
import com.newuniverse.nusmp.requip.RequipRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the special attacks of the armours of Requip: The Knight (Sword Rain, Telekinesis, Inferno Burst, Thunder Dash, Null Cut ...). Each {@link Armour.Attack} is data (kind, element, damage,
 * reach, cost, cooldown); this class has one branch per {@link Armour.Kind}. Every hit goes through {@link SkillDamage} (so the balance law scales it with the wearer's EP and the mastery, caps a hit on
 * a player and keeps allies and summons out), every cost is an {@link EnergyBridge#aura} spend (a share of the max Aura, else Magicule, so it scales with the wearer's EP), and the anti-magic
 * attacks go through the {@link Nullification} helpers (temporary on players, like all of them).
 */
final class ErzaRequipAttacks {
    private ErzaRequipAttacks() {}

    static String cdKey(Armour.Attack atk) { return "Cd." + atk.id(); }

    /** Ticks left on the attack's own cooldown (the two attacks of an armour do not share one). */
    static long cooldownLeft(ManasSkillInstance inst, ServerPlayer p, Armour.Attack atk) {
        return Math.max(0L, inst.getOrCreateTag().getLong(cdKey(atk)) - p.level().getGameTime());
    }

    /** Casts the attack. Returns true if it was cast (cost spent, cooldown started). */
    static boolean cast(ServerPlayer p, ManasSkillInstance inst, Armour a, Armour.Attack atk, double mastery) {
        long left = cooldownLeft(inst, p, atk);
        if (left > 0) { SkillUtil.fail(p, SkillMsg.t("erza.cooldown", "%s is recharging (%s s).", atk.name(), (left + 19) / 20)); return false; }
        LivingEntity aimed = null;
        if (atk.kind() == Armour.Kind.LIFT) {
            aimed = SkillTargets.lookFoe(p, atk.range());
            if (aimed == null) { SkillUtil.fail(p, SkillMsg.t("erza.no_target", "Telekinesis needs a target in sight.")); return false; }
        }
        if (!EnergyBridge.aura(p, atk.cost(), RequipRules.ATTACK_FLOOR)) { SkillUtil.fail(p, SkillMsg.t("erza.no_power", "Not enough magic power for %s.", atk.name())); return false; }

        ServerLevel level = p.serverLevel();
        int argb = 0xFF000000 | a.glow();
        switch (atk.kind()) {
            case CONE -> cone(p, a, atk, mastery, argb);
            case LINE -> line(p, a, atk, mastery, argb);
            case BURST -> burst(p, a, atk, mastery, argb);
            case DASH -> dash(p, a, atk, mastery, argb);
            case VOLLEY -> volley(p, a, atk, mastery, argb);
            case LIFT -> lift(p, a, atk, mastery, aimed, argb);
            case PULL -> pull(p, a, atk, mastery, argb);
            case FLURRY -> flurry(p, a, atk, mastery, argb);
            case GUARD -> guard(p, inst, a, atk, argb);
            case NULLIFY -> nullify(p, a, atk, mastery, argb);
            case FIELD -> field(p, a, atk, mastery, argb);
            case HEAL -> heal(p, a, atk, mastery, argb);
        }
        ErzaRequipFx.themed(level, a, p.position().add(0, 1.0, 0), 14, 0.5);
        float self = RequipRules.selfDamage(p.getHealth(), atk.selfDamage());
        if (self > 0f) {
            p.setHealth(p.getHealth() - self);
            level.broadcastEntityEvent(p, (byte) 2);
            SkillFx.sound(level, p.position(), SoundEvents.PLAYER_HURT, 0.8F, 0.7F);
        }
        inst.getOrCreateTag().putLong(cdKey(atk), level.getGameTime() + DMUtil.cooldown(atk.cooldown()));
        return true;
    }

    // ---------------------------------------------------------------- helpers
    private static VfxShape lineShape(Armour.Element e) {
        return switch (e) {
            case FIRE -> VfxShape.FLAME_TRAIL;
            case LIGHTNING -> VfxShape.LIGHTNING_SPEAR;
            case WATER -> VfxShape.WATER_JAVELIN;
            case WIND, DARK -> VfxShape.WIND_SLASH;
            case ANTI_MAGIC -> VfxShape.ANTI_MAGIC_SLASH;
            case LIGHT, NONE -> VfxShape.LIGHT_BLADE;
        };
    }

    private static VfxShape burstShape(Armour.Element e) {
        return switch (e) {
            case FIRE -> VfxShape.FIRE_BURST;
            case LIGHTNING -> VfxShape.LIGHTNING_GOD;
            case WATER -> VfxShape.WATER_BURST;
            case WIND -> VfxShape.WIND_RING;
            case DARK -> VfxShape.DEVIL_CIRCLE;
            case LIGHT -> VfxShape.LIGHT_FLARE;
            case ANTI_MAGIC -> VfxShape.NIHILITY_ZONE;
            case NONE -> VfxShape.MAGIC_CIRCLE_EXPLOSION;
        };
    }

    /** One hit of the attack on one target: damage through the balance law, then the element's effect. Returns the damage dealt. */
    private static float hit(ServerPlayer p, LivingEntity t, Armour a, Armour.Attack atk, double raw, double mastery) {
        double scaled = raw * RequipRules.waterBonus(a, p.isInWater());
        float dealt = SkillDamage.hit(p, t, scaled, mastery, atk.element() == Armour.Element.NONE ? SkillDamage.Kind.ATTACK : SkillDamage.Kind.MAGIC);
        if (dealt > 0f || t.isAlive()) element(p, t, atk.element());
        return dealt;
    }

    private static void element(ServerPlayer p, LivingEntity t, Armour.Element e) {
        switch (e) {
            case FIRE -> t.igniteForSeconds(5);
            case LIGHTNING -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                if (t.level() instanceof ServerLevel sl) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) { bolt.moveTo(t.getX(), t.getY(), t.getZ()); bolt.setVisualOnly(true); sl.addFreshEntity(bolt); }
                }
            }
            case WATER -> { t.clearFire(); t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); }
            case WIND -> { Vec3 away = t.position().subtract(p.position()).normalize(); t.setDeltaMovement(away.x * 0.6, 0.45, away.z * 0.6); t.hurtMarked = true; }
            case DARK -> { t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)); t.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0)); }
            case LIGHT -> t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            default -> { }
        }
    }

    private static void push(LivingEntity t, Vec3 from, double strength, double up) {
        Vec3 away = t.position().subtract(from);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1e-4) away = new Vec3(0, 0, 1);
        away = away.normalize();
        t.setDeltaMovement(away.x * strength, up, away.z * strength);
        t.hurtMarked = true;
    }

    private static void stripBuffs(LivingEntity t) {
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
    }

    // ---------------------------------------------------------------- kinds
    private static void cone(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity t : SkillTargets.foesInCone(p, atk.range(), 0.35)) {
            hit(p, t, a, atk, atk.raw(), mastery);
            push(t, p.position(), 0.7, 0.25);
        }
        SkillFx.shape(level, lineShape(atk.element() == Armour.Element.NONE ? Armour.Element.WIND : atk.element()), eye.add(look.scale(0.6)), eye.add(look.scale(atk.range())), argb, 10, 1.4f);
        SkillFx.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.9F);
    }

    private static void line(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.3, 0), to = DMUtil.lookPoint(p, atk.range());
        for (LivingEntity t : SkillTargets.foesAlong(p, from, to, 1.5)) {
            hit(p, t, a, atk, atk.raw(), mastery);
            if (atk.element() == Armour.Element.WATER) push(t, p.position(), 1.4, 0.35);
        }
        SkillFx.shape(level, lineShape(atk.element()), from, to, argb, 14, 1.6f);
        SkillFx.sound(level, p.position(), atk.element() == Armour.Element.FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.0F, 1.0F);
    }

    private static void burst(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        for (LivingEntity t : SkillTargets.foes(p, c, atk.range())) {
            hit(p, t, a, atk, atk.raw(), mastery);
            push(t, c, atk.element() == Armour.Element.WIND ? 0.9 : 0.8, atk.element() == Armour.Element.WIND ? 0.7 : 0.35);
        }
        SkillFx.shape(level, burstShape(atk.element()), c, c.add(0, 1, 0), argb, 24, (float) atk.range());
        SkillFx.ring(level, c, atk.range(), a.theme());
        SkillFx.sound(level, c, atk.selfDamage() > 0 ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.ANVIL_LAND, 1.2F, 0.8F);
    }

    private static void dash(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        boolean flies = a.flight() != Armour.Flight.NONE;
        Vec3 dir = new Vec3(look.x, flies ? look.y : look.y * 0.3, look.z);
        dir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 from = p.position().add(0, 1.0, 0), to = from.add(dir.scale(atk.range()));
        for (LivingEntity t : SkillTargets.foesAlong(p, from, to, 1.6)) hit(p, t, a, atk, atk.raw(), mastery);
        p.setDeltaMovement(dir.scale(Math.min(3.2, 0.5 + atk.range() * 0.16)).add(0, 0.1, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        if (a == Armour.CLEAR_HEART) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24, 3));
        SkillFx.shape(level, lineShape(atk.element()), from, to, argb, 12, 1.4f);
        SkillFx.sound(level, p.position(), SoundEvents.PHANTOM_FLAP, 1.0F, 1.6F);
    }

    private static void volley(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        List<LivingEntity> foes = SkillTargets.foes(p, p.position(), atk.range());
        int n = RequipRules.volleyTargets(foes.size(), atk.count(), atk.count());
        int[] share = RequipRules.shareSwords(atk.count(), n);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        boolean sky = atk.element() == Armour.Element.LIGHTNING;
        int beams = 0;
        for (int i = 0; i < n; i++) {
            LivingEntity t = foes.get(i);
            Vec3 at = t.getBoundingBox().getCenter();
            hit(p, t, a, atk, atk.raw() * share[i], mastery);
            for (int k = 0; k < Math.min(share[i], 3) && beams < 16; k++, beams++) {
                double ang = (beams * 2.4) % Mth.TWO_PI;
                Vec3 origin = sky ? at.add(Math.cos(ang) * 1.5, 12, Math.sin(ang) * 1.5)
                        : eye.subtract(look.scale(1.0)).add(right.scale(Math.cos(ang) * 2.2)).add(0, 0.8 + Math.sin(ang) * 1.0, 0);
                SkillFx.shape(level, sky ? VfxShape.LIGHTNING_SPEAR : VfxShape.LIGHT_BLADE, origin, at, argb, 12, 0.9f);
            }
        }
        if (n == 0) {                                          // no foes: the swords still fall, on the spot you look at
            Vec3 c = DMUtil.lookPoint(p, Math.min(atk.range(), 20));
            for (int k = 0; k < Math.min(atk.count(), 10); k++) {
                double ang = k * 2.4;
                SkillFx.shape(level, sky ? VfxShape.LIGHTNING_SPEAR : VfxShape.LIGHT_BLADE, c.add(Math.cos(ang) * 2.0, 10, Math.sin(ang) * 2.0), c.add(Math.cos(ang), 0, Math.sin(ang)), argb, 12, 0.9f);
            }
        }
        SkillFx.sound(level, p.position(), SoundEvents.TRIDENT_THUNDER.value(), 0.9F, 1.4F);
    }

    private static void lift(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, LivingEntity first, int argb) {
        ServerLevel level = p.serverLevel();
        List<LivingEntity> picked = new ArrayList<>();
        picked.add(first);
        for (LivingEntity t : SkillTargets.foes(p, first.position(), 5.0)) if (t != first && picked.size() < 5) picked.add(t);
        boolean control = BalanceLaw.beginControl(p);
        Vec3 from = p.getEyePosition();
        for (LivingEntity t : picked) {
            hit(p, t, a, atk, atk.raw(), mastery);
            if (control) {
                t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, BalanceLaw.controlTicks(t, 30), 1));
                t.setDeltaMovement(0, 0.9, 0);
                t.hurtMarked = true;
            }
            SkillFx.shape(level, VfxShape.THREAD_LINE, from, t.getBoundingBox().getCenter(), 0xFF000000 | a.glow(), 16, 1.0f);
        }
        SkillFx.sound(level, p.position(), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.5F);
    }

    private static void pull(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        boolean control = BalanceLaw.beginControl(p);
        for (LivingEntity t : SkillTargets.foes(p, c, atk.range())) {
            hit(p, t, a, atk, atk.raw(), mastery);
            if (control) {
                Vec3 in = c.subtract(t.position()).normalize();
                t.setDeltaMovement(in.x * 1.0, 0.25, in.z * 1.0);
                t.hurtMarked = true;
            }
        }
        SkillFx.shape(level, VfxShape.WATER_NEST, c, c.add(0, 1, 0), argb, 40, (float) atk.range());
        SkillFx.sound(level, c, SoundEvents.PLAYER_SPLASH, 1.2F, 0.7F);
    }

    private static void flurry(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = SkillTargets.lookFoe(p, atk.range());
        if (t == null) { List<LivingEntity> near = SkillTargets.foes(p, p.position(), atk.range()); if (!near.isEmpty()) t = near.get(0); }
        if (t == null) { SkillFx.shape(level, VfxShape.WIND_SLASH, p.getEyePosition(), DMUtil.lookPoint(p, atk.range()), argb, 8, 1.0f); return; }
        Vec3 c = t.getBoundingBox().getCenter();
        for (int i = 0; i < atk.count(); i++) {
            t.invulnerableTime = 0;                                 // every strike of the flurry lands
            hit(p, t, a, atk, atk.raw(), mastery);
            double ang = i * 2.1;
            SkillFx.shape(level, VfxShape.WIND_SLASH, c.add(Math.cos(ang) * 1.6, Math.sin(ang * 0.7) * 0.8, Math.sin(ang) * 1.6), c, argb, 6, 0.8f);
        }
        SkillFx.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.6F);
    }

    private static void guard(ServerPlayer p, ManasSkillInstance inst, Armour a, Armour.Attack atk, int argb) {
        ServerLevel level = p.serverLevel();
        int ticks = RequipRules.guardTicks(a);
        inst.getOrCreateTag().putLong(ErzaRequipSkill.GUARD_UNTIL, level.getGameTime() + ticks);
        if (a == Armour.HEART_KREUZ) {
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 2));
            for (LivingEntity t : SkillTargets.foes(p, p.position(), 4.0)) push(t, p.position(), 0.9, 0.3);
        } else {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        }
        SkillFx.ring(level, p.position(), 2.2, a.theme());
        SkillFx.shape(level, VfxShape.MIRROR_PANE, p.position().add(0, 1.0, 0).add(p.getLookAngle().scale(1.2)), p.getEyePosition().add(p.getLookAngle().scale(3)), argb, 40, 1.6f);
        SkillFx.sound(level, p.position(), SoundEvents.ANVIL_PLACE, 1.0F, 1.3F);
    }

    private static void nullify(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        LivingEntity look = SkillTargets.lookFoe(p, atk.range());
        Vec3 c = look != null ? look.getBoundingBox().getCenter() : DMUtil.lookPoint(p, atk.range());
        Nullification.shatterBarriers(level, c, 6.0, p);
        for (LivingEntity t : SkillTargets.foes(p, c, 4.0)) {
            double bonus = 1.0 + RequipRules.antiCasterBonus(Nullification.maxEP(t));
            hit(p, t, a, atk, atk.raw() * bonus, mastery);
            stripBuffs(t);
            Nullification.interfere(t, 8, 0.8f, 0.5f);
            Nullification.bleed(t, 0.15);
        }
        SkillFx.shape(level, VfxShape.ANTI_MAGIC_SLASH, p.getEyePosition(), c, argb, 16, 1.6f);
        SkillFx.sound(level, c, SoundEvents.GLASS_BREAK, 1.2F, 0.6F);
    }

    private static void field(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        Nullification.shatterBarriers(level, c, atk.range(), p);
        for (LivingEntity t : SkillTargets.foes(p, c, atk.range())) {
            hit(p, t, a, atk, atk.raw(), mastery);
            stripBuffs(t);
            Nullification.jamAll(t, 6, 0.4f);
            Nullification.bleed(t, 0.10);
        }
        SkillFx.shape(level, VfxShape.NIHILITY_ZONE, c, c.add(0, 1, 0), argb, 80, (float) atk.range());
        SkillFx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 1.2F, 0.8F);
    }

    private static void heal(ServerPlayer p, Armour a, Armour.Attack atk, double mastery, int argb) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        float amount = (float) (atk.raw() * Math.sqrt(BalanceLaw.epScale(p)) * (0.85 + 0.3 * mastery));
        for (LivingEntity ally : SkillTargets.alliesAround(p, c, atk.range())) {
            BalanceLaw.heal(ally, amount);
            SkillUtil.cleanse(ally);
        }
        for (LivingEntity t : SkillTargets.foes(p, c, atk.range())) {
            if (t.isInvertedHealAndHarm()) hit(p, t, a, atk, atk.raw() * 2.0, mastery);
        }
        SkillFx.shape(level, VfxShape.LIGHT_FLARE, c.add(0, 1, 0), c.add(0, 2, 0), argb, 24, (float) atk.range() * 0.5f);
        SkillFx.ring(level, c, atk.range() * 0.6, a.theme());
        SkillFx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.0F);
    }
}
