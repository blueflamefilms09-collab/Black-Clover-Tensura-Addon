package com.newuniverse.nusmp.companion.growth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: OPTIONAL. A companion with a screen of her own (gauges, a tree of steps, buttons) says so by implementing this; her talk screen then offers one button with the id {@code g:panel} (see
 * {@code TalkView.Builder.panel}) and, when it is picked, the server calls {@link #panelOpen} and closes the talk screen. This mod ships no panel screen: the implementation opens its own (a menu, or its own
 * packet). Nothing in the bond system needs it.
 */
public interface Panelable {
    /** The id of the panel button. */
    String PANEL_ID = "g:panel";

    /** Is the panel available to this player right now (a button that is shown but closed gives {@link #panelClosedReason}). */
    default boolean panelAvailable(ServerPlayer player) { return true; }

    /** Why the panel is closed to this player, shown on the blocked button. */
    default Component panelClosedReason(ServerPlayer player) { return Component.empty(); }

    /** Opens the panel for this player. Called on the server thread, only after the usual checks (reach, alive, rate limit, the button was on the last screen). */
    void panelOpen(ServerPlayer player);
}
