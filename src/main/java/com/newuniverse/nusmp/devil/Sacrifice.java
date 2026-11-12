package com.newuniverse.nusmp.devil;

/**
 * Why: a price of the contract that stays through death (3.48.0). The three old prices of {@code ForbiddenMagic} (two hearts, a sealed page, the mana tax) are not listed here: they stay exactly as they were.
 * These are the new ones: half of the maximum health, and a lost eye. Stored BY NAME, appended only; {@link #bit} is the position in the mask the network sends.
 */
public enum Sacrifice {
    HALF_HEALTH, LOST_EYE_LEFT, LOST_EYE_RIGHT,
    /** 3.76.0 (appended): a piece of the body (two hearts of maximum health); the soul (every death takes all experience levels); the Brand (monsters hit a fifth harder). */
    FLESH, SOUL_DEBT, BRANDED;

    public int bit() { return 1 << ordinal(); }

    public boolean isEye() { return this == LOST_EYE_LEFT || this == LOST_EYE_RIGHT; }

    public static Sacrifice byName(String s) {
        if (s != null) for (Sacrifice x : values()) if (x.name().equals(s)) return x;
        return null;
    }

    /** The mask bit of the two eyes together. */
    public static final int EYES = LOST_EYE_LEFT.bit() | LOST_EYE_RIGHT.bit();
}
