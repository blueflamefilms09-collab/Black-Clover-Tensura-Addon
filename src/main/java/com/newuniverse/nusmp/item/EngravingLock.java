package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.franchise.FranchiseEngravings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;

/**
 * Engravings of this mod's weapons are part of the weapon: they cannot be taken off it and they cannot be moved to another item (server setting
 * {@code engravingLock}, default on; off restores the old behaviour). The stack carries a marker once it is engraved ({@code NUEngraved} for the
 * weapon engravings, {@code NUFranchise} for the franchise engravings, see {@link WeaponEngravings} and {@link FranchiseEngravings}); such a stack is "locked":
 * <ul>
 *   <li>the anvil refuses it as the sacrifice (right slot), so its enchantments cannot be merged onto another item or a book;</li>
 *   <li>the grindstone refuses it in either slot, so nothing is stripped (and nothing is moved);</li>
 *   <li>anything else that strips enchantments (another mod, a command) is undone: the once-a-second pass of both engraving classes puts every
 *       table engraving back (it only ever raises a missing or lowered level to the table level, it never lowers or removes anything).</li>
 * </ul>
 * Cost: two events that run only while a player has the anvil or grindstone open and read two stacks' custom data; the restore pass reuses the
 * existing once-a-second inventory pass (at most the stack's own table, a handful of lookups per engraved weapon). No state, no entities, no packets.
 */
public final class EngravingLock {
    private EngravingLock() {}

    /** True while the lock is on (the server setting). */
    public static boolean on() { return NUConfig.ENGRAVING_LOCK.get(); }

    /** True for a stack of this mod that has been engraved: its engravings are locked to it. */
    public static boolean locked(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null || !(cd.contains(WeaponEngravings.FLAG) || cd.contains(FranchiseEngravings.FLAG))) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id != null && "anime_multiverse".equals(id.getNamespace());
    }

    /** Anvil: a locked stack cannot be the right-hand item (it would give its enchantments away). */
    public static void onAnvil(AnvilUpdateEvent e) {
        if (on() && locked(e.getRight())) e.setCanceled(true);
    }

    /** Grindstone: a locked stack in either slot gives no output (no stripping, no merging away). */
    public static void onGrindstone(GrindstoneEvent.OnPlaceItem e) {
        if (on() && (locked(e.getTopItem()) || locked(e.getBottomItem()))) e.setCanceled(true);
    }
}
