package com.newuniverse.nusmp.requip;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the game side of Instant Requip. {@link LoadoutRules} decides on copies; this class reads the player's worn set, inventory and armoury into those copies, asks for the plan and, only when the plan
 * says it may, writes the three places back in one go. It also saves a loadout (a template of what is worn now) and clears one. The vanilla armour slots swap instantly on the server; nothing is dropped
 * on the ground: the old set goes to the armoury, and when it does not fit nothing at all changes.
 */
public final class RequipLoadouts {
    private RequipLoadouts() {}

    /** The result of an equip: the status, how many slots were changed and how many items of the loadout were missing. */
    public record Outcome(LoadoutRules.Status status, int moved, int missing) {
        public boolean done() { return status == LoadoutRules.Status.OK || status == LoadoutRules.Status.PARTIAL; }
    }

    /** How well a candidate fits a template: exactly the same stack 2, the same item, name, enchantments and custom data (a damaged weapon) 1. */
    static final LoadoutRules.Matcher<ItemStack> MATCHER = (t, c) -> {
        if (t.isEmpty() || c.isEmpty()) return 0;
        if (ItemStack.isSameItemSameComponents(t, c)) return 2;
        return RequipArmoury.sameIdentity(t, c) ? 1 : 0;
    };

    // ------------------------------------------------------------ reading the player
    /** Head, chest, legs, feet, offhand, then the nine hotbar slots (copies). */
    public static List<ItemStack> capture(ServerPlayer p) {
        Inventory inv = p.getInventory();
        List<ItemStack> out = new ArrayList<>(LoadoutRules.SLOTS);
        for (int i = 0; i < LoadoutRules.ARMOUR_SLOTS; i++) out.add(inv.armor.get(3 - i).copy());
        out.add(inv.offhand.get(0).copy());
        for (int i = 0; i < LoadoutRules.HOTBAR; i++) out.add(inv.items.get(i).copy());
        return out;
    }

    private static List<ItemStack> worn(Inventory inv) {
        List<ItemStack> out = new ArrayList<>(LoadoutRules.SLOTS);
        for (int i = 0; i < LoadoutRules.ARMOUR_SLOTS; i++) out.add(inv.armor.get(3 - i));
        out.add(inv.offhand.get(0));
        for (int i = 0; i < LoadoutRules.HOTBAR; i++) out.add(inv.items.get(i));
        return out;
    }

    private static List<ItemStack> rest(Inventory inv) {
        List<ItemStack> out = new ArrayList<>(27);
        for (int i = LoadoutRules.HOTBAR; i < inv.items.size(); i++) out.add(inv.items.get(i));
        return out;
    }

    // ------------------------------------------------------------ save and clear
    /** Saves what is worn now as loadout {@code n}. Returns the number of pieces saved (0 = nothing worn, nothing saved). */
    public static int save(ServerPlayer p, int n) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        if (n < 0 || n >= d.loadouts.size()) return 0;
        List<ItemStack> now = capture(p);
        for (int i = 0; i < now.size(); i++) if (RequipWeaponItem.flagged(now.get(i))) now.set(i, ItemStack.EMPTY);          // the weapon an armour made is not a loadout piece
        RequipArmoury.Loadout lo = new RequipArmoury.Loadout(now);
        if (!lo.saved()) return 0;
        d.loadouts.set(n, lo);
        d.active = n;
        return lo.pieces();
    }

    public static void clear(ServerPlayer p, int n) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        if (n < 0 || n >= d.loadouts.size()) return;
        d.loadouts.get(n).clear();
        if (d.active == n) d.active = -1;
        if (d.reflex == n) d.reflex = -1;
    }

    // ------------------------------------------------------------ the swap
    /**
     * Swaps to loadout {@code n}. All or nothing: the plan is made on copies and written back only when it applies. A stack that an armour made (the flagged Requip weapon) standing in a slot the loadout
     * replaces is simply unmade (it comes back with its armour); everything else the loadout replaces goes to the armoury.
     */
    public static Outcome equip(ServerPlayer p, int n, int capacity) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        if (n < 0 || n >= d.loadouts.size()) return new Outcome(LoadoutRules.Status.EMPTY_LOADOUT, 0, 0);
        RequipArmoury.Loadout lo = d.loadouts.get(n);
        Inventory inv = p.getInventory();
        List<ItemStack> w = new ArrayList<>(worn(inv));
        for (int i = 0; i < w.size(); i++) {
            if (RequipWeaponItem.flagged(w.get(i)) && !lo.template.get(i).isEmpty() && LoadoutRules.validSlot(i)) w.set(i, ItemStack.EMPTY);
        }
        LoadoutRules.Plan<ItemStack> plan = LoadoutRules.plan(lo.template, w, rest(inv), d.cells, capacity, RequipArmoury.OPS, MATCHER);
        if (!plan.applies()) return new Outcome(plan.status(), 0, plan.missing());
        for (int i = 0; i < LoadoutRules.ARMOUR_SLOTS; i++) inv.armor.set(3 - i, plan.worn().get(i));
        inv.offhand.set(0, plan.worn().get(LoadoutRules.OFFHAND));
        for (int i = 0; i < LoadoutRules.HOTBAR; i++) inv.items.set(i, plan.worn().get(LoadoutRules.HOTBAR_FIRST + i));
        for (int i = 0; i < plan.rest().size(); i++) inv.items.set(LoadoutRules.HOTBAR + i, plan.rest().get(i));
        for (int i = 0; i < plan.armoury().size(); i++) d.cells.set(i, plan.armoury().get(i));
        d.active = n;
        inv.setChanged();
        return new Outcome(plan.status(), plan.moved(), plan.missing());
    }
}
