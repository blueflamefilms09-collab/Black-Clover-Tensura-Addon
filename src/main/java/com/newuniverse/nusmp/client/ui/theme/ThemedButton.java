package com.newuniverse.nusmp.client.ui.theme;

import com.newuniverse.nusmp.client.ui.gs.GsDraw;
import com.newuniverse.nusmp.client.ui.gs.GsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A normal vanilla {@link Button} (same click handling, narration and focus) whose drawing follows the theme: in the anime look a
 * translucent panel with an accent edge that brightens when hovered, in the classic look the vanilla button, unchanged. In the great_spirit
 * look (2.x) a hover also starts the slime ripple: up to three rings from the point where the pointer entered, clipped to the button.
 */
public final class ThemedButton extends Button {
    private float hover;
    private float lastNow = -1f;
    private boolean wasHot;                                  // great_spirit: the ripple starts when the pointer enters
    private float rippleAt = -1000f, rippleX, rippleY;

    public ThemedButton(int x, int y, int w, int h, Component message, Button.OnPress onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (!UiTheme.anime()) {
            super.renderWidget(g, mouseX, mouseY, partial);
            return;
        }
        float now = UiTheme.now();
        float dt = lastNow < 0f ? 1f : Math.min(6f, now - lastNow);
        lastNow = now;
        hover = UiMath.approach(hover, isHoveredOrFocused() && active ? 1f : 0f, 0.25f, dt, 0.01f);
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int accent = UiTheme.greatSpirit() ? 0xFF000000 | GsDraw.NEON[GsUser.skin().ordinal()] : UiTheme.CYAN;      // the skin's neon in the great_spirit look
        UiPanel.panel(g, x, y, w, h, active ? UiMath.mix(UiTheme.TEXT_DIM, accent, hover) : UiTheme.TEXT_FAINT, 0.25f + 0.75f * hover, 1f);
        UiPanel.hover(g, x + 2, y + 2, w - 4, h - 4, hover, accent);
        int color = active ? UiMath.mix(UiTheme.TEXT, UiTheme.WHITE, hover) : UiTheme.TEXT_FAINT;
        if (UiTheme.greatSpirit() && UiTheme.motion()) {
            boolean hot = isHoveredOrFocused() && active;
            if (hot && !wasHot) {
                rippleAt = UiTheme.realNow();
                rippleX = Math.max(x, Math.min(x + w, mouseX));
                rippleY = Math.max(y, Math.min(y + h, mouseY));
            }
            wasHot = hot;
            GsDraw.ripple(g, x, y, w, h, rippleX, rippleY, UiTheme.realNow(), rippleAt, GsUser.skin());
        }
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + (h - 8) / 2, color);
    }
}
