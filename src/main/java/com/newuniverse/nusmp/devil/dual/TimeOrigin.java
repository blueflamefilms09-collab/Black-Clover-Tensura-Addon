package com.newuniverse.nusmp.devil.dual;

/**
 * Why: Time magic now has two origins that stand side by side (docs/devil_double_magic.md): the Julius grimoire version (the TIME grimoire magic) and the devil path (the devil attribute TIME: Astaroth, the
 * Kairovore pact). Both identify as Time magic where a rule asks ({@link #isTime}); neither replaces the other, and a player may carry both. Pure; the game side reads the player in {@link DevilMagic#timeOrigin}
 * (one grimoire lookup, only when asked: the status line, a self-test).
 */
public enum TimeOrigin {
    NONE("none"), GRIMOIRE("grimoire"), DEVIL("devil"), BOTH("grimoire and devil");

    private final String label;

    TimeOrigin(String label) { this.label = label; }

    public String label() { return label; }

    public boolean isTime() { return this != NONE; }

    public static TimeOrigin of(boolean grimoireTime, boolean devilTime) {
        return grimoireTime ? (devilTime ? BOTH : GRIMOIRE) : (devilTime ? DEVIL : NONE);
    }
}
