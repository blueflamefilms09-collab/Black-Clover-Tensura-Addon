package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Why: the damage rule of each boss weapon of the exclusive tombs (wave 78, its passive: {@link Wave78Passives}). It is the handler of wave 71 ({@link Wave71Events}) with this wave's table and flag prefix
 * and nothing else changed: the rules are {@link Wave71Rules}, the facts about a blow are read by {@link Wave71Events#facts}, the extra damage is added to the blow that is being dealt (never a second
 * {@code hurt} inside the first), a weapon only counts while it is in the main hand and its flag is switched on by the engine (attuned, not de-attuned), the blows of an act are the act's own (no passive
 * on top), and the whole handler runs under {@link SafeCast#tick}. Melee blows count; for the bow the arrow it loosed counts instead.
 */
final class Wave78Events {
    private Wave78Events() {}

    private static boolean registered;

    static synchronized void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(Wave78Events::onIncoming);
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        if (Wave71Acts.running()) return;
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer atk)) return;
        SafeCast.tick("relic.w78.events", () -> apply(e, atk, victim, src));
    }

    private static void apply(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, DamageSource src) {
        String id = Wave20Util.heldId(atk);
        Wave71Rules.Rule rule = Wave78Passives.RULES.get(id);
        if (rule == null || !RelicFlags.has(atk, "w78_" + id)) return;
        boolean ranged = Wave71Rules.isRangedId(id);
        boolean melee = src.getDirectEntity() == atk && !src.is(DamageTypeTags.IS_PROJECTILE);
        boolean arrow = src.getDirectEntity() instanceof AbstractArrow;
        if (ranged ? !arrow : !melee) return;
        ServerLevel level = atk.serverLevel();
        Wave71Rules.Facts f = Wave71Events.facts(atk, victim, level);
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
}
