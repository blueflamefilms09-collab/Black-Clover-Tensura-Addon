package com.newuniverse.nusmp.skilltree.line;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: the scale tests need hundreds of lines, and the real ones are written by other hands. This builds lines in the SAME SHAPE the generator writes ({@code tools/lines/gen_lines.py}: two stances, four
 * stats, eight arts, eight passives, two finishers, the same columns, parents, per-level tables and cooldown floors) from the catalogue itself, cycling through every kind, so the numbers are legal by
 * construction and the files exercise every executor's inputs. It is a test helper only.
 */
final class LineFixtures {
    private LineFixtures() {}

    private static final Set<String> INT_PARAMS = Set.of("cooldown", "seconds", "seconds2", "targets", "jumps", "hits", "shots", "pierce", "interval", "delay", "rest", "max_stacks", "amp", "amp2",
            "resist_amp", "regen_seconds", "resist_seconds");

    /** Level tables for a kind at {@code levels} levels, the generator's formula: base * (1 + grow * (level - 1)) clamped to the range; a zero base stays zero. */
    static JsonObject values(LineCatalog.Kind kind, int levels, boolean finisher) {
        JsonObject out = new JsonObject();
        for (var e : kind.nums().entrySet()) {
            LineCatalog.Num n = e.getValue();
            double base = finisher && e.getKey().equals("cooldown") ? 1500 : n.base();
            JsonArray a = new JsonArray();
            for (int lv = 1; lv <= levels; lv++) {
                double v = base == 0 ? 0 : base * (1 + n.grow() * (lv - 1));
                v = Math.max(n.min(), Math.min(n.max(), v));
                a.add(INT_PARAMS.contains(e.getKey()) ? (double) Math.round(v) : Math.round(v * 10000.0) / 10000.0);
            }
            out.add(e.getKey(), a);
        }
        return out;
    }

    static JsonObject params(LineCatalog.Kind kind, int salt) {
        JsonObject p = new JsonObject();
        for (var e : kind.enums().entrySet()) {
            List<String> allowed = e.getValue();
            p.addProperty(e.getKey(), e.getKey().equals("effect2") ? "none" : allowed.get(Math.floorMod(salt * 7 + e.getKey().length(), allowed.size())));
        }
        return p;
    }

    private static List<String[]> pairs(String a, String b) {
        List<String[]> l = new ArrayList<>();
        l.add(new String[]{a, b});
        return l;
    }

    private static JsonObject node(String id, String name, String type, String kind, JsonObject params, JsonObject values, int levels, int[] costs, List<String[]> parents, int x, int y) {
        JsonObject n = new JsonObject();
        n.addProperty("id", id);
        n.addProperty("name", name);
        n.addProperty("desc", "A generated node of kind " + kind + " used by the scale test, long enough to look like a real description of a real art.");
        JsonArray ps = new JsonArray();
        for (String[] p : parents) {
            JsonObject o = new JsonObject();
            o.addProperty("node", p[0]);
            o.addProperty("level", Integer.parseInt(p[1]));
            ps.add(o);
        }
        n.add("parents", ps);
        n.addProperty("cost", costs[0]);
        JsonArray cs = new JsonArray();
        for (int c : costs) cs.add(c);
        n.add("costs", cs);
        n.addProperty("max_level", levels);
        n.addProperty("type", type);
        n.addProperty("kind", kind);
        if (params.size() > 0) n.add("params", params);
        n.add("values", values);
        n.addProperty("x", x);
        n.addProperty("y", y);
        n.addProperty("icon", "minecraft:stick");
        return n;
    }

