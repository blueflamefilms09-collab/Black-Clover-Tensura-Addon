package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroWeaponItem;
import io.github.manasmods.tensura.menu.ResearcherEnchantingMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Why (4.100.0): a hero or vassal weapon is only a PROJECTION of the bond, so two things stood between it and an enchanting table: the loan wiped it out of
 * every menu slot that is not the player's own inventory, and a projection that is made again (put away and summoned, or put back by the loan after the menu)
 * is built fresh, so whatever it was enchanted with was gone. This class is the answer: the enchanting menus (the vanilla table, the anvil and Tensura's
 * Researcher screen, which the Researcher, Great Sage and the other enchanting skills open) may hold the player's OWN projection; while one does, the loan
 * does not make a second one; and the enchantments of the live projection are kept in the bond ({@code extra("enchants")}) whenever the loan would rebuild
 * it, so the next projection is made with them. Engravings stay what they were: {@code WeaponEngravings} only raises a missing one. Server thread only; the cost
 * is a few slot reads per four-tick beat while such a menu is open, and one encode when the enchantments changed (never per tick, never a packet).
 */
public final class HeroEnchants {
    private HeroEnchants() {}

    private static final String KEY = "enchants";

    /** Is this one of the menus that enchant an item put into it? */
    public static boolean isEnchantMenu(AbstractContainerMenu m) {
        return m instanceof EnchantmentMenu || m instanceof AnvilMenu || m instanceof ResearcherEnchantingMenu;
    }

    /** The slots of an enchanting menu that hold the item itself (never the preview that an anvil or the Researcher shows as its result). */
    private static boolean isInputSlot(AbstractContainerMenu m, Slot s) {
        if (m instanceof AnvilMenu) return s.index != 2;
        if (m instanceof ResearcherEnchantingMenu r) return s.container != r.getItemOutput();
        return true;
    }

    /** May the player's own projection of {@code kind} stay in the slots of the menu the player has open? */
    public static boolean allowsMenu(ServerPlayer p) {
        return HeroConfig.enchantMenus() && isEnchantMenu(p.containerMenu);
    }

    /** The player's own current projection that sits in a slot of the open enchanting menu (not the player's inventory, not a preview), or empty. */
    static ItemStack inMenu(ServerPlayer p, HeroBond b) {
        if (!allowsMenu(p) || !HeroWeaponItem.enchantable(b.kind())) return ItemStack.EMPTY;
        Inventory inv = p.getInventory();
        for (Slot s : p.containerMenu.slots) {
            if (s.container == inv || !isInputSlot(p.containerMenu, s)) continue;
            ItemStack st = s.getItem();
            if (HeroLoan.isProjection(st) && HeroLoan.current(p, b.kind(), st)) return st;
        }
        return ItemStack.EMPTY;
    }

    /** Empties every hero weapon out of the slots of the open menu that are not the player's inventory (the loan ends: a logout, a death, a dismiss). */
    static void wipeMenu(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (Slot s : p.containerMenu.slots) {
            if (s.container == inv) continue;
            if (HeroItems.kindOf(s.getItem()) != null) s.set(ItemStack.EMPTY);
        }
    }

    /** Keeps the enchantments of the live projection (in the menu first, else in the inventory) in the bond. Called when the loan is about to rebuild or end. */
    static void capture(ServerPlayer p, HeroBond b) {
        if (!b.isBound()) return;
        ItemStack live = inMenu(p, b);
        if (live.isEmpty()) {
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize() && live.isEmpty(); i++) {
                ItemStack st = inv.getItem(i);
                if (HeroLoan.isProjection(st) && HeroLoan.current(p, b.kind(), st)) live = st;
            }
            ItemStack carried = p.containerMenu.getCarried();
            if (live.isEmpty() && HeroLoan.isProjection(carried) && HeroLoan.current(p, b.kind(), carried)) live = carried;
        }
        if (live.isEmpty()) return;
        ItemEnchantments now = live.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (now.equals(stored(p, b))) return;
        var ops = p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemEnchantments.CODEC.encodeStart(ops, now).result().ifPresent(tag -> {
            CompoundTag c = b.extra(KEY);
            c.putString("kind", b.kind().itemId());
            c.put("data", tag);
        });
    }

    /** The enchantments the bond keeps for its weapon (empty when it keeps none, or when they belong to another weapon). */
    static ItemEnchantments stored(ServerPlayer p, HeroBond b) {
        if (!b.hasExtra(KEY)) return ItemEnchantments.EMPTY;
        CompoundTag c = b.extra(KEY);
        if (!b.kind().itemId().equals(c.getString("kind")) || !c.contains("data")) return ItemEnchantments.EMPTY;
        Tag data = c.get("data");
        var ops = p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return ItemEnchantments.CODEC.parse(ops, data).result().orElse(ItemEnchantments.EMPTY);
    }

    /** Puts the kept enchantments on a projection that is being made. */
    static void apply(ServerPlayer p, HeroBond b, ItemStack projection) {
        ItemEnchantments kept = stored(p, b);
        if (!kept.isEmpty()) projection.set(DataComponents.ENCHANTMENTS, kept);
    }

    /** The bond is released or its weapon changes: what was kept belongs to the old weapon. */
    static void forget(HeroBond b) {
        if (b.hasExtra(KEY)) {
            CompoundTag c = b.extra(KEY);
            for (String k : new java.util.ArrayList<>(c.getAllKeys())) c.remove(k);
        }
    }

    /** For the self-test and the pure checks: is this kind one the menus accept at all? */
    public static boolean accepts(HeroKind kind) { return HeroWeaponItem.enchantable(kind); }
}
