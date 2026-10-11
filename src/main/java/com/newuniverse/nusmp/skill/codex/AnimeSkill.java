package com.newuniverse.nusmp.skill.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One skill of the Anime Skill Codex: data, never executable code. Fields as in the boss spec: id, anime, name, tier, cast_ticks, range,
 * primitives (checked against {@link SkillSandbox}), counters (what target traits make it score higher), resisted_by (what blunts it),
 * animation (a clip name) and the spoken line.
 */
public record AnimeSkill(String id, String anime, String name, int tier, int castTicks, double range, List<String> primitives, List<String> counters,
                         List<String> resistedBy, String animation, String line) {

    public boolean has(String primitive) { return primitives.contains(primitive); }

    /** True for the skills that deal damage (the ones the emotional high multiplies). */
    public boolean lethal() { return has("magic_damage") || has("physical_damage") || has("projectile") || has("melee_arc"); }

    /** Parses and validates one skill; throws IllegalArgumentException with the reason when it is not acceptable. */
    public static AnimeSkill parse(JsonObject o) {
        String id = str(o, "id");
        if (!id.contains(":")) throw new IllegalArgumentException("id must be namespaced: " + id);
        List<String> prims = list(o, "primitives");
        if (prims.isEmpty()) throw new IllegalArgumentException(id + ": no primitives");
        for (String p : prims) if (!SkillSandbox.allowed(p)) throw new IllegalArgumentException(id + ": unknown primitive '" + p + "'");
        String anim = o.has("animation") ? str(o, "animation") : "cast_grimoire";
        if (!SkillSandbox.CLIPS.contains(anim)) throw new IllegalArgumentException(id + ": unknown animation '" + anim + "'");
        int tier = clamp(o.has("tier") ? o.get("tier").getAsInt() : 1, 1, 4);
        int cast = clamp(o.has("cast_ticks") ? o.get("cast_ticks").getAsInt() : 12, 1, 80);
        double range = Math.max(2, Math.min(32, o.has("range") ? o.get("range").getAsDouble() : 16));
        return new AnimeSkill(id, o.has("anime") ? str(o, "anime") : "generic", o.has("name") ? str(o, "name") : id, tier, cast, range, prims,
                list(o, "counters"), list(o, "resisted_by"), anim, o.has("line") ? str(o, "line") : "");
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) throw new IllegalArgumentException("missing '" + key + "'");
        String s = e.getAsString();
        if (s.length() > 120) throw new IllegalArgumentException("'" + key + "' too long");
        return s;
    }

    private static List<String> list(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (JsonElement e : a) if (e.isJsonPrimitive()) out.add(e.getAsString());
        }
        return out;
    }
}
