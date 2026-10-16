package com.newuniverse.nusmp.avatar.appearance;

import java.util.Locale;

/**
 * Why: the renderer keeps three body skins (a person, a bulky brute, a small folk) rather than one per race, because the race list is data-driven and
 * grows with datapacks; this maps any race id to the nearest skin by its name, so a race added tomorrow still gets a sensible body without a code change.
 * Pure.
 */
public enum AvatarRaceGroup {
    HUMAN("human"), BULKY("bulky"), SMALL("small");

    public final String file;

    AvatarRaceGroup(String file) { this.file = file; }

    public static AvatarRaceGroup of(String raceId) {
        if (raceId == null) return HUMAN;
        String r = raceId.toLowerCase(Locale.ROOT);
        int colon = r.indexOf(':');
        if (colon >= 0) r = r.substring(colon + 1);
        for (String k : new String[]{"ogre", "orc", "troll", "giant", "oni", "minotaur", "lizard", "dragonoid", "titan"}) if (r.contains(k)) return BULKY;
        for (String k : new String[]{"goblin", "kobold", "gnome", "halfling", "fairy", "pixie", "sprite", "imp", "dwarf"}) if (r.contains(k)) return SMALL;
        return HUMAN;
    }
}
