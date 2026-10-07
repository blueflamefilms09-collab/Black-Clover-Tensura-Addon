package net.minecraft.world.entity;

import net.minecraft.world.level.Level;

/** Preview stub of LivingEntity. */
public class LivingEntity extends Entity {
    public LivingEntity(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }

    public float getHealth() { return 20.0F; }
    public float getMaxHealth() { return 20.0F; }
    public boolean isSprinting() { return false; }
    public boolean isSwimming() { return false; }
    public boolean isFallFlying() { return false; }
    public boolean isUsingItem() { return false; }
}
