package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.devil.DevilRules;

/**
 * Why: devil magic can not be adapted to and the law-domination effect has no effect on it (the owner's rule). The decision is pure (no Minecraft class, unit tested) so the Adaptability hook, the
 * self-test and any mechanic that wants to ask say the same thing. A blow or effect is devil magic when its damage type is in the tag {@code anime_multiverse:devil_magic} (the arts of the Ultimate skill
 * Devil Magic hurt with that type), or when the host who struck has let the devil in (assimilation at or above {@link DevilRules#EFFECT_FROM}): then the whole blow carries the devil.
 */
public final class DevilMagicRules {
    private DevilMagicRules() {}

    /** The host's blows carry the devil: the system is on, there is a contract, and the devil is let in from 20 %. */
    public static boolean hostCarries(boolean systemOn, boolean hasContract, float assimilation) {
        return systemOn && hasContract && assimilation >= DevilRules.EFFECT_FROM;
    }

    /** Is this blow devil magic? {@code tagged}: its damage type is in the devil_magic tag. */
    public static boolean isDevilMagic(boolean tagged, boolean hostCarries) { return tagged || hostCarries; }

    /** Adaptability learns from a blow (and softens the next) only when this is true; false for devil magic while the setting is on. */
    public static boolean mayAdaptTo(boolean devilMagic, boolean immunityOn) { return !(devilMagic && immunityOn); }

    /** A law-domination effect (a rule that removes, bars or rewrites what hits a body) acts on a blow or effect only when this is true; false for devil magic while the setting is on. */
    public static boolean lawDominationApplies(boolean devilMagic, boolean immunityOn) { return !(devilMagic && immunityOn); }
}
