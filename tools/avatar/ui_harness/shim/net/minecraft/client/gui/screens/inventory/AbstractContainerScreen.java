package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Why: harness shim: the parts of the container screen the Soul Forge relies on, with no behaviour of their own. */
public abstract class AbstractContainerScreen<T> {
    protected int imageWidth = 176, imageHeight = 166, leftPos, topPos;
    public int width, height;
    protected Font font = new Font();
    protected Minecraft minecraft = Minecraft.getInstance();
    protected T menu;
    public boolean closed;

    public AbstractContainerScreen(T menu, Inventory inv, Component title) { this.menu = menu; }

    public final void harnessInit(int w, int h) { width = w; height = h; init(); }

    protected void init() { leftPos = (width - imageWidth) / 2; topPos = (height - imageHeight) / 2; }
    protected void containerTick() {}
    public void render(GuiGraphics g, int mx, int my, float partial) {}
    protected abstract void renderBg(GuiGraphics g, float partial, int mx, int my);
    protected void renderLabels(GuiGraphics g, int mx, int my) {}
    protected <W> W addWidget(W w) { return w; }
    public boolean keyPressed(int key, int scan, int mods) { if (key == 256) { onClose(); return true; } return false; }
    public boolean charTyped(char c, int mods) { return false; }
    public boolean mouseClicked(double mx, double my, int button) { return false; }
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return false; }
    public boolean mouseReleased(double mx, double my, int button) { return false; }
    public boolean mouseScrolled(double mx, double my, double sx, double sy) { return false; }
    public void onClose() { closed = true; }
    public boolean isPauseScreen() { return false; }
    public final void harnessTick() { containerTick(); }
}
