package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.relics.RelicComponents2;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The serial of a relic stack: the data component {@code relic_serial} (a UUID, registered by the relic core in {@link RelicComponents2}).
 * Every access to it from the Order goes through here. A stack that has none is a stack nobody ever issued.
 */
final class RelicSerial {
    private RelicSerial() {}

    /** The serial on the stack, or null. */
    static UUID get(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.get(type());
    }

    /** Writes the serial onto this very stack object. */
    static void set(ItemStack stack, UUID serial) {
        if (stack != null && !stack.isEmpty() && serial != null) stack.set(type(), serial);
    }

    /** Takes the serial off (not used by the Order itself; a tool for tests and commands). */
    static void clear(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) stack.remove(type());
    }

    private static DataComponentType<UUID> type() { return RelicComponents2.SERIAL.get(); }
}
