package com.newuniverse.nusmp.elite.nations.war;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: all wars of the server in one pure object: the open wars, the capped WAR HISTORY ("war history tracking"), the truce between a pair of nations after a war and the cooldown of a declarer. The
 * service wraps it with the clock, the config and the events; the book itself has no Minecraft types and is what the tests drive.
 */
public final class WarBook {
    /** A finished war for the history. Times are game ticks. */
    public record Record(long id, String attacker, String defender, long declaredAt, long endedAt, War.Outcome outcome, String winner,
                         int scoreAttacker, int scoreDefender, long spoils, String reason) {}

    public final Map<Long, War> wars = new LinkedHashMap<>();
    public final List<Record> history = new ArrayList<>();
    /** Pair key to the game tick until which the pair may not fight again. */
    public final Map<String, Long> truce = new HashMap<>();
    /** Nation to the game tick until which it may not declare again. */
    public final Map<String, Long> declareCooldown = new HashMap<>();
    public long nextId = 1L;
    private final int historyCap;

    public WarBook(int historyCap) { this.historyCap = Math.max(1, historyCap); }

    public War find(String a, String b) {
        for (War w : wars.values()) if (w.open() && w.isPair(a, b)) return w;
        return null;
    }

    /** The war between two nations that currently counts as hostile (declared, preparing or fighting), or null. */
    public War hostileBetween(String a, String b) {
        War w = find(a, b);
        return w != null && w.hostile() ? w : null;
    }

    public War fightingBetween(String a, String b) {
        War w = find(a, b);
        return w != null && w.fighting() ? w : null;
    }

    public List<War> warsOf(String nation) {
        List<War> out = new ArrayList<>();
        for (War w : wars.values()) if (w.open() && w.involves(nation)) out.add(w);
        return out;
    }

    public int openWarCount(String nation) {
        int n = 0;
        for (War w : wars.values()) if (w.open() && w.hostile() && w.involves(nation)) n++;
        return n;
    }

    public boolean truceBetween(String a, String b, long now) {
        Long until = truce.get(War.pairKey(a, b));
        return until != null && now < until;
    }

    public boolean declarerCoolingDown(String nation, long now) {
        Long until = declareCooldown.get(nation);
        return until != null && now < until;
    }

    public War add(War w) {
        wars.put(w.id, w);
        return w;
    }

    public long newId() { return nextId++; }

    /** Archives a war that reached ENDED: into the history, out of the open list, and sets the pair truce. */
    public Record archive(War w, long now, long truceTicks) {
        wars.remove(w.id);
        Record r = new Record(w.id, w.attacker, w.defender, w.declaredAt, now, w.outcome, w.winner, w.scoreAttacker, w.scoreDefender, w.spoilsCoins, w.reason);
        history.add(r);
        while (history.size() > historyCap) history.remove(0);
        if (truceTicks > 0) truce.put(w.pairKey(), now + truceTicks);
        return r;
    }

    /** Removes every trace of a disbanded nation: its open wars vanish (no history line), its history stays. */
    public List<War> removeNation(String nation) {
        List<War> gone = new ArrayList<>();
        for (War w : new ArrayList<>(wars.values())) if (w.involves(nation)) { wars.remove(w.id); gone.add(w); }
        declareCooldown.remove(nation);
        return gone;
    }

    /** History lines involving a nation, newest first. */
    public List<Record> historyOf(String nation, int limit) {
        List<Record> out = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0 && out.size() < limit; i--) {
            Record r = history.get(i);
            if (nation == null || r.attacker().equals(nation) || r.defender().equals(nation)) out.add(r);
        }
        return out;
    }

    public List<Record> historyView() { return Collections.unmodifiableList(history); }
}
