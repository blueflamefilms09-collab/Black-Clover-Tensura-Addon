package com.newuniverse.nusmp.skilltree.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Why: the rarity rules ({@code RarityRules}) are pure and take two plain numbers, the killer's Luck and the weapon's Looting level; somebody has to read them off the live game. This is that reader, kept out of
 * the three loot doors so they read a kill in the same way and the cost is one place to audit: each value is read ONCE per drop (a kill or a chest, never per tick), with no scan and no allocation beyond the
 * registry lookup of one enchantment holder. Luck is the player's {@code Attributes.LUCK} (the opening player for a chest); Looting is the level of the Looting enchantment on the weapon that dealt the killing
 * blow (the damage source's weapon item, else the main hand), and 0 whenever it cannot be found: a missing weapon, a registry that has no such enchantment, an exception of any kind. A failed read is never an
 * error, it is "no bonus". Server thread only.
 */
public final class LootStats {
    private LootStats() {}

    /** The Luck of a player; 0 for null or when the attribute cannot be read. NaN and negatives are cleaned by the rules. */
    public static double luckOf(ServerPlayer player) {
        if (player == null) return 0.0;
        try {
            return player.getAttributeValue(Attributes.LUCK);
        } catch (RuntimeException | LinkageError e) {
            return 0.0;
        }
    }

    /** The Looting level of the weapon of a kill (the source's weapon item, else the killer's main hand); 0 when unknown. */
    public static int lootingOf(ServerLevel level, ServerPlayer killer, DamageSource source) {
        if (level == null) return 0;
        try {
            ItemStack weapon = source == null ? ItemStack.EMPTY : source.getWeaponItem();
            if ((weapon == null || weapon.isEmpty()) && killer != null) weapon = killer.getMainHandItem();
            if (weapon == null || weapon.isEmpty()) return 0;
            var looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
            return Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(looting, weapon));
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }
}
