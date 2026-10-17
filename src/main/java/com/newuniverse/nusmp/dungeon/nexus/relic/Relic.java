package com.newuniverse.nusmp.dungeon.nexus.relic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Relics of the Treasury (docs/living_dungeon.md section 7), after D&amp;D 5e homebrew artifacts. Every relic is worn in one of the two Dungeon
 * Relic Curios slots ({@code dungeon_relic}) and works only while worn and attuned to its wearer. Append new relics at the end: the ordinal is
 * part of the bit mask {@link Worn} keeps. The Treasuries of the Living Dungeon that held them, and the pedestal they stood on, were removed in
 * 1.58.0; the relics stay registered and usable ({@code /multiverse dungeon relic give}), and a vault of the Mega-Dungeon may hold them later.
 */
public enum Relic {
    GLUTTONS_CHALICE("glutton_chalice", "The Glutton's Chalice", Slot.RELIC, "Symbiotic Hunger"),
    MANTLE_OF_THE_SCAVENGER("mantle_of_the_scavenger", "Mantle of the Scavenger", Slot.RELIC, "Prey's Weakness"),
    BAND_OF_THE_SPATIAL_ANCHOR("band_of_the_spatial_anchor", "Band of the Spatial Anchor", Slot.RELIC, null),
    DEVIL_BOUND_SHEATH("devil_bound_sheath", "Devil-Bound Weapon Sheath", Slot.RELIC, "Mana Siphon"),
    GAMBLERS_GRIMOIRE_PAGE("gamblers_grimoire_page", "The Gambler's Grimoire Page", Slot.RELIC, "Wild Magic Surge"),
    AMULET_OF_THE_JUGGERNAUTS_BLINDNESS("amulet_of_the_juggernauts_blindness", "Amulet of the Juggernaut's Blindness", Slot.RELIC, "Muffled Mana");

    /**
     * Where a relic is worn: a Curios slot (their ids), or the off-hand. Since the Sentient Dungeon every relic is {@link #RELIC}; the older
     * entries stay so nothing that named them breaks.
     */
    public enum Slot {
        CHARM("charm"), BACK("back"), RING("ring"), BELT("belt"), OFF_HAND("the off-hand"), RELIC("dungeon_relic");

        public final String label;

        Slot(String label) { this.label = label; }
    }

    /** The item's registry path. */
    public final String id;
    public final String label;
    public final Slot slot;
    /** The name of its curse, or null for a relic without one. */
    public final String curse;

    Relic(String id, String label, Slot slot, String curse) {
        this.id = id;
        this.label = label;
        this.slot = slot;
        this.curse = curse;
    }

    /** The item of this relic. */
    public Item item() { return RelicItems.item(this); }

    /** A fresh, unattuned stack of this relic. */
    public ItemStack stack() { return new ItemStack(item()); }

    /** This relic's bit in the mask of what is worn. */
    public int bit() { return 1 << ordinal(); }

    /** The relic with this registry path or enum name (any case), or null. */
    public static Relic byName(String name) {
        if (name == null) return null;
        String n = name.toLowerCase(Locale.ROOT);
        if (n.startsWith("nusmp:")) n = n.substring(6);
        for (Relic r : values()) if (r.id.equals(n) || r.name().toLowerCase(Locale.ROOT).equals(n)) return r;
        return null;
    }

    /** The relic a stack is, or null. */
    public static Relic of(ItemStack stack) { return stack.getItem() instanceof RelicItem r ? r.relic : null; }

    /** The registry paths of every relic (for command suggestions). */
    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Relic r : values()) out.add(r.id);
        return out;
    }
}
