package com.newuniverse.nusmp.wizard;

import java.util.List;

/**
 * The sections of the long questionnaire, in the order they are asked: Nature (magic), Hearth (kingdom), Blood (race), Origin (social class), Resolve (leaves). Pure text, shared by the
 * server (the sheet carries one id per question) and the client screen (header and progress line). An old sheet has no ids and shows no sections.
 */
public final class SheetSections {
    private SheetSections() {}

    public static final String NATURE = "NATURE", HEARTH = "HEARTH", BLOOD = "BLOOD", ORIGIN = "ORIGIN", RESOLVE = "RESOLVE";
    /** The asking order. */
    public static final List<String> ORDER = List.of(NATURE, HEARTH, BLOOD, ORIGIN, RESOLVE);

    public static String title(String id) {
        return switch (id) {
            case NATURE -> "Nature";
            case HEARTH -> "Hearth";
            case BLOOD -> "Blood";
            case ORIGIN -> "Origin";
            case RESOLVE -> "Resolve";
            default -> "";
        };
    }

    /** Part number (1-based) of the section of question {@code index}, counted over the distinct sections in the order they first appear. */
    public static int part(List<String> cats, int index) {
        int n = 0;
        String last = null;
        for (int i = 0; i <= index && i < cats.size(); i++) if (!cats.get(i).equals(last)) { n++; last = cats.get(i); }
        return n;
    }

    public static int parts(List<String> cats) {
        int n = 0;
        String last = null;
        for (String c : cats) if (!c.equals(last)) { n++; last = c; }
        return n;
    }

    /** "Part 2 of 5: Hearth (3/4)", or "" when the sheet has no sections. */
    public static String progress(List<String> cats, int index) {
        if (cats == null || cats.size() <= index || cats.get(index).isEmpty()) return "";
        String c = cats.get(index);
        int first = index, last = index;
        while (first > 0 && cats.get(first - 1).equals(c)) first--;
        while (last + 1 < cats.size() && cats.get(last + 1).equals(c)) last++;
        return "Part " + part(cats, index) + " of " + parts(cats) + ": " + title(c) + " (" + (index - first + 1) + "/" + (last - first + 1) + ")";
    }
}
