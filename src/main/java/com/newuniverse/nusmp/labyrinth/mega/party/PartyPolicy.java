package com.newuniverse.nusmp.labyrinth.mega.party;

import java.util.Locale;

/**
 * Why: three small decisions of the party layer have corners that a person finds only by playing, so they are made here on plain values and pinned by tests. (1) The
 * MODE: {@code party} (every party has chains of its own) or {@code shared} (everyone is party 0, the way the dungeon worked before 1.58.0); the config is a string, and a
 * typo in a config file must neither throw nor silently turn the instancing off. (2) WHEN parties are instanced at all: the floor model only. The old ten-floor sectors
 * ({@code floors.perSector} above 1) wind through the dimension by the planner's choice, a chain of them can wander thousands of blocks, and nothing then guarantees that
 * it stays in its own lane, so a server that keeps that legacy model keeps one shared chain; the floor model is a straight run along one lane and cannot leave it.
 * (3) The VERDICT of the guard that keeps a party's floors to that party: what to do with a player who stands in a sector, given the party the server has on record for
 * them and the party of the sector under their feet. Operators are never moved (an admin looks at any floor); a player with no record adopts the party of the floor they
 * stand in (a world saved before parties, a teleport by command, a login after an update), so nobody is thrown out of a floor they were already in; a player whose record
 * and floor disagree is sent out. Pure.
 */
public final class PartyPolicy {
    private PartyPolicy() {}

    /** How parties are told apart: {@code PARTY} gives each party chains of its own, {@code SHARED} puts every player in party 0. */
    public enum Mode {
        PARTY, SHARED;

        /** The mode a config string names (case and spaces do not matter); anything else is {@link #PARTY}, the default. */
        public static Mode parse(String text) {
            if (text == null) return PARTY;
            return text.trim().toLowerCase(Locale.ROOT).equals("shared") ? SHARED : PARTY;
        }

        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    /** What the guard does with one player. */
    public enum Verdict {
        /** Leave the player alone. */
        OK,
        /** The player has no record: write the party of the floor they stand in as their record. */
        ADOPT,
        /** The floor belongs to another party: send the player out of the dungeon. */
        EVICT
    }

    /** Are parties separate chains right now? Only in {@code party} mode and only in the floor model (see the class comment). */
    public static boolean instanced(Mode mode, boolean floorModel) { return mode == Mode.PARTY && floorModel; }

    /** The party index a player is entered under: the index the book has for their party when parties are instanced, otherwise always 0 (the shared chain). */
    public static int entryIndex(boolean instanced, int bookIndex) { return instanced ? Math.max(0, bookIndex) : 0; }

    /**
     * What to do with a player who stands in a sector. {@code assigned} is the party the server has on record for the player (null when there is none), {@code sectorParty}
     * the party of the sector under their feet (null when they stand in no sector: the guard decides nothing about a gap), {@code exempt} an operator the config lets pass
     * or a spectator.
     */
    public static Verdict verdict(boolean instanced, boolean guardOn, boolean exempt, Integer assigned, Integer sectorParty) {
        if (!instanced || !guardOn || sectorParty == null) return Verdict.OK;
        if (exempt) return Verdict.OK;
        if (assigned == null) return Verdict.ADOPT;
        return assigned.intValue() == sectorParty.intValue() ? Verdict.OK : Verdict.EVICT;
    }

    /**
     * The name to show for a party from the short name FTB Teams gives its team ({@code Notch_Party#5E6F8A9B}: the display name, a {@code #}, then the first eight
     * characters of the team's id). The suffix is only there to tell two teams of one name apart, so players are not shown it; a name that does not have one is shown
     * whole; nothing at all becomes {@code ?}.
     */
    public static String displayName(String shortName) {
        if (shortName == null) return "?";
        String s = shortName.trim();
        int hash = s.lastIndexOf('#');
        if (hash > 0 && s.length() - hash - 1 == 8 && isHex(s, hash + 1)) s = s.substring(0, hash);
        return s.isEmpty() ? "?" : s;
    }

    private static boolean isHex(String s, int from) {
        for (int i = from; i < s.length(); i++) if (Character.digit(s.charAt(i), 16) < 0) return false;
        return true;
    }
}
