package net.minecraft.client;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.resources.ResourceManager;

/** Why: harness shim. */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    private final ResourceManager resources = new ResourceManager();
    private final SoundManager sounds = new SoundManager();
    public static Minecraft getInstance() { return INSTANCE; }
    public ResourceManager getResourceManager() { return resources; }
    public SoundManager getSoundManager() { return sounds; }
}
