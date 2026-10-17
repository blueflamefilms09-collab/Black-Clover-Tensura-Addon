package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: a Dungeon Master does not roll for "a player", it rolls for "the table": the people sitting round the same map. In the Mega-Dungeon
 * that means players who stand near each other, and two things follow. First, two players who walk side by side must be one party (one roll,
 * one encounter, a budget for the pair), or the dungeon would hit a group of four with four separate ambushes. Second, a lone scout who is
 * far from the others is a separate party with its own noise, light and cooldown. Grouping is the transitive closure of "within
 * {@code radius} blocks" (a chain of players each within 48 blocks of the next is one party), done with a tiny union-find because a server has
 * a handful of players, not hundreds. The party size also drives the encounter budget (a pair is not simply twice a soloist: the factor table
 * grows more slowly than the head count, the way a tabletop group's strength does, and the table is config-driven so a server can tune it).
 * Pure (no Minecraft types) so the grouping and the factors are tested without a server.
 */
public final class PartyMath {
    private PartyMath() {}

    /** The factors for one, two, three and four players; every player beyond the table adds {@link #DEFAULT_EXTRA}. */
    public static final String DEFAULT_FACTORS = "1.0,1.6,2.1,2.5";
    public static final double DEFAULT_EXTRA = 0.3;
    private static final double[] FALLBACK = {1.0, 1.6, 2.1, 2.5};

    /** One player as the grouping sees it: an id and a position. */
    public record Member(UUID id, double x, double y, double z) {}

    /** A group of players that stand near each other. The leader is the member with the lowest UUID, so it is stable while the group keeps its members. */
    public record Party(UUID leader, List<Member> members, double cx, double cy, double cz) {
        public int size() { return members.size(); }

        public boolean has(UUID id) {
            for (Member m : members) if (m.id().equals(id)) return true;
            return false;
        }
    }

    /**
     * Splits the players into parties: two players are in the same party when a chain of players, each within {@code radius} blocks of the
     * next, links them. The result is ordered by leader id and each party's members by id, so the same positions always give the same list.
     */
    public static List<Party> group(List<Member> players, double radius) {
        int n = players.size();
        int[] root = new int[n];
        for (int i = 0; i < n; i++) root[i] = i;
        double r2 = radius * radius;
        for (int i = 0; i < n; i++) {
            Member a = players.get(i);
            for (int j = i + 1; j < n; j++) {
                Member b = players.get(j);
                double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
                if (dx * dx + dy * dy + dz * dz <= r2) union(root, i, j);
            }
        }
        List<List<Member>> buckets = new ArrayList<>();
        int[] slot = new int[n];
        Arrays.fill(slot, -1);
        for (int i = 0; i < n; i++) {
            int r = find(root, i);
            if (slot[r] < 0) {
                slot[r] = buckets.size();
                buckets.add(new ArrayList<>());
            }
            buckets.get(slot[r]).add(players.get(i));
        }
        List<Party> out = new ArrayList<>(buckets.size());
        for (List<Member> b : buckets) {
            b.sort(Comparator.comparing(Member::id));
            double sx = 0, sy = 0, sz = 0;
            for (Member m : b) {
                sx += m.x();
                sy += m.y();
                sz += m.z();
            }
            out.add(new Party(b.get(0).id(), List.copyOf(b), sx / b.size(), sy / b.size(), sz / b.size()));
        }
        out.sort(Comparator.comparing(Party::leader));
        return out;
    }

    private static int find(int[] root, int i) {
        while (root[i] != i) {
            root[i] = root[root[i]];
            i = root[i];
        }
        return i;
    }

    private static void union(int[] root, int a, int b) {
        int ra = find(root, a), rb = find(root, b);
        if (ra != rb) root[Math.max(ra, rb)] = Math.min(ra, rb);
    }

    // ---------------------------------------------------------------- the size factor
    /**
     * Reads the config string ({@code "1.0,1.6,2.1,2.5"}). A bad token, an empty list or a factor below 0.1 gives the default table: a typo in a
     * config file must not make the dungeon free or impossible.
     */
    public static double[] parseFactors(String csv) {
        if (csv == null || csv.isBlank()) return FALLBACK.clone();
        String[] parts = csv.split(",");
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException bad) {
                return FALLBACK.clone();
            }
            if (!(out[i] >= 0.1) || out[i] > 50.0) return FALLBACK.clone();
        }
        return out;
    }

    /** The budget factor of a party of this size: the table entry, then {@code extra} for every player beyond the table. */
    public static double factor(int size, double[] table, double extra) {
        double[] t = table == null || table.length == 0 ? FALLBACK : table;
        if (size <= 1) return t[0];
        if (size <= t.length) return t[size - 1];
        return t[t.length - 1] + Math.max(0.0, extra) * (size - t.length);
    }

    /** The d20 modifier of the party's size: a bigger group draws more attention, capped (a crowd is not a siege). One player gives 0. */
    public static int mod(int size, int cap) {
        return Math.max(0, Math.min(Math.max(0, cap), size - 1));
    }
}
