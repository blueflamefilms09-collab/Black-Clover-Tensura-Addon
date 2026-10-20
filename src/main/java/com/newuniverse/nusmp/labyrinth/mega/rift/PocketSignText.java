package com.newuniverse.nusmp.labyrinth.mega.rift;

/**
 * Why: the words on the signs of the hidden realms and the tombs (1.101.0, docs/labyrinth/pocket_fix.md), as plain strings with no game class in them, so a unit test can check that every sign fits its board
 * (four lines, sixteen characters at the most) and names what opens the door. {@link PocketSigns} turns them into glowing wall signs with lang keys and these strings as the English fallback.
 */
public final class PocketSignText {
    private PocketSignText() {}

    /** The longest line a sign board holds. */
    public static final int MAX_LINE = 16;

    /** The keys of every sign the builders place. */
    public static final String[] KEYS = {"lever", "vault", "chamber", "seal", "tomb_lever", "sigil"};

    /** The four lines of a sign, by key. */
    public static String[] lines(String key) {
        return switch (key) {
            case "lever" -> new String[]{"RELEASE LEVER", "Pull to lift", "the vault bars.", "Stiff at first."};
            case "vault" -> new String[]{"THE VAULT", "Barred door.", "Lever is in the", "north chamber"};
            case "chamber" -> new String[]{"<- LEVER ROOM", "Opens the", "barred vault", "when pulled"};
            case "seal" -> new String[]{"THE SEAL", "Magic, Sigil,", "force or the", "KEY ROOM LEVER"};
            case "tomb_lever" -> new String[]{"SEAL LEVER", "Opens the door", "of the treasury", "Stiff at first."};
            case "sigil" -> new String[]{"THE SIGIL", "Press it to", "the seal. Spent", "when it opens."};
            default -> new String[]{key, "", "", ""};
        };
    }
}
