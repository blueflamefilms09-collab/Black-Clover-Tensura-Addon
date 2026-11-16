package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Guide P2 (follow-up of P1 #1): the payment path of the two skill families that extend Tensura's {@code Skill} instead of {@link NUSkill}
 * ({@link GrimoirePageSkill}, {@link GrimoireMagicSkill}), the same as {@code NUSkill.charge}: the skill's {@code getMagiculeCost} answers the
 * price (read from {@link #paying()}) and {@code EnergyHelper.isOutOfEnergy(player, instance, mode)} checks and takes it once (creative pays
 * nothing, Tensura's own "not enough magicule" message). The price is the old flat price scaled by {@code BalanceLaw.classCost(player, null, flat)}
 * (the plain, unclassified law: never below the flat price). SERVER setting ultimateClasses.flatCosts = true keeps the old direct write.
 * Server thread only; the ThreadLocal is cleared in finally and never holds a player.
 */
final class LegacyPay {
    private LegacyPay() {}

    private static final ThreadLocal<Double> PAYING = new ThreadLocal<>();

    /** What the skill's getMagiculeCost answers: the cast being paid, 0 outside a payment. */
    static double paying() {
        Double p = PAYING.get();
        return p != null ? p : 0.0;
    }

    /** True when it is paid (nothing owed, or taken); false = not paid, nothing taken. */
    static boolean charge(ManasSkillInstance instance, ServerPlayer player, int mode, double flat) {
        if (!(flat > 0)) return true;
        if (NUConfig.ucFlatCosts() || instance == null || !(instance.getSkill() instanceof GrimoirePageSkill || instance.getSkill() instanceof GrimoireMagicSkill))
            return SkillUtil.spendMagicules(player, flat);
        PAYING.set(BalanceLaw.classCost(player, null, flat));
        try {
            return !EnergyHelper.isOutOfEnergy(player, instance, mode);
        } finally {
            PAYING.remove();
        }
    }
}
