package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Sector;

/**
 * Why: the one door between the pure table of the Underworld Root ({@link UnderworldRoot}) and the mega-dungeon's question "which theme do the floors after this boss wear?". It is called by
 * {@code MegaDungeon.themeOverride} only when the Crucible's relic had no opinion (the relic always wins), answers only for the Abyssal road, only while the Decennial bosses are enabled, and never
 * throws: a null is "no opinion" and the seeded draw goes on as it always did.
 */
public final class DecennialThemes {
    private DecennialThemes() {}

    /** The theme the floors after {@code previous} (a boss floor) must wear because of the Underworld Root, or null for no opinion. */
    public static RegionTheme themeAfter(Sector previous) {
        try {
            if (previous == null || previous.polarity != Polarity.ABYSSAL || !DecennialConfig.get(DecennialConfig.ENABLED)) return null;
            String id = UnderworldRoot.themeIdAfter(previous.lastFloor());
            if (id == null) return null;
            RegionTheme t = RegionTheme.fromId(id);
            return t.id().equals(id) ? t : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
