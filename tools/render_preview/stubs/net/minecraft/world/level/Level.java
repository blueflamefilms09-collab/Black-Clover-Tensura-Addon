package net.minecraft.world.level;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/** Preview stub of Level: the game clock the painters read (getGameTime / getDayTime), a random source and the entities by id. */
public class Level {
    public final boolean isClientSide;
    private long gameTime;
    private final RandomSource random = RandomSource.create(42L);
    private final Map<Integer, Entity> entities = new HashMap<>();

    public Level(boolean clientSide) { this.isClientSide = clientSide; }

    public boolean isClientSide() { return isClientSide; }
    public long getGameTime() { return gameTime; }
    public long getDayTime() { return gameTime % 24000L; }
    public RandomSource getRandom() { return random; }
    public Entity getEntity(int id) { return entities.get(id); }

    /** Preview helpers (not in the game). */
    public void previewSetGameTime(long t) { gameTime = t; }
    public void previewAdd(Entity e) { entities.put(e.getId(), e); }
}
