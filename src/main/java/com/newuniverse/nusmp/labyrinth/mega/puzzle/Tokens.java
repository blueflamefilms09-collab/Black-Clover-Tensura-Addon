package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.List;
import java.util.Map;

/**
 * Why: the puzzles that ask the party to carry or offer something (riddle offerings, keys and locks, elemental shards) need a vocabulary that is the same in the pure rules, the
 * block palettes and the world: a TOKEN is a short word. The rule only ever sees the word; the world gives the player a real item that carries the word (a named, tagged item that cannot be
 * confused with loot and is removed when the puzzle ends) and, for the riddle offerings, also accepts the ordinary item the word stands for (a gold ingot for {@code gold}), so a delver
 * who happens to carry the answer can use it. The order of {@link #ITEMS} is the order of the {@code token} palette. Pure.
 */
public final class Tokens {
    private Tokens() {}

    /** The answers of the item riddles, in palette order. */
    public static final List<String> ITEMS = List.of("coal", "bone", "gold", "lapis", "amethyst", "redstone", "iron", "quartz", "emerald", "diamond", "copper");

    /** The four elements, in palette order of the {@code elem} palette. */
    public static final List<String> ELEMENTS = List.of("fire", "water", "earth", "air");

    /** The ordinary item each token also accepts, and the item a token is issued as. */
    public static final Map<String, String> ITEM_ID = Map.ofEntries(
            Map.entry("coal", "minecraft:coal"), Map.entry("bone", "minecraft:bone"), Map.entry("gold", "minecraft:gold_ingot"), Map.entry("lapis", "minecraft:lapis_lazuli"),
            Map.entry("amethyst", "minecraft:amethyst_shard"), Map.entry("redstone", "minecraft:redstone"), Map.entry("iron", "minecraft:iron_ingot"), Map.entry("quartz", "minecraft:quartz"),
            Map.entry("emerald", "minecraft:emerald"), Map.entry("diamond", "minecraft:diamond"), Map.entry("copper", "minecraft:copper_ingot"),
            Map.entry("fire", "minecraft:blaze_powder"), Map.entry("water", "minecraft:prismarine_shard"), Map.entry("earth", "minecraft:clay_ball"), Map.entry("air", "minecraft:feather"),
            Map.entry("key0", "minecraft:tripwire_hook"), Map.entry("key1", "minecraft:tripwire_hook"), Map.entry("key2", "minecraft:tripwire_hook"), Map.entry("key3", "minecraft:tripwire_hook"));

    /** Does the token stand for an everyday item that may also be offered as it is (not a key or a shard)? */
    public static boolean ordinary(String token) { return ITEMS.contains(token); }

    public static int itemIndex(String token) { return ITEMS.indexOf(token); }
}
