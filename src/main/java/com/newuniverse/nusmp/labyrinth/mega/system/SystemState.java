package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: this is everything the System remembers about ONE player, in one object that is the data attachment {@code nusmp:system_state}: the quest they are on, the
 * permanent stat points they have earned (the "permanent stat boost capability" of the brief), how many Shadow Cores they were handed (statistics), the deepest
 * floor they have reached (so that coming back to an old floor never starts a quest again) and the kind of their last quest (so that two quests in a row are
 * never the same kind). It lives on the player, not in a world table, because it must follow the player through death (the attachment is copied on death),
 * dimensions and servers' restarts for free. The object itself is pure and mutable (it changes a few times a minute); the codec that saves it is in
 * {@link SystemAttachments}, built on {@link Data}, which tolerates every missing or damaged field, so an old world or a hand-edited file still loads.
 */
public final class SystemState {
    /** The most points one stat may hold in a save, whatever the config says (a damaged save cannot hold more). */
    public static final int HARD_CAP = 1000;

    private final int[] points = new int[Stat.COUNT];
    private int cores;
    private int maxFloor;
    private String lastKind = "";
    private int done;
    private int failed;
    private QuestProgress quest;
    private boolean hintShown;

    /**
     * The saved form: plain values only. {@code hint} (1.58.0) says the player has been told once how to move the window; the seven-argument constructor is the
     * form before it existed, so every older caller and every older save reads as "not told yet".
     */
    public record Data(List<Integer> points, int cores, int maxFloor, String lastKind, int done, int failed, QuestProgress.Data quest, boolean hint) {
        public Data(List<Integer> points, int cores, int maxFloor, String lastKind, int done, int failed, QuestProgress.Data quest) {
            this(points, cores, maxFloor, lastKind, done, failed, quest, false);
        }
    }

    // ---------------------------------------------------------------- stats
    public int points(Stat s) { return points[s.ordinal()]; }

    public int[] pointsCopy() { return points.clone(); }

    public int totalPoints() {
        int t = 0;
        for (int p : points) t += p;
        return t;
    }

    /** Sets a stat's points (clamped to 0..{@link #HARD_CAP}). */
    public void setPoints(Stat s, int value) { points[s.ordinal()] = Math.max(0, Math.min(HARD_CAP, value)); }

    /**
     * Adds points to a stat, never above {@code cap} (nor {@link #HARD_CAP}) and never below 0; a negative number takes points away. Returns the change made.
     */
    public int addPoints(Stat s, int n, int cap) {
        int before = points[s.ordinal()];
        int limit = Math.max(0, Math.min(HARD_CAP, cap));
        long sum = (long) before + n;
        // adding never lowers a value (a cap that was lowered since leaves points already earned alone) and never lifts it above the cap; taking away stops at 0
        int after = n >= 0 ? Math.max(before, (int) Math.min(sum, limit)) : (int) Math.max(0L, sum);
        points[s.ordinal()] = after;
        return after - before;
    }

    // ---------------------------------------------------------------- counters
    /** Shadow Cores handed out by quest rewards (statistics only). */
    public int cores() { return cores; }

    public void addCore() { cores++; }

    /** The deepest floor depth ({@code |floor|}) the player has reached on the road the System covers. */
    public int maxFloor() { return maxFloor; }

    public void setMaxFloor(int depth) { maxFloor = Math.max(0, depth); }

    /** The kind (by name) of the last quest rolled, or "". */
    public String lastKind() { return lastKind; }

    public QuestKind lastKindOrNull() { return QuestKind.fromId(lastKind); }

    public void setLastKind(QuestKind k) { lastKind = k == null ? "" : k.name(); }

    public int done() { return done; }

    public int failed() { return failed; }

    public void countDone() { done++; }

    public void countFailed() { failed++; }

    // ---------------------------------------------------------------- the quest
    /** The current quest (ACTIVE, DONE or FAILED), or null when the player has none. */
    public QuestProgress quest() { return quest; }

    public void setQuest(QuestProgress q) { quest = q; }

    // ---------------------------------------------------------------- the one-time window hint
    /** Has the player been told how to move the System window ({@code system.hudHintOnEntry})? */
    public boolean hintShown() { return hintShown; }

    public void setHintShown(boolean shown) { hintShown = shown; }

    // ---------------------------------------------------------------- saving
    /** Does this state hold anything worth saving? (An empty one is not written, so players who never entered the Mega-Dungeon add nothing to their file.) */
    public boolean hasContent() {
        return totalPoints() > 0 || cores > 0 || maxFloor > 0 || done > 0 || failed > 0 || quest != null || hintShown;
    }

    public Data toData() {
        List<Integer> list = new ArrayList<>(Stat.COUNT);
        for (int p : points) list.add(p);
        return new Data(list, cores, maxFloor, lastKind, done, failed, quest == null ? null : quest.toData(), hintShown);
    }

    /** Restores from the saved form; a null, short, long or damaged record gives a valid state. */
    public static SystemState of(Data d) {
        SystemState s = new SystemState();
        if (d == null) return s;
        if (d.points() != null) {
            for (int i = 0; i < Stat.COUNT && i < d.points().size(); i++) {
                Integer v = d.points().get(i);
                s.points[i] = v == null ? 0 : Math.max(0, Math.min(HARD_CAP, v));
            }
        }
        s.cores = Math.max(0, d.cores());
        s.maxFloor = Math.max(0, d.maxFloor());
        s.lastKind = d.lastKind() == null ? "" : d.lastKind();
        s.done = Math.max(0, d.done());
        s.failed = Math.max(0, d.failed());
        s.quest = QuestProgress.of(d.quest());
        s.hintShown = d.hint();
        return s;
    }
}
