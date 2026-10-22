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
 * Why: the damage rule of each weapon of wave 102 (its passive, {@link Wave102Passives}), the same mechanism as wave 71's ({@link Wave71Events}, the arithmetic is {@link Wave71Rules}).
 * The extra damage is added to the blow that is being dealt (never a second {@code hurt} inside the first), a weapon only counts while it is in the main hand and its flag is switched on by
 * the engine (attuned where the grade needs it, not de-attuned), the blows of an act are the act's own (no passive on top), and the whole handler runs under {@link SafeCast#tick}. Server
 * side only; melee blows count, and for the bow the arrow it loosed counts instead.
 */
final class Wave102Events {
    private Wave102Events() {}

    static void register() { NeoForge.EVENT_BUS.addListener(Wave102Events::onIncoming); }

    /** 1.112.0 (lag audit): the cost of this wave's damage handler (off by default: a flag read). */
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE = com.newuniverse.nusmp.perf.PerfProbe.section("s.relic.w102");

    /** The flag key of each weapon with a rule, built once (a string concatenation per swing before). */
    private static final java.util.Map<String, String> FLAGS = new java.util.HashMap<>();

    static {
        for (String id : Wave102Passives.RULES.keySet()) FLAGS.put(id, "w102_" + id);
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onIncoming0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE, t0);
        }
    }

    private static void onIncoming0(LivingIncomingDamageEvent e) {
        if (Wave71Acts.running()) return;                                              // the blows of an act are the act's own: no passive on top
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer atk)) return;
        if (!Wave102Passives.RULES.containsKey(Wave20Util.heldId(atk))) return;       // 1.112.0 (lag audit): a player who does not hold one of these weapons costs one map lookup, no closure, no guard
        SafeCast.tick("relic.w102.events", () -> apply(e, atk, victim, src));
    }

    private static void apply(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, DamageSource src) {
        String id = Wave20Util.heldId(atk);
        Wave71Rules.Rule rule = Wave102Passives.RULES.get(id);
        if (rule == null || !RelicFlags.has(atk, FLAGS.get(id))) return;
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
