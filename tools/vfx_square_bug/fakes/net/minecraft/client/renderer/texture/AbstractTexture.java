package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.systems.RenderSystem;

public class AbstractTexture {
    private final String name;
    public AbstractTexture(String name) { this.name = name; }
    public void setFilter(boolean blur, boolean mipmap) { RenderSystem.LOG.add("filter " + name + " blur=" + blur + " mip=" + mipmap); }
}
