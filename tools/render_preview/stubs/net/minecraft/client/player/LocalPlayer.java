package net.minecraft.client.player;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Preview stub of LocalPlayer (the player of this client: Minecraft.getInstance().player). */
public class LocalPlayer extends AbstractClientPlayer {
    public LocalPlayer(EntityType<?> type, Level level) { super(type, level); }
}
