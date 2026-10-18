package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.heroweapon.HeroKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the survival route to a hero weapon is nineteen crafting recipes, and their ingredients are a game-design decision (what a player must find to be
 * chosen), not an accident of some JSON files. This pure class (no Minecraft types, so JUnit can run it) is the table of that decision: the Holy Calling
 * Gem's pattern, the Gem Fragment's shard recipe, and for each of the seventeen Seven Star and Vassal Weapons the signature material its Seal is made from,
 * each with the reason it was chosen. {@code tools/heroweapon/gen_gem_recipes.py} reads THIS file to write the recipe JSON under
 * {@code data/nusmp/recipe/}, and {@code GemRecipesTest} reads the JSON back and fails when the two ever differ, so the table and the shipped data cannot
 * drift apart. Every ingredient is a vanilla item the player can always obtain (no Tensura item, nothing that only an operator can give), the sets are all
 * different, and no recipe needs more than the nine slots of the crafting grid. The cost is a design default the owner may change by editing this table
 * and running the script.
 *
 * <p>The logic: the Holy Calling Gem is the rarest thing in the route because it chooses among the four Legendary Weapons: a nether star at the centre
 * (the Wither is the late-game gate), four netherite ingots (the Nether's top metal), and one block of each of the four canon jewel colours at the corners:
 * green for the Shield (emerald), blue for the Sword (lapis), red for the Spear (redstone) and yellow for the Bow (gold). A Seal is cheaper: the
 * series says the Vassal Weapons choose natives who prove themselves, so a Seal needs a Gem Fragment (4 amethyst shards, a gold ingot and an ender pearl:
 * a day's work in the Overworld and a night's hunt) plus one signature material, which is the weapon itself in vanilla clothes: a hoe for the scythe, a
 * goat horn for the instrument, shears and wool for the sewing set. A Holy Gem is NOT part of a Seal, as the series has the Vassal Weapons come to people
 * of their own world, but the cost of a Seal is still a real errand.
 */
public final class GemRecipes {
    private GemRecipes() {}

    /** One kind of ingredient and how many of it a recipe uses; {@code item} is a registry id such as {@code minecraft:iron_block}. */
    public record Part(String item, int count) {}

    /** The recipe of one Seal: its weapon, what it is made from besides the fragment, and why that was chosen. */
    public record Seal(HeroKind kind, String why, List<Part> parts) {
        /** The crafting slots used, counting the Gem Fragment (always one). */
        public int slots() {
            int n = 1;
            for (Part p : parts) n += p.count();
            return n;
        }

        /** One registry id per slot, the fragment included, sorted: the multiset that identifies the recipe (shapeless, so order does not matter). */
        public List<String> slotIds() {
            List<String> out = new ArrayList<>();
            out.add(FRAGMENT_ITEM);
            for (Part p : parts) for (int i = 0; i < p.count(); i++) out.add(p.item());
            out.sort(null);
            return out;
        }
    }

    /** The registry id of the Gem Fragment, the one ingredient every Seal shares. */
    public static final String FRAGMENT_ITEM = "nusmp:gem_fragment";

    // ------------------------------------------------------------------ the Holy Calling Gem (shaped, 3 x 3)
    /** The pattern of the Holy Calling Gem: emerald, lapis, redstone and gold blocks at the corners (the four jewel colours), netherite on the edges, a nether star in the middle. */
    static final List<String> HOLY_PATTERN = List.of("ENL", "NSN", "RNG");
    /** The key of that pattern. */
    static final Map<Character, String> HOLY_KEYS = Map.of(
            'E', "minecraft:emerald_block",
            'L', "minecraft:lapis_block",
            'R', "minecraft:redstone_block",
            'G', "minecraft:gold_block",
            'N', "minecraft:netherite_ingot",
            'S', "minecraft:nether_star");

    // ------------------------------------------------------------------ the Gem Fragment (shapeless)
    /** What one Gem Fragment is made from: amethyst for the crystal, gold for the setting, an ender pearl for the call that crosses worlds. */
    static final List<Part> FRAGMENT_PARTS = List.of(
            new Part("minecraft:amethyst_shard", 4),
            new Part("minecraft:gold_ingot", 1),
            new Part("minecraft:ender_pearl", 1));

    // ------------------------------------------------------------------ the seventeen Seals (shapeless; the fragment is added to every one)
    private static final Map<HeroKind, Seal> SEALS = new LinkedHashMap<>();

    private static void seal(HeroKind kind, String why, Part... parts) { SEALS.put(kind, new Seal(kind, why, List.of(parts))); }

    private static Part p(String item, int count) { return new Part(item, count); }

    static {
        // ---- the Seven Star Weapons
        seal(HeroKind.HAMMER, "A hammer is a block of iron on a stick; two blocks make a head worth the name.", p("minecraft:iron_block", 2), p("minecraft:stick", 1));
        seal(HeroKind.CLAW, "Claws of bone with the hide of a night hunter drawn over them; phantoms only come to those who skip sleep.", p("minecraft:phantom_membrane", 2), p("minecraft:bone", 2));
        seal(HeroKind.AXE, "The axe's own namesake given up whole, a diamond axe, with an iron block for the weight behind the blade.", p("minecraft:diamond_axe", 1), p("minecraft:iron_block", 1));
        seal(HeroKind.WHIP, "A lash is braided hide and cord; a lead (four strings and a slime ball) is the nearest thing to a whip the world makes.", p("minecraft:lead", 2), p("minecraft:leather", 2));
        seal(HeroKind.STAFF, "A rod of the Nether's fire with a crystal set at its head.", p("minecraft:blaze_rod", 2), p("minecraft:amethyst_block", 1));
        seal(HeroKind.GAUNTLET, "An armoured glove: plates of iron riveted onto leather.", p("minecraft:iron_ingot", 4), p("minecraft:leather", 2));
        seal(HeroKind.PROJECTILE, "Throwing stars are hammered from iron nuggets and edged with flint.", p("minecraft:iron_nugget", 6), p("minecraft:flint", 2));
        // ---- the other Vassal Weapons
        seal(HeroKind.CARRIAGE, "A Filolial queen's coach: a cart on wheels, a saddle for the bird that draws it, and its feathers.", p("minecraft:minecart", 1), p("minecraft:saddle", 1), p("minecraft:feather", 4));
        seal(HeroKind.FAN, "Ribs of bamboo, paper folded between them, and feathers for the wind.", p("minecraft:feather", 3), p("minecraft:paper", 3), p("minecraft:bamboo", 2));
        seal(HeroKind.SCYTHE, "The hoe is the farmer's scythe; souls drawn from the Nether's sand make it a reaper's.", p("minecraft:diamond_hoe", 1), p("minecraft:soul_sand", 2));
        seal(HeroKind.KATANA, "Tatara steel is iron folded in charcoal fire, and flint is the edge it is ground against.", p("minecraft:iron_ingot", 3), p("minecraft:charcoal", 2), p("minecraft:flint", 1));
        seal(HeroKind.BOOK, "A written book for the words, ink for the next page, and a whole bookshelf for the library the book remembers.", p("minecraft:written_book", 1), p("minecraft:ink_sac", 2), p("minecraft:bookshelf", 1));
        seal(HeroKind.MIRROR, "Glass darkened with amethyst, a gilded frame, and a pearl of the End so that it shows what is not here.", p("minecraft:tinted_glass", 2), p("minecraft:gold_ingot", 2), p("minecraft:ender_pearl", 1));
        seal(HeroKind.INSTRUMENT, "The goat horn is the only instrument the wild gives up, and note blocks tune it.", p("minecraft:goat_horn", 1), p("minecraft:note_block", 2));
        seal(HeroKind.HARPOON, "A barbed iron head on a line, with shards from the ocean monument's keepers.", p("minecraft:fishing_rod", 1), p("minecraft:prismarine_shard", 3), p("minecraft:iron_ingot", 2));
        seal(HeroKind.SEWING_SET, "Shears cut the cloth, string is the thread, wool is the cloth.", p("minecraft:shears", 1), p("minecraft:string", 3), p("minecraft:white_wool", 2));
        seal(HeroKind.CHAIN, "Four links of the heaviest chain the world makes, and a block of iron to drag.", p("minecraft:chain", 4), p("minecraft:iron_block", 1));
    }

    /** The Seal recipes in catalogue order (one per weapon that has a Seal, when the table is complete). */
    public static List<Seal> seals() {
        List<Seal> out = new ArrayList<>();
        for (HeroKind k : GemCatalog.sealKinds()) {
            Seal s = SEALS.get(k);
            if (s != null) out.add(s);
        }
        return out;
    }

    /** The Seal recipe of a weapon, or null when it has none. */
    public static Seal sealOf(HeroKind kind) { return SEALS.get(kind); }

    /** The Holy Calling Gem's nine slots read row by row, one registry id per cell (all nine cells are filled). */
    public static List<String> holySlotIds() {
        List<String> out = new ArrayList<>();
        for (String row : HOLY_PATTERN) for (char c : row.toCharArray()) out.add(HOLY_KEYS.get(c));
        return out;
    }

    /** The Gem Fragment's ingredients, one registry id per slot, sorted. */
    public static List<String> fragmentSlotIds() {
        List<String> out = new ArrayList<>();
        for (Part p : FRAGMENT_PARTS) for (int i = 0; i < p.count(); i++) out.add(p.item());
        out.sort(null);
        return out;
    }

    /** Every vanilla registry id the table uses, sorted and without repeats (the test checks each against the game's own item list). */
    public static List<String> usedVanillaIds() {
        List<String> all = new ArrayList<>(holySlotIds());
        all.addAll(fragmentSlotIds());
        for (Seal s : seals()) all.addAll(s.slotIds());
        all.removeIf(id -> !id.startsWith("minecraft:"));
        String[] sorted = all.stream().distinct().sorted().toArray(String[]::new);
        return Arrays.asList(sorted);
    }
}
