package com.newuniverse.nusmp.elite.records.rule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a title is earned when its requirements hold, and the owner's inventory says a requirement can be "record ids / EP / other conditions". This is that: one requirement is exactly one of
 * <ul>
 *   <li>{@code {"record":"id"}} the record is unlocked;</li>
 *   <li>{@code {"ep":N}} the player's EP reached N (the same as an ep_reached condition);</li>
 *   <li>{@code {"condition":{"type":...}}} any of the twenty condition types, evaluated live;</li>
 *   <li>{@code {"title":"id"}} another title is held;</li>
 *   <li>{@code {"records":{"count":N,"category":"war","rarity":"rare"}}} at least N unlocked records (of a category, of at least a rarity);</li>
 *   <li>{@code {"points":N}} the record points (see {@link Rarity#points}) reached N;</li>
 *   <li>{@code {"any":[ ... ]}} at least one of the listed requirements.</li>
 * </ul>
 * Pure: it is evaluated against a {@link Ctx} the server (or a test) supplies.
 */
public final class Requirement {
    public enum Kind { RECORD, EP, CONDITION, TITLE, RECORDS, POINTS, ANY }

    /** What a requirement may ask about the player. */
    public interface Ctx {
        Facts facts();

        boolean recordUnlocked(String id);

        boolean titleHeld(String id);

        /** Unlocked records in {@code category} (null = all) of at least {@code minRarity} (null = any). */
        int recordCount(Category category, Rarity minRarity);

        int points();
    }

    private final Kind kind;
    private final String id;
    private final double number;
    private final Condition condition;
    private final Category category;
    private final Rarity rarity;
    private final List<Requirement> any;

    private Requirement(Kind kind, String id, double number, Condition condition, Category category, Rarity rarity, List<Requirement> any) {
        this.kind = kind;
        this.id = id;
        this.number = number;
        this.condition = condition;
        this.category = category;
        this.rarity = rarity;
        this.any = any;
    }

    public static Requirement record(String id) { return new Requirement(Kind.RECORD, id, 0, null, null, null, List.of()); }

    public static Requirement ep(double ep) { return new Requirement(Kind.EP, "", ep, null, null, null, List.of()); }

    public static Requirement condition(Condition c) { return new Requirement(Kind.CONDITION, "", 0, c, null, null, List.of()); }

    public static Requirement title(String id) { return new Requirement(Kind.TITLE, id, 0, null, null, null, List.of()); }

    public static Requirement records(int count, Category cat, Rarity min) { return new Requirement(Kind.RECORDS, "", count, null, cat, min, List.of()); }

    public static Requirement points(int n) { return new Requirement(Kind.POINTS, "", n, null, null, null, List.of()); }

    public static Requirement any(List<Requirement> of) { return new Requirement(Kind.ANY, "", 0, null, null, null, List.copyOf(of)); }

    public Kind kind() { return kind; }

    public String id() { return id; }

    public double number() { return number; }

    public Condition condition() { return condition; }

    public Category category() { return category; }

    public Rarity rarity() { return rarity; }

    public List<Requirement> anyOf() { return any; }

    // ------------------------------------------------------------------ evaluation
    public Eval eval(Ctx c) {
        switch (kind) {
            case RECORD:
                return Eval.atLeast(c.recordUnlocked(id) ? 1 : 0, 1);
            case EP:
                return ConditionEvaluator.evaluate(new Condition(CondType.EP_REACHED, num("ep", number)), c.facts());
            case CONDITION:
                return ConditionEvaluator.evaluate(condition, c.facts());
            case TITLE:
                return Eval.atLeast(c.titleHeld(id) ? 1 : 0, 1);
            case RECORDS:
                return Eval.atLeast(c.recordCount(category, rarity), number);
            case POINTS:
                return Eval.atLeast(c.points(), number);
            case ANY: {
                Eval best = null;
                boolean allUnavailable = !any.isEmpty();
                for (Requirement r : any) {
                    Eval e = r.eval(c);
                    if (e.met()) return e;
                    if (!e.unavailable()) allUnavailable = false;
                    if (best == null || e.fraction() > best.fraction()) best = e;
                }
                if (best == null || allUnavailable) return Eval.UNAVAILABLE;
                return best;
            }
            default:
                return Eval.UNAVAILABLE;
        }
    }

    private static JsonObject num(String key, double v) {
        JsonObject o = new JsonObject();
        o.addProperty(key, v);
        return o;
    }

    /** Every record id this requirement names (for the data validation tests). */
    public void collectRecordIds(List<String> out) {
        if (kind == Kind.RECORD) out.add(id);
        for (Requirement r : any) r.collectRecordIds(out);
    }

    /** Every title id this requirement names. */
    public void collectTitleIds(List<String> out) {
        if (kind == Kind.TITLE) out.add(id);
        for (Requirement r : any) r.collectTitleIds(out);
    }

    // ------------------------------------------------------------------ JSON
    public static Requirement parse(JsonElement e, String where, List<String> problems) {
        if (e == null || !e.isJsonObject()) {
            problems.add(where + ": a requirement must be an object");
            return null;
        }
        JsonObject o = e.getAsJsonObject();
        if (o.has("record")) {
            String id = J.str(o, "record", "");
            if (id.isEmpty()) { problems.add(where + ": 'record' is empty"); return null; }
            return record(id);
        }
        if (o.has("ep")) {
            double v = J.num(o, "ep", 0);
            if (v <= 0) { problems.add(where + ": 'ep' must be above 0"); return null; }
            return ep(v);
        }
        if (o.has("condition")) {
            Condition c = Condition.parse(o.get("condition"), where, problems);
            return c == null ? null : condition(c);
        }
        if (o.has("title")) {
            String id = J.str(o, "title", "");
            if (id.isEmpty()) { problems.add(where + ": 'title' is empty"); return null; }
            return title(id);
        }
        if (o.has("records")) {
            JsonObject r = J.obj(o, "records");
            if (r == null) { problems.add(where + ": 'records' must be an object"); return null; }
            int n = (int) J.num(r, "count", 1);
            Category cat = r.has("category") ? Category.parse(J.str(r, "category", ""), null) : null;
            if (r.has("category") && cat == null) problems.add(where + ": unknown category '" + J.str(r, "category", "") + "'");
            Rarity rar = r.has("rarity") ? Rarity.parse(J.str(r, "rarity", ""), null) : null;
            if (r.has("rarity") && rar == null) problems.add(where + ": unknown rarity '" + J.str(r, "rarity", "") + "'");
            if (n < 1) { problems.add(where + ": records.count must be at least 1"); return null; }
            return records(n, cat, rar);
        }
        if (o.has("points")) {
            int n = (int) J.num(o, "points", 0);
            if (n < 1) { problems.add(where + ": 'points' must be above 0"); return null; }
            return points(n);
        }
        if (o.has("any")) {
            JsonArray a = J.arr(o, "any");
            if (a == null || a.isEmpty()) { problems.add(where + ": 'any' must be a non-empty array"); return null; }
            List<Requirement> list = new ArrayList<>();
            for (JsonElement x : a) {
                Requirement r = parse(x, where + ".any", problems);
                if (r != null) list.add(r);
            }
            return list.isEmpty() ? null : any(list);
        }
        problems.add(where + ": a requirement needs one of record, ep, condition, title, records, points, any");
        return null;
    }
}
