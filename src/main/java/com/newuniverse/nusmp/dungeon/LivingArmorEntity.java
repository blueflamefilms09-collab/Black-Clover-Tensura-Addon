package com.newuniverse.nusmp.dungeon;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A Living Armor (tier 2): a colony of mollusks that has made a home of armour somebody left in the Dungeon. It stalks the slimes and eats
 * them; a hungry one takes players too. The armour it wears is what it found, and is not loot.
 */
public class LivingArmorEntity extends DungeonCreature {
    public LivingArmorEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 22).add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.ARMOR, 4).add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override public int tier() { return 2; }

    @Override public boolean isPrey(LivingEntity e) { return e instanceof DungeonSlimeEntity; }

    @Override public boolean huntsPlayers() { return true; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return out;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource rnd, DifficultyInstance difficulty) {
        ItemStack[][] sets = {
                {new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_BOOTS)},
                {new ItemStack(Items.CHAINMAIL_HELMET), new ItemStack(Items.CHAINMAIL_CHESTPLATE), new ItemStack(Items.CHAINMAIL_LEGGINGS), new ItemStack(Items.CHAINMAIL_BOOTS)},
                {new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.GOLDEN_CHESTPLATE), new ItemStack(Items.GOLDEN_LEGGINGS), new ItemStack(Items.GOLDEN_BOOTS)}};
        ItemStack[] set = sets[rnd.nextInt(sets.length)];
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) if (rnd.nextFloat() < 0.85f || i == 1) {
            setItemSlot(slots[i], set[i].copy());
            setDropChance(slots[i], 0f);                                      // found armour, not loot
        }
    }
}
