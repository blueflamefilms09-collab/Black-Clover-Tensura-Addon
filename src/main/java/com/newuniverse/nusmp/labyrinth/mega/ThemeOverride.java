package com.newuniverse.nusmp.labyrinth.mega;

/**
 * Why: the owner's brief says the relic taken from a Crucible decides the look of the next ten floors. The generator picks a sector's theme, the theme
 * module knows what a relic means, and neither may import the other (they are written in parallel). So the theme module registers ONE override
 * ({@link MegaDungeon#setThemeOverride}) and the generator asks {@link MegaDungeon#themeOverride} before it falls back to the seeded draw. The answer is
 * null whenever the previous sector's Crucible was not decided (nothing was taken, the vault was skipped), which is the seeded draw's cue.
 */
public interface ThemeOverride {
    /** The theme the sector built after {@code previous} must wear because of the relic taken in {@code previous}'s Crucible, or null for "no opinion". */
    RegionTheme themeAfter(Sector previous);
}
