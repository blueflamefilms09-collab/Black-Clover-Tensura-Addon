package com.newuniverse.nusmp.labyrinth.mega.decennial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: every Decennial boss pays one item of its level's grade, and which item is a choice with rules: the grade must be the level's (the loot tiers rise with depth), the kind (weapon, relic or
 * armour) and the weapon type the profile names should fit, the relic keywords of the profile (a lich pays something dark, a sun-knight something golden) should be matched when they can be, an
 * item somebody already holds must not be paid again, and the same boss must make the same choice on every server (so the doc can say what a level pays). That choice is a pure ranking over a
 * plain description of the catalog's relics, so a test can run it over a fake catalog; {@code DecennialRewards} feeds it the real one and does the part that touches the game (the ledger, the drop).
 * This is the owner-requested exception to "Unique-or-higher relics come only from Crucibles", exactly like the Metal Vessel of the Holy Guardians; it is switched by the config {@code bossRelic}.
 */
public final class DecennialLoot {
    private DecennialLoot() {}

    /** The three kinds of loot a profile asks for. */
    public static final String WEAPON = "weapon", ARMOUR = "armour", RELIC = "relic";

    /**
     * One thing the catalog could pay: its id, its card name, the kind it is ({@link #WEAPON}, {@link #ARMOUR}, {@link #RELIC} or {@code ""} for what may not be paid at all: a consumable, one half of a
     * pair), its weapon type ({@code ""} when it is not a weapon) and its grade (0 normal to 6 genesis).
     */
    public record Candidate(String id, String name, String kind, String weaponType, int grade) {}

    /** The kind a slot name stands for (the caller maps {@code RelicSlot} to these names). */
    public static String kindOfSlot(String slotName) {
        if (slotName == null) return "";
        return switch (slotName.toUpperCase(Locale.ROOT)) {
            case "MAIN_HAND" -> WEAPON;
            case "HEAD", "CHEST", "FEET" -> ARMOUR;
            case "NECK", "BACK", "BELT", "HANDS", "CHARM" -> RELIC;
            default -> "";
        };
    }

    /** The grade ordinal 0..6 of a grade name ({@code normal} is 0, {@code special} 1, {@code genesis} 6; -1 for an unknown name). */
    public static int gradeOrdinal(String grade) { return DecennialRules.gradeIndex(grade); }

    /** How well a candidate suits a profile: -1 for one that may not be paid at all, else a score where higher is better. */
    public static int score(DecennialProfile p, Candidate c) {
        if (c.kind().isEmpty() || c.grade() != gradeOrdinal(p.loot().grade())) return -1;
        int s = 1;
        if (c.kind().equals(p.loot().kind())) s += 40;
        if (c.kind().equals(WEAPON) && p.loot().kind().equals(WEAPON) && !p.loot().type().isEmpty() && c.weaponType().equalsIgnoreCase(p.loot().type())) s += 20;
        String hay = (c.id() + " " + c.name()).toLowerCase(Locale.ROOT);
        for (String k : p.loot().keywords()) if (!k.isEmpty() && hay.contains(k.toLowerCase(Locale.ROOT))) s += 8;
        return s;
    }

    /**
     * The candidates that may be paid to the boss of {@code p}, best first. Ties are broken by a stable hash of the boss id and the candidate id, so every server ranks alike and two bosses of the same
     * profile shape do not all pay the same item.
     */
    public static List<Candidate> rank(DecennialProfile p, List<Candidate> pool, Set<String> taken) {
        List<Candidate> out = new ArrayList<>();
        for (Candidate c : pool) if (score(p, c) >= 0 && !taken.contains(c.id())) out.add(c);
        out.sort(Comparator.<Candidate>comparingInt(c -> -score(p, c)).thenComparingInt(c -> tie(p.id(), c.id())));
        return out;
    }

    /** The first of {@link #rank}, or null when the grade has nothing left to pay. */
    public static Candidate pick(DecennialProfile p, List<Candidate> pool, Set<String> taken) {
        List<Candidate> r = rank(p, pool, taken);
        return r.isEmpty() ? null : r.get(0);
    }

    private static int tie(String bossId, String candidateId) {
        int h = 17;
        for (int i = 0; i < bossId.length(); i++) h = h * 31 + bossId.charAt(i);
        for (int i = 0; i < candidateId.length(); i++) h = h * 31 + candidateId.charAt(i);
        h ^= h >>> 15;
        h *= 0x2c1b3c6d;
        h ^= h >>> 12;
        return h;
    }
}
