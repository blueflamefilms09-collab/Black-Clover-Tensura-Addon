package net.minecraft.client;

import net.minecraft.client.renderer.texture.TextureManager;

public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    private final TextureManager textures = new TextureManager();
    public static Minecraft getInstance() { return INSTANCE; }
    public TextureManager getTextureManager() { return textures; }
}
