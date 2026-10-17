package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.Curve;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.GradeTable;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.IntRange;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.Pool;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a data file is written by people (the owner, a pack author) and a wrong letter must be REPORTED, not swallowed and not thrown into a death
 * event. This turns the parsed JSON tree (plain maps, lists, strings, doubles and booleans; the game converts Gson's tree into exactly that shape in
 * one place, the tests read the real file with a tiny reader) into the immutable {@link GearTables}: it clamps every number into a sane range, fixes a
 * reversed range, sorts the curves, drops an id that is not shaped like a resource location, and returns a list of plain-English problems for
 * everything it had to change or ignore (an unknown key, a slot or grade it does not know, an item list that is empty). It never throws: a root that is
 * not an object gives {@link GearTables#EMPTY} and one problem. Pure (no Minecraft types), so the shipped file is parsed by a unit test and a mistake in
 * it fails the build instead of silently turning a grade off in the game.
 */
public final class GearTablesParser {
    private GearTablesParser() {}

    /** The parse result: the tables (never null) and every complaint, each one sentence. */
    public record Result(GearTables tables, List<String> problems) {
        public Result {
            problems = List.copyOf(problems);
        }
    }

    /** The shape of a resource location: lower-case namespace, a colon, a lower-case path (the same alphabet the game accepts). */
    static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> TOP = Set.of("why", "curve", "caps", "slotWeights", "grades");
    private static final Set<String> GRADE = Set.of("why", "items", "power", "durability", "enchants", "engravings", "resist");

    @SuppressWarnings("unchecked")
    public static Result parse(Object root) {
        List<String> problems = new ArrayList<>();
        if (!(root instanceof Map)) {
            problems.add("the tables file is not a JSON object");
            return new Result(GearTables.EMPTY, problems);
        }
        Map<String, Object> m = (Map<String, Object>) root;
        for (String k : m.keySet()) if (!TOP.contains(k)) problems.add("unknown key '" + k + "' at the top of the file");

        Map<GearGrade, Curve> curves = new EnumMap<>(GearGrade.class);
        Object c = m.get("curve");
        if (c instanceof Map<?, ?> cm) {
            for (Map.Entry<?, ?> e : cm.entrySet()) {
                if (note(e.getKey())) continue;
                GearGrade g = GearGrade.fromId(String.valueOf(e.getKey()));
                if (g == null) { problems.add("curve: unknown grade '" + e.getKey() + "'"); continue; }
                Curve cv = curve(e.getValue(), "curve." + g.id(), problems);
                if (cv != null) curves.put(g, cv);
            }
        } else if (c != null) problems.add("curve is not an object");

        Map<GearGrade, Double> caps = new EnumMap<>(GearGrade.class);
        double total = 0.5;
        Object cp = m.get("caps");
        if (cp instanceof Map<?, ?> cpm) {
            for (Map.Entry<?, ?> e : cpm.entrySet()) {
                if (note(e.getKey())) continue;
                String key = String.valueOf(e.getKey());
                Double v = number(e.getValue());
                if (v == null) { problems.add("caps." + key + " is not a number"); continue; }
                double clamped = clamp(v, 0.0, 1.0);
                if (clamped != v) problems.add("caps." + key + " " + v + " was clamped to " + clamped);
                if (key.equals("total")) { total = clamped; continue; }
                GearGrade g = GearGrade.fromId(key);
                if (g == null) problems.add("caps: unknown grade '" + key + "'");
                else caps.put(g, clamped);
            }
        } else if (cp != null) problems.add("caps is not an object");

        Map<GearSlot, Double> weights = new EnumMap<>(GearSlot.class);
        Object sw = m.get("slotWeights");
        if (sw instanceof Map<?, ?> swm) {
            for (Map.Entry<?, ?> e : swm.entrySet()) {
                if (note(e.getKey())) continue;
                GearSlot s = GearSlot.fromId(String.valueOf(e.getKey()));
                Double v = number(e.getValue());
                if (s == null) { problems.add("slotWeights: unknown slot '" + e.getKey() + "'"); continue; }
                if (v == null) { problems.add("slotWeights." + s.id() + " is not a number"); continue; }
                weights.put(s, clamp(v, 0.0, 1000.0));
            }
        } else if (sw != null) problems.add("slotWeights is not an object");
        else for (GearSlot s : GearSlot.values()) weights.put(s, 1.0);

        Map<GearGrade, GradeTable> grades = new EnumMap<>(GearGrade.class);
        Object gs = m.get("grades");
        if (gs instanceof Map<?, ?> gm) {
            for (Map.Entry<?, ?> e : gm.entrySet()) {
                if (note(e.getKey())) continue;
                GearGrade g = GearGrade.fromId(String.valueOf(e.getKey()));
                if (g == null) { problems.add("grades: unknown grade '" + e.getKey() + "'"); continue; }
                GradeTable t = grade(e.getValue(), g, problems);
                if (t != null) grades.put(g, t);
            }
        } else problems.add("grades is missing or not an object");
        for (GearGrade g : GearGrade.values()) {
            GradeTable t = grades.get(g);
            if (t == null) problems.add("grade " + g.id() + " has no table: it will never drop");
            else {
                for (GearSlot s : GearSlot.values()) if (!t.offers(s)) problems.add("grade " + g.id() + " has no base item for " + s.id() + ": that slot is never rolled for it");
            }
        }
        GearTables tables = new GearTables(grades, curves, caps, total, weights);
        if (!tables.isUsable()) problems.add("no grade offers any item: nothing will drop");
        return new Result(tables, problems);
    }

    // ---------------------------------------------------------------- one grade
    @SuppressWarnings("unchecked")
    private static GradeTable grade(Object o, GearGrade g, List<String> problems) {
        String at = "grades." + g.id();
        if (!(o instanceof Map)) { problems.add(at + " is not an object"); return null; }
        Map<String, Object> m = (Map<String, Object>) o;
        for (String k : m.keySet()) if (!GRADE.contains(k)) problems.add(at + ": unknown key '" + k + "'");

        Map<GearSlot, List<String>> items = new EnumMap<>(GearSlot.class);
        Object it = m.get("items");
        if (it instanceof Map<?, ?> im) {
            for (Map.Entry<?, ?> e : im.entrySet()) {
                if (note(e.getKey())) continue;
                GearSlot s = GearSlot.fromId(String.valueOf(e.getKey()));
                if (s == null) { problems.add(at + ".items: unknown slot '" + e.getKey() + "'"); continue; }
                List<String> ids = new ArrayList<>();
                if (e.getValue() instanceof List<?> l) {
                    for (Object x : l) {
                        if (x instanceof String id && ID.matcher(id).matches()) ids.add(id);
                        else problems.add(at + ".items." + s.id() + ": '" + x + "' is not an item id (namespace:path)");
                    }
                } else problems.add(at + ".items." + s.id() + " is not a list");
                if (!ids.isEmpty()) items.put(s, List.copyOf(ids));
            }
        } else problems.add(at + ".items is missing or not an object");

        IntRange power = range(m.get("power"), at + ".power", 0, 200, problems);
        IntRange durability = range(m.get("durability"), at + ".durability", 0, 200, problems);

        IntRange enchantCount = IntRange.ZERO;
        int levelCap = 5;
        Map<GearSlot, List<Pool>> enchantPool = new EnumMap<>(GearSlot.class);
        Object en = m.get("enchants");
        if (en instanceof Map<?, ?> enm) {
            for (Object k : enm.keySet()) if (!Set.of("why", "count", "levelCap", "pool").contains(String.valueOf(k))) problems.add(at + ".enchants: unknown key '" + k + "'");
            enchantCount = range(enm.get("count"), at + ".enchants.count", 0, 6, problems);
            Double lc = number(enm.get("levelCap"));
            if (lc != null) levelCap = (int) clamp(Math.round(lc), 1, 10);
            enchantPool = pools(enm.get("pool"), at + ".enchants.pool", problems);
        } else if (en != null) problems.add(at + ".enchants is not an object");

        IntRange engraveCount = IntRange.ZERO;
        Map<GearSlot, List<Pool>> engravePool = new EnumMap<>(GearSlot.class);
        Object eg = m.get("engravings");
        if (eg instanceof Map<?, ?> egm) {
            for (Object k : egm.keySet()) if (!Set.of("why", "count", "pool").contains(String.valueOf(k))) problems.add(at + ".engravings: unknown key '" + k + "'");
            engraveCount = range(egm.get("count"), at + ".engravings.count", 0, 4, problems);
            engravePool = pools(egm.get("pool"), at + ".engravings.pool", problems);
        } else if (eg != null) problems.add(at + ".engravings is not an object");

        IntRange resistArmour = IntRange.ZERO, resistHand = IntRange.ZERO;
        Object rs = m.get("resist");
        if (rs instanceof Map<?, ?> rm) {
            for (Object k : rm.keySet()) if (!Set.of("why", "armour", "hand").contains(String.valueOf(k))) problems.add(at + ".resist: unknown key '" + k + "'");
            resistArmour = range(rm.get("armour"), at + ".resist.armour", 0, 100, problems);
            resistHand = range(rm.get("hand"), at + ".resist.hand", 0, 100, problems);
        } else if (rs != null) problems.add(at + ".resist is not an object");

        return new GradeTable(items, power, durability, enchantCount, levelCap, enchantPool, engraveCount, engravePool, resistArmour, resistHand);
    }

    private static Map<GearSlot, List<Pool>> pools(Object o, String at, List<String> problems) {
        Map<GearSlot, List<Pool>> out = new EnumMap<>(GearSlot.class);
        if (o == null) return out;
        if (!(o instanceof Map<?, ?> pm)) { problems.add(at + " is not an object"); return out; }
        for (Map.Entry<?, ?> e : pm.entrySet()) {
            if (note(e.getKey())) continue;
            GearSlot s = GearSlot.fromId(String.valueOf(e.getKey()));
            if (s == null) { problems.add(at + ": unknown slot '" + e.getKey() + "'"); continue; }
            if (!(e.getValue() instanceof List<?> l)) { problems.add(at + "." + s.id() + " is not a list"); continue; }
            List<Pool> pool = new ArrayList<>();
            for (Object x : l) {
                if (!(x instanceof Map<?, ?> xm)) { problems.add(at + "." + s.id() + ": an entry is not an object"); continue; }
                Object id = xm.get("id");
                if (!(id instanceof String sid) || !ID.matcher(sid).matches()) { problems.add(at + "." + s.id() + ": '" + id + "' is not an id (namespace:path)"); continue; }
                Double lo = number(xm.get("min")), hi = number(xm.get("max"));
                int min = lo == null ? 1 : (int) clamp(Math.round(lo), 1, 10);
                int max = hi == null ? min : (int) clamp(Math.round(hi), 1, 10);
                pool.add(new Pool(sid, min, max));
            }
            if (!pool.isEmpty()) out.put(s, List.copyOf(pool));
        }
        return out;
    }

    // ---------------------------------------------------------------- curves and numbers
    private static Curve curve(Object o, String at, List<String> problems) {
        if (!(o instanceof List<?> l) || l.isEmpty()) { problems.add(at + " is not a list of [x, multiplier] points"); return null; }
        List<double[]> pts = new ArrayList<>();
        for (Object p : l) {
            if (!(p instanceof List<?> pair) || pair.size() != 2 || number(pair.get(0)) == null || number(pair.get(1)) == null) {
                problems.add(at + ": a point is not [x, multiplier]");
                continue;
            }
            double x = number(pair.get(0)), y = number(pair.get(1));
            double cx = clamp(x, 0.0, 1.0), cy = clamp(y, 0.0, 100.0);
            if (cx != x || cy != y) problems.add(at + ": point [" + x + ", " + y + "] was clamped to [" + cx + ", " + cy + "]");
            pts.add(new double[] {cx, cy});
        }
        if (pts.isEmpty()) return null;
        pts.sort((a, b) -> Double.compare(a[0], b[0]));
        double[] xs = new double[pts.size()], ys = new double[pts.size()];
        for (int i = 0; i < xs.length; i++) { xs[i] = pts.get(i)[0]; ys[i] = pts.get(i)[1]; }
        return new Curve(xs, ys);
    }

    /** A [min, max] pair, or a bare number meaning [n, n]; clamped into lo..hi; a missing value is [0, 0]; a bad one is reported and [0, 0]. */
    private static IntRange range(Object o, String at, int lo, int hi, List<String> problems) {
        if (o == null) return IntRange.ZERO;
        Double a, b;
        if (o instanceof List<?> l && l.size() == 2 && number(l.get(0)) != null && number(l.get(1)) != null) {
            a = number(l.get(0));
            b = number(l.get(1));
        } else if (number(o) != null) {
            a = b = number(o);
        } else {
            problems.add(at + " is not [min, max] or a number");
            return IntRange.ZERO;
        }
        int x = (int) clamp(Math.round(a), lo, hi), y = (int) clamp(Math.round(b), lo, hi);
        if (x != Math.round(a) || y != Math.round(b)) problems.add(at + " was clamped into " + lo + ".." + hi);
        return new IntRange(x, y);
    }

    private static Double number(Object o) {
        if (o instanceof Number n && !Double.isNaN(n.doubleValue()) && !Double.isInfinite(n.doubleValue())) return n.doubleValue();
        return null;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    /** A {@code why} key is the file's own documentation (JSON has no comments) and is skipped wherever a map of ids or numbers is read. */
    private static boolean note(Object key) { return "why".equals(key); }

    /** A readable dump of the parsed tables' headline numbers, for the log line after a reload. */
    public static String summary(GearTables t) {
        List<String> parts = new ArrayList<>();
        for (GearGrade g : GearGrade.values()) {
            GradeTable gt = t.grade(g);
            int items = 0;
            if (gt != null) for (GearSlot s : GearSlot.values()) items += gt.items(s).size();
            parts.add(g.id() + "=" + items + " items");
        }
        return parts + ", total cap " + t.totalCap();
    }
}
