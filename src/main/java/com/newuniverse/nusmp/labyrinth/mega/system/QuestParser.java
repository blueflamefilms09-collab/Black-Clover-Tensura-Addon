package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: {@code quests.json} is written by hand (by the owner or a datapack author), and a typo in data fails silently in the game: a quest with an unknown kind
 * is simply never rolled. This parser turns the generic tree the reload listener builds from Gson (maps, lists, doubles, booleans, strings) into
 * {@link QuestTemplate}s, checks every field, REPORTS each problem as one line with the quest it belongs to and skips only the broken row, so one bad row never
 * costs the other quests. It takes plain Java objects instead of Gson types on purpose: it stays library-free, so the unit test can feed it the real shipped
 * file through a tiny JSON reader and a typo fails the build instead of the game. Pure.
 */
public final class QuestParser {
    private QuestParser() {}

    /** Upper bound on the rows of one file (a runaway datapack cannot make the roller scan thousands). */
    public static final int MAX_QUESTS = 200;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,48}");

    /** The templates that parsed and one line per problem found. */
    public record Result(List<QuestTemplate> templates, List<String> problems) {}

    @SuppressWarnings("unchecked")
    public static Result parse(Map<String, Object> root) {
        List<String> problems = new ArrayList<>();
        List<QuestTemplate> out = new ArrayList<>();
        if (root == null) {
            problems.add("the file is not a JSON object");
            return new Result(out, problems);
        }
        if (!(root.get("quests") instanceof List<?> rows)) {
            problems.add("no \"quests\" list");
            return new Result(out, problems);
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            if (i >= MAX_QUESTS) {
                problems.add("more than " + MAX_QUESTS + " quests: the rest are ignored");
                break;
            }
            if (!(rows.get(i) instanceof Map<?, ?> row)) {
                problems.add("quest #" + i + " is not an object");
                continue;
            }
            QuestTemplate t = one((Map<String, Object>) row, i, problems);
            if (t == null) continue;
            if (!seen.add(t.id())) {
                problems.add("quest '" + t.id() + "' appears twice in one file: the later row wins");
                out.removeIf(x -> x.id().equals(t.id()));
            }
            out.add(t);
        }
        return new Result(out, problems);
    }

    private static QuestTemplate one(Map<String, Object> m, int index, List<String> problems) {
        String id = str(m.get("id"));
        String where = "quest #" + index + (id == null ? "" : " '" + id + "'");
        if (id == null || !ID.matcher(id).matches()) {
            problems.add(where + ": the id must be 1 to 48 characters of a-z, 0-9 and _");
            return null;
        }
        QuestKind kind = QuestKind.fromId(str(m.get("kind")));
        if (kind == null) {
            problems.add(where + ": unknown kind '" + m.get("kind") + "' (one of CLEAR_ROOMS KILL_N NO_DAMAGE_STRETCH SPEED SURVIVE_AMBUSH)");
            return null;
        }
        double base = number(m.get("base"), Double.NaN);
        if (Double.isNaN(base) || base < 0 || base > 100_000) {
            problems.add(where + ": \"base\" must be a number from 0 to 100000");
            return null;
        }
        double perFloor = number(m.get("perFloor"), 0.0);
        if (Double.isNaN(perFloor) || Math.abs(perFloor) > 1000) {
            problems.add(where + ": \"perFloor\" must be a number between -1000 and 1000");
            return null;
        }
        int min = (int) Math.round(number(m.get("min"), 1.0));
        int max = (int) Math.round(number(m.get("max"), 99.0));
        if (min < 1) {
            problems.add(where + ": \"min\" is raised to 1");
            min = 1;
        }
        if (max < min) {
            problems.add(where + ": \"max\" is below \"min\": raised to " + min);
            max = min;
        }
        int weight = (int) Math.round(number(m.get("weight"), 10.0));
        if (weight < 0 || weight > 1000) {
            problems.add(where + ": \"weight\" must be 0 to 1000 (0 switches the quest off)");
            weight = Math.max(0, Math.min(1000, weight));
        }
        boolean noMagic = Boolean.TRUE.equals(m.get("noMagic"));
        if (noMagic && kind != QuestKind.CLEAR_ROOMS && kind != QuestKind.KILL_N && kind != QuestKind.SURVIVE_AMBUSH) {
            problems.add(where + ": \"noMagic\" only means something for a counting kind: ignored");
            noMagic = false;
        }
        List<Stat> stats = new ArrayList<>();
        int points = 1;
        boolean core = true;
        if (m.get("rewards") instanceof Map<?, ?> rw) {
            if (rw.get("stats") instanceof List<?> l) {
                for (Object o : l) {
                    Stat s = Stat.fromId(str(o));
                    if (s == null) problems.add(where + ": unknown stat '" + o + "' in \"rewards.stats\" (one of STR AGI VIT INT SENSE): skipped");
                    else if (!stats.contains(s)) stats.add(s);
                }
            }
            points = (int) Math.round(number(rw.get("points"), 1.0));
            if (points < 1 || points > 10) {
                problems.add(where + ": \"rewards.points\" must be 1 to 10");
                points = Math.max(1, Math.min(10, points));
            }
            if (rw.get("shadowCore") != null) core = !Boolean.FALSE.equals(rw.get("shadowCore"));
        }
        String title = str(m.get("title"));
        String why = str(m.get("why"));
        return new QuestTemplate(id, kind, title, base, perFloor, min, max, weight, noMagic, stats, points, core, why);
    }

    private static String str(Object o) { return o instanceof String s && !s.isBlank() ? s.trim() : null; }

    private static double number(Object o, double def) {
        if (o instanceof Number n) {
            double d = n.doubleValue();
            return Double.isFinite(d) ? d : Double.NaN;
        }
        return def;
    }
}
