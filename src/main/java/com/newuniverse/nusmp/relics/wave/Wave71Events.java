package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Why: the damage rule of each weapon of wave 71 (its passive, {@link Wave71Passives}). The extra damage is added to the blow that is being dealt (never a second
 * {@code hurt} inside the first), a weapon only counts while it is in the main hand and its flag is switched on by the engine (attuned, not de-attuned), and the whole
 * handler runs under {@link SafeCast#tick}. Melee blows count; for a bow or a crossbow the arrow it loosed counts instead.
 */
final class Wave71Events {
    private Wave71Events() {}

    static void register() { NeoForge.EVENT_BUS.addListener(Wave71Events::onIncoming); }

    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer atk)) return;
        SafeCast.tick("relic.w71.events", () -> apply(e, atk, victim, src));
    }

    private static void apply(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, DamageSource src) {
        String id = Wave20Util.heldId(atk);
        Wave71Rules.Rule rule = Wave71Passives.RULES.get(id);
        if (rule == null || !RelicFlags.has(atk, "w71_" + id)) return;
        boolean ranged = Wave71Rules.isRangedId(id);
        boolean melee = src.getDirectEntity() == atk && !src.is(DamageTypeTags.IS_PROJECTILE);
        boolean arrow = src.getDirectEntity() instanceof AbstractArrow;
        if (ranged ? !arrow : !melee) return;
        ServerLevel level = atk.serverLevel();
        Wave71Rules.Facts f = facts(atk, victim, level);
        double roll = atk.getRandom().nextDouble();
        double factor = Wave71Rules.factor(rule, f, roll);
        if (factor != 1.0) e.setAmount((float) (e.getAmount() * factor));
        if (Wave71Rules.lands(rule, f, roll)) {
            if (!rule.effect().isEmpty() && rule.secs() > 0) Fx.addEffect(victim, rule.effect(), rule.secs() * 20, rule.amp());
            if (rule.ignite() > 0 && !victim.fireImmune()) victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), rule.ignite() * 20));
        }
        float back = Wave71Rules.steal(rule, e.getAmount());
        if (back > 0f) Fx.heal(atk, back);
    }

    /** A creature that is in the air by nature (a ghast, a phantom, a bee, a bat, a glider, a dragon) and is off the ground now. */
    private static boolean flying(LivingEntity v, String path) {
        boolean flier = v instanceof net.minecraft.world.entity.FlyingMob || v instanceof net.minecraft.world.entity.animal.FlyingAnimal
                || v instanceof net.minecraft.world.entity.ambient.Bat || v.isFallFlying() || v.isNoGravity() || Wave71Rules.isWyrm(path) || path.contains("wing") || path.contains("harpy");
        return flier && !v.onGround();
    }

    static Wave71Rules.Facts facts(ServerPlayer atk, LivingEntity v, ServerLevel level) {
        String path = Wave20Util.path(v);
        boolean player = v instanceof Player;
        boolean undead = v.getType().is(EntityTypeTags.UNDEAD);
        float hpFoe = v.getMaxHealth() <= 0f ? 1f : v.getHealth() / v.getMaxHealth();
        float hpSelf = atk.getMaxHealth() <= 0f ? 1f : atk.getHealth() / atk.getMaxHealth();
        boolean sky = level.canSeeSky(atk.blockPosition());
        boolean day = sky && level.isDay() && !level.isRaining();
        boolean night = level.isNight();
        return new Wave71Rules.Facts(hpFoe <= Wave71Rules.LOW_FOE, !player && (undead || Wave71Rules.isFiend(path)), !player && Wave71Rules.isWyrm(path),
                flying(v, path), v.isInWaterOrRain(), v.fireImmune() || v.isOnFire(),
                day, night, hpSelf >= 0.999f, hpSelf <= Wave71Rules.LOW_SELF, !player && BalanceLaw.isBoss(v), atk.isShiftKeyDown(), atk.fallDistance > 1.5f && !atk.onGround(),
                v.getMaxHealth() >= Wave71Rules.BIG_FOE_HEALTH);
    }
}
