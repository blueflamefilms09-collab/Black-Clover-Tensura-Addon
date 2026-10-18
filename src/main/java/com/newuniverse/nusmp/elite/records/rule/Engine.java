package com.newuniverse.nusmp.elite.records.rule;

import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the unlock loop, pure. Given the catalog, the player's {@link Facts} and the saved {@link RecordData}, it unlocks every record whose condition is met and grants every title whose
 * requirements all hold, repeating (at most five passes) because one unlock can complete another: a record counts toward a title, a title toward another title, a record count toward a "records"
 * requirement. It reports what is new so the caller can toast and sync, and it never takes anything away. {@link #recordEval} / {@link #titleEvals} give the progress the screen draws.
 */
public final class Engine {
    private Engine() {}

    public record Result(List<String> newRecords, List<String> newTitles) {
        public boolean any() { return !newRecords.isEmpty() || !newTitles.isEmpty(); }
    }

    /** The requirement context of one player. */
    public static Requirement.Ctx ctx(Catalog cat, Facts facts, RecordData data) {
        return new Requirement.Ctx() {
            @Override public Facts facts() { return facts; }

            @Override public boolean recordUnlocked(String id) { return data.isUnlocked(id); }

            @Override public boolean titleHeld(String id) { return data.holds(id); }

            @Override public int recordCount(Category category, Rarity minRarity) {
                int n = 0;
                for (String id : data.unlockedIds()) {
                    RecordDef r = cat.record(id);
                    if (r == null) continue;
                    if (category != null && r.category() != category) continue;
                    if (minRarity != null && r.rarity().ordinal() < minRarity.ordinal()) continue;
                    n++;
                }
                return n;
            }

            @Override public int points() {
                int n = 0;
                for (String id : data.unlockedIds()) n += cat.pointsOf(id);
                return n;
            }
        };
    }

    public static Result run(Catalog cat, Facts facts, RecordData data, long nowSec) {
        List<String> newRecords = new ArrayList<>(), newTitles = new ArrayList<>();
        Requirement.Ctx ctx = ctx(cat, facts, data);
        for (int pass = 0; pass < 5; pass++) {
            boolean changed = false;
            for (RecordDef r : cat.records()) {
                if (data.isUnlocked(r.id())) continue;
                if (ConditionEvaluator.evaluate(r.condition(), facts).met() && data.unlock(r.id(), nowSec)) {
                    newRecords.add(r.id());
                    changed = true;
                }
            }
            for (TitleDef t : cat.titles()) {
                if (data.holds(t.id())) continue;
                boolean all = true;
                for (Requirement q : t.requirements()) {
                    if (!q.eval(ctx).met()) { all = false; break; }
                }
                if (all && data.grantTitle(t.id(), nowSec)) {
                    newTitles.add(t.id());
                    changed = true;
                }
            }
            if (!changed) break;
        }
        return new Result(newRecords, newTitles);
    }

    public static Eval recordEval(RecordDef r, Facts facts) { return ConditionEvaluator.evaluate(r.condition(), facts); }

    /** The evaluation of each requirement of a title, in order. */
    public static List<Eval> titleEvals(Catalog cat, TitleDef t, Facts facts, RecordData data) {
        Requirement.Ctx ctx = ctx(cat, facts, data);
        List<Eval> out = new ArrayList<>();
        for (Requirement q : t.requirements()) out.add(q.eval(ctx));
        return out;
    }
}
