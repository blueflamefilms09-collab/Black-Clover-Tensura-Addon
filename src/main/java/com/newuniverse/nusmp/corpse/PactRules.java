package com.newuniverse.nusmp.corpse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: True Soul Corpse Magic is a pact, and a pact is mostly rules: it lasts a fixed time, it covers the caster and at most three
 * tapped allies, it saves each of them ONCE, it never saves someone who is already a revived corpse, and it never saves a boss.
 * Getting one of those wrong makes a free-death exploit (or a pact that silently does nothing), so the decision is a pure class with
 * no game types that is tested on its own; the glue ({@link Pacts}) feeds it ids and the clock and does the real healing, the real
 * mana payment and the real effects only when the verdict is {@link Verdict#REVIVE}. Minecraft cannot revive a player who is already on
 * the respawn screen, so this is the check that runs inside the death event, while the death can still be cancelled.
 *
 * @param <K> the player / creature id type (a UUID in the game)
 */
public final class PactRules<K> {
    /** Most allies besides the caster. */
    public static final int MAX_ALLIES = 3;

    public enum Verdict { REVIVE, NO_PACT, EXPIRED, ALREADY_USED, STILL_DECAYING, BOSS, UNSAVEABLE }

    public enum Tap { ADDED, ALREADY, FULL, NO_PACT, SELF }

    private static final class Pact<K> {
        long until;
        final Set<K> allies = new LinkedHashSet<>();
        final Set<K> used = new LinkedHashSet<>();
    }

    private final Map<K, Pact<K>> pacts = new HashMap<>();
    private final Map<K, Long> decaying = new HashMap<>();

    /** Arms a pact for the caster: lasts until now + duration. Re-arming an active pact is refused (false); an expired one is replaced. */
    public boolean arm(K caster, long now, long durationTicks) {
        Pact<K> old = pacts.get(caster);
        if (old != null && now < old.until) return false;
        Pact<K> p = new Pact<>();
        p.until = now + Math.max(1, durationTicks);
        pacts.put(caster, p);
        return true;
    }

    public boolean disarm(K caster) { return pacts.remove(caster) != null; }

    public boolean armed(K caster, long now) {
        Pact<K> p = pacts.get(caster);
        return p != null && now < p.until;
    }

    public long remaining(K caster, long now) {
        Pact<K> p = pacts.get(caster);
        return p == null ? 0 : Math.max(0, p.until - now);
    }

    /** Taps an ally into the caster's pact (at most {@link #MAX_ALLIES}). */
    public Tap tap(K caster, K ally, long now) {
        if (caster.equals(ally)) return Tap.SELF;
        Pact<K> p = pacts.get(caster);
        if (p == null || now >= p.until) return Tap.NO_PACT;
        if (p.allies.contains(ally)) return Tap.ALREADY;
        if (p.allies.size() >= MAX_ALLIES) return Tap.FULL;
        p.allies.add(ally);
        return Tap.ADDED;
    }

    public List<K> allies(K caster) {
        Pact<K> p = pacts.get(caster);
        return p == null ? List.of() : new ArrayList<>(p.allies);
    }

    /** The caster of an active pact that covers 'who' (the caster himself, or a tapped ally) and has not saved him yet, or null. */
    public K pactFor(K who, long now) {
        Pact<K> own = pacts.get(who);
        if (own != null && now < own.until && !own.used.contains(who)) return who;
        for (Map.Entry<K, Pact<K>> e : pacts.entrySet()) {
            Pact<K> p = e.getValue();
            if (now < p.until && p.allies.contains(who) && !p.used.contains(who)) return e.getKey();
        }
        return null;
    }

    /**
     * Would a pact save 'who' right now? {@code boss} is true for a boss (never saved), {@code unsaveable} for a death that cannot be
     * undone from inside the death event (the void, a kill command). Pure: nothing changes until {@link #commit}.
     */
    public Verdict check(K who, boolean boss, boolean unsaveable, long now) {
        if (boss) return Verdict.BOSS;
        if (unsaveable) return Verdict.UNSAVEABLE;
        if (isDecaying(who, now)) return Verdict.STILL_DECAYING;
        if (pactFor(who, now) != null) return Verdict.REVIVE;
        boolean everCovered = false, expired = false, used = false;
        for (Map.Entry<K, Pact<K>> e : pacts.entrySet()) {
            Pact<K> p = e.getValue();
            if (!e.getKey().equals(who) && !p.allies.contains(who)) continue;
            everCovered = true;
            if (now >= p.until) expired = true;
            if (p.used.contains(who)) used = true;
        }
        if (!everCovered) return Verdict.NO_PACT;
        return used ? Verdict.ALREADY_USED : expired ? Verdict.EXPIRED : Verdict.NO_PACT;
    }

    /** Spends 'who''s one use in the pact that covers him and starts his decay window; returns the caster who pays, or null if no pact covers him. */
    public K commit(K who, long now, long decayTicks) {
        K caster = pactFor(who, now);
        if (caster == null) return null;
        pacts.get(caster).used.add(who);
        decaying.put(who, now + Math.max(1, decayTicks));
        return caster;
    }

    public boolean isDecaying(K who, long now) {
        Long end = decaying.get(who);
        return end != null && now < end;
    }

    public long decayEnd(K who) { return decaying.getOrDefault(who, 0L); }

    /** Everyone whose decay window is open (for the server's once-a-half-second look at them). */
    public List<K> decayingIds(long now) {
        List<K> out = new ArrayList<>();
        for (Map.Entry<K, Long> e : decaying.entrySet()) if (now < e.getValue()) out.add(e.getKey());
        return out;
    }

    /** A real death ends the decay window: whoever respawns is themselves again and may be covered by a pact once more. */
    public void clearDecay(K who) { decaying.remove(who); }

    /** Puts a decay window back after a reload (the same end tick). */
    public void restoreDecay(K who, long end) { decaying.put(who, end); }

    /** Drops expired pacts and finished decay windows; returns the ids whose decay window just ended. */
    public List<K> prune(long now) {
        pacts.values().removeIf(p -> now >= p.until + 20L * 60);          // an expired pact is kept a minute so the verdict can say "expired"
        List<K> ended = new ArrayList<>();
        decaying.entrySet().removeIf(e -> {
            if (now >= e.getValue()) { ended.add(e.getKey()); return true; }
            return false;
        });
        return ended;
    }

    public void clear() { pacts.clear(); decaying.clear(); }
}
