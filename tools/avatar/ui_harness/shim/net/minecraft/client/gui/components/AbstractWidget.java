package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;

/** Why: harness shim. */
public abstract class AbstractWidget {
    protected int x, y, width, height;
    protected boolean focused;
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setX(int v) { x = v; }
    public void setY(int v) { y = v; }
    public void setWidth(int v) { width = v; }
    public void setFocused(boolean f) { focused = f; }
    public boolean isFocused() { return focused; }
    public abstract void render(GuiGraphics g, int mx, int my, float partial);
}
