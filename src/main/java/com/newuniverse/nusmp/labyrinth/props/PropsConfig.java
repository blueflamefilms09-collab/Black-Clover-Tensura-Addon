package com.newuniverse.nusmp.labyrinth.props;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the dungeon props, {@code nusmp-labyrinth-props-server.toml}, section {@code [props]}. Since the rooms were rebuilt (docs/labyrinth/rooms_rebuilt.md) the dungeon
 * places NO weapon racks and its armour stands are a rare prop instead of scenery: {@code armourStandRarity} is the chance, in percent, that a room where a stand makes sense (armoury,
 * barracks, guard post, training hall, treasure and vault rooms) holds ONE (0 = never; the stands the shipped templates carry are then dropped too), {@code armourStandFloorCap} the most a
 * floor may hold. The weapon rack itself (block, item, block entity, renderer, loot tables) stays registered and working, so racks in old worlds and racks a player crafts keep working; only
 * the generator no longer builds them: {@code legacyRackConversion} (default false) is the old path that turned the templates' stands into racks, and {@code rackInsteadOfArmourStand} (now
 * default false) only has an effect while it is on. {@code rackLootChance} is the share of converted racks that are the loot-filled variant. Plain {@code define} / {@code defineInRange};
 * every reader goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class PropsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue RACK_INSTEAD_OF_STAND, LEGACY_RACK_CONVERSION;
    public static final ModConfigSpec.DoubleValue RACK_LOOT_CHANCE, ARMOUR_STAND_RARITY;
    public static final ModConfigSpec.IntValue ARMOUR_STAND_FLOOR_CAP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("props");
        RACK_INSTEAD_OF_STAND = b.comment("LEGACY. The armour stands the dungeon's templates carry are built as weapon racks instead. Only has an effect while legacyRackConversion is true. The dungeon no longer places weapon racks by default.")
                .define("rackInsteadOfArmourStand", false);
        LEGACY_RACK_CONVERSION = b.comment("LEGACY switch for the old rack path (turn the templates' armour stands into weapon racks). false (default) = the generator never places a weapon rack. The rack block, item and renderer stay for old worlds and for players.")
                .define("legacyRackConversion", false);
        RACK_LOOT_CHANCE = b.comment("The share of converted racks that are the loot-filled variant (themed gear that can be taken, one piece at a time). 0 = none. Only used by the legacy rack path.")
                .defineInRange("rackLootChance", 0.25, 0.0, 1.0);
        ARMOUR_STAND_RARITY = b.comment("The chance, in percent, that a room where an armour stand makes sense (armoury, barracks, guard post, training hall, treasure and vault rooms) holds ONE, wearing two to four pieces of the dungeon's armour for that depth. Never in corridors, never in two rooms that touch. 0 = no armour stand is ever built (the stands of the old templates are dropped too).")
                .defineInRange("armourStandRarity", 6.0, 0.0, 100.0);
        ARMOUR_STAND_FLOOR_CAP = b.comment("The most armour stands one floor may hold, whatever the chance says.")
                .defineInRange("armourStandFloorCap", 2, 0, 8);
        b.pop();
        SPEC = b.build();
    }

    private PropsConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean rackInsteadOfStand() { return get(RACK_INSTEAD_OF_STAND); }

    /** Is the old rack path on? Both the legacy switch and the old flag must be true: with the defaults the generator never places a rack. */
    public static boolean legacyRackConversion() { return get(LEGACY_RACK_CONVERSION) && get(RACK_INSTEAD_OF_STAND); }

    public static double rackLootChance() { return get(RACK_LOOT_CHANCE); }

    /** The chance in percent that an eligible room holds the rare stand (0..100). */
    public static double armourStandRarity() { return ArmourStandRarity.clampPercent(get(ARMOUR_STAND_RARITY)); }

    public static int armourStandFloorCap() { return Math.max(0, Math.min(ArmourStandRarity.HARD_CAP, get(ARMOUR_STAND_FLOOR_CAP))); }
}
