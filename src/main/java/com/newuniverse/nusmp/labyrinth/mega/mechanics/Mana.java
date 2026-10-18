package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.player.Player;

/**
 * Why: several primitives drain or restore a player's magicules (the Black Clover mana tide, the Tensura anti-magic field), and the way to do it is Tensura's own energy store, the
 * same two calls the Decennial bosses' anti-magic aura uses. One helper, wrapped so that a player without a Tensura record (a fake player in a self-test, a mod change) simply
 * has nothing to lose or gain and the primitive carries on.
 */
final class Mana {
    private Mana() {}

    /** Adds {@code fraction} (negative drains) of the player's maximum magicules, clamped at 0 and at the maximum. Returns false when the player has no magicule record. */
    static boolean add(Player p, double fraction) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return false;
            double max = EnergyHelper.getMaxMagicule(p);
            ex.setMagicule(Math.max(0.0, Math.min(max, ex.getMagicule() + max * fraction)));
            ex.markDirty();
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
