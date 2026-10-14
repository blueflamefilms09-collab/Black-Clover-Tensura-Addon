package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The {@code nusmp:djinn_temp} component of a Djinn weapon, read and written as a view over its compound: who the weapon was summoned for, which
 * Djinn, when it lapses (a game time, 0 = it lasts as long as the Equip) and whether it is an admin weapon (given by command or taken from the creative
 * tab: it does not depend on a Djinn Equip, is never taken back and can be dropped). A stack without the component is a bare weapon item.
 */
public final class DjinnTemp {
    private DjinnTemp() {}

    public record Data(UUID owner, String djinn, long expires, boolean admin) {}

    /** The component of a stack, or null if it has none. */
    public static Data get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag t = stack.get(DjinnComponents.TEMP.get());
        if (t == null) return null;
        return new Data(t.hasUUID("owner") ? t.getUUID("owner") : null, t.getString("djinn"), t.getLong("expires"), t.getBoolean("admin"));
    }

    /** Writes a fresh compound (a stack's component values are shared, never mutated in place). */
    public static void set(ItemStack stack, UUID owner, Djinn djinn, long expires, boolean admin) {
        CompoundTag t = new CompoundTag();
        if (owner != null) t.putUUID("owner", owner);
        t.putString("djinn", djinn.name());
        if (expires > 0) t.putLong("expires", expires);
        if (admin) t.putBoolean("admin", true);
        stack.set(DjinnComponents.TEMP.get(), t);
    }

    /** Is this stack one of the loan's temporary weapons (not an admin one)? These are the only items the weapon loan wipes. */
    public static boolean isLoanWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof DjinnWeaponItem)) return false;
        Data d = get(stack);
        return d != null && !d.admin();
    }

    /** Is this a temporary weapon made for exactly this owner and Djinn (the one the loan wants in the hand)? */
    public static boolean isFor(ItemStack stack, UUID owner, Djinn djinn) {
        Data d = get(stack);
        return d != null && !d.admin() && owner != null && owner.equals(d.owner()) && djinn.name().equals(d.djinn());
    }
}
