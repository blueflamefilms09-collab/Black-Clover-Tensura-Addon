package com.newuniverse.nusmp.saiyan.armor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: what a wearer has on of the Saiyan alloy set, read from the four armour slots (no allocation, no scan: four slot reads). Common code: the server reads it for the set bonus once a second per
 * Saiyan, the client reads it to draw the worn set instead of the cosmetic battle armour of a form (never both).
 */
public final class SaiyanArmorSet {
    private SaiyanArmorSet() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** How many of the four pieces the entity wears (0..4). */
    public static int worn(LivingEntity e) {
        int n = 0;
        for (EquipmentSlot s : SLOTS) if (e.getItemBySlot(s).getItem() instanceof SaiyanArmorItem) n++;
        return n;
    }

    /** The extra Ki regeneration (percent of the maximum per second) the worn set gives this entity right now. */
    public static double kiRegenBonus(LivingEntity e) {
        return SaiyanArmorRules.kiRegenBonus(worn(e), SaiyanArmorConfig.setBonus(), SaiyanArmorConfig.kiRegenPercent());
    }
}
