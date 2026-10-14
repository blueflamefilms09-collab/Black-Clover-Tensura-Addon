package com.newuniverse.nusmp.core.magic.profile;

/**
 * The last resort: a profile for an id that is neither an enum constant nor a registered merged magic. Every part is the interface
 * default (generic wiki-style effect, GLOW particle, Resistance guard, a colour generated from the id). It exists so that a stale id in a
 * save file, a command typo or a datapack can never become a null pointer.
 */
public final class FallbackMagicProfile implements MagicProfile {
    private final String id;

    public FallbackMagicProfile(String id) { this.id = MagicKeys.canonical(id).isEmpty() ? "UNKNOWN" : MagicKeys.canonical(id); }

    @Override public String id() { return id; }
}
