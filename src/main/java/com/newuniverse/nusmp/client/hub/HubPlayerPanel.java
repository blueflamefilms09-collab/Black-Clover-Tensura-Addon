package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;
import com.newuniverse.nusmp.client.ui.UiDraw;
import com.newuniverse.nusmp.client.ui.UiPoses;
import com.newuniverse.nusmp.client.ui.theme.CodexSkin;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Why: Hub v2 gives the Status page (and the Codex overview) the player's own 3D model doing animations, the idea of the old Codex "You" page carried over (docs/ui_hub.md, "Hub v2"). The model is the real, fully dressed
 * player entity drawn by {@link UiDraw#player} (the vanilla inventory-screen way, one entity render per frame) and posed by the mod's existing {@link UiPoses} (a mixin on the player model that only touches that one
 * entity). {@link HubPoseCycle} decides which pose comes next: idle, on guard, a weapon swing with whatever is in the main hand (the real entity draws its own held item), a cast, a gathering aura, a victory. The
 * figure sways slowly, follows the mouse a little while the pointer is over the panel, can be dragged round, and a click jumps to a new pose. With {@code ui.motion} or {@code ui.hubFx} off it stands still (the
 * "standing" pose, no sway, no follow). Client only, reads only the local player; no packet. Cost per frame: one entity render, one floor circle (at most 8 textured quads), one panel, two strings; no allocation (the
 * name is cached, the quaternions of {@link UiDraw#player} are reused).
 */
final class HubPlayerPanel {
    private float t;                                   // the cycle clock in ticks
    private int step = -1;
    private float pause;                               // ticks the cycle waits after a click
    private float yaw = 25f;                           // the yaw the user dragged to
    private boolean dragging, stillSet;
    private LocalPlayer namedFor;
    private String name = "";

    /** Advances the cycle by {@code dt} ticks (a frame's worth) and eases the pose. Call once per frame while a panel is visible. */
    void update(float dt, boolean fx, float now) {
        if (!fx) {                                     // still: the standing pose, once
            if (!stillSet) {
                stillSet = true;
                step = -1;
                UiPoses.set(UiPoses.indexOf("standing"), 2f);
                UiPoses.step(100f, now);
            }
            return;
        }
        stillSet = false;
        LocalPlayer p = Minecraft.getInstance().player;
        boolean armed = p != null && !p.getMainHandItem().isEmpty();
        if (pause > 0f) pause -= dt;
        else t += dt;
        int s = HubPoseCycle.at(t, armed);
        if (s != step) {
            step = s;
            HubPoseCycle.Step st = HubPoseCycle.STEPS[s];
            int idx = UiPoses.indexOf(st.pose());
            if (idx >= 0) UiPoses.set(idx, st.blend());
        }
        UiPoses.step(dt, now);
    }

    /** The yaw the figure is drawn at: the dragged yaw, swaying slowly, turned a little towards the mouse while it is over {@code r}. */
    float yawNow(boolean fx, Rect r, float mx, float my) {
        if (!fx) return yaw;
        float y = HubPoseCycle.sway(t, yaw);
        if (r.contains(mx, my)) y += HubPoseCycle.follow(mx - (r.x() + r.w() / 2f), r.w() / 2f);
        return y;
    }

    /** Draws the panel: a well, a floor circle, the figure, the player's name above it and the name of the pose under it. */
    void draw(GuiGraphics g, Font font, Rect r, float mx, float my, float now, boolean fx, float alpha) {
        LocalPlayer p = Minecraft.getInstance().player;
        UiPanel.well(g, r.x(), r.y(), r.w(), r.h(), 0xFF000000 | UiTheme.CYAN, alpha);
        if (p == null) return;
        if (namedFor != p) {
            namedFor = p;
            name = font.plainSubstrByWidth(p.getName().getString(), r.w() - 8);
        }
        int cx = r.x() + r.w() / 2;
        CodexSkin.floorCircle(g, cx, r.bottom() - 24, Math.min(r.w() / 2f - 4f, 42f), fx ? now : 40f);
        UiDraw.player(g, p, cx, r.y() + r.h() / 2 + 2, r.h() - 44, yawNow(fx, r, mx, my));
        g.drawCenteredString(font, name, cx, r.y() + 4, UiMath.withAlpha(UiTheme.TEXT, alpha));
        g.drawCenteredString(font, UiPoses.current().caption(), cx, r.bottom() - 12, UiMath.withAlpha(UiTheme.TEXT_DIM, alpha));
    }

    /** True when (mx, my) is on the panel. */
    static boolean contains(Rect r, double mx, double my) { return r.contains(mx, my); }

    /** A click on the figure: a new pose now, and the cycle waits a while before it moves on. */
    void click() {
        UiPoses.roll();
        pause = 140f;
        dragging = true;
    }

    void drag(double dx) {
        if (dragging) yaw += (float) dx * 1.6f;
    }

    void release() { dragging = false; }
}
