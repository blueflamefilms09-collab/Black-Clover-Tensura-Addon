package com.newuniverse.nusmp.heroweapon.curse;

import java.util.Locale;

/**
 * Why: despair is not one number that something bumps, it is a short list of terrible things that can happen to a wielder, each with its own weight and its own
 * pause (an ally dying is awful, but twenty pets dying in one blast must not be twenty times as awful; a near-death is counted once a minute, not once a
 * heartbeat). The cause list is closed and lives in one enum so the event handlers, the config keys, the commands, the log lines and the tests all use the same
 * words, and a report from another system ({@code HeroCurse.addDespair(player, amount, why)}) is matched against {@link #key} to find the pause that cause
 * deserves. The default amounts and pauses are the numbers the config starts with. No Minecraft types: the pure machine and its tests use this.
 */
public enum Cause {
    /** A tamed animal or other owned creature of the wielder died within reach. */
    PET_DEATH(150, Pause.ALLY),
    /** The wielder's Avatar died within reach. */
    AVATAR_DEATH(300, Pause.ALLY),
    /** A party or FTB team member died within reach. */
    PARTY_DEATH(250, Pause.ALLY),
    /** A friendly character of the arcs (an arc NPC) died within reach. */
    NPC_DEATH(200, Pause.ALLY),
    /** An ally, a party member or the wielder's own summon hurt them. */
    BETRAYAL(200, Pause.BETRAYAL),
    /** Health fell below the near-death line (counted once a minute). */
    NEAR_DEATH(120, Pause.NEAR_DEATH),
    /** The wielder died while bonded. */
    DEFEAT(200, Pause.NONE),
    /** A boss killed the wielder (on top of {@link #DEFEAT}). */
    BOSS_LOSS(400, Pause.NONE),
    /** A wielder in an Overrun killed somebody near this one. */
    OVERRUN_KILL(300, Pause.ALLY),
    /** Anything another system reports with {@code HeroCurse.addDespair}; the caller's amount is used, no pause applies. */
    OTHER(0, Pause.NONE);

    /** Which configured pause a cause waits for between two reports. */
    public enum Pause { NONE, ALLY, BETRAYAL, NEAR_DEATH }

    private final int defaultAmount;
    private final Pause pause;

    Cause(int defaultAmount, Pause pause) {
        this.defaultAmount = defaultAmount;
        this.pause = pause;
    }

    /** The lower-case word used in logs, commands and rate-limit records. */
    public String key() { return name().toLowerCase(Locale.ROOT); }

    /** Despair points this cause adds unless the config says otherwise. */
    public int defaultAmount() { return defaultAmount; }

    public Pause pause() { return pause; }

    /** The cause with this key (case-insensitive), or {@link #OTHER} for an unknown or empty word. */
    public static Cause byKey(String key) {
        if (key == null) return OTHER;
        String k = key.trim();
        for (Cause c : values()) if (c.key().equalsIgnoreCase(k)) return c;
        return OTHER;
    }
}
