package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The one place that knows how a stack is a Metal Vessel: a relic of Legend grade or above that the catalog flagged {@code vessel()}, with the
 * {@code djinn_bound} / {@code djinn_serial} components once a Djinn sits in it. Everything else asks here.
 */
public final class VesselAccess {
    private VesselAccess() {}

    /** Is this stack a relic that can hold a Djinn (flagged vessel and Legend grade or higher)? */
    public static boolean isVessel(ItemStack s) {
        RelicDef d = Relics.defOf(s);
        return d != null && d.isVessel() && d.grade().djinnCapable();
    }

    /** Does this stack work for the player (no attunement needed, or attuned to them)? */
    public static boolean attunedTo(ItemStack s, Player p) {
        RelicDef d = Relics.defOf(s);
        return d != null && Attune.works(s, d, p);
    }

    /** The Djinn bound into this stack, or null. */
    public static Djinn boundDjinn(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        String id = s.get(DjinnComponents.BOUND.get());
        return id == null ? null : Djinn.byName(id);
    }

    public static UUID serial(ItemStack s) {
        return s == null || s.isEmpty() ? null : s.get(DjinnComponents.SERIAL.get());
    }

    public static void stamp(ItemStack s, Djinn d, UUID serial) {
        s.set(DjinnComponents.BOUND.get(), d.id());
        s.set(DjinnComponents.SERIAL.get(), serial);
    }

    public static void strip(ItemStack s) {
        s.remove(DjinnComponents.BOUND.get());
        s.remove(DjinnComponents.SERIAL.get());
    }

    /** The relics the player has equipped (hands included, off-hand too), from the core: held, armour, Curios. */
    public static List<ItemStack> equipped(Player p) {
        List<ItemStack> out = new ArrayList<>(Relics.equipped(p));
        // 1.101.0: the raw inventory, not getOffhandItem(): Better Combat answers EMPTY for the off hand behind a two-handed weapon (see BcHands), which made a
        // vessel held in the off hand "leave" the bearer the moment a two-handed Djinn weapon (or any two-handed weapon) was in the main hand.
        for (ItemStack h : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem(), rawMain(p), rawOff(p)}) if (!h.isEmpty() && !out.contains(h)) out.add(h);
        return out;
    }

    /**
     * The equipped stack whose binding has this serial (and a Djinn), or EMPTY: the vessel is on the player. A vessel that is a weapon and sits under
     * the Djinn weapon (the weapon overlays it in the same slot, {@link com.newuniverse.nusmp.relics.djinn.weapon.VesselOverlay}) counts as on the body.
     */
    public static ItemStack findBound(Player p, UUID serial) {
        if (serial == null) return ItemStack.EMPTY;
        for (ItemStack s : equipped(p)) if (serial.equals(serial(s)) && boundDjinn(s) != null) return s;
        return com.newuniverse.nusmp.relics.djinn.weapon.VesselOverlay.read(p, serial);
    }

    /** Takes the binding off every stack of the player's with this serial (inventory, armour, off-hand, equipped Curios). Returns how many. */
    public static int stripEverywhere(ServerPlayer p, UUID serial) {
        if (serial == null) return 0;
        int[] n = {0};
        Predicate<ItemStack> hit = s -> !s.isEmpty() && serial.equals(serial(s));
        Inventory inv = p.getInventory();
        for (List<ItemStack> list : List.of(inv.items, inv.armor, inv.offhand))
            for (ItemStack s : list) if (hit.test(s)) { strip(s); n[0]++; }
        for (ItemStack s : Relics.equipped(p)) if (hit.test(s)) { strip(s); n[0]++; }
        n[0] += com.newuniverse.nusmp.relics.djinn.weapon.VesselOverlay.strip(p, serial);                           // a vessel under the Djinn weapon is in the loan record, not in a slot
        return n[0];
    }

    /** The stack in the selected hotbar slot, read from the inventory (Better Combat cannot hide it). */
    public static ItemStack rawMain(Player p) {
        ItemStack s = p.getInventory().getSelected();
        return s == null ? ItemStack.EMPTY : s;
    }

    /** The off-hand stack, read from the inventory: {@code getOffhandItem()} is EMPTY under Better Combat when a two-handed weapon is in either hand ({@link BcHands}). */
    public static ItemStack rawOff(Player p) {
        ItemStack s = p.getInventory().offhand.get(0);
        return s == null ? ItemStack.EMPTY : s;
    }

    /** The stack in a hand, read from the inventory. */
    public static ItemStack rawIn(Player p, net.minecraft.world.InteractionHand hand) {
        return hand == net.minecraft.world.InteractionHand.MAIN_HAND ? rawMain(p) : rawOff(p);
    }

    /** The stack in the hand that is not {@code hand}, read from the inventory (the ritual's vessel; the old reads went through Better Combat's off-hand lie). */
    public static ItemStack rawOtherThan(Player p, net.minecraft.world.InteractionHand hand) {
        return rawIn(p, hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    /** The stack in the player's other hand than the one holding {@code seal}, or EMPTY (kept for the old callers; reads the inventory). */
    public static ItemStack otherHand(Player p, ItemStack seal) {
        ItemStack main = rawMain(p), off = rawOff(p);
        return main == seal ? off : main;
    }

    /** How the ritual sees a stack as a vessel for this player: the item through the catalog, plus the two components the ritual owns, nothing else. */
    public static BindGesture.Look look(ItemStack s, Player p) {
        if (s == null || s.isEmpty()) return BindGesture.Look.NONE;
        RelicDef d = Relics.defOf(s);
        boolean bound = boundDjinn(s) != null;
        if (d == null) return new BindGesture.Look(false, false, false, false, bound, false);
        return new BindGesture.Look(false, true, d.isVessel(), d.grade().djinnCapable(), bound, Attune.works(s, d, p));
    }

    /** The reason a stack cannot be the vessel for this player, or OK. */
    public static BindGesture.Why verdict(ItemStack s, Player p) { return BindGesture.judge(look(s, p)); }
}
