package com.newuniverse.nusmp.avatar.aptitude;

import java.util.List;

/**
 * Why: a Magic Aptitude is what the Avatar's creator chooses (or gambles for) in the Soul Forge: a magic family the Avatar will be good at, with a price. It is
 * pure data (no game types) so the Soul Forge screen (client), the creation validator (server), the Avatar's casting loadout and the tests all read the same
 * numbers. The rarity decides how likely the random roll is to land on it and how large the permanent tax on the creator's magicule pool is when it is picked on
 * purpose: a rare aptitude chosen by hand costs a lot, the same aptitude won by the free gamble costs nothing, which is the whole point of the gamble.
 *
 * @param id         stable id ("fire", "spatial", "anti_magic"), saved in the Avatar's NBT
 * @param name       display name
 * @param rarity     how rare it is (random weights and the tax scale read it)
 * @param taxPercent permanent percent of the creator's maximum magicule taken when the aptitude is chosen by hand (0 for a common one; the Soul Forge shows it)
 * @param magic      the id of the mod magic family it maps to (an existing {@code MagicType} id such as "fire"), or empty for a pure Tensura aptitude
 * @param skills     Tensura skill ids ("tensura:flame_manipulation") the Avatar may use because of it (only ids that exist)
 */
public record Aptitude(String id, String name, Rarity rarity, double taxPercent, String magic, List<String> skills) {
    public Aptitude {
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("an aptitude id is snake_case: " + id);
        name = name == null || name.isBlank() ? id : name;
        rarity = rarity == null ? Rarity.COMMON : rarity;
        taxPercent = Math.max(0.0, Math.min(90.0, taxPercent));
        magic = magic == null ? "" : magic;
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    /** How rare an aptitude is. Only ever APPEND entries (the name is saved in data packs). */
    public enum Rarity {
        COMMON(60), UNCOMMON(25), RARE(10), EPIC(4), LEGENDARY(1);

        private final int weight;

        Rarity(int weight) { this.weight = weight; }

        /** The base weight of the random roll (before the creator's luck shifts it towards the rarer ones). */
        public int weight() { return weight; }
    }
}
