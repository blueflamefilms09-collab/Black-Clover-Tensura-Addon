package com.newuniverse.nusmp.labyrinth.loot;

import java.util.List;
import java.util.Locale;

/**
 * Why: the drops of a monster follow its FAMILY, and the dungeon already has one: the Dungeon Master tags every monster it makes with its faction ({@code nusmp_dm_<faction>}),
 * and the entity-type tags {@code nusmp:mega/<faction>} list the members of the eleven factions. Those two are the first answer. A monster that has neither (a mob from another
 * mod that wandered in, a vanilla creature in a rift) is placed by the words of its registry path: {@code ghoul} is undead, {@code kobold} a goblin, {@code kraken} aquatic. A
 * monster none of that places is of the family {@code any}, which only the generic rows of the tables serve. Pure: the game side finds the tag and passes the strings.
 */
public final class LootFamilies {
    private LootFamilies() {}

    /** The generic family: its rows drop from every monster. */
    public static final String ANY = "any";

    private record Words(String family, List<String> words) {}

    private static final List<Words> TABLE = List.of(
            new Words("goblin", List.of("goblin", "kobold")),
            new Words("ogre", List.of("orc", "ogre", "troll", "giant", "minotaur", "piglin", "brute")),
            new Words("undead", List.of("skeleton", "zombi", "ghoul", "lich", "wraith", "undead", "wither", "husk", "drowned", "stray", "revenant", "ghost", "spectre", "vampire", "mummy", "bone")),
            new Words("slime", List.of("slime", "jelly", "ooze", "blob", "pudding")),
            new Words("construct", List.of("golem", "construct", "automaton", "armor", "armour", "gargoyle", "statue", "mimic", "clockwork")),
            new Words("cultist", List.of("cultist", "witch", "mage", "wizard", "illager", "pillager", "evoker", "vindicator", "priest", "necromancer", "sorcerer", "acolyte", "warlock")),
            new Words("demon", List.of("imp", "demon", "devil", "daemon", "fiend", "succubus", "hellhound", "infernal", "oni")),
            new Words("elemental", List.of("wisp", "spirit", "elemental", "blaze", "phantom", "sylph", "salamander", "undine", "gnome", "djinn")),
            new Words("beast", List.of("wolf", "dire", "drake", "dragon", "wyvern", "beast", "hound", "bear", "boar", "lion", "tiger", "fox", "lizard", "basilisk", "serpent", "snake", "hydra", "chimera", "harpy", "hoglin")),
            new Words("aquatic", List.of("kraken", "guardian", "squid", "fish", "naga", "siren", "merfolk", "leviathan", "turtle", "dolphin", "crab", "shark")),
            new Words("insect", List.of("spider", "bee", "silverfish", "insect", "ant", "mantis", "beetle", "scorpion", "centipede", "wasp", "endermite")));

    /** The family of a registry path by its words, or {@link #ANY}. The path is the part after the colon ({@code wither_skeleton}); words match inside it. */
    public static String fromPath(String path) {
        if (path == null) return ANY;
        String p = path.toLowerCase(Locale.ROOT);
        for (Words w : TABLE) for (String word : w.words()) {
            if (word.length() <= 3 ? matchesWord(p, word) : p.contains(word)) return w.family();
        }
        return ANY;
    }

    /** Short words ("ant", "imp", "bee", "orc") only match as a whole underscore-separated word, so "giant" is not an ant and "pimp" is not an imp. */
    private static boolean matchesWord(String path, String word) {
        for (String part : path.split("[_/.\\-]")) if (part.equals(word)) return true;
        return false;
    }

    /** The family of a monster: its Dungeon Master faction tag, else the faction whose type tag holds it, else its words; {@link #ANY} when nothing places it. */
    public static String resolve(String dmFaction, String tagFaction, String entityPath) {
        if (known(dmFaction)) return dmFaction;
        if (known(tagFaction)) return tagFaction;
        return fromPath(entityPath);
    }

    public static boolean known(String family) {
        if (family == null) return false;
        for (Words w : TABLE) if (w.family().equals(family)) return true;
        return false;
    }

    public static List<String> all() { return TABLE.stream().map(Words::family).toList(); }
}
