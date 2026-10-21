package com.newuniverse.nusmp.labyrinth.props;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the dungeon props, {@code nusmp-labyrinth-props-server.toml}, section {@code [props]}. {@code rackInsteadOfArmourStand} is the switch the owner's spec asks for: an armour
 * stand is a full entity (it ticks, it is synced, it is saved) and a hall of forty of them is a lag source, so by default the dungeon's templates show their weapons and armour on racks, a block
 * entity drawn by a renderer; false = the armour stands the templates carry stay as they always were. {@code rackLootChance} is the share of converted racks that are the loot-filled variant. Plain
 * {@code define} / {@code defineInRange}; every reader goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class PropsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue RACK_INSTEAD_OF_STAND;
    public static final ModConfigSpec.DoubleValue RACK_LOOT_CHANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("props");
        RACK_INSTEAD_OF_STAND = b.comment("The armour stands the dungeon's templates carry are built as weapon racks (a block entity drawn by a renderer: no entity, no tick) instead. false = the armour stands stay.")
                .define("rackInsteadOfArmourStand", true);
        RACK_LOOT_CHANCE = b.comment("The share of those racks that are the loot-filled variant (themed gear that can be taken, one piece at a time). 0 = none.")
                .defineInRange("rackLootChance", 0.25, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private PropsConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean rackInsteadOfStand() { return get(RACK_INSTEAD_OF_STAND); }

    public static double rackLootChance() { return get(RACK_LOOT_CHANCE); }
}
