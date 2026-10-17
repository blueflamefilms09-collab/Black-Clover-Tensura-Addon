package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Why: {@code stats.json} is hand-written like the quests, with the same hazard: a misspelt operation or an attribute id with a capital letter is just a stat
 * that silently does nothing. This turns the generic tree of the reload listener into {@link StatTable.Def}s, validates every field, reports each problem with
 * the stat it belongs to and drops only the broken effect or stat. It does NOT check that an attribute exists: that depends on which mods are loaded, and the
 * game side skips an unknown attribute at run time on purpose. Library-free and pure, so the unit test reads the real shipped file with it.
 */
public final class StatParser {
    private StatParser() {}

    private static final Pattern ATTRIBUTE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    /** The most effects one stat may have. */
    public static final int MAX_EFFECTS = 6;

    /** The rows that parsed and one line per problem found. */
    public record Result(List<StatTable.Def> defs, List<String> problems) {}

    public static Result parse(Map<String, Object> root) {
        List<String> problems = new ArrayList<>();
        List<StatTable.Def> out = new ArrayList<>();
        if (root == null) {
            problems.add("the file is not a JSON object");
            return new Result(out, problems);
        }
        if (!(root.get("stats") instanceof Map<?, ?> stats)) {
            problems.add("no \"stats\" object");
            return new Result(out, problems);
        }
        for (Map.Entry<?, ?> e : stats.entrySet()) {
            Stat stat = Stat.fromId(String.valueOf(e.getKey()));
            if (stat == null) {
                problems.add("unknown stat '" + e.getKey() + "' (one of STR AGI VIT INT SENSE): skipped");
                continue;
            }
            if (!(e.getValue() instanceof Map<?, ?> row)) {
                problems.add(stat + ": not an object");
                continue;
            }
            String name = row.get("name") instanceof String s && !s.isBlank() ? s.trim() : stat.name();
            List<StatTable.Effect> effects = new ArrayList<>();
            if (!(row.get("effects") instanceof List<?> list) || list.isEmpty()) {
                problems.add(stat + ": no \"effects\" list: the stat would do nothing");
            } else {
                for (int i = 0; i < list.size() && i < MAX_EFFECTS; i++) {
                    StatTable.Effect eff = effect(stat, i, list.get(i), problems);
                    if (eff != null) effects.add(eff);
                }
                if (list.size() > MAX_EFFECTS) problems.add(stat + ": more than " + MAX_EFFECTS + " effects: the rest are ignored");
            }
            out.add(new StatTable.Def(stat, name, effects));
        }
        return new Result(out, problems);
    }

    private static StatTable.Effect effect(Stat stat, int index, Object o, List<String> problems) {
        String where = stat + " effect #" + index;
        if (!(o instanceof Map<?, ?> m)) {
            problems.add(where + ": not an object");
            return null;
        }
        String attribute = m.get("attribute") instanceof String s ? s.trim() : null;
        if (attribute == null || !ATTRIBUTE.matcher(attribute).matches()) {
            problems.add(where + ": \"attribute\" must be a registry id such as minecraft:generic.max_health");
            return null;
        }
        StatTable.Op op = StatTable.Op.fromId(m.get("operation") instanceof String s ? s : null);
        if (op == null) {
            problems.add(where + ": \"operation\" must be add_value, add_multiplied_base or add_multiplied_total");
            return null;
        }
        double per = m.get("perPoint") instanceof Number n ? n.doubleValue() : Double.NaN;
        if (!Double.isFinite(per) || per == 0.0 || Math.abs(per) > 1000.0) {
            problems.add(where + ": \"perPoint\" must be a non-zero number between -1000 and 1000");
            return null;
        }
        double cap = m.get("cap") instanceof Number n ? n.doubleValue() : 0.0;
        if (!Double.isFinite(cap) || cap < 0.0) {
            problems.add(where + ": \"cap\" must be a number of 0 or more (0 = no ceiling)");
            return null;
        }
        return new StatTable.Effect(attribute, op, per, cap);
    }
}
