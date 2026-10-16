package net.minecraft.client;

public class Minecraft {
    public static final class Level { public long time; public long getGameTime() { return time; } }
    private static final Minecraft I = new Minecraft();
    public final Level level = new Level();
    public static final class Camera { public net.minecraft.world.phys.Vec3 getPosition() { return net.minecraft.world.phys.Vec3.ZERO; } }
    public static final class GameRenderer { public Camera getMainCamera() { return new Camera(); } }
    public final GameRenderer gameRenderer = new GameRenderer();
    public static Minecraft getInstance() { return I; }
}
