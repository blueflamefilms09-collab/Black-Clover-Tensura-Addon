package com.newuniverse.nusmp.elite.nations.war;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;

/**
 * Why: the alliances ("alliance proposals (propose/accept/break)"), pure. An alliance is symmetric; a proposal is directed (from, to) and expires. The rules: no alliance with yourself, no duplicate, none
 * between nations at war, none with a nation at war with one of your allies (an alliance may not make your ally's enemy your friend), a cap on allies per nation, and a cooldown after a break so alliances
 * cannot be flipped for tactical effect. A war declaration against an ally is refused elsewhere ({@link WarRules#canDeclare}); allies may not hurt each other (see {@code PvpRules}).
 */
public final class AllianceBook {
    public enum Result { OK, SELF, ALREADY_ALLIED, AT_WAR, ALLY_AT_WAR, LIMIT_SELF, LIMIT_OTHER, COOLDOWN, DUPLICATE, NO_PROPOSAL, EXPIRED, NOT_ALLIED }

    /** Pair key to the tick the alliance started. */
    public final Map<String, Long> allied = new LinkedHashMap<>();
    /** "from>to" to the expiry tick. */
    public final Map<String, Long> proposals = new LinkedHashMap<>();
    /** Pair key to the tick until which the pair may not ally again. */
    public final Map<String, Long> cooldown = new HashMap<>();

    private static String dir(String from, String to) { return from + ">" + to; }

    public boolean areAllied(String a, String b) { return a != null && b != null && !a.equals(b) && allied.containsKey(War.pairKey(a, b)); }

    public Set<String> alliesOf(String nation) {
        Set<String> out = new TreeSet<>();
        for (String k : allied.keySet()) {
            int bar = k.indexOf('|');
            String x = k.substring(0, bar), y = k.substring(bar + 1);
            if (x.equals(nation)) out.add(y);
            else if (y.equals(nation)) out.add(x);
        }
        return out;
    }

    public int allyCount(String nation) { return alliesOf(nation).size(); }

    public boolean hasProposal(String from, String to) { return proposals.containsKey(dir(from, to)); }

    /** Pending proposals addressed to a nation (the proposer ids). */
    public List<String> proposalsTo(String to) {
        List<String> out = new ArrayList<>();
        for (String k : proposals.keySet()) {
            int gt = k.indexOf('>');
            if (k.substring(gt + 1).equals(to)) out.add(k.substring(0, gt));
        }
        return out;
    }

    public List<String> proposalsFrom(String from) {
        List<String> out = new ArrayList<>();
        for (String k : proposals.keySet()) {
            int gt = k.indexOf('>');
            if (k.substring(0, gt).equals(from)) out.add(k.substring(gt + 1));
        }
        return out;
    }

    private Result checkPair(String a, String b, long now, int maxAllies, BiPredicate<String, String> atWar) {
        if (a == null || b == null || a.equals(b)) return Result.SELF;
        if (areAllied(a, b)) return Result.ALREADY_ALLIED;
        if (atWar.test(a, b)) return Result.AT_WAR;
        Long cd = cooldown.get(War.pairKey(a, b));
        if (cd != null && now < cd) return Result.COOLDOWN;
        if (maxAllies > 0 && allyCount(a) >= maxAllies) return Result.LIMIT_SELF;
        if (maxAllies > 0 && allyCount(b) >= maxAllies) return Result.LIMIT_OTHER;
        for (String ally : alliesOf(a)) if (atWar.test(b, ally)) return Result.ALLY_AT_WAR;
        for (String ally : alliesOf(b)) if (atWar.test(a, ally)) return Result.ALLY_AT_WAR;
        return Result.OK;
    }

    public Result propose(String from, String to, long now, long ttl, int maxAllies, BiPredicate<String, String> atWar) {
        Result r = checkPair(from, to, now, maxAllies, atWar);
        if (r != Result.OK) return r;
        if (hasProposal(from, to)) return Result.DUPLICATE;
        proposals.put(dir(from, to), now + Math.max(1L, ttl));
        // a counter-proposal means both want it: accept immediately
        if (hasProposal(to, from)) return accept(from, to, now, maxAllies, atWar);
        return Result.OK;
    }

    /** {@code to} accepts the proposal {@code from} made. */
    public Result accept(String to, String from, long now, int maxAllies, BiPredicate<String, String> atWar) {
        Long exp = proposals.get(dir(from, to));
        if (exp == null) return Result.NO_PROPOSAL;
        if (now >= exp) { proposals.remove(dir(from, to)); return Result.EXPIRED; }
        Result r = checkPair(from, to, now, maxAllies, atWar);
        if (r != Result.OK) return r;
        proposals.remove(dir(from, to));
        proposals.remove(dir(to, from));
        allied.put(War.pairKey(from, to), now);
        return Result.OK;
    }

    public Result decline(String to, String from) { return proposals.remove(dir(from, to)) != null ? Result.OK : Result.NO_PROPOSAL; }

    /** Withdraws a proposal the caller made. */
    public Result withdraw(String from, String to) { return proposals.remove(dir(from, to)) != null ? Result.OK : Result.NO_PROPOSAL; }

    public Result breakAlliance(String a, String b, long now, long cooldownTicks) {
        if (allied.remove(War.pairKey(a, b)) == null) return Result.NOT_ALLIED;
        if (cooldownTicks > 0) cooldown.put(War.pairKey(a, b), now + cooldownTicks);
        return Result.OK;
    }

    /** Drops expired proposals. Returns how many. */
    public int expire(long now) {
        int n = 0;
        for (var it = proposals.entrySet().iterator(); it.hasNext(); ) {
            if (now >= it.next().getValue()) { it.remove(); n++; }
        }
        cooldown.values().removeIf(until -> now >= until);
        return n;
    }

    /** A nation disbanded: all its alliances and proposals go. */
    public void removeNation(String nation) {
        allied.keySet().removeIf(k -> pairHas(k, nation));
        proposals.keySet().removeIf(k -> { int gt = k.indexOf('>'); return k.substring(0, gt).equals(nation) || k.substring(gt + 1).equals(nation); });
        cooldown.keySet().removeIf(k -> pairHas(k, nation));
    }

    private static boolean pairHas(String key, String nation) {
        int bar = key.indexOf('|');
        return key.substring(0, bar).equals(nation) || key.substring(bar + 1).equals(nation);
    }
}
