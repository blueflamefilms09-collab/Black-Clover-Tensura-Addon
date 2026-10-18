package com.newuniverse.nusmp.elite.hub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Why: this is the "IPlayerQuestData" of the brief, the player's quest memory, and the state machine that goes with it. In NeoForge 1.21 a capability is not the place for saved per-player data;
 * the data ATTACHMENT is ({@link HubAttachments}, id {@code nusmp:hub_quests}, the same way the System's {@code SystemState} does it), so this class is the pure value inside the attachment: no
 * Minecraft type, tested without a game. A quest is ACTIVE from the moment it is accepted, becomes READY when its objective is met (a kill count reached, a floor entered, or enough of the
 * item in the bag), and ends exactly once when it is handed in at the Quest Giver (reward paid, daily marked done for today or progression marked done for good); after that every method is a
 * no-op for it, so a duplicated event or two kills in one tick can never pay twice. Daily quests belong to a day (the world's day count): on a new day the done-today set is cleared and a daily
 * quest accepted on an earlier day is dropped. The same value keeps the deepest floor the player has reached (it unlocks harder quests) and the shop's per-day purchase counters (the stock of the
 * Merchant is per player, see docs/elite/hub.md). Mutable on purpose and saved through {@link Data}; every number read back is clamped, so a damaged save cannot break the machine.
 */
public final class PlayerQuestData {
    public enum State { ACTIVE, READY }

    /** What a feeding call did to one quest. */
    public enum Change { NONE, PROGRESSED, READY }

    /** The result of {@link #accept}. */
    public enum AcceptResult { OK, ALREADY_ACTIVE, ALREADY_DONE, FULL, LOCKED, NOT_OFFERED }

    /** One accepted quest. */
    public static final class Active {
        private final String id;
        private final int target;
        private final long acceptedDay;
        private int progress;
        private State state = State.ACTIVE;

        Active(String id, int target, long acceptedDay) {
            this.id = id;
            this.target = Math.max(1, target);
            this.acceptedDay = acceptedDay;
        }

        public String id() { return id; }

        public int target() { return target; }

        public int progress() { return progress; }

        public State state() { return state; }

        public boolean ready() { return state == State.READY; }

        public long acceptedDay() { return acceptedDay; }
    }

    /** One quest's change after an event. */
    public record Update(String id, Change change) {}

    /** The saved form (plain values; the codec of {@link HubAttachments} writes it). */
    public record ActiveData(String id, int progress, int target, String state, long accepted) {}

    public record Data(List<ActiveData> active, List<String> completed, long dailyDay, List<String> dailyDone, int maxHoly, int maxAbyssal, long shopDay, Map<String, Integer> shopBought, int totalDone) {
        public static final Data EMPTY = new Data(List.of(), List.of(), -1L, List.of(), 0, 0, -1L, Map.of(), 0);
    }

    /** At most this many ids are kept in each saved list (a hand-edited or hostile save cannot grow it without limit). */
    public static final int MAX_LIST = 2048;

    private final List<Active> active = new ArrayList<>();
    private final Set<String> completed = new LinkedHashSet<>();
    private final Set<String> dailyDone = new LinkedHashSet<>();
    private final Map<String, Integer> shopBought = new LinkedHashMap<>();
    private long dailyDay = -1L;
    private long shopDay = -1L;
    private int maxHoly;
    private int maxAbyssal;
    private int totalDone;

    // ---------------------------------------------------------------- reading
    public List<Active> active() { return Collections.unmodifiableList(active); }

    public Active find(String id) {
        for (Active a : active) if (a.id.equals(id)) return a;
        return null;
    }

    public Set<String> activeIds() {
        Set<String> out = new LinkedHashSet<>();
        for (Active a : active) out.add(a.id);
        return out;
    }

    public Set<String> completed() { return Collections.unmodifiableSet(completed); }

    public Set<String> dailyDoneToday() { return Collections.unmodifiableSet(dailyDone); }

    /** The deepest floor (absolute value) the player has reached on either road, 0 for none. */
    public int maxFloor() { return Math.max(maxHoly, maxAbyssal); }

    /** The deepest floor reached on one road (absolute value). */
    public int maxFloor(boolean holy) { return holy ? maxHoly : maxAbyssal; }

    /** Quests handed in over the player's whole life (progression and daily). */
    public int totalDone() { return totalDone; }

    public long dailyDay() { return dailyDay; }

    public boolean hasContent() {
        return !active.isEmpty() || !completed.isEmpty() || !dailyDone.isEmpty() || maxFloor() > 0 || !shopBought.isEmpty() || totalDone > 0;
    }

    // ---------------------------------------------------------------- days
    /**
     * A new day (or the first call): clears the done-today set and the shop counters, and drops every daily quest accepted on an earlier day. Returns the ids dropped. Does nothing when
     * {@code day} is the day already recorded.
     *
     * @param defs resolves a quest id to its definition (an id that no longer resolves is dropped too, since its type is unknown)
     */
    public List<String> rollover(long day, Function<String, QuestDef> defs) {
        List<String> dropped = new ArrayList<>();
        if (day == dailyDay) return dropped;
        dailyDay = day;
        dailyDone.clear();
        active.removeIf(a -> {
            QuestDef d = defs.apply(a.id);
            boolean drop = d == null || (d.daily() && a.acceptedDay != day);
            if (drop) dropped.add(a.id);
            return drop;
        });
        if (shopDay != day) {
            shopDay = day;
            shopBought.clear();
        }
        return dropped;
    }

    // ---------------------------------------------------------------- accepting and abandoning
    /**
     * Accepts a quest. {@code onBoard} says whether a daily quest is on today's board (the caller asks {@link QuestRules#dailyBoard}); a progression quest is checked against the chain
     * (its requirements must be done and its {@code minFloor} reached).
     */
    public AcceptResult accept(QuestDef q, long day, int maxActive, boolean onBoard) {
        if (find(q.id()) != null) return AcceptResult.ALREADY_ACTIVE;
        if (q.daily() ? dailyDone.contains(q.id()) : completed.contains(q.id())) return AcceptResult.ALREADY_DONE;
        if (q.daily() && !onBoard) return AcceptResult.NOT_OFFERED;
        if (q.minFloor() > maxFloor() || !completed.containsAll(q.requires())) return AcceptResult.LOCKED;
        if (active.size() >= Math.max(1, maxActive)) return AcceptResult.FULL;
        Active a = new Active(q.id(), q.kind() == ObjectiveKind.REACH_FLOOR ? 1 : q.target(), day);
        // a reach-floor quest the player already satisfies is ready at once; a floor reached before accepting counts
        if (q.kind() == ObjectiveKind.REACH_FLOOR && QuestRules.reaches(q, q.floor() > 0 ? maxHoly : -maxAbyssal)) {
            a.progress = a.target;
            a.state = State.READY;
        }
        active.add(a);
        return AcceptResult.OK;
    }

    /** Drops an accepted quest without a reward; true when it was there. */
    public boolean abandon(String id) { return active.removeIf(a -> a.id.equals(id)); }

    // ---------------------------------------------------------------- feeding
    private Change add(Active a, int n) {
        if (a.state != State.ACTIVE || n <= 0) return Change.NONE;
        int before = a.progress;
        a.progress = (int) Math.min(a.target, (long) a.progress + n);
        if (a.progress >= a.target) {
            a.state = State.READY;
            return Change.READY;
        }
        return a.progress != before ? Change.PROGRESSED : Change.NONE;
    }

    /** A creature fell to the player: every ACTIVE kill quest it matches moves by one. */
    public List<Update> onKill(KillFact fact, Function<String, QuestDef> defs) {
        List<Update> out = new ArrayList<>();
        for (Active a : active) {
            QuestDef d = defs.apply(a.id);
            if (d == null || !QuestRules.matches(d, fact)) continue;
            Change c = add(a, 1);
            if (c != Change.NONE) out.add(new Update(a.id, c));
        }
        return out;
    }

    /** The player moved on from a floor to a deeper one of the same road: every ACTIVE clear-floors quest it matches moves by one. */
    public List<Update> onFloorCleared(int floor, Function<String, QuestDef> defs) {
        List<Update> out = new ArrayList<>();
        for (Active a : active) {
            QuestDef d = defs.apply(a.id);
            if (d == null || !QuestRules.matchesFloor(d, floor)) continue;
            Change c = add(a, 1);
            if (c != Change.NONE) out.add(new Update(a.id, c));
        }
        return out;
    }

    /** The player stepped onto a floor: notes the deepest floor reached and finishes every ACTIVE reach-floor quest it satisfies. */
    public List<Update> onFloorEntered(int floor, Function<String, QuestDef> defs) {
        List<Update> out = new ArrayList<>();
        if (floor == 0) return out;
        noteFloor(floor);
        for (Active a : active) {
            QuestDef d = defs.apply(a.id);
            if (d == null || !QuestRules.reaches(d, floor)) continue;
            Change c = add(a, a.target);
            if (c != Change.NONE) out.add(new Update(a.id, c));
        }
        return out;
    }

    /** Raises the recorded deepest floor (a login on a deep floor, an admin); never lowers it. */
    public void noteFloor(int floor) {
        if (floor > 0) maxHoly = Math.max(maxHoly, floor);
        else if (floor < 0) maxAbyssal = Math.max(maxAbyssal, -floor);
    }

    // ---------------------------------------------------------------- handing in
    /**
     * Can this quest be handed in now? A READY quest always can; a delivery can once the bag holds enough ({@code have}). False for a quest that is not accepted.
     */
    public boolean canTurnIn(QuestDef q, long have) {
        Active a = find(q.id());
        if (a == null) return false;
        return a.ready() || (q.kind() == ObjectiveKind.DELIVER && have >= a.target);
    }

    /**
     * Hands the quest in: removes it from the active list and records it as done (a daily for today, a progression for good). The caller has already taken the delivered items and pays
     * the reward. False (nothing changes) when the quest is not accepted or not finished.
     */
    public boolean complete(QuestDef q, long have) {
        if (!canTurnIn(q, have)) return false;
        active.removeIf(a -> a.id.equals(q.id()));
        if (q.daily()) {
            if (dailyDone.size() < MAX_LIST) dailyDone.add(q.id());
        } else if (completed.size() < MAX_LIST) {
            completed.add(q.id());
        }
        totalDone++;
        return true;
    }

    // ---------------------------------------------------------------- the shop's per-day counters
    /** How many of an offer the player bought on {@code day} (the counters belong to one day). */
    public int boughtToday(String offerId, long day) { return shopDay == day ? shopBought.getOrDefault(offerId, 0) : 0; }

    /** Counts a purchase on {@code day} (a new day starts the counters again). */
    public void recordPurchase(String offerId, long day, int n) {
        if (shopDay != day) {
            shopDay = day;
            shopBought.clear();
        }
        if (shopBought.size() < MAX_LIST || shopBought.containsKey(offerId)) shopBought.merge(offerId, Math.max(1, n), Integer::sum);
    }

    // ---------------------------------------------------------------- saving
    public Data toData() {
        List<ActiveData> list = new ArrayList<>();
        for (Active a : active) list.add(new ActiveData(a.id, a.progress, a.target, a.state.name(), a.acceptedDay));
        return new Data(list, new ArrayList<>(completed), dailyDay, new ArrayList<>(dailyDone), maxHoly, maxAbyssal, shopDay, new LinkedHashMap<>(shopBought), totalDone);
    }

    /** Restores from the saved form; values are clamped, an unusable row is skipped. */
    public static PlayerQuestData of(Data d) {
        PlayerQuestData p = new PlayerQuestData();
        if (d == null) return p;
        if (d.active() != null) {
            for (ActiveData ad : d.active()) {
                if (ad == null || ad.id() == null || ad.id().isEmpty() || p.find(ad.id()) != null || p.active.size() >= 64) continue;
                Active a = new Active(ad.id(), ad.target(), ad.accepted());
                a.progress = Math.max(0, Math.min(a.target, ad.progress()));
                a.state = "READY".equalsIgnoreCase(ad.state()) || a.progress >= a.target ? State.READY : State.ACTIVE;
                p.active.add(a);
            }
        }
        if (d.completed() != null) for (String s : d.completed()) if (s != null && !s.isEmpty() && p.completed.size() < MAX_LIST) p.completed.add(s);
        if (d.dailyDone() != null) for (String s : d.dailyDone()) if (s != null && !s.isEmpty() && p.dailyDone.size() < MAX_LIST) p.dailyDone.add(s);
        if (d.shopBought() != null) {
            for (Map.Entry<String, Integer> e : d.shopBought().entrySet()) {
                if (e.getKey() != null && e.getValue() != null && e.getValue() > 0 && p.shopBought.size() < MAX_LIST) p.shopBought.put(e.getKey(), Math.min(e.getValue(), 1_000_000));
            }
        }
        p.dailyDay = d.dailyDay();
        p.shopDay = d.shopDay();
        p.maxHoly = Math.max(0, d.maxHoly());
        p.maxAbyssal = Math.max(0, d.maxAbyssal());
        p.totalDone = Math.max(0, d.totalDone());
        return p;
    }
}
