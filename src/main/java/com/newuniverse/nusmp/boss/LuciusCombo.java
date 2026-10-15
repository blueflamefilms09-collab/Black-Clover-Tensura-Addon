package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.entity.zogratis.LuciusTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Why: Lucius's old chooser was a weighted draw per think tick, so the heaviest-weighted cheap spell (the Towers, Soul Motes) came up
 * again and again. The owner asked for a cooldown-managed combo system that chains distinct offensive, defensive and spatial magic. This
 * pure planner (no Minecraft types, deterministic for a seed) decides <i>which</i> of the moves the game already allows comes next; the
 * game still decides what is <i>allowed</i> (power gate at 20/40/60/80%, range, mana, ultimate pacing), so the HP phases widen the
 * roster exactly as before. Each spell has a role. A combo is four casts: SPATIAL (reposition or cut space), OFFENSIVE (damage),
 * DEFENSIVE or CONTROL (alternating from combo to combo), then one MAGIC cast (the Towers answer: any spell of any of the 36+ magics,
 * drawn by the existing deck so every magic comes before one repeats). Rules, in order of relaxation when the pool is thin: the move
 * is off its own cooldown; it is not among the last min(3, pool - 1) casts; it has the wanted role; then the role is relaxed, then the
 * last-cast rule, then the cooldown (the least remaining one), so a non-empty pool never yields nothing.
 */
public final class LuciusCombo {
    /** What a spell is for. */
    public enum Role { SPATIAL, OFFENSIVE, DEFENSIVE, CONTROL, MAGIC }

    /** A move the game allows right now, with its table weight. */
    public record Candidate(String id, int weight) {}

    /** How many of the latest casts a move may not repeat within (capped at pool size - 1). */
    public static final int RECENT = 3;
    /** Casts in one combo. */
    public static final int COMBO_LENGTH = 4;

    private static final Map<String, Role> ROLES = new HashMap<>();
    static {
        // his table (LuciusTable)
        ROLES.put(LuciusTable.SOUL_MOTES, Role.OFFENSIVE);
        ROLES.put(LuciusTable.SOUL_DRAIN, Role.OFFENSIVE);
        ROLES.put(LuciusTable.LIGHT_STEP, Role.SPATIAL);
        ROLES.put(LuciusTable.TOWER_SPELL, Role.MAGIC);
        ROLES.put(LuciusTable.BONE_ARSENAL, Role.OFFENSIVE);
        ROLES.put(LuciusTable.MIRROR_COURT, Role.DEFENSIVE);
        ROLES.put(LuciusTable.BLOOD_TIDE, Role.OFFENSIVE);
        ROLES.put(LuciusTable.GRAVITY_CRUSH, Role.CONTROL);
        ROLES.put(LuciusTable.SWAP_STRIKE, Role.SPATIAL);
        ROLES.put(LuciusTable.MEMORY_SCRAMBLE, Role.CONTROL);
        ROLES.put(LuciusTable.TIME_HALT, Role.CONTROL);
        ROLES.put(LuciusTable.ACCELERATE, Role.DEFENSIVE);
        ROLES.put(LuciusTable.PALADIN_CALL, Role.DEFENSIVE);
        ROLES.put(LuciusTable.TIME_REVERSAL, Role.DEFENSIVE);
        ROLES.put(LuciusTable.RESURRECTION, Role.DEFENSIVE);
        ROLES.put(LuciusTable.LIGHT_COMBO, Role.SPATIAL);
        ROLES.put(LuciusTable.SOUL_PURGE, Role.OFFENSIVE);
        ROLES.put(LuciusTable.SIBLINGS, Role.OFFENSIVE);
        ROLES.put(LuciusTable.TIME_STOP, Role.CONTROL);
        ROLES.put(LuciusTable.TOWER_STORM, Role.MAGIC);
        // the cast spells (LuciusSpellTable)
        ROLES.put(LuciusNumbers.MEGIDDO, Role.OFFENSIVE);
        ROLES.put(LuciusNumbers.DIMENSION_CUTTER, Role.SPATIAL);
        ROLES.put(LuciusNumbers.TRUE_DARKNESS, Role.CONTROL);
        ROLES.put(LuciusNumbers.JUDGMENT, Role.OFFENSIVE);
        ROLES.put(LuciusNumbers.CHRONO_RINGS, Role.CONTROL);
    }

    private static final Role[] CYCLE = {Role.SPATIAL, Role.OFFENSIVE, Role.DEFENSIVE, Role.CONTROL, Role.MAGIC};

    /** The role of a move id; a move this class does not know counts as OFFENSIVE. */
    public static Role roleOf(String id) { return ROLES.getOrDefault(id, Role.OFFENSIVE); }

    /** Every move id with a role (the roster the planner knows). */
    public static List<String> roster() { return List.copyOf(ROLES.keySet()); }

    private final Random rng;
    private final Map<String, Long> readyAt = new HashMap<>();
    private final Deque<String> recent = new ArrayDeque<>();
    private int slot, combos;

