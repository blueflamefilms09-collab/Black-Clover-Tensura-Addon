package com.newuniverse.nusmp.relics.order;

import java.util.Locale;

/**
 * Every word the Order says that does not name a relic (pure, unit tested). The God of Order is polite, exact and without anger: it speaks in
 * short sentences, like a clerk who is also a judge. Lines that name a relic are put together by {@link OrderChat}.
 */
public final class OrderText {
    private OrderText() {}

    public static final String PREFIX = "[Order] ";
    public static final String NAME = "God of Order";

    // ---- the announcement: "[Order] Two of <relic> cannot be."
    public static final String TWO_OF = "Two of ";
    public static final String CANNOT_BE = " cannot be.";

    // ---- the verdict (said by the God while it reads, one line per read)
    private static final String[] READING = {
            "The ledger names one. I find two.",
            "What is written once may not be written twice.",
            "I have read the entry. It is not in dispute.",
            "A copy is not a gift. It is an error.",
    };
    private static final String[] BEAM = {
            "The second line is struck.",
            "Order is restored. It does not hurt.",
            "What was copied returns to the dust it was made of.",
    };
    private static final String[] FAREWELL = {
            "The ledger is whole. Be well.",
            "You are not blamed. You are corrected.",
            "I will not trouble you again, unless I must.",
    };
    private static final String[] PROVOKED = {
            "You raise a hand against the Ledger. It will answer.",
            "That is not a petition. It is an offence.",
    };
    private static final String[] QUIPS_DECREE = {"Be still, and be counted.", "Bound by the letter."};
    private static final String[] QUIPS_WRIT = {"A writ, served.", "Read, and stand aside."};
    private static final String[] QUIPS_JUDGEMENT = {"The circle is drawn. Step out of it, or be judged.", "Judgement is given."};
    private static final String[] ERASURE = {"It is enough. I will not destroy what I may simply erase."};
    private static final String[] WITHDRAW = {"The hour is over. The Order withdraws."};

    public static String reading(int pick) { return pick(READING, pick); }

    public static String beam(int pick) { return pick(BEAM, pick); }

    public static String farewell(int pick) { return pick(FAREWELL, pick); }

    public static String provoked(int pick) { return pick(PROVOKED, pick); }

    public static String erasure() { return ERASURE[0]; }

    public static String withdraw() { return WITHDRAW[0]; }

    public static String quip(String moveId, int pick) {
        if (OrderTable.DECREE.equals(moveId)) return pick(QUIPS_DECREE, pick);
        if (OrderTable.WRIT.equals(moveId)) return pick(QUIPS_WRIT, pick);
        if (OrderTable.JUDGEMENT.equals(moveId)) return pick(QUIPS_JUDGEMENT, pick);
        return pick(PROVOKED, pick);
    }

    /** What it says as its authority grows with the blows it takes (the Zogratis power system, renamed). */
    public static String power(int old, int now) {
        return now >= 100 ? "Enough. I will read this in full." : "My authority stands at " + now + " in the hundred.";
    }

    public static String gates() { return "The Ledger is open to its last page."; }

    public static String death() { return "Order... does not end. It is... kept."; }

    // ---- the Notice of Order
    public static final String NOTICE_TITLE = "Notice of Order";
    public static final String NOTICE_LINE_1 = "A second copy was found where only one may be.";
    public static final String NOTICE_LINE_2 = "It has been struck from the ledger. No fault is recorded.";

    // ---- the census of an existing world
    public static String censusStart(int minutes) {
        return PREFIX + "The Order counts the relics of this world. For " + minutes + " minutes it only counts.";
    }

    public static String censusWarning(String relicName, int minutesLeft) {
        return PREFIX + relicName + " exists more than once. In " + Math.max(1, minutesLeft) + " minutes the Order will come for the extra copy.";
    }

    public static String censusEnd(int adopted, int duplicateGroups) {
        return PREFIX + "The count is over: " + adopted + " relic(s) written into the ledger, " + duplicateGroups + " with extra copies. The Order now keeps the ledger.";
    }

    // ---- helpers
    private static String pick(String[] lines, int pick) { return lines[Math.floorMod(pick, lines.length)]; }

    /** "bloodbound_katana" -> "Bloodbound Katana" (for logs and the console; players get the translated item name instead). */
    public static String titleCase(String id) {
        if (id == null || id.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char c : id.toCharArray()) {
            if (c == '_' || c == ' ') {
                sb.append(' ');
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(c) : Character.toLowerCase(c));
                up = false;
            }
        }
        return sb.toString().trim();
    }

    /** The registry path of a relic id as the lang key suffix (lower case, no spaces). */
    public static String key(String relicId) { return relicId == null ? "" : relicId.toLowerCase(Locale.ROOT).trim(); }
}
