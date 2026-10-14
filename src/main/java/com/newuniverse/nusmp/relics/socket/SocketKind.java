package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.relics.djinn.BondRules;

/**
 * What a Core binds: a Spirit or an Underworld Devil. Pure (no Minecraft types). The kind also names the exclusive bond it would fall under if a
 * player took it for themselves ({@link BondRules#SPIRIT} / {@link BondRules#DEVIL}); a Core in a relic never counts as that bond, but the
 * socketing refuses a player who already holds the other kind (see {@link SocketRules#insert}).
 */
public enum SocketKind {
    SPIRIT("spirit", BondRules.SPIRIT),
    DEVIL("devil", BondRules.DEVIL);

    private final String prefix;
    private final int bondBit;

    SocketKind(String prefix, int bondBit) {
        this.prefix = prefix;
        this.bondBit = bondBit;
    }

    /** "spirit" or "devil": the head of a socket id ("spirit:fire"). */
    public String prefix() { return prefix; }

    /** The {@link BondRules} bit of the same kind of bond. */
    public int bondBit() { return bondBit; }
}
