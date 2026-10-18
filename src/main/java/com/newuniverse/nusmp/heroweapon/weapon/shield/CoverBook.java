package com.newuniverse.nusmp.heroweapon.weapon.shield;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Shield protects other people, and the protection has to be remembered between the cast and the blows that come later: who is covered by whom
 * (Protect: part of the damage an ally takes goes to the wielder instead) and who carries a ward (Second Shield: a floating shield that soaks the next blow).
 * The book is the bookkeeping of both, with no Minecraft types so it is tested alone. It is bounded on purpose, because a book that grows without limit is a leak
 * on a long-running server: at most {@link ShieldNumbers#COVER_MAX_ALLIES} allies are covered by one wielder, at most {@link ShieldNumbers#MAX_WARDS} wards stand
 * at once for one wielder (a new ward replaces the one that would end first), a covered ally has only one cover, and expired entries are dropped whenever the book
 * is asked. The arithmetic of how much of a blow goes to the wielder is {@link #redirect}.
 */
public final class CoverBook {
    /** An ally's cover: whose it is, until when, and the share of each blow that goes to the wielder. */
    public record Cover(UUID wielder, long until, double share) {}

    /** An ally's ward: whose it is, until when, and how much damage it can still soak. */
    public record Ward(UUID wielder, long until, double bank) {}

    private final Map<UUID, Cover> covers = new HashMap<>();
    private final Map<UUID, Ward> wards = new HashMap<>();

    // ------------------------------------------------------------------ cover
    /**
     * Covers {@code allies} for {@code ticks}: replaces every earlier cover of this wielder (a recast refreshes it) and binds at most
     * {@link ShieldNumbers#COVER_MAX_ALLIES} of them, in the order given. The wielder is never covered by themselves. Returns how many were bound.
     */
    public int cover(UUID wielder, Collection<UUID> allies, long now, int ticks, double share) {
        prune(now);
        endCover(wielder);
        if (ticks <= 0 || share <= 0) return 0;
        int bound = 0;
        for (UUID a : allies) {
            if (bound >= ShieldNumbers.COVER_MAX_ALLIES) break;
            if (a == null || a.equals(wielder)) continue;
            covers.put(a, new Cover(wielder, now + ticks, Math.min(1.0, share)));
            bound++;
        }
        return bound;
    }

    /** The cover of an ally at {@code now}, or null (an expired cover is dropped). */
    public Cover coverOf(UUID ally, long now) {
        Cover c = covers.get(ally);
        if (c == null) return null;
        if (c.until() <= now || c.until() - now > 100_000) {                         // expired, or a clock that went far back
            covers.remove(ally);
            return null;
        }
        return c;
    }

    /** Ends every cover of a wielder. */
    public void endCover(UUID wielder) { covers.values().removeIf(c -> c.wielder().equals(wielder)); }

    // ------------------------------------------------------------------ wards
    /** Puts a ward of {@code bank} damage on an ally for {@code ticks}, replacing their old one; the wielder keeps at most {@link ShieldNumbers#MAX_WARDS}. */
    public void ward(UUID wielder, UUID ally, long now, int ticks, double bank) {
        prune(now);
        if (ticks <= 0 || !(bank > 0) || ally == null) return;
        wards.remove(ally);
        List<UUID> mine = new ArrayList<>();
        for (Map.Entry<UUID, Ward> e : wards.entrySet()) if (e.getValue().wielder().equals(wielder)) mine.add(e.getKey());
        while (mine.size() >= ShieldNumbers.MAX_WARDS) {
            UUID soonest = mine.get(0);
            for (UUID id : mine) if (wards.get(id).until() < wards.get(soonest).until()) soonest = id;
            wards.remove(soonest);
            mine.remove(soonest);
        }
        wards.put(ally, new Ward(wielder, now + ticks, bank));
    }

    /** The ward of an ally at {@code now}, or null. */
    public Ward wardOf(UUID ally, long now) {
        Ward w = wards.get(ally);
        if (w == null) return null;
        if (w.until() <= now || w.until() - now > 100_000 || !(w.bank() > 0)) {
            wards.remove(ally);
            return null;
        }
        return w;
    }

    /**
     * A blow of {@code damage} meets the ward of an ally: the ward soaks what it can. Returns the damage soaked (0 when there is no ward); an emptied ward is
     * removed.
     */
    public double soak(UUID ally, long now, double damage) {
        Ward w = wardOf(ally, now);
        if (w == null || !(damage > 0)) return 0;
        double soaked = Math.min(damage, w.bank());
        double left = w.bank() - soaked;
        if (left <= 1.0e-6) wards.remove(ally);
        else wards.put(ally, new Ward(w.wielder(), w.until(), left));
        return soaked;
    }

    /** Ends every ward of a wielder. */
    public void endWards(UUID wielder) { wards.values().removeIf(w -> w.wielder().equals(wielder)); }

    // ------------------------------------------------------------------ housekeeping
    /** Drops what has expired. */
    public void prune(long now) {
        for (Iterator<Cover> it = covers.values().iterator(); it.hasNext(); ) if (it.next().until() <= now) it.remove();
        for (Iterator<Ward> it = wards.values().iterator(); it.hasNext(); ) if (it.next().until() <= now) it.remove();
    }

    /** Forgets everything of one wielder (they left, died or put the weapon away). */
    public void forget(UUID wielder) {
        endCover(wielder);
        endWards(wielder);
    }

    public void clear() {
        covers.clear();
        wards.clear();
    }

    public int coverCount() { return covers.size(); }

    public int wardCount() { return wards.size(); }

    // ------------------------------------------------------------------ the arithmetic of a redirected blow
    /**
     * How much of a blow of {@code amount} goes to the wielder: {@code share} of it, but never more than the wielder can give while keeping {@code floor} of their
     * maximum health (so covering a friend can never kill the wielder), and never more than the blow. 0 when the wielder is at or under the floor.
     */
    public static double redirect(double amount, double share, double wielderHealth, double wielderMax, double floor) {
        if (!(amount > 0) || !(share > 0)) return 0;
        double room = wielderHealth - Math.max(0.0, floor) * wielderMax;
        if (!(room > 0)) return 0;
        return Math.max(0.0, Math.min(Math.min(amount, amount * share), room));
    }
}
