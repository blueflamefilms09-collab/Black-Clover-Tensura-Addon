package net.minecraft.client.player;

public class AbstractClientPlayer {
    public boolean isInvisible() { return false; }
    public int getId() { return 1; }
    public boolean isCrouching() { return false; }
    public double distanceToSqr(net.minecraft.world.phys.Vec3 v) { return 4.0; }
}
