package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: "the quest succeeds BEFORE the player finds the next staircase" is the whole rule of the System, and a rule like that is only trustworthy as a small state
 * machine whose every transition is written down and tested, not as flags scattered over event handlers. A quest starts ACTIVE and ends exactly once, DONE or
 * FAILED; after that every method is a no-op (so a kill and a room counted in the same tick, a duplicate event or an admin command can never complete it twice
 * and pay two rewards). The counting kinds succeed the moment the count is reached; when the player enters a DEEPER floor with such a quest still ACTIVE it
 * FAILS. The two "reach the next floor" kinds (no damage, speed) are judged by that very arrival: ACTIVE on arrival means the condition held, so they succeed
 * there. Every method returns what changed ({@link Change}) so the caller knows when to pay, toast and sync, and does nothing otherwise. A quest belongs to ONE
 * floor and one player. Mutable on purpose (it is advanced up to a few times per second) and saved through {@link Data}; pure, so it is unit tested without a
 * game.
 */
public final class QuestProgress {
    public enum State { ACTIVE, DONE, FAILED }

    /** What a call did: nothing, moved the count, completed the quest, or failed it. */
    public enum Change { NONE, PROGRESSED, COMPLETED, FAILED }

    /** The saved form (plain values; the codec of {@link SystemState} writes it). */
    public record Data(String id, String kind, int floor, int target, int progress, String state, boolean noMagic, int timeLeft, List<Integer> counted) {}

    private final String id;
    private final QuestKind kind;
    private final int floor;
    private final int target;
    private final boolean noMagic;
    private int progress;
    private State state = State.ACTIVE;
    private int timeLeft;
    private final LinkedHashSet<Integer> counted = new LinkedHashSet<>();

    private QuestProgress(String id, QuestKind kind, int floor, int target, boolean noMagic, int timeLeft) {
        this.id = id;
        this.kind = kind;
        this.floor = floor;
        this.target = Math.max(1, target);
        this.noMagic = noMagic;
        this.timeLeft = timeLeft;
    }

    /** A fresh ACTIVE quest of this template on this floor with this target. A timed quest's target is its length in seconds. */
    public static QuestProgress start(QuestTemplate t, int floor, int target) {
        int t1 = Math.max(1, target);
        return new QuestProgress(t.id(), t.kind(), floor, t1, t.noMagic(), t.kind().timed() ? (int) Math.min(Integer.MAX_VALUE / 2, t1 * 20L) : -1);
    }

    // ---------------------------------------------------------------- reading
    public String id() { return id; }

    public QuestKind kind() { return kind; }

    public int floor() { return floor; }

    public int target() { return target; }

    public int progress() { return progress; }

    public State state() { return state; }

    public boolean isActive() { return state == State.ACTIVE; }

    public boolean noMagic() { return noMagic; }

    /** Ticks left on the clock of a timed quest, -1 when the quest has no clock. */
    public int timeLeft() { return timeLeft; }

    /** The ids of the rooms already counted (a clearing quest). */
    public Set<Integer> counted() { return Collections.unmodifiableSet(counted); }

    // ---------------------------------------------------------------- feeding
    /** Adds to the count of a counting kind; completes at the target. Nothing for a finished quest or a "reach the next floor" kind. */
    public Change addProgress(int n) {
        if (state != State.ACTIVE || kind.completesOnArrival() || n <= 0) return Change.NONE;
        int before = progress;
        progress = (int) Math.min(target, (long) progress + n);
        if (progress >= target) {
            state = State.DONE;
            return Change.COMPLETED;
        }
        return progress != before ? Change.PROGRESSED : Change.NONE;
    }

    /** A cleared room: counts once per room id, only for the clearing kind. */
    public Change countRoom(int roomId) {
        if (kind != QuestKind.CLEAR_ROOMS || state != State.ACTIVE || !counted.add(roomId)) return Change.NONE;
        return addProgress(1);
    }

