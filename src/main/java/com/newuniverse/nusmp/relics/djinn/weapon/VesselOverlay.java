package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.relics.djinn.VesselAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The vessel a Djinn weapon sits over. When the bearer's vessel is itself a weapon and stands in the selected hotbar slot, the loan does not move it
 * ({@link WeaponLoan}, {@link WeaponLoan.Ops#overlays}): the Djinn weapon takes the slot, and the vessel stack is kept whole in the loan record of the
 * player's Djinn state (key {@value #KEY}, saved with {@code saveOptional}, so enchantments and every data component survive), in no inventory slot
 * at all. While it is there the vessel counts as on the body ({@link VesselAccess#findBound}); at the end of the Equip, on death, logout, a dimension
 * change or creative mode, the loan puts it back in the slot it came from, unchanged.
 *
 * <p>This class is only the record's reader and writer; the decisions are the loan's.
 */
public final class VesselOverlay {
    private VesselOverlay() {}

    /** The key of the overlaid vessel inside the {@code wloan} compound. */
    public static final String KEY = "overlay";

    /** Is this stack the bound vessel of the owner's Djinn {@code d} with this serial (the only item a Djinn weapon overlays)? */
    public static boolean isVesselOf(ItemStack stack, Djinn d, UUID serial) {
        return d != null && serial != null && !stack.isEmpty() && VesselAccess.boundDjinn(stack) == d && serial.equals(VesselAccess.serial(stack));
    }

    /** The overlaid vessel of this player when it is the one with this serial, else EMPTY. Cheap when there is no loan (one compound lookup). */
    public static ItemStack read(Player p, UUID serial) {
        if (serial == null) return ItemStack.EMPTY;
        DjinnState s = DjinnState.of(p);
        if (!s.hasLoan()) return ItemStack.EMPTY;
        CompoundTag loan = s.loan();
        if (!loan.contains(KEY, 10)) return ItemStack.EMPTY;
        ItemStack st = ItemStack.parseOptional(p.registryAccess(), loan.getCompound(KEY));
        return !st.isEmpty() && serial.equals(VesselAccess.serial(st)) && VesselAccess.boundDjinn(st) != null ? st : ItemStack.EMPTY;
    }

    /** Writes (or, for an empty stack, removes) the overlaid vessel in a loan compound. */
    static void write(CompoundTag loan, ItemStack vessel, HolderLookup.Provider registries) {
        if (vessel == null || vessel.isEmpty()) loan.remove(KEY);
        else loan.put(KEY, vessel.saveOptional(registries));
    }

    /** Reads the overlaid vessel from a loan compound (EMPTY if none or unreadable). */
    static ItemStack readFrom(CompoundTag loan, HolderLookup.Provider registries) {
        return loan.contains(KEY, 10) ? ItemStack.parseOptional(registries, loan.getCompound(KEY)) : ItemStack.EMPTY;
    }

    /** Takes the Djinn binding off the overlaid vessel with this serial (a Djinn that is let go while its vessel is under the weapon). Returns 1 if it did. */
    public static int strip(Player p, UUID serial) {
        if (serial == null) return 0;
        DjinnState s = DjinnState.of(p);
        if (!s.hasLoan()) return 0;
        CompoundTag loan = s.loan();
        ItemStack st = readFrom(loan, p.registryAccess());
        if (st.isEmpty() || !serial.equals(VesselAccess.serial(st))) return 0;
        VesselAccess.strip(st);
        write(loan, st, p.registryAccess());
        return 1;
    }
}
