package com.newuniverse.nusmp.requip;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the weapon on call. Requip: Blade summons the selected armoury weapon into the main hand and puts it away again; Requip: Barrage and Requip: Strike take their damage from the weapon in the
 * hand. A summon is a SWAP between the hand and the armoury cell the weapon left, so it can never run out of room and never copies anything; a thrown weapon (Strike) waits in the saved attachment
 * ({@link RequipArmoury.Data#limbo}) and comes home to the hand, then the inventory, then the armoury: never to the ground, and never lost with a relog or a death.
 */
public final class RequipWeapons {
    private RequipWeapons() {}

    public enum Result { OK, NO_WEAPON, HAND_FLAGGED, NOT_OUT, FULL, BUSY }

    // ------------------------------------------------------------ what counts as a weapon
    /** The weapon's own attack damage in the main hand, the player's 1 included (the figure the Tensura grade table is made of). */
    public static double damageOf(ItemStack s) {
        if (s.isEmpty()) return 0.0;
        double[] sum = {1.0};
        s.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.is(Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow()) && mod.operation() == AttributeModifier.Operation.ADD_VALUE) sum[0] += mod.amount();
        });
        return sum[0];
    }

    public static boolean isWeapon(ItemStack s) {
        return !s.isEmpty() && s.getMaxStackSize() == 1 && RequipMagicRules.isWeapon(damageOf(s));
    }

    /** The cell of the next weapon after {@code from} (wrapping), or -1 when the armoury holds none. */
    public static int nextWeapon(RequipArmoury.Data d, int from) {
        int n = d.cells.size();
        for (int step = 1; step <= n; step++) {
            int i = Math.floorMod(from + step, n);
            if (isWeapon(d.cells.get(i))) return i;
        }
        return -1;
    }

    /** The cell Blade and Barrage use: the selected one when it still holds a weapon, else the first weapon found (and it becomes the selection). -1 when there is none. */
    public static int selectedCell(RequipArmoury.Data d) {
        if (d.selected >= 0 && isWeapon(d.cells.get(d.selected))) return d.selected;
        int i = nextWeapon(d, -1);
        d.selected = i;
        return i;
    }

    /** The weapon to use for a Barrage or Strike: the one in the main hand, else the selected armoury weapon (not taken out). Empty when there is none. */
    public static ItemStack aimedWeapon(ServerPlayer p) {
        ItemStack hand = p.getMainHandItem();
        if (isWeapon(hand)) return hand;
        RequipArmoury.Data d = RequipArmoury.data(p);
        int i = selectedCell(d);
        return i < 0 ? ItemStack.EMPTY : d.cells.get(i);
    }

    // ------------------------------------------------------------ Requip: Blade
    /** True when the weapon Blade summoned is still in the main hand. */
    public static boolean bladeOut(ServerPlayer p) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        return d.blade >= 0 && !d.bladeSig.isEmpty() && RequipArmoury.sameIdentity(d.bladeSig, p.getMainHandItem());
    }

    /** Summons the selected weapon into the main hand; whatever was in the hand goes to the armoury cell the weapon left. */
    public static Result summon(ServerPlayer p) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        int cell = selectedCell(d);
        if (cell < 0) return Result.NO_WEAPON;
        Inventory inv = p.getInventory();
        ItemStack hand = inv.getItem(inv.selected);
        if (RequipWeaponItem.flagged(hand)) return Result.HAND_FLAGGED;
        ItemStack weapon = d.cells.get(cell);
        d.cells.set(cell, hand);
        inv.setItem(inv.selected, weapon);
        d.blade = cell;
        d.bladeSig = weapon.copy();
        d.bladeBack = hand.copy();
        return Result.OK;
    }

    /** Puts the summoned weapon away: back to the cell it left (swapping the displaced item back into the hand), else any free cell. */
    public static Result putAway(ServerPlayer p, int capacity) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        if (!bladeOut(p)) { d.blade = -1; return Result.NOT_OUT; }
        Inventory inv = p.getInventory();
        ItemStack weapon = inv.getItem(inv.selected);
        int cell = d.blade;
        ItemStack there = d.cells.get(cell);
        boolean displacedStillThere = !d.bladeBack.isEmpty() && ItemStack.isSameItemSameComponents(there, d.bladeBack);
        if (displacedStillThere || (d.bladeBack.isEmpty() && there.isEmpty())) {
            d.cells.set(cell, weapon);
            inv.setItem(inv.selected, there);
        } else if (there.isEmpty()) {
            d.cells.set(cell, weapon);
            inv.setItem(inv.selected, ItemStack.EMPTY);
        } else {
            ItemStack rest = ArmouryRules.insert(d.cells, capacity, weapon, RequipArmoury.OPS);
            if (!rest.isEmpty()) return Result.FULL;
            inv.setItem(inv.selected, ItemStack.EMPTY);
        }
        d.blade = -1;
        d.bladeSig = ItemStack.EMPTY;
        d.bladeBack = ItemStack.EMPTY;
        return Result.OK;
    }

    /** Selects the next weapon of the armoury (the weapon Blade and Barrage use). Returns the cell, or -1. */
    public static int cycleSelection(ServerPlayer p) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        d.selected = nextWeapon(d, d.selected);
        return d.selected;
    }

    // ------------------------------------------------------------ Requip: Strike (the weapon in flight waits in the saved limbo)
    public static boolean thrown(ServerPlayer p) { return !RequipArmoury.data(p).limbo.isEmpty(); }

    /** Takes the weapon in the main hand out of the hand into the limbo. Returns a copy of it, or empty when the hand holds no weapon or a weapon is already out. */
    public static ItemStack throwOut(ServerPlayer p) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        Inventory inv = p.getInventory();
        ItemStack hand = inv.getItem(inv.selected);
        if (!d.limbo.isEmpty() || !isWeapon(hand) || RequipWeaponItem.flagged(hand)) return ItemStack.EMPTY;
        d.limbo = hand;
        d.limboSlot = inv.selected;
        inv.setItem(inv.selected, ItemStack.EMPTY);
        if (d.blade >= 0 && RequipArmoury.sameIdentity(d.bladeSig, d.limbo)) d.blade = -1;        // a summoned blade that was thrown is just a weapon now
        return d.limbo.copy();
    }

    /**
     * Brings the thrown weapon home: into the slot it left if that is empty, else the main hand if empty, else any free inventory slot, else the armoury. If nothing has room it stays in the limbo (saved)
     * and the next call tries again. Returns true when the weapon is back.
     */
    public static boolean recall(ServerPlayer p, int capacity) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        if (d.limbo.isEmpty()) return true;
        Inventory inv = p.getInventory();
        ItemStack w = d.limbo;
        int slot = d.limboSlot;
        if (slot >= 0 && slot < inv.items.size() && inv.items.get(slot).isEmpty()) inv.items.set(slot, w);
        else if (inv.items.get(inv.selected).isEmpty()) inv.items.set(inv.selected, w);
        else {
            int free = inv.getFreeSlot();
            if (free >= 0) inv.items.set(free, w);
            else {
                List<ItemStack> cells = d.cells;
                if (!ArmouryRules.insert(cells, capacity, w, RequipArmoury.OPS).isEmpty()) return false;
            }
        }
        d.limbo = ItemStack.EMPTY;
        d.limboSlot = -1;
        inv.setChanged();
        return true;
    }

    /** Every stack the pocket space holds, for the self-test and the doc's no-loss check (copies). */
    public static List<ItemStack> everything(ServerPlayer p) {
        RequipArmoury.Data d = RequipArmoury.data(p);
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack s : d.cells) if (!s.isEmpty()) all.add(s.copy());
        if (!d.limbo.isEmpty()) all.add(d.limbo.copy());
        return all;
    }
}
