package com.newuniverse.nusmp.charter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What this client knows about the world's drop lock (filled by the {@code nusmp:charter_sync} packet, read by the tooltips). A tiny static: it
 * is only ever replaced as a whole, so a reader sees either the old or the new picture. Nothing here is saved; the server sends it again at login.
 */
public final class CharterClaims {
    private CharterClaims() {}

    /** Who claimed a grimoire and on which day (from 1). */
    public record Claim(String name, long day) {}

    private static volatile Map<String, Claim> claims = Collections.emptyMap();

    /** The claim of a boss id, or null if the grimoire is unclaimed (as far as this client has heard). */
    public static Claim get(String bossId) { return claims.get(bossId); }

    public static Map<String, Claim> all() { return claims; }

    /** Replaces the whole picture. */
    public static void replace(Map<String, Claim> now) { claims = Collections.unmodifiableMap(new LinkedHashMap<>(now)); }
}
