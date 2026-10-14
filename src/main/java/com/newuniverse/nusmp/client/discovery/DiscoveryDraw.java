package com.newuniverse.nusmp.client.discovery;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

/**
 * How an undiscovered entry looks in any screen: a dark slot with a question mark where the icon would be, "???" where the name would be.
 * Nothing of the real thing (its icon, name, text, tooltip) is ever handed to these methods.
 */
public final class DiscoveryDraw {
    private DiscoveryDraw() {}

    public static final int SLOT_FILL = 0xFF16161E, SLOT_EDGE = 0xFF3A3A4A, MARK = 0xFF6A6A80, TEXT = 0xFF8A7F70;

    /** The silhouette of an item icon: a 16 x 16 dark slot with a "?". */
    public static void icon(GuiGraphics g, Font font, int x, int y) {
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
        g.renderOutline(x, y, 16, 16, SLOT_EDGE);
        g.drawCenteredString(font, "?", x + 8, y + 4, MARK);
    }

    /** The "undiscovered" hint line (translated). */
    public static String hint() { return I18n.get("nusmp.discovery.hint"); }

    /** The "Undiscovered" caption (translated). */
    public static String caption() { return I18n.get("nusmp.discovery.undiscovered"); }
}
