package com.newuniverse.nusmp.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 1.97.0: the lineage of one Elsdocia, pure Java (no Minecraft types) so the rules are tested. Ordered owners (the same player may appear again once
 * somebody else has held it in between), the magics it has learned, and which ability is active.
 * <ul>
 *   <li>The FIRST owner claims it (no pass, no bonus). A later owner who is a NEW distinct player and Grand Magic rank or above at that moment adds
 *       +25 base damage ({@link ElsdociaMath#damage}). A player who held it before gives no second bonus; a non-Grand player gives none.</li>
 *   <li>It can never go back to the LAST owner: the owner immediately before the current one. Owners further back may take it again.</li>
 *   <li>Every distinct magic among owners is learned once and kept ({@link #learned}), as one ability each.</li>
 * </ul>
 * Stored on the stack as lines ({@link #encode}/{@link #decode}) so the data component is a plain saved and synced string list.
 */
public record ElsdociaLineage(List<Owner> owners, List<String> learned, int active) {

    /** One holder: id, name, Black Clover magic type name (MagicType#name, "" = none), rank step at the time, and whether this pass gave +25. */
    public record Owner(UUID id, String name, String magic, int rank, boolean bonus) {
        public Owner {
            name = clean(name);
            magic = norm(magic);
        }
    }

    public enum Verdict { CLAIM, SAME, PASS, BLOCKED }

    public static final ElsdociaLineage EMPTY = new ElsdociaLineage(List.of(), List.of(), 0);

    public ElsdociaLineage {
        owners = List.copyOf(owners);
        learned = List.copyOf(learned);
        active = Math.max(0, active);
    }

    static String clean(String s) { return s == null ? "" : s.trim().replace('|', '_').replace('\n', ' '); }

    static String norm(String s) { return s == null ? "" : s.trim().toUpperCase(Locale.ROOT).replace('|', '_'); }

    // ---------------------------------------------------------------- reading
    public Owner current() { return owners.isEmpty() ? null : owners.get(owners.size() - 1); }

    /** The last owner: the holder before the current one, or null. */
    public Owner last() { return owners.size() < 2 ? null : owners.get(owners.size() - 2); }

    public int bonuses() { int n = 0; for (Owner o : owners) if (o.bonus()) n++; return n; }

    public int damage() { return ElsdociaMath.damage(bonuses()); }

    public boolean hasOwned(UUID id) { for (Owner o : owners) if (o.id().equals(id)) return true; return false; }

    /** What taking it would mean for {@code taker}: SAME (already the current owner), BLOCKED (the last owner), CLAIM (nobody owned it yet) or PASS. */
    public Verdict verdict(UUID taker) {
        Owner cur = current();
        if (cur == null) return Verdict.CLAIM;
        if (cur.id().equals(taker)) return Verdict.SAME;
        Owner l = last();
        if (l != null && l.id().equals(taker)) return Verdict.BLOCKED;
        return Verdict.PASS;
    }

    /** Number of ability slots: the two base techniques plus one per learned magic. */
    public int slots() { return ElsdociaMath.BASE_SLOTS + learned.size(); }

    /** The active slot, kept inside the slots. */
    public int activeSlot() { return active >= slots() ? 0 : active; }

    /** The learned magic of slot {@code slot} (>= BASE_SLOTS), or null. */
    public String learnedAt(int slot) {
        int i = slot - ElsdociaMath.BASE_SLOTS;
        return i >= 0 && i < learned.size() ? learned.get(i) : null;
    }

    // ---------------------------------------------------------------- writing (each returns a new lineage)
    /** The taker becomes the current owner unless the verdict is SAME (only magic and rank refresh) or BLOCKED (unchanged); a PASS to a new Grand owner gives +25. */
    public ElsdociaLineage take(UUID id, String name, String magic, int rank) {
        Verdict v = verdict(id);
        if (v == Verdict.BLOCKED) return this;
        if (v == Verdict.SAME) return withMagic(id, magic);
        return append(id, name, magic, rank);
    }

    /** An admin sets the owner whatever the rules say (still +25 only for a new distinct Grand owner after the first). */
    public ElsdociaLineage force(UUID id, String name, String magic, int rank) {
        Owner cur = current();
        if (cur != null && cur.id().equals(id)) return withMagic(id, magic);
        return append(id, name, magic, rank);
    }

    private ElsdociaLineage append(UUID id, String name, String magic, int rank) {
        boolean bonus = !owners.isEmpty() && !hasOwned(id) && ElsdociaMath.isGrand(rank);
        List<Owner> o = new ArrayList<>(owners);
        o.add(new Owner(id, name, magic, rank, bonus));
        return new ElsdociaLineage(o, learn(learned, magic), active);
    }

    /** The current owner got a magic after claiming: fills an empty magic and learns it. */
    public ElsdociaLineage withMagic(UUID id, String magic) {
        Owner cur = current();
        String m = norm(magic);
        if (cur == null || !cur.id().equals(id) || m.isEmpty()) return this;
        boolean fill = cur.magic().isEmpty();
        if (!fill && learned.contains(m)) return this;
        List<Owner> o = new ArrayList<>(owners);
        if (fill) o.set(o.size() - 1, new Owner(cur.id(), cur.name(), m, cur.rank(), cur.bonus()));
        return new ElsdociaLineage(o, learn(learned, m), active);
    }

    static List<String> learn(List<String> learned, String magic) {
        String m = norm(magic);
        if (m.isEmpty() || learned.contains(m)) return learned;
        List<String> l = new ArrayList<>(learned);
        l.add(m);
        return l;
    }

    public ElsdociaLineage withActive(int slot) { return new ElsdociaLineage(owners, learned, slot); }

    /** Shift-use: the next ability, wrapping round. */
    public ElsdociaLineage cycled() { return withActive((activeSlot() + 1) % slots()); }

    // ---------------------------------------------------------------- the page of a magic
    /**
     * Which page of a grimoire is its signature: the dearest one (highest cost percent) that may be used, a later page winning a tie.
     * {@code cost[i]} is the page's cost percent and {@code eligible[i]} whether it may be used (not a weapon page, not a universal page). -1 if none.
     */
    public static int pickSignature(double[] cost, boolean[] eligible) {
        int best = -1;
        for (int i = 0; i < cost.length; i++) {
            if (!eligible[i]) continue;
            if (best < 0 || cost[i] >= cost[best]) best = i;
        }
        return best;
    }

    // ---------------------------------------------------------------- stack form
    /** Lines: {@code v1|active}, then {@code O|uuid|name|MAGIC|rank|bonus} per owner and {@code L|MAGIC} per learned magic. */
    public List<String> encode() {
        List<String> out = new ArrayList<>();
        out.add("v1|" + active);
        for (Owner o : owners) out.add("O|" + o.id() + "|" + o.name() + "|" + o.magic() + "|" + o.rank() + "|" + (o.bonus() ? 1 : 0));
        for (String m : learned) out.add("L|" + m);
        return out;
    }

    /** Reads {@link #encode}; lines it does not understand are skipped, so an old or damaged value still loads. */
    public static ElsdociaLineage decode(List<String> lines) {
        if (lines == null || lines.isEmpty()) return EMPTY;
        List<Owner> owners = new ArrayList<>();
        List<String> learned = new ArrayList<>();
        int active = 0;
        for (String line : lines) {
            if (line == null) continue;
            String[] p = line.split("\\|", -1);
            try {
                if (p[0].equals("v1") && p.length >= 2) active = Integer.parseInt(p[1].trim());
                else if (p[0].equals("O") && p.length >= 6)
                    owners.add(new Owner(UUID.fromString(p[1]), p[2], p[3], Integer.parseInt(p[4].trim()), p[5].trim().equals("1")));
                else if (p[0].equals("L") && p.length >= 2 && !p[1].isBlank() && !learned.contains(p[1].trim())) learned.add(p[1].trim());
            } catch (RuntimeException ignored) { /* skip a damaged line */ }
        }
        return new ElsdociaLineage(owners, learned, active);
    }
}