    /** Line number {@code i} in the generator's shape. */
    static JsonObject line(int i) {
        LineCatalog cat = LineCatalog.get();
        List<String> act = new ArrayList<>(cat.activeKinds()), pas = new ArrayList<>(cat.passiveKinds());
        JsonArray nodes = new JsonArray();
        String s0 = "stance_a", s1 = "stance_b";
        LineCatalog.Kind stance = cat.kind(LineCatalog.Group.STANCE, "stance");
        nodes.add(node(s0, "Stance A " + i, "active_ability", "stance", params(stance, i), values(stance, 3, false), 3, new int[]{1, 2, 2}, List.of(), 0, 0));
        nodes.add(node(s1, "Stance B " + i, "active_ability", "stance", params(stance, i + 1), values(stance, 3, false), 3, new int[]{1, 2, 2}, pairs(s0, "2"), 0, 2));
        LineCatalog.Kind attr = cat.kind(LineCatalog.Group.ATTR, "attr");
        String[] attrs = {"max_health", "armor", "attack_damage", "movement_speed"};
        for (int k = 0; k < 4; k++) {
            JsonObject p = new JsonObject();
            p.addProperty("attribute", attrs[k]);
            p.addProperty("op", "add");
            JsonObject v = new JsonObject();
            JsonArray a = new JsonArray();
            for (int lv = 1; lv <= 5; lv++) a.add(Math.round(Math.min(40, 1.0 * (1 + 0.25 * (lv - 1))) * 10000.0) / 10000.0);
            v.add("amount", a);
            nodes.add(node("stat_" + k, "Stat " + k + " of " + i, "attribute_modifier", "attr", p, v, 5, new int[]{1, 1, 2, 2, 3}, pairs(k % 2 == 0 ? s0 : s1, "1"), 2, k));
        }
        String lastA = s0, lastB = s1;
        List<String> artIds = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            String kind = act.get(Math.floorMod(i * 8 + k, act.size()));
            LineCatalog.Kind ck = cat.kind(LineCatalog.Group.ACTIVE, kind);
            String id = "art_" + k;
            String parent = k % 2 == 0 ? lastA : lastB;
            nodes.add(node(id, "Art " + k + " of " + i, "active_ability", kind, params(ck, i + k), values(ck, 4, false), 4, new int[]{1, 2, 2, 3}, pairs(parent, parent.startsWith("stance") ? "1" : "2"), 4 + 2 * (k / 2), 4 + (k % 2) * 6));
            if (k % 2 == 0) lastA = id;
            else lastB = id;
            artIds.add(id);
        }
        List<String> pasIds = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            String kind = pas.get(Math.floorMod(i * 7 + k, pas.size()));
            LineCatalog.Kind ck = cat.kind(LineCatalog.Group.PASSIVE, kind);
            String id = "pas_" + k;
            nodes.add(node(id, "Passive " + k + " of " + i, "passive_toggle", kind, params(ck, i + k), values(ck, 4, false), 4, new int[]{1, 2, 2, 3}, pairs(artIds.get(k), "2"), 10 + 2 * (k / 2), 8 + k % 2));
            pasIds.add(id);
        }
        for (int k = 0; k < 2; k++) {
            String kind = act.get(Math.floorMod(i * 5 + k + 3, act.size()));
            LineCatalog.Kind ck = cat.kind(LineCatalog.Group.ACTIVE, kind);
            nodes.add(node("fin_" + k, "Finisher " + k + " of " + i, "active_ability", kind, params(ck, i + k), values(ck, 3, true), 3, new int[]{4, 5, 6},
                    List.of(new String[]{artIds.get(7 - k), "3"}, new String[]{pasIds.get(7 - k), "2"}), 16, 14 + k));
        }
        JsonObject r = new JsonObject();
        r.addProperty("magic", "tensura");
        r.addProperty("franchise", "tensura");
        r.addProperty("name", "Scale Line " + i);
        r.addProperty("desc", "A synthetic line number " + i + " for the scale test.");
        r.addProperty("book", "nusmp:scale_line_" + i + "_book");
        r.addProperty("book_name", "Scale Book " + i);
        r.addProperty("book_lore", "Made by LineFixtures.");
        r.addProperty("color", String.format("#%06X", (i * 7919) & 0xFFFFFF));
        r.addProperty("icon", "minecraft:stick");
        r.addProperty("class_id", "scale_line_" + i);
        r.addProperty("duplicate_sp", 3);
        r.addProperty("loot_weight", 1);
        r.addProperty("gui_background", "nusmp:textures/gui/skill_trees/scale_line_" + i + ".png");
        r.addProperty("book_texture", "nusmp:item/skill_book_3d/tensura");
        r.addProperty("background_theme", cat.backgrounds().get(i % cat.backgrounds().size()));
        r.add("nodes", nodes);
        return r;
    }

    /** The parsed route of line {@code i}; fails the test when the schema refuses it. */
    static SkillRoute route(int i) {
        String id = "nusmp:scale_line_" + i;
        RouteSchema.Parsed p = RouteSchema.parse(id, line(i));
        if (!p.clean()) throw new AssertionError(id + ": " + String.join(" | ", p.problems()));
        return p.route();
    }
}
