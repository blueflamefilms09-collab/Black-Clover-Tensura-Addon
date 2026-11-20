package com.newuniverse.nusmp.lords;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.sinweapons.SinWeaponBoot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Why: the eight NEW forms of the four Mortal Sin weapons are eight item ids in their own register (attached to the mod bus by {@link LordBoot}); form 0 of each weapon is the old item of {@code SinWeaponBoot}, kept.
 * The form switch swaps the item id in the slot and keeps the components.
 */
public final class LordItems {
    private LordItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    private static final List<DeferredItem<LordItem>> NEW_FORMS = new ArrayList<>();

    /** attack bonus on top of netherite, attack speed, per new form (form 1, form 2) of each weapon. */
    private static final float[][][] STATS = {
            {{8.0f, -3.3f}, {5.0f, -2.8f}},       // Sloth: Titan's Cleaver, Chained Anchor
            {{2.0f, -2.2f}, {4.0f, -2.0f}},       // Envy: Coveted Rifle, Serpent Coil
            {{5.0f, -2.5f}, {6.0f, -2.9f}},       // Vanity: Mirror Glaive, Peacock Halberd
            {{7.0f, -2.8f}, {4.0f, -2.0f}}};      // Holy: Seifort Radiant, Seifort Sent Forth

    static {
        for (LordRules.Sin s : LordRules.Sin.values()) {
            if (!s.isLord()) continue;
            for (int f = 1; f < s.forms(); f++) {
                final LordRules.Sin sin = s;
                final int form = f;
                final float[] st = STATS[s.ordinal()][f - 1];
                NEW_FORMS.add(REGISTER.register(s.items[f], () -> new LordItem(sin, form, st[0], st[1])));
            }
        }
    }

    /** The four old item ids that were remade in Blender (their old meshes and skins live in weapon_meshes/classic): the client setting lords.classicModels draws those. */
    public static boolean isRemade(String id) {
        for (LordRules.Sin s : LordRules.Sin.values()) if (s.isLord() && s.items[0].equals(id)) return true;
        return false;
    }

    /** The item of a form of a Lord weapon: form 0 is the old item, the others are registered here. */
    public static Item of(LordRules.Sin sin, int form) {
        if (form <= 0) {
            return switch (sin) {
                case SLOTH -> SinWeaponBoot.SLOTH.get();
                case ENVY -> SinWeaponBoot.ENVY.get();
                case VANITY -> SinWeaponBoot.VANITY.get();
                default -> SinWeaponBoot.HOLY.get();
            };
        }
        return BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, sin.items[Math.min(form, sin.forms() - 1)]));
    }

    /** The Lord weapon a stack is (any form), or null. */
    public static LordRules.Sin sinOf(ItemStack s) {
        if (s.isEmpty()) return null;
        Item it = s.getItem();
        if (it instanceof LordItem l) return l.sin();
        if (it instanceof com.newuniverse.nusmp.sinweapons.SinWeaponItem w) return fromKind(w.kind());
        return null;
    }

    /** The form of a stack of a Lord weapon (0 for the old item), or -1. */
    public static int formOf(ItemStack s) {
        if (s.isEmpty()) return -1;
        Item it = s.getItem();
        if (it instanceof LordItem l) return l.form();
        if (it instanceof com.newuniverse.nusmp.sinweapons.SinWeaponItem w && fromKind(w.kind()) != null) return 0;
        return -1;
    }

    /** The sin of a sinweapons kind; null for the Chakrams (not a Lord weapon). */
    public static LordRules.Sin fromKind(com.newuniverse.nusmp.sinweapons.SinRules.Kind k) {
        return switch (k) {
            case SLOTH -> LordRules.Sin.SLOTH;
            case ENVY -> LordRules.Sin.ENVY;
            case VANITY -> LordRules.Sin.VANITY;
            case HOLY -> LordRules.Sin.HOLY;
            default -> null;
        };
    }

    /** For the weapon renderer's list of 3D weapons. */
    public static List<Supplier<? extends Item>> renderables() { return new ArrayList<>(NEW_FORMS); }

    /** A bare stack of form 0 (the creative tab's old entry); it becomes THE weapon only if none exists. */
    public static ItemStack bare(LordRules.Sin sin) { return new ItemStack(of(sin, 0)); }
}
