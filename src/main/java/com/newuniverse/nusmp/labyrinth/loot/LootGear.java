package com.newuniverse.nusmp.labyrinth.loot;

import net.minecraft.world.entity.EquipmentSlotGroup;

/**
 * Why: the three item classes of loot gear (a held weapon, a worn piece of armour, a worn trinket) share one job, to carry a {@link GearRoll}, and everything that builds,
 * rolls, prices or shows a roll needs the same five facts about the item: what kind of piece it is, which stat is its primary line, which equipment slot group its modifiers
 * belong to, its base sell value, and for a weapon its type and attack speed. This interface is those facts, so {@link GearStacks} and the tooltip never switch on a
 * class.
 */
public interface LootGear {
    GearKind gearKind();

    /** The registry path of the item. */
    String lootId();

    /** The stat that is always the first line of a roll of this piece. */
    StatKind primary();

    /** The equipment slot group the piece's attribute modifiers belong to. */
    EquipmentSlotGroup slotGroup();

    /** The base sell value in bronze coins (before the rarity's factor). */
    int baseValue();

    /** The weapon type of a weapon (a class name of {@code weapon_classes.json}); empty for everything else. */
    default String weaponType() { return ""; }

    /** The attacks per second of a weapon; 0 for everything else. */
    default double attackSpeed() { return 0.0; }
}
