package com.newuniverse.nusmp.client.multiverse;

import com.newuniverse.nusmp.client.hub.HubWeaponsBody;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.client.weaponpage.WeaponPageClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Why: the Weapon tab of the old Multiverse Status screen (docs/ui_weapon_page.md). It is the same page as the hub's Weapons tab, not a copy: {@link HubWeaponsBody} draws it from {@code WeaponPageClient} (one
 * validated {@code weaponpage_act} packet, the server decides everything) over the pure {@code HubWeaponLayout}; this class only gives it a rectangle on the screen, a scroll offset and the mouse. Cost per
 * frame: one scissor, one translate and the body's own 30 to 60 draw calls; nothing is built per frame. The screen creates it once and calls {@link #opened()} when the tab is switched on.
 */
final class WeaponStatusPage {
    private final HubWeaponsBody body = new HubWeaponsBody();
    private int scroll;

    /** The tab was switched on: translate the fixed texts, cook the page afresh and send the one open request (the server answers only when this client's copy is old). */
    void opened() {
        scroll = 0;
        WeaponPageClient.opened();
        body.opened();
        WeaponPageClient.ensureAsked();
    }

    /** The area of the page: centred, at most 280 wide, from under the title to above the buttons. */
    static int[] area(int screenW, int screenH) {
        int w = Math.max(100, Math.min(280, screenW - 24));
        int h = Math.max(40, screenH - 26 - 52);
        return new int[] {(screenW - w) / 2, 26, w, h};
    }

    private int maxScroll(Font font, int w, int h) { return Math.max(0, body.height(font, w) - h); }

    /** Draws the page into the area; returns the tooltip text (a cut text under the pointer) or null. */
    String draw(GuiGraphics g, Font font, int[] a, int mx, int my) {
        int x = a[0], y = a[1], w = a[2], h = a[3];
        scroll = Mth.clamp(scroll, 0, maxScroll(font, w, h));
        boolean in = mx >= x && mx < x + w && my >= y && my < y + h;
        int lx = mx - x, ly = my - y + scroll;
        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        g.pose().translate(x, y - scroll, 0f);
        body.draw(g, font, w, scroll, scroll + h, in ? lx : -1000, in ? ly : -1000, UiTheme.now());
        g.pose().popPose();
        g.disableScissor();
        int max = maxScroll(font, w, h);
        if (max > 0) {                                                                   // the same thin scroll bar as the hub
            int bar = Math.max(12, h * h / (max + h)), pos = (int) ((h - bar) * (scroll / (float) max));
            g.fill(x + w + 2, y, x + w + 4, y + h, UiMath.withAlpha(0xFFFFFF, 0.08f));
            g.fill(x + w + 2, y + pos, x + w + 4, y + pos + bar, UiMath.withAlpha(UiTheme.GOLD, 0.85f));
        }
        return in ? body.tipAt(font, w, lx, ly) : null;
    }

    boolean click(Font font, int[] a, double mx, double my) {
        if (mx < a[0] || mx >= a[0] + a[2] || my < a[1] || my >= a[1] + a[3]) return false;
        return body.click(font, a[2], mx - a[0], my - a[1] + scroll);
    }

    boolean scrolled(Font font, int[] a, double sy) {
        int max = maxScroll(font, a[2], a[3]);
        if (max <= 0 || sy == 0) return false;
        scroll = Mth.clamp(scroll - (int) Math.signum(sy) * 22, 0, max);
        return true;
    }
}
