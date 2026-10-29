package com.newuniverse.nusmp.skilltree.loot;

/**
 * Why: the Skill Book loot rules (the odds, the rarity tiers, the pity memory) were removed together with the books (see {@code docs/skill_books_removed.md}). What the Mega-Dungeon's coin loot and quest
 * kill counts still need from them is the plain answer to "what is the dying thing": {@link LootVictims#kindOf} classifies a death, and the coin and quest code only asks that. This enum is that answer and
 * nothing else; it is free of game types.
 */
public final class LootRules {
    private LootRules() {}

    /** What the dying thing is, as far as the coin drops and the quest counts are concerned. */
    public enum Kind {
        /** Anything that does not pay (an NPC, a pet, a boss that is not a sector Guardian, a servant of a player...). */
        OTHER,
        /** An ordinary hostile monster of the dungeon: a vanilla-style {@code Enemy} or one of the Dungeon Master's creatures, neither a boss nor anyone's servant. */
        MONSTER,
        /** The Guardian of a sector (the colossal boss that gates a floor). */
        GUARDIAN
    }
}
