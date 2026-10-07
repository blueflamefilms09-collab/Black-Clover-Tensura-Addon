package net.minecraft.client;

/** Preview stub of DeltaTracker (Minecraft.getTimer() in 1.21.1). The preview reports the frame's partial tick. */
public interface DeltaTracker {
    float getGameTimeDeltaTicks();

    float getGameTimeDeltaPartialTick(boolean runsNormally);

    float getRealtimeDeltaTicks();
}
