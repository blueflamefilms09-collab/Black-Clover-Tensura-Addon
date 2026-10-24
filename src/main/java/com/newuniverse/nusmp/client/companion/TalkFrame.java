package com.newuniverse.nusmp.client.companion;

import java.util.List;

/**
 * Why: everything the painter needs to draw one frame of the talk screen, as plain strings and numbers (no Minecraft type), so the real screen, the preview tool and the tests all fill the same value. The screen turns a
 * server {@code TalkView} into one of these (resolving every Component into the player's language).
 *
 * @param color     the frame colour 0xRRGGBB
 * @param rapport   0..99, or -1 for none
 * @param tierName  the name of the rapport tier ("a friend"), may be empty
 * @param stance    index into {@code stanceNames}, or -1
 * @param useIt     1 / 0 / -1 (not offered)
 * @param slots     nine strings ("" = empty slot, else the count to draw in the corner), or an empty list
 */
public record TalkFrame(String title, String sub, int color, String text, String[] tabNames, int tab, List<Row> rows, int scroll, int hoverRow, int hoverTab, boolean hoverClose,
                        int rapport, String tierName, String statusLine, int stance, int useIt, List<String> slots, int hoverSlot, String closeLabel, String hint) {
    /** One choice. */
    public record Row(String label, boolean blocked) {}
}
