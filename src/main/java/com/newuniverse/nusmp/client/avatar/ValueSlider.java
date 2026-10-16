package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/** Why: the creation screen needs a slider in the mod's glass look (the vanilla slider is grey), with a label that follows the value and one callback; value 0..1. */
final class ValueSlider extends AbstractSliderButton {
    private final DoubleFunction<Component> label;
    private final Consumer<Double> onChange;
    private final int rgb;

    ValueSlider(int x, int y, int w, int h, double value, int rgb, DoubleFunction<Component> label, Consumer<Double> onChange) {
        super(x, y, w, h, label.apply(value), value);
        this.label = label;
        this.onChange = onChange;
        this.rgb = rgb;
    }

    @Override protected void updateMessage() { setMessage(label.apply(value)); }

    @Override protected void applyValue() { onChange.accept(value); }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        UiPanel.well(g, getX(), getY(), getWidth(), getHeight(), UiTheme.CYAN, 0.8f);
        UiBar.draw(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, (float) value, rgb, UiTheme.now(), isHoveredOrFocused() ? 1f : 0.8f);
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, UiTheme.TEXT);
    }
}
