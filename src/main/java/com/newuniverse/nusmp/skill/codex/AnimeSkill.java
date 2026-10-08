package com.newuniverse.nusmp.skill.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One skill of the Anime Skill Codex. Pure data: a skill is a name, a few numbers and a list of sandbox primitives
 * ({@link SkillSandbox#PRIMITIVES}); nothing here is executable. {@code nativeId} names an optional first-party pre-hook
 * (hex, soul bond, inspiration, page flip) that the boss implements itself.
 */
public record AnimeSkill(String id, String anime, String name, int tier, int castTicks, int range, List<String> primitives,
                         List<String> counters, List<String> resistedBy, String animation, String line, String nativeId,
                         String constructKind, int cooldownTicks) {

    public boolean has(String primitive) { return primitives.contains(primitive); }

    /** True if it hurts: used by the planner to weigh lethal skills. */
    public boolean lethal() { return has("magic_damage") || has("physical_damage"); }

    public static AnimeSkill parse(JsonObject o) {
        String id = str(o, "id", "");
        String name = str(o, "name", id);
        int cast = num(o, "cast_ticks", 12);
        return new AnimeSkill(id.toLowerCase(Locale.ROOT), str(o, "anime", "generic").toLowerCase(Locale.ROOT), name, num(o, "tier", 1), cast,
                num(o, "range", 12), list(o, "primitives"), list(o, "counters"), list(o, "resisted_by"),
                str(o, "animation", "cast_grimoire"), str(o, "line", ""), str(o, "native", ""), str(o, "construct", "weapon"),
                num(o, "cooldown_ticks", 60 + cast));
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("anime", anime);
        o.addProperty("name", name);
        o.addProperty("tier", tier);
        o.addProperty("cast_ticks", castTicks);
        o.addProperty("range", range);
        o.add("primitives", array(primitives));
        o.add("counters", array(counters));
        o.add("resisted_by", array(resistedBy));
        o.addProperty("animation", animation);
        o.addProperty("line", line);
        return o;
    }

    private static JsonArray array(List<String> l) {
        JsonArray a = new JsonArray();
        l.forEach(a::add);
        return a;
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d;
    }

    private static int num(JsonObject o, String k, int d) {
        try { return o.has(k) ? o.get(k).getAsInt() : d; } catch (RuntimeException e) { return d; }
    }

    private static List<String> list(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        if (o.has(k) && o.get(k).isJsonArray()) for (JsonElement e : o.getAsJsonArray(k)) if (e.isJsonPrimitive()) out.add(e.getAsString().toLowerCase(Locale.ROOT));
        return List.copyOf(out);
    }
}
