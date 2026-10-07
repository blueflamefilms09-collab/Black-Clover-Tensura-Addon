package net.minecraft.world.entity.player;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Preview stub of Player. */
public abstract class Player extends LivingEntity {
    public Player(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }

    public boolean isCreative() { return false; }
}