    public LuciusCombo(long seed) { rng = new Random(seed); }

    /** A fresh fight: the history and the combo position are forgotten (cooldowns too). */
    public void reset() {
        readyAt.clear();
        recent.clear();
        slot = 0;
        combos = 0;
    }

    /** The role the next cast wants. */
    public Role wanted() {
        return switch (slot % COMBO_LENGTH) {
            case 0 -> Role.SPATIAL;
            case 1 -> Role.OFFENSIVE;
            case 2 -> combos % 2 == 0 ? Role.DEFENSIVE : Role.CONTROL;
            default -> Role.MAGIC;
        };
    }

    public int slot() { return slot; }

    /** Ticks until a move is off the planner's own cooldown (0 = ready). */
    public long cooldownLeft(String id, long now) { return Math.max(0, readyAt.getOrDefault(id, 0L) - now); }

    /** Casts so far in this fight, newest last (at most the ring size). */
    public List<String> lastCasts() { return List.copyOf(recent); }

    /** The roles to try, the wanted one first, then the rest in the cycle's order. */
    private List<Role> order(Role first) {
        List<Role> out = new ArrayList<>();
        out.add(first);
        int at = 0;
        for (int i = 0; i < CYCLE.length; i++) if (CYCLE[i] == first) at = i;
        for (int k = 1; k < CYCLE.length; k++) out.add(CYCLE[(at + k) % CYCLE.length]);
        return out;
    }

    /** Is 'id' among the latest 'window' casts? */
    private boolean inRecent(String id, int window) {
        int skip = Math.max(0, recent.size() - window), i = 0;
        for (String r : recent) if (i++ >= skip && r.equals(id)) return true;
        return false;
    }

    private Optional<String> draw(List<Candidate> from) {
        if (from.isEmpty()) return Optional.empty();
        int total = 0;
        for (Candidate c : from) total += Math.max(1, c.weight());
        int roll = rng.nextInt(total);
        for (Candidate c : from) {
            roll -= Math.max(1, c.weight());
            if (roll < 0) return Optional.of(c.id());
        }
        return Optional.of(from.get(from.size() - 1).id());
    }

    /**
     * The next move among 'pool' (the moves the game allows now), or empty only when the pool is empty. Does not change the combo
     * position or the history: call {@link #commit} when the move is really started.
     */
    public Optional<String> pick(List<Candidate> pool, long now) {
        if (pool == null || pool.isEmpty()) return Optional.empty();
        List<Candidate> p = new ArrayList<>();
        for (Candidate c : pool) {                                              // one entry per id
            boolean dup = false;
            for (Candidate q : p) if (q.id().equals(c.id())) { dup = true; break; }
            if (!dup && c.id() != null) p.add(c);
        }
        if (p.isEmpty()) return Optional.empty();
        int window = Math.min(RECENT, Math.max(0, p.size() - 1));
        List<Candidate> ready = new ArrayList<>(), fresh = new ArrayList<>();
        for (Candidate c : p) {
            if (cooldownLeft(c.id(), now) > 0) continue;
            ready.add(c);
            if (!inRecent(c.id(), window)) fresh.add(c);
        }
        for (List<Candidate> tier : List.of(fresh, notLast(ready))) {           // 1. cooldown + history, by role in cycle order; 2. cooldown + not the last cast
            for (Role r : order(wanted())) {
                List<Candidate> hit = new ArrayList<>();
                for (Candidate c : tier) if (roleOf(c.id()) == r) hit.add(c);
                Optional<String> d = draw(hit);
                if (d.isPresent()) return d;
            }
            if (tier == fresh) continue;
            Optional<String> any = draw(tier);
            if (any.isPresent()) return any;
        }
        if (!ready.isEmpty()) return draw(ready);                               // 3. only the cooldown
        Candidate best = p.get(0);                                              // 4. everything cools: the one that is ready soonest (not the last cast if avoidable)
        String last = recent.peekLast();
        for (Candidate c : p) {
            long a = cooldownLeft(c.id(), now), b = cooldownLeft(best.id(), now);
            boolean cLast = c.id().equals(last), bLast = best.id().equals(last);
            if (a < b || (a == b && bLast && !cLast)) best = c;
        }
        return Optional.of(best.id());
    }

    private List<Candidate> notLast(List<Candidate> ready) {
        String last = recent.peekLast();
        List<Candidate> out = new ArrayList<>();
        for (Candidate c : ready) if (!c.id().equals(last)) out.add(c);
        return out.isEmpty() ? ready : out;
    }

    /** The move 'id' was started at 'now' with a cooldown of 'cooldownTicks': history, cooldown and the combo position move on. */
    public void commit(String id, long now, int cooldownTicks) {
        if (id == null) return;
        readyAt.put(id, now + Math.max(0, cooldownTicks));
        recent.addLast(id);
        while (recent.size() > 8) recent.pollFirst();
        slot++;
        if (slot % COMBO_LENGTH == 0) combos++;
    }
}
