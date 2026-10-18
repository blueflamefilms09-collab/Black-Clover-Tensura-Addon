package com.newuniverse.nusmp.client.skilltree.line;

import com.newuniverse.nusmp.client.skilltree.ArtsBarClient;

/**
 * Why: the line engine's packet handlers live in common code that a dedicated server also loads, and they must reach client-only classes without naming them where a server would resolve them. The pattern
 * the Skill Tree already uses (see {@code RouteUnlockedPayload}) is a lambda that calls a public static method of a client class: the lambda body is not resolved until it runs, and it only ever runs on a client.
 * This tiny class is that seam for the line engine: the packet handler calls {@link #onArtState}, which hands the server's word to the Arts bar. It holds no state and does nothing else.
 */
public final class LineClientHooks {
    private LineClientHooks() {}

    /** The server started a cooldown for art {@code key} ({@code route#node}) and says which stance is up. Client thread. */
    public static void onArtState(String key, int cooldownTicks, String stance) { ArtsBarClient.onArtState(key, cooldownTicks, stance); }
}
