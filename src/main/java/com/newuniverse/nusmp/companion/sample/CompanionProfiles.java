package com.newuniverse.nusmp.companion.sample;

import com.newuniverse.nusmp.companion.bond.Favourites;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/**
 * Why: what the sample companions love, are glad of and cannot stand, and where their lines are, as data in one place (a companion character is one record here, the entity class is shared). Charmy Pappitson is the
 * Cotton and Food Mage of the Black Bulls, who lives to eat, so food is how you win her over; her tastes below are CHOSEN FOR PLAY (a baker's list with a few things no cook would serve), not taken from a wiki page, and
 * are the first thing the owner may want to change. Adding another companion is one more record here, one more egg, and the lang lines under its prefix.
 */
public final class CompanionProfiles {
    private CompanionProfiles() {}

    /** One sample companion's data. {@code characterId} is her record in the arcs' roster (the model, the name and the colour come from it). */
    public record Profile(String characterId, String bondKey, String langPrefix, int fallbackColor, List<Item> loves, List<Item> accepts, List<Item> hates) {}

    public static final Profile CHARMY = new Profile("charmy_pappitson", "nusmp_charmy_pappitson", "nusmp.companion.charmy.", 0xF4EAD0,
            List.of(Items.CAKE, Items.PUMPKIN_PIE, Items.COOKIE, Items.BREAD, Items.COOKED_BEEF),
            List.of(Items.SUGAR, Items.WHEAT),
            List.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.PUFFERFISH));

    private static final Map<String, Profile> BY_CHARACTER = Map.of(CHARMY.characterId(), CHARMY);

    /** The profile of a character id, or Charmy's for anything unknown (the entity type is Charmy's). */
    public static Profile of(String characterId) { return BY_CHARACTER.getOrDefault(characterId == null ? "" : characterId, CHARMY); }

    /** Registers every profile's tastes (call once at setup). */
    public static void registerAll() {
        for (Profile p : BY_CHARACTER.values()) Favourites.register(p.bondKey(), p.loves(), p.accepts(), p.hates());
    }

    public static List<Profile> all() { return List.copyOf(BY_CHARACTER.values()); }
}
