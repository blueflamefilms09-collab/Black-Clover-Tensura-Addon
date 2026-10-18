package com.newuniverse.nusmp.heroweapon.weapon.shield;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Shield remembers a few things about its wielder between one call and the next: the guard's Resolve and Counter, the souls the Soul Eater form has eaten,
 * which foes are in a Shield Prison (Iron Maiden needs one), how long ago a needle last answered each attacker, the floating shields that stand, and the timers of the
 * form passives. They are gathered in one plain object per wielder (no Minecraft types, so the parts that have rules are tested alone) which the runtime keeps in a
 * map and drops when the wielder leaves or the server stops. Everything here is bounded: the tables hold a handful of entries and are pruned whenever they are read.
 */
public final class ShieldState {
    /** A floating shield (Air Strike Shield): its geometry, what it can still take, when it ends, and whether it has been ended early. */
    public static final class Floating {
        public final PanelMath.Panel geo;
        public int hp;
        public final long until;
        public boolean dead;

        public Floating(PanelMath.Panel geo, int hp, long until) {
            this.geo = geo;
            this.hp = hp;
            this.until = until;
        }
    }

    public final GuardMeter guard = new GuardMeter();
    public final SoulPool souls = new SoulPool();
    /** Game time of the last blow the wielder took (any blow), for the healing passive. */
    public long lastHurt = Long.MIN_VALUE / 4;
    /** Game time before which Rotting Endurance may not trigger again. */
    public long enduranceReadyAt;
    /** Rage Fury: the strength level now (0 = none, 1 = the second level) and when it ends. */
    public int furyLevel;
    public long furyUntil;
    /** The last Counter charge announced to the wielder, in steps of 25, so a charge is told once per step. */
    public int announcedStep;
    /** Whether the wielder was told the guard is failing (told once when the Resolve falls under a quarter, again only after it has recovered past a half). */
    public boolean warnedLow;

    private final Map<UUID, Long> cages = new HashMap<>();
    private final Map<UUID, Long> answered = new HashMap<>();
    private final List<Floating> floating = new ArrayList<>();

    // ------------------------------------------------------------------ prisons
    /** Marks a foe as held in a Shield Prison until {@code until} (at most a few entries are kept). */
    public void cage(UUID foe, long until, long now) {
        pruneCages(now);
        if (cages.size() >= ShieldNumbers.MAX_TARGETS) {
            UUID soonest = null;
            for (Map.Entry<UUID, Long> e : cages.entrySet()) if (soonest == null || e.getValue() < cages.get(soonest)) soonest = e.getKey();
            if (soonest != null) cages.remove(soonest);
        }
        cages.put(foe, until);
    }

    /** True if the foe is still in a prison of this wielder at {@code now}. */
    public boolean caged(UUID foe, long now) {
        Long u = cages.get(foe);
        if (u == null) return false;
        if (u <= now || u - now > ShieldNumbers.CAGE_MARK_TICKS * 4L) {
            cages.remove(foe);
            return false;
        }
        return true;
    }

    /** The prison mark goes away (the foe was struck by the Iron Maiden, or died). */
    public void release(UUID foe) { cages.remove(foe); }

    private void pruneCages(long now) { cages.values().removeIf(u -> u <= now); }

    public int cagedCount(long now) {
        pruneCages(now);
        return cages.size();
    }

    // ------------------------------------------------------------------ the needle's pause per attacker
    /** True and records it if the needle may answer this attacker now (not within {@link ShieldNumbers#NEEDLE_ICD} ticks of its last answer). */
    public boolean mayAnswer(UUID attacker, long now) {
        Long last = answered.get(attacker);
        if (last != null && now >= last && now - last < ShieldNumbers.NEEDLE_ICD) return false;
        if (answered.size() >= 16) {
            for (Iterator<Map.Entry<UUID, Long>> it = answered.entrySet().iterator(); it.hasNext(); ) if (now - it.next().getValue() >= ShieldNumbers.NEEDLE_ICD) it.remove();
            if (answered.size() >= 16) answered.clear();
        }
        answered.put(attacker, now);
        return true;
    }

    // ------------------------------------------------------------------ floating shields
    /**
     * Adds a floating shield; if {@link ShieldNumbers#MAX_PANELS} already stand, the oldest is marked dead (its task ends it) to make room. Returns the ones that were
     * marked dead so the caller can show their end.
     */
    public List<Floating> addFloating(Floating f, long now) {
        List<Floating> gone = new ArrayList<>();
        floating.removeIf(x -> x.dead || x.until <= now);
        while (floating.size() >= ShieldNumbers.MAX_PANELS) {
            Floating oldest = floating.remove(0);
            oldest.dead = true;
            gone.add(oldest);
        }
        floating.add(f);
        return gone;
    }

    public void removeFloating(Floating f) { floating.remove(f); }

    public int floatingCount() { return floating.size(); }

    /** Ends every floating shield of this wielder (the weapon was put away). */
    public void endFloating() {
        for (Floating f : floating) f.dead = true;
        floating.clear();
    }

    /** Back to the start (the form changed, the weapon was put away). */
    public void reset() {
        guard.reset();
        souls.clear();
        cages.clear();
        answered.clear();
        endFloating();
        lastHurt = Long.MIN_VALUE / 4;
        enduranceReadyAt = 0;
        furyLevel = 0;
        furyUntil = 0;
        announcedStep = 0;
        warnedLow = false;
    }
}
