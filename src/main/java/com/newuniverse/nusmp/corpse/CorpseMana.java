package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.mana.ManaLaw;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: a pact that saves someone from death must cost the caster something at the moment it pays out, not only when it was armed,
 * and that payment has to go through the mod's mana system (the same magicule pool and cost multipliers a grimoire page uses) so
 * stages, affinity and the Mana Zone apply to it too. Creative players pay nothing. Returns false (and takes nothing) when the pool
 * cannot cover it, which makes the pact fail to save instead of drawing mana below zero.
 */
final class CorpseMana {
    private CorpseMana() {}

    /** Takes 'percent' of the player's maximum magicule (scaled by their mana cost multipliers); true if it was paid. */
    static boolean pay(ServerPlayer p, double percent) {
        if (p == null) return false;
        if (percent <= 0 || p.isCreative()) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        double cost = BalanceLaw.cost(p, percent, 0) * ManaLaw.costMult(p);
        if (ex.getMagicule() < cost) return false;
        ex.setMagicule(ex.getMagicule() - cost);
        ex.markDirty();
        return true;
    }
}
