package com.newuniverse.nusmp.fairytail.storm;

import com.newuniverse.nusmp.mana.ManaLaw;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: what a storm costs. The same rule the Fairy Tail skills use (a share of the caster's maximum Aura, paid from the Aura first and from the magicules when the Aura is short),
 * with the mod's mana law on top ({@link ManaLaw#costMult}: the caster's stage, affinity, an open Mana Zone, exhaustion), so a storm is cheaper inside a Mana Zone and dearer while
 * exhausted, exactly like every other spell. Creative players and a missing pool pay nothing. It takes nothing from the food bar: a storm never makes the caster hungrier.
 */
final class StormCost {
    private StormCost() {}

    /** What the spell costs this player right now (never below {@link StormMath#MIN_COST} times the law's multiplier). */
    static double costOf(ServerPlayer p, StormSpell s) {
        return StormMath.cost(EnergyHelper.getMaxAura(p), s.costPercent, ManaLaw.costMult(p));
    }

    /** Pays for the spell; false (and a message on the action bar, nothing taken) when the caster cannot afford it. */
    static boolean pay(ServerPlayer p, StormSpell s) {
        if (p.isCreative()) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        double cost = costOf(p, s);
        if (ex.getAura() >= cost) ex.setAura(ex.getAura() - cost);
        else if (ex.getMagicule() >= cost) ex.setMagicule(ex.getMagicule() - cost);
        else {
            p.displayClientMessage(Component.translatableWithFallback("nusmp.storm.no_energy", "Not enough Aura or magicules for %s (need %s).",
                    Component.translatableWithFallback("nusmp.storm.spell." + s.id, s.displayName), (long) cost).withStyle(ChatFormatting.RED), true);
            return false;
        }
        ex.markDirty();
        return true;
    }
}
