package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Why: harness shim: a single-line text box with a caret at the end. */
public class EditBox extends AbstractWidget {
    private final Font font;
    private String value = "";
    private int max = 32;
    private Component hint = Component.empty();
    private Consumer<String> responder = s -> {};
    private boolean bordered = true;

    public EditBox(Font font, int x, int y, int w, int h, Component title) { this.font = font; this.x = x; this.y = y; this.width = w; this.height = h; }
    public void setBordered(boolean b) { bordered = b; }
    public void setMaxLength(int m) { max = m; }
    public void setValue(String s) { value = s == null ? "" : s; responder.accept(value); }
    public String getValue() { return value; }
    public void setHint(Component c) { hint = c; }
    public void setResponder(Consumer<String> r) { responder = r; }
    public boolean mouseClicked(double mx, double my, int button) { focused = true; return true; }
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 259 && !value.isEmpty()) { value = value.substring(0, value.length() - 1); responder.accept(value); return true; }
        return false;
    }
    public boolean charTyped(char c, int mods) {
        if (!focused || value.length() >= max || c < 32) return false;
        value += c;
        responder.accept(value);
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (value.isEmpty() && !focused) g.drawString(font, hint.getString(), x, y, 0xFF707070, false);
        else g.drawString(font, value + (focused ? "_" : ""), x, y, 0xFFE0E0E0, false);
    }
}
