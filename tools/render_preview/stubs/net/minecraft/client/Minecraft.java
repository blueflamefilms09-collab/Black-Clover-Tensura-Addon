package net.minecraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

/**
 * Preview stub of Minecraft: the singleton the painters reach through Minecraft.getInstance(): options (camera perspective), player
 * (the local player), level (game time), gameRenderer (main camera), the entity render dispatcher and the frame timer.
 */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();

    public final Options options = new Options();
    public final GameRenderer gameRenderer = new GameRenderer();
    public LocalPlayer player;
    public ClientLevel level;
    private final EntityRenderDispatcher entityRenderDispatcher = new EntityRenderDispatcher(gameRenderer.getMainCamera());
    private float partialTick;

    private final DeltaTracker timer = new DeltaTracker() {
        @Override public float getGameTimeDeltaTicks() { return 20.0F / 60.0F; }
        @Override public float getGameTimeDeltaPartialTick(boolean runsNormally) { return partialTick; }
        @Override public float getRealtimeDeltaTicks() { return 20.0F / 60.0F; }
    };

    private Minecraft() {}

    public static Minecraft getInstance() { return INSTANCE; }

    public DeltaTracker getTimer() { return timer; }
    public EntityRenderDispatcher getEntityRenderDispatcher() { Camera.previewUses++; return entityRenderDispatcher; }
    public boolean isPaused() { return false; }

    /** Preview helper (not in the game): the partial tick the frame being recorded reports. */
    public void previewSetPartialTick(float p) { partialTick = p; }
}
