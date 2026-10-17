package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: "a goblin patrol" in the table means "some goblin-faction monsters", and the faction is a TAG (every mod that adds a goblin-ish thing can
 * join it), so the set of members is not known when the table is written and varies from world to world. The Dungeon Master must still keep
 * its promise that deeper means harder and that sector one is survivable: a faction tag may hold a 20-health goblin and a 400-health ogre
 * lord side by side. So members are filtered by TOUGHNESS (their default maximum health, a number the game knows for any living entity):
 * only those between 12 percent of a cap and the cap itself are drawn, where the cap rises with the sector's threat ({@code 12 + 14 * threat}:
 * 26 health in sector one, 82 in sector five, 152 in sector ten). The lower bound is the other half of "bands that grow with threat": in
 * sector ten a pack of 20-health zombies would be a joke, so they drop out of the draw while stronger members of the same faction are
 * there. When a faction has nothing inside the window the rule relaxes (everything under the cap, then the weakest thing at most 60 percent
 * over it) rather than skipping the encounter, so a thin tag still produces something. Pure: the game hands in ids and health numbers.
 */
public final class MobPicker {
    private MobPicker() {}

    /** What the game uses for a type whose default health cannot be read. */
    public static final double DEFAULT_TOUGHNESS = 20.0;
    private static final double FLOOR_SHARE = 0.12, SOFT_CAP = 1.6;

    /** A faction member: its entity id and its default maximum health. */
    public record Candidate(String id, double toughness) {}

    /** The toughest member a sector of this threat may draw. */
    public static double cap(int threat) { return 12.0 + 14.0 * Math.max(1, threat); }

    /** The weakest member worth drawing under this cap. */
    public static double floor(double cap) { return cap * FLOOR_SHARE; }

    /** The members that fit a cap, relaxing in three steps (see the class comment). Empty only when every member is more than 60 percent over the cap. */
    public static List<Candidate> window(List<Candidate> all, double cap) {
        List<Candidate> inside = new ArrayList<>(), under = new ArrayList<>(), soft = new ArrayList<>();
        double floor = floor(cap);
        for (Candidate c : all) {
            double t = Double.isFinite(c.toughness()) && c.toughness() > 0.0 ? c.toughness() : DEFAULT_TOUGHNESS;
            if (t <= cap) {
                under.add(c);
                if (t >= floor) inside.add(c);
            } else if (t <= cap * SOFT_CAP) {
                soft.add(c);
            }
        }
        if (!inside.isEmpty()) return inside;
        if (!under.isEmpty()) return under;
        return soft;
    }

    /** One member of the window, chosen with a mild preference for the tougher ones (weight = health, but never less than 8). Null when there is none. */
    public static Candidate pick(List<Candidate> all, int threat, Random r) {
        List<Candidate> w = window(all, cap(threat));
        if (w.isEmpty()) return null;
        double total = 0.0;
        for (Candidate c : w) total += weight(c);
        double roll = r.nextDouble() * total;
        for (Candidate c : w) {
            roll -= weight(c);
            if (roll < 0.0) return c;
        }
        return w.get(w.size() - 1);
    }

    private static double weight(Candidate c) {
        double t = Double.isFinite(c.toughness()) && c.toughness() > 0.0 ? c.toughness() : DEFAULT_TOUGHNESS;
        return Math.max(8.0, t);
    }
}
