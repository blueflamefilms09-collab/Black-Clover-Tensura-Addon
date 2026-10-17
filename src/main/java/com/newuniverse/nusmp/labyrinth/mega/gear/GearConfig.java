package com.newuniverse.nusmp.labyrinth.mega.gear;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the standard gear of the Mega-Dungeon, {@code world/serverconfig/nusmp-labyrinth-mega-gear-server.toml}, section {@code [gear]}.
 * Its own file (the pattern of {@code DndConfig}, {@code MegaConfig} and the faction layer's config) so this builder's switches never share a spec with
 * another builder's. These are the KNOBS of one server (how often each grade drops, how fast the mix shifts with depth, the Looting bonus, the ceiling of
 * the magic resistance); the SHAPES (curves, caps, items, enchantments) are data in {@code data/nusmp/mega/gear/tables.json}. The defaults are read from
 * {@link GearRates#DEFAULT}, the one place the numbers live, so the config and the unit tests cannot drift apart. Every value is a plain
 * {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader
 * goes through {@link #get}, which answers the default while the spec is not loaded yet. Since 1.55.0 the chest side has four keys of its own ({@code chestNormal},
 * {@code chestSpecial}, {@code chestRare}, {@code chestTreasureFactor}) and shares the shift and the Looting bonus with the kills. {@code enabled} switches the DROPS from monsters
 * and chests (and the quest hook);
 * {@code maxResist = 0} switches the magic resistance off, so each half has its own off switch.
 */
public final class GearConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, BOSS_GUARANTEED, ENGRAVINGS;
    public static final ModConfigSpec.DoubleValue NORMAL_CHANCE, SPECIAL_CHANCE, RARE_CHANCE, FLOOR_SHIFT, LOOTING_BONUS;
    public static final ModConfigSpec.DoubleValue CHEST_NORMAL, CHEST_SPECIAL, CHEST_RARE, CHEST_TREASURE_FACTOR;
    public static final ModConfigSpec.IntValue MAX_RESIST;
    public static final ModConfigSpec.ConfigValue<String> EXTRA_MAGIC_TYPES;

    static {
        GearRates d = GearRates.DEFAULT;
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("gear");
        ENABLED = b.comment("Monsters and chests of the Mega-Dungeon drop standard Normal / Special / Rare equipment (ordinary vanilla items with a grade; never relics, so there is no world limit). false = no gear drops from either. The magic resistance of gear you already hold is switched off with maxResist = 0, not with this.")
                .define("enabled", true);
        NORMAL_CHANCE = b.comment("Chance per kill that a Normal-grade piece drops at floor 1, before Looting. Hostile non-boss monsters killed by a player only; at most one piece per kill in all.")
                .defineInRange("normalChance", d.normal(), 0.0, 1.0);
        SPECIAL_CHANCE = b.comment("The same for a Special-grade piece (refined craftsmanship).")
                .defineInRange("specialChance", d.special(), 0.0, 1.0);
        RARE_CHANCE = b.comment("The same for a Rare-grade piece (high-grade make with magic resistance).")
                .defineInRange("rareChance", d.rare(), 0.0, 1.0);
        FLOOR_SHIFT = b.comment("How fast the grade mix moves with depth: progress along the data file's curves per floor above the first. 0.02 reaches the end of the curves at floor 51 (Normal falls, Special peaks about floor 26, Rare keeps rising; the total barely changes). 0 = the mix is the same on every floor.")
                .defineInRange("floorShift", d.floorShift(), 0.0, 0.2);
        LOOTING_BONUS = b.comment("Chance added to the TOTAL chance of a drop per level of Looting on the killer's main-hand weapon, shared out between the grades in proportion to their own chances (Looting makes drops more frequent, it does not change their grade).")
                .defineInRange("lootingBonus", d.lootingBonus(), 0.0, 0.25);
        GearRates c = GearRates.CHEST_DEFAULT;
        CHEST_NORMAL = b.comment("CHESTS: chance that a chest rolled in the Mega-Dungeon holds ONE Normal-grade piece at floor 1 (the global loot modifier nusmp:gear_chests; any loot table with a chests/ segment, except the two relic tables). At most one piece per chest in all. 0 for all three chest chances = no gear in chests.")
                .defineInRange("chestNormal", c.normal(), 0.0, 1.0);
        CHEST_SPECIAL = b.comment("CHESTS: the same for a Special-grade piece.")
                .defineInRange("chestSpecial", c.special(), 0.0, 1.0);
        CHEST_RARE = b.comment("CHESTS: the same for a Rare-grade piece.")
                .defineInRange("chestRare", c.rare(), 0.0, 1.0);
        CHEST_TREASURE_FACTOR = b.comment("CHESTS: a treasure chest (a table whose path names a vault, treasure, boss, guardian, crucible, antechamber or reward) multiplies its Special and Rare chances by this (Normal stays as it is: a treasure chest holds better things, not more of them). 1 = treasure chests are like any other. The depth shift (floorShift) and the per-level bonus (lootingBonus, counted per point of Luck of the player who opens the chest) are shared with the kills.")
                .defineInRange("chestTreasureFactor", GearRates.DEFAULT_TREASURE_FACTOR, 1.0, 10.0);
        BOSS_GUARANTEED = b.comment("The Labyrinth Guardian of a sector (the entity with the tag nusmp_sector_guardian) drops one guaranteed Rare piece when a player kills it. Other bosses never drop standard gear.")
                .define("bossGuaranteed", true);
        MAX_RESIST = b.comment("The most magic resistance, in percent, that all the gear a creature wears and holds can give together (armour worn, weapon and shield held). 0 = standard gear gives no resistance at all. Capped at 90 whatever is written here.")
                .defineInRange("maxResist", 40, 0, 90);
        ENGRAVINGS = b.comment("Special and Rare weapons and shields carry Tensura engravings (the enchantments of the #tensura:engraving tag) chosen by the data file. false = vanilla enchantments only. An engraving Tensura does not have is skipped either way.")
                .define("engravings", true);
        EXTRA_MAGIC_TYPES = b.comment("Extra damage types that count as MAGIC for the resistance, on top of vanilla magic and indirect magic and the Tensura spell types: damage-type ids or namespace:* wildcards separated by commas, for example \"mymod:arcane_bolt, othermod:*\". Empty by default.")
                .define("extraMagicTypes", "");
        b.pop();
        SPEC = b.build();
    }

    private GearConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The five rate numbers as one record, read from the config at the moment of a roll. */
    public static GearRates rates() {
        return new GearRates(get(NORMAL_CHANCE), get(SPECIAL_CHANCE), get(RARE_CHANCE), get(FLOOR_SHIFT), get(LOOTING_BONUS));
    }

    /** The same record for a CHEST roll: the three chest chances with the shift and the per-level bonus the kills use. */
    public static GearRates chestRates() {
        return new GearRates(get(CHEST_NORMAL), get(CHEST_SPECIAL), get(CHEST_RARE), get(FLOOR_SHIFT), get(LOOTING_BONUS));
    }
}
