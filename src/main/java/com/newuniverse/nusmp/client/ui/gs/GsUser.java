package com.newuniverse.nusmp.client.ui.gs;

import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;

/**
 * The skin the local player sees. The "devil user" flag is computed by the server ({@code multiverse.DevilUser}) and arrives inside the status
 * summary the mod already syncs, so this reads one boolean of a tag already in memory: no packet, no scan. The client option
 * {@code ui.greatSpirit.skin} can force a skin.
 */
public final class GsUser {
    private GsUser() {}

    /** True when the server marked the local player as a devil user (false before the first status arrives). */
    public static boolean devilUser() { return MultiverseStatusClient.status().getBoolean("Devil"); }

    /** The skin to draw with right now. */
    public static GsSkin skin() { return GsSkin.choose(UiThemeOptions.skinChoice(), devilUser()); }
}
