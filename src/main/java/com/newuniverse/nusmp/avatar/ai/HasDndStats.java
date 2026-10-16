package com.newuniverse.nusmp.avatar.ai;

/**
 * Why: the Avatar's rolled D&D scores (STR DEX CON INT WIS CHA) scale how fast it casts, but the cast goal must not depend on how the entity stores
 * them. AvatarEntity implements this small interface (see AVATAR_NOTES); without it every score reads as an average 10.
 */
public interface HasDndStats {
    /** The score for "STR", "DEX", "CON", "INT", "WIS" or "CHA" (3 to 20 or so); 10 when unknown. */
    int dndStat(String code);

    default int intelligence() { return dndStat("INT"); }

    default int wisdom() { return dndStat("WIS"); }
}
