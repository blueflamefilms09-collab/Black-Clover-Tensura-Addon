package com.newuniverse.nusmp.relics.socket;

import java.util.Locale;

/**
 * The six elements of a bound Spirit (and the look of a bound Devil). Entries are only appended. The colour is the emissive pass and the particles'
 * colour; the {@link SocketFxKind} says which ambient particle family the client draws. Pure.
 */
public enum SocketElement {
    FIRE(0xFF6A1A, SocketFxKind.EMBER),
    WATER(0x3AA8FF, SocketFxKind.DROPLET),
    WIND(0xA8F0D0, SocketFxKind.WISP),
    EARTH(0xB08040, SocketFxKind.MOTE),
    LIGHT(0xFFF0A0, SocketFxKind.LIGHT),
    DARK(0x6A2AA0, SocketFxKind.SMOKE);

    private final int rgb;
    private final SocketFxKind fx;

    SocketElement(int rgb, SocketFxKind fx) {
        this.rgb = rgb;
        this.fx = fx;
    }

    public int rgb() { return rgb; }

    public SocketFxKind fx() { return fx; }

    /** "fire", "water", ...: the key of a Spirit Core and the name in the lang keys. */
    public String key() { return name().toLowerCase(Locale.ROOT); }
}
