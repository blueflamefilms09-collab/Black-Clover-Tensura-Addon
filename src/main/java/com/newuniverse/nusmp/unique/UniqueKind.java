package com.newuniverse.nusmp.unique;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * Why: one small adapter per kind of 1-of-1 (the Black Sword Greed, Shishou, a Canon Grimoire, a Dragon Ball, a unique relic and so every Infinity Stone). The guardian never touches the rules of those systems: the adapter says
 * which unique an item is a copy of, whether it is THE valid copy by the system's own rules (token, serial, scatter generation), whether the system still believes a copy exists, and how to put the unique back into its
 * normal sources with the system's own door. The guardian never puts an item into anybody's inventory: "back in the pool" means the system's own flag, ledger entry or claim is reset so the item can come out of its normal
 * source again, and every old copy is invalid. Server thread only; every method must be cheap (called for stacks the scan meets) except {@link #release}.
 */
public interface UniqueKind {
    /** Short name of the kind (greed, shishou, charter, dragonball, relic). */
    String name();

    /** Does this kind own this unique id? */
    boolean owns(String id);

    /** The unique id this stack is a copy of (valid or not), or null when it is not an item of this kind. The stack is never empty. */
    String idOf(ItemStack stack);

    /** Is this stack THE valid copy by the owning system's own rules? */
    boolean valid(MinecraftServer server, String id, ItemStack stack);

    /**
     * True for kinds whose items carry no token of their own (the Canon Grimoire, the Dragon Ball): the guardian stamps the copies it sees with the unique's epoch, so a copy made before the last return to the pool is an
     * old copy and dissolves.
     */
    default boolean stamped() { return false; }

    /** Does the owning system still believe a valid item copy of this unique exists right now? When it does not, there is nothing to lose (its own rule already returned it). */
    boolean exists(MinecraftServer server, String id);

    /** Puts the unique back into its normal sources with the owning system's own door and invalidates the old copy. False when there was nothing to put back. */
    boolean release(MinecraftServer server, String id);

    /** A readable name for logs and commands. */
    String label(String id);

    /** One sentence: where the unique comes from once it is back in the pool (for the command and the docs). */
    String pool(String id);
}
