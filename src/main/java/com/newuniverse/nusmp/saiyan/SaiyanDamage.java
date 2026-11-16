package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.balance.BalanceLaw;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: Saiyan techniques III (Dragon Ball rework): the Ki techniques of the rework hurt through Tensura's own magic damage type, so a target's Tensura resistances and nullifications count, like every
 * other addon spell ({@code skill.SkillDamage} is package private and needs a skill instance, so the registry holder of {@code TensuraDamageTypes.MAGIC_GENERIC} is read directly, the way the Zogratis
 * boss does it). The amount is the same balance-law number as for the older Ki attacks; only the source differs. Server setting {@code phase.tensuraDamageSource} = false keeps plain indirect magic.
 * Cost: one registry lookup and one hurt per target; nothing is stored.
 */
final class SaiyanDamage {
    private SaiyanDamage() {}

    /** The damage source of a Saiyan's Ki technique: Tensura magic with the Saiyan as the cause, or plain indirect magic if the setting is off or the type is missing. */
    static DamageSource source(ServerPlayer p) {
        if (!SaiyanPhaseConfig.tensuraSource()) return p.damageSources().indirectMagic(p, p);
        return p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(TensuraDamageTypes.MAGIC_GENERIC)
                .<DamageSource>map(h -> new DamageSource(h, p, p))
                .orElseGet(() -> p.damageSources().indirectMagic(p, p));
    }

    /** Deals {@code raw} (already raised by the form) through the balance law as a Ki hit; returns what was dealt (0 if the law leaves nothing). */
    static float ki(ServerPlayer p, LivingEntity t, double raw, double mastery) {
        float dmg = BalanceLaw.damage(p, t, (float) Math.max(0.0, raw), mastery);
        if (dmg <= 0.01f) return 0f;
        t.hurt(source(p), dmg);
        SaiyanManager.Run r = SaiyanManager.peekRun(p);
        if (r != null) r.lastCombat = p.level().getGameTime();
        return dmg;
    }

    /** The outgoing factor of the Saiyan's active form and Ki Reinforcement (the same number the older Ki attacks use). */
    static double formFactor(ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        SaiyanManager.Run r = SaiyanManager.peekRun(p);
        return s != null && r != null ? SaiyanFormEngine.damageFactor(s, r) : 1.0;
    }
}
