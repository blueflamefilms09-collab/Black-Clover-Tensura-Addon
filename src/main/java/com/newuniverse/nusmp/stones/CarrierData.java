package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.RelicDef;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: where a gauntlet or the scepter keeps its stones. They stay real items: the socketed stone sits in the carrier's {@code container} component (slot = the stone's ordinal), the same component a shulker
 * box uses, which the Order's ledger already reads one level deep (relics/order/OrderNest), so a socketed stone keeps its serial and the one-of-each-stone-per-server rule keeps counting it. No new data
 * component is registered and an old carrier simply has no stones. Also holds the Snap mode of a gauntlet (a small int in the custom data). Server side; cheap (one component read).
 */
final class CarrierData {
    private CarrierData() {}

    static final String MODE_KEY = "nusmp_snap_mode";

    /** The stone stack in a carrier's socket for this stone, or EMPTY. Only a real stone of that kind counts. */
    static ItemStack get(ItemStack carrier, InfinityStone s) {
        if (carrier == null || carrier.isEmpty() || s == null) return ItemStack.EMPTY;
        ItemContainerContents c = carrier.get(DataComponents.CONTAINER);
        if (c == null || s.ordinal() >= c.getSlots()) return ItemStack.EMPTY;
        ItemStack in = c.getStackInSlot(s.ordinal());
        return !in.isEmpty() && StoneSlot.stoneOf(in) == s ? in : ItemStack.EMPTY;
    }

    /** The socket mask of a carrier: the stones it holds, limited to what that carrier may hold (a gauntlet all six, the scepter the Mind Stone). */
    static int mask(ItemStack carrier) {
        if (carrier == null || carrier.isEmpty()) return 0;
        RelicDef d = Relics.defOf(carrier);
        if (d == null) return 0;
        int allowed = CarrierRules.allowedMask(d.id());
        if (allowed == 0 || !carrier.has(DataComponents.CONTAINER)) return 0;
        int m = 0;
        for (InfinityStone s : InfinityStone.values()) if (CarrierRules.has(allowed, s) && !get(carrier, s).isEmpty()) m = CarrierRules.with(m, s);
        return m;
    }

    private static NonNullList<ItemStack> slots(ItemStack carrier) {
        NonNullList<ItemStack> list = NonNullList.withSize(InfinityStone.values().length, ItemStack.EMPTY);
        ItemContainerContents c = carrier.get(DataComponents.CONTAINER);
        if (c != null) {
            int n = Math.min(c.getSlots(), list.size());
            for (int i = 0; i < n; i++) list.set(i, c.getStackInSlot(i).copy());
        }
        return list;
    }

    /** Puts a stone into its socket (a copy). False if the carrier may not hold it, the socket is taken, or the write did not read back. The caller removes its own copy only after a true. */
    static boolean put(ItemStack carrier, InfinityStone s, ItemStack stone) {
        RelicDef d = Relics.defOf(carrier);
        if (d == null || s == null || stone == null || stone.isEmpty() || StoneSlot.stoneOf(stone) != s) return false;
        if (!CarrierRules.has(CarrierRules.allowedMask(d.id()), s) || !get(carrier, s).isEmpty()) return false;
        NonNullList<ItemStack> list = slots(carrier);
        ItemStack one = stone.copy();
        one.setCount(1);
        list.set(s.ordinal(), one);
        carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return !get(carrier, s).isEmpty();
    }

    /** Takes every stone out (returned in the order of the Gauntlet's knuckles) and leaves the sockets empty. */
    static List<ItemStack> takeAll(ItemStack carrier) {
        List<ItemStack> out = new ArrayList<>(6);
        for (InfinityStone s : InfinityStone.values()) {
            ItemStack in = get(carrier, s);
            if (!in.isEmpty()) out.add(in.copy());
        }
        if (!out.isEmpty()) carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(NonNullList.withSize(InfinityStone.values().length, ItemStack.EMPTY)));
        return out;
    }

    // ---------------------------------------------------------------- the Snap mode of a gauntlet
    static CarrierRules.SnapMode mode(ItemStack carrier) {
        CustomData d = carrier == null ? null : carrier.get(DataComponents.CUSTOM_DATA);
        return CarrierRules.snapMode(d == null ? 0 : d.copyTag().getInt(MODE_KEY));
    }

    static void setMode(ItemStack carrier, CarrierRules.SnapMode m) {
        CustomData.update(DataComponents.CUSTOM_DATA, carrier, (CompoundTag t) -> t.putInt(MODE_KEY, m.ordinal()));
    }
}
