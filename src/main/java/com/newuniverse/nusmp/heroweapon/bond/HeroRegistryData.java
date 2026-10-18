package com.newuniverse.nusmp.heroweapon.bond;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Why: "one holder per Holy Weapon" cannot be answered from the players' own bonds, because the holder may be offline: the world keeps a small register of who
 * holds which weapon (weapon id to holder uuids), claimed when a bond is made and freed when it is released. This is the pure core of that register, with no
 * Minecraft types, so the rules a mistake would break (a claim is idempotent, releasing frees only the releaser's claim, a stale claim can be swept, a weapon
 * several players may share keeps all of them) are unit tests. The saved-data wrapper {@code HeroRegistry} only reads and writes this map to NBT. A weapon that
 * is not unique (a Seven Star or Vassal Weapon, unless the config says so) is recorded too, so {@code /multiverse hero list} can say who holds what.
 */
public final class HeroRegistryData {
    private final Map<String, Set<String>> holders = new LinkedHashMap<>();

    /** The holders of a weapon (uuid texts), in the order they claimed it; empty when nobody holds it. */
    public List<String> holdersOf(String weapon) {
        Set<String> s = holders.get(weapon);
        return s == null ? List.of() : List.copyOf(s);
    }

    /** The first holder of a weapon, or null. */
    public String firstHolder(String weapon) {
        Set<String> s = holders.get(weapon);
        return s == null || s.isEmpty() ? null : s.iterator().next();
    }

    /** A holder of the weapon other than {@code self}, or null when there is none (the question a uniqueness gate asks). */
    public String otherHolder(String weapon, String self) {
        Set<String> s = holders.get(weapon);
        if (s == null) return null;
        for (String h : s) if (!h.equals(self)) return h;
        return null;
    }

    /** Records {@code uuid} as a holder of the weapon; true when that changed anything (a second claim of the same holder changes nothing). */
    public boolean claim(String weapon, String uuid) {
        if (weapon == null || weapon.isEmpty() || uuid == null || uuid.isEmpty()) return false;
        return holders.computeIfAbsent(weapon, k -> new LinkedHashSet<>()).add(uuid);
    }

    /** Frees {@code uuid}'s claim of the weapon; true when there was one. Other holders of the same weapon are untouched. */
    public boolean release(String weapon, String uuid) {
        Set<String> s = holders.get(weapon);
        if (s == null || !s.remove(uuid)) return false;
        if (s.isEmpty()) holders.remove(weapon);
        return true;
    }

    /** Frees every claim {@code uuid} has except the one for {@code keep} (null keeps none); true when anything was freed. */
    public boolean releaseAllExcept(String uuid, String keep) {
        boolean any = false;
        for (String w : new ArrayList<>(holders.keySet())) {
            if (w.equals(keep)) continue;
            any |= release(w, uuid);
        }
        return any;
    }

    /** Frees every claim of one weapon (an operator's cleanup after a holder's data was lost); returns how many holders were dropped. */
    public int clearWeapon(String weapon) {
        Set<String> s = holders.remove(weapon);
        return s == null ? 0 : s.size();
    }

    /** Drops every claim for which {@code stale} answers true (weapon, uuid); returns how many were dropped. */
    public int sweep(BiPredicate<String, String> stale) {
        int n = 0;
        for (String w : new ArrayList<>(holders.keySet())) {
            Set<String> s = holders.get(w);
            for (String h : new ArrayList<>(s)) {
                if (stale.test(w, h)) {
                    s.remove(h);
                    n++;
                }
            }
            if (s.isEmpty()) holders.remove(w);
        }
        return n;
    }

    /** The weapons that have at least one holder. */
    public Set<String> weapons() { return Collections.unmodifiableSet(new LinkedHashSet<>(holders.keySet())); }

    public boolean isEmpty() { return holders.isEmpty(); }

    /** A copy of the whole register (for saving). */
    public Map<String, List<String>> toMap() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : holders.entrySet()) out.put(e.getKey(), List.copyOf(e.getValue()));
        return out;
    }

    /** Replaces the register with a saved one. */
    public void load(Map<String, List<String>> saved) {
        holders.clear();
        if (saved == null) return;
        for (Map.Entry<String, List<String>> e : saved.entrySet()) for (String h : e.getValue()) claim(e.getKey(), h);
    }
}
