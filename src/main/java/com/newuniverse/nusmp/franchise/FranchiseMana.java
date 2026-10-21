package com.newuniverse.nusmp.franchise;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;

/**
 * The one place the franchise engravings read and give back magicule (Tensura's mana). Every call is guarded: a Tensura without the call only means no mana is returned.
 * Server side only.
 */
final class FranchiseMana {
    private FranchiseMana() {}

    /** The entity's maximum magicule, or 0. */
    static double max(LivingEntity e) {
        try {
            return EnergyHelper.getMaxMagicule(e);
        } catch (RuntimeException | LinkageError failed) {
            return 0.0;
        }
    }

    /** Gives magicule back (up to the maximum); a zero or negative amount does nothing. */
    static void refund(LivingEntity e, double amount) {
        if (amount > 0.0) TensuraBridge.refundMagicule(e, amount);
    }
}
