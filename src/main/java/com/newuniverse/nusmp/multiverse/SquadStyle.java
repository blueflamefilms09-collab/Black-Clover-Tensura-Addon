package com.newuniverse.nusmp.multiverse;

import java.util.Locale;

/**
 * Why: the nine Magic Knight squads of the anime as a style a player squad can take (name, colour, captain and a line about it). Facts come from web search snippets (the wiki pages themselves could not be
 * fetched, see docs/magic_knights.md): captains William Vangeance (Golden Dawn), Fuegoleon Vermillion (Crimson Lion Kings), Charlotte Roselei (Blue Rose Maidens), Nozel Silva (Silver Eagle), Yami Sukehiro
 * (Black Bull), Rill Boismortier (Aqua Deer, once the Grey Deer under Julius Novachrono), Jack the Ripper (Green Praying Mantis), Kaiser Granvorka (Purple Orca), Dorothy Unsworth (Coral Peacock). The colours of
 * Golden Dawn, Silver Eagle, Crimson Lion, Black Bull and Coral Peacock are the mod's own robe colours; the other four are taken from the squad's name. A style is cosmetic (name, colour, text); it changes no number.
 */
public enum SquadStyle {
    GOLDEN_DAWN("Golden Dawn", "William Vangeance", 0xD9B44A, "The elite squad; named the squad with the most stars at the Star Awards."),
    SILVER_EAGLE("Silver Eagle", "Nozel Silva", 0xB8C8E0, "Led by Nozel Silva of the royal Silva family."),
    CRIMSON_LION_KINGS("Crimson Lion Kings", "Fuegoleon Vermillion", 0x9A1E1E, "The fire mages of the Vermillion line."),
    BLUE_ROSE_MAIDENS("Blue Rose Maidens", "Charlotte Roselei", 0x3A6FD8, "Charlotte Roselei's squad."),
    BLACK_BULL("Black Bull", "Yami Sukehiro", 0x1C1C20, "Yami Sukehiro's squad, known for its negative star count."),
    AQUA_DEER("Aqua Deer", "Rill Boismortier", 0x4FB6D8, "Painting Magic; once the Grey Deer under Julius Novachrono."),
    GREEN_PRAYING_MANTIS("Green Praying Mantis", "Jack the Ripper", 0x4FA84F, "Slash Magic; recruits commoners."),
    PURPLE_ORCA("Purple Orca", "Kaiser Granvorka", 0x7A4FB0, "Kaiser Granvorka took the captaincy over from Gueldre Poizot."),
    CORAL_PEACOCK("Coral Peacock", "Dorothy Unsworth", 0x2FA8A0, "Dream Magic.");

    public final String display, captain, blurb;
    public final int color;

    SquadStyle(String display, String captain, int color, String blurb) { this.display = display; this.captain = captain; this.color = color; this.blurb = blurb; }

    /** The id used in commands: the display name in lower case with underscores. */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The style with this id (any case, spaces or underscores), or null. */
    public static SquadStyle byId(String s) {
        if (s == null) return null;
        String k = s.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        for (SquadStyle v : values()) if (v.id().equals(k)) return v;
        return null;
    }
}