    /** A hostile creature fell on the floor to the player: counts for the kill kind only. */
    public Change onKill() { return kind == QuestKind.KILL_N ? addProgress(1) : Change.NONE; }

    /** A creature of the Dungeon Master's ambush fell: counts for the ambush kind only. */
    public Change onAmbushKill() { return kind == QuestKind.SURVIVE_AMBUSH ? addProgress(1) : Change.NONE; }

    /** The player took magic damage: fails an ACTIVE quest that says "without taking magic damage". */
    public Change onMagicDamage() {
        if (state != State.ACTIVE || !noMagic) return Change.NONE;
        state = State.FAILED;
        return Change.FAILED;
    }

    /** The player took damage of any kind: fails an ACTIVE "reach the next floor without losing health" quest. */
    public Change onDamage() {
        if (state != State.ACTIVE || kind != QuestKind.NO_DAMAGE_STRETCH) return Change.NONE;
        state = State.FAILED;
        return Change.FAILED;
    }

    /** Time passes on the quest's floor: a timed quest runs out of clock and fails. Everything else ignores it. */
    public Change tickTime(int ticks) {
        if (state != State.ACTIVE || timeLeft < 0 || ticks <= 0) return Change.NONE;
        timeLeft = Math.max(0, timeLeft - ticks);
        if (timeLeft > 0) return Change.NONE;
        state = State.FAILED;
        return Change.FAILED;
    }

    /** Is {@code newFloor} the NEXT floor from a quest on {@code questFloor}: deeper along the same road? (Going back down, or onto the other road, is not.) */
    public static boolean isNextFloor(int questFloor, int newFloor) {
        return questFloor != 0 && newFloor != 0 && (questFloor > 0) == (newFloor > 0) && Math.abs(newFloor) > Math.abs(questFloor);
    }

    /**
     * The player entered the next floor. A counting quest still ACTIVE fails (it was not done in time); a "reach the next floor" quest still ACTIVE succeeds
     * (the condition held all the way). A finished quest is not touched.
     */
    public Change onNextFloor() {
        if (state != State.ACTIVE) return Change.NONE;
        if (kind.completesOnArrival()) {
            progress = target;
            state = State.DONE;
            return Change.COMPLETED;
        }
        state = State.FAILED;
        return Change.FAILED;
    }

    /** An admin (or a test) completes an ACTIVE quest. */
    public Change forceComplete() {
        if (state != State.ACTIVE) return Change.NONE;
        progress = target;
        state = State.DONE;
        return Change.COMPLETED;
    }

    /** An admin (or a test) fails an ACTIVE quest. */
    public Change forceFail() {
        if (state != State.ACTIVE) return Change.NONE;
        state = State.FAILED;
        return Change.FAILED;
    }

    // ---------------------------------------------------------------- saving
    public Data toData() {
        return new Data(id, kind.name(), floor, target, progress, state.name(), noMagic, timeLeft, new ArrayList<>(counted));
    }

    /** Restores a quest from its saved form; null when the saved kind or id is unusable (an old save, a removed kind). Values are clamped, so a damaged save cannot break the machine. */
    public static QuestProgress of(Data d) {
        if (d == null || d.id() == null || d.id().isEmpty()) return null;
        QuestKind k = QuestKind.fromId(d.kind());
        if (k == null) return null;
        QuestProgress q = new QuestProgress(d.id(), k, d.floor(), d.target(), d.noMagic(), k.timed() ? Math.max(0, d.timeLeft()) : -1);
        q.progress = Math.max(0, Math.min(q.target, d.progress()));
        q.state = stateOf(d.state());
        if (d.counted() != null) for (Integer r : d.counted()) if (r != null && q.counted.size() < 4096) q.counted.add(r);
        return q;
    }

    private static State stateOf(String s) {
        if (s == null) return State.FAILED;
        String t = s.trim().toUpperCase(Locale.ROOT);
        for (State st : State.values()) if (st.name().equals(t)) return st;
        return State.FAILED;
    }
}
