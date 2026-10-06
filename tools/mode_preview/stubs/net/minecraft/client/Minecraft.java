package net.minecraft.client;

public class Minecraft {
    public static final class Level { public long time; public long getGameTime() { return time; } }
    private static final Minecraft I = new Minecraft();
    public final Level level = new Level();
    public static Minecraft getInstance() { return I; }
}
