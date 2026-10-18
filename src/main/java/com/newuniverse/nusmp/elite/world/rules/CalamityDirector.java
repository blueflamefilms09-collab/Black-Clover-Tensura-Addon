package com.newuniverse.nusmp.elite.world.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.ToLongFunction;

/**
 * Why: the world-event director decides WHEN a calamity wakes, and it must be testable without a world. This is the pure half: it is fed one {@link Observation} per player per check
 * (the density of the chunk the player stands in, the Mother Rocks around, whether it is night and which moon), keeps the long-running timers ("very high magicule for long") and answers
 * with at most one {@link Decision}. The game half (CalamityManager) gathers the observations, applies the decision (an omen, then the boss) and remembers when each calamity last woke.
 *
 * <p>The rules, all conservative by default: nothing wakes while another calamity is active or announced; a calamity has its own cooldown since it last woke; a dimension must be allowed by
 * the calamity; a timer only runs while its condition holds and drains twice as fast when it does not (so a single step out of the dense chunk does not erase an hour, but a long absence does);
 * a night trigger rolls a small chance per check, so a calamity at night is an event, not a clock.
 */
public final class CalamityDirector {
    /** What one player's surroundings look like at a check. */
    public record Observation(UUID player, String dimension, double density, int motherRocks, boolean night, int moonPhase, long gameTime) {}

    /** The answer: wake {@code calamityId} near {@code player}, because of {@code reason}. */
    public record Decision(String calamityId, CalamityDef.Type reason, UUID player) {}

    private final Map<String, Integer> timers = new HashMap<>();

    /** Forgets every timer of a player (after their calamity woke, or when they leave). */
    public void reset(UUID player) {
        String prefix = player + ":";
        timers.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public int timer(UUID player, String calamityId, int triggerIndex) { return timers.getOrDefault(key(player, calamityId, triggerIndex), 0); }

    private static String key(UUID p, String c, int i) { return p + ":" + c + ":" + i; }

    private int advance(UUID p, String c, int i, boolean holds, int dt) {
        String k = key(p, c, i);
        int cur = timers.getOrDefault(k, 0);
        cur = holds ? cur + dt : Math.max(0, cur - 2 * dt);
        if (cur == 0) timers.remove(k);
        else timers.put(k, cur);
        return cur;
    }

    /**
     * One check of one player. {@code busy} is true while a calamity is active or announced; {@code lastWoke} gives the game time a calamity last woke (or a very negative number);
     * {@code rng} makes the night roll and the weighted choice (seeded in tests). Always advances the timers, even when busy (the stored time keeps running).
     */
    public Optional<Decision> check(List<CalamityDef> defs, Observation o, int dtTicks, boolean busy, ToLongFunction<String> lastWoke, Random rng) {
        List<CalamityDef> ready = new ArrayList<>();
        List<CalamityDef.Type> why = new ArrayList<>();
        for (CalamityDef d : defs) {
            if (!d.enabled() || !d.allowsDimension(o.dimension())) continue;
            boolean cooled = o.gameTime() - lastWoke.applyAsLong(d.id()) >= d.cooldownTicks();
            CalamityDef.Type fired = null;
            for (int i = 0; i < d.triggers().size(); i++) {
                CalamityDef.Trigger t = d.triggers().get(i);
                if (!t.automatic()) continue;
                boolean ok = switch (t.type()) {
                    case SUSTAINED_MAGICULE -> advance(o.player(), d.id(), i, o.density() >= t.density(), dtTicks) >= t.seconds() * 20;
                    case MOTHER_ROCKS -> advance(o.player(), d.id(), i, o.motherRocks() >= t.count() && o.density() >= t.density(), dtTicks) >= t.seconds() * 20;
                    case NIGHT -> nightOk(t, o, rng);
                    default -> false;
                };
                if (ok && fired == null) fired = t.type();
            }
            if (fired != null && cooled) {
                ready.add(d);
                why.add(fired);
            }
        }
        if (busy || ready.isEmpty()) return Optional.empty();
        int total = 0;
        for (CalamityDef d : ready) total += d.weight();
        int roll = rng.nextInt(Math.max(1, total)), acc = 0;
        for (int i = 0; i < ready.size(); i++) {
            acc += ready.get(i).weight();
            if (roll < acc) return Optional.of(new Decision(ready.get(i).id(), why.get(i), o.player()));
        }
        return Optional.of(new Decision(ready.get(0).id(), why.get(0), o.player()));
    }

    private static boolean nightOk(CalamityDef.Trigger t, Observation o, Random rng) {
        if (!o.night() || o.density() < t.density() || o.motherRocks() < t.count()) return false;
        if (t.moonPhase() >= 0 && t.moonPhase() != o.moonPhase()) return false;
        return rng.nextDouble() < Math.max(0.0, Math.min(1.0, t.chance()));
    }
}
