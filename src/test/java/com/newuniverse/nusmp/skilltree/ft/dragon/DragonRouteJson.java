package com.newuniverse.nusmp.skilltree.ft.dragon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the shipped route files are read by the game's loader (the core builder's), but the tests of THIS route content must not depend on that loader being merged, and
 * they must read the file exactly as written. This is a tiny, test-only reader of the schema of design.md section 2 that builds the same immutable {@link SkillRoute} and
 * {@link SkillNode} records the game uses, so a field that is misspelled or missing in a route file fails here, at build time, instead of loading as a silent default in the
 * game. It is not shipped and has no Minecraft dependency.
 */
final class DragonRouteJson {
    private DragonRouteJson() {}

    /** The route file {@code data/nusmp/skill_trees/<file>.json} of the working tree, as a record (the id is {@code nusmp:<file>}). */
    static SkillRoute read(String file) throws IOException {
        Path p = Path.of("src/main/resources/data/nusmp/skill_trees/" + file + ".json");
        JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
        List<SkillNode> nodes = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("nodes")) nodes.add(node(e.getAsJsonObject()));
        String color = o.get("color").getAsString();
        return new SkillRoute("nusmp:" + file, str(o, "magic"), str(o, "name"), str(o, "desc"), str(o, "book"), str(o, "book_name"), str(o, "book_lore"),
                Integer.parseInt(color.substring(1), 16), str(o, "icon"), str(o, "class_id"), o.get("duplicate_sp").getAsInt(), o.get("loot_weight").getAsInt(), nodes);
    }

    /** The raw color string of the file ({@code #RRGGBB}), for the format check. */
    static String colorText(String file) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/nusmp/skill_trees/" + file + ".json"))).getAsJsonObject().get("color").getAsString();
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) throw new IllegalArgumentException("missing string field '" + key + "'");
        return e.getAsString();
    }

    private static SkillNode node(JsonObject o) {
        List<SkillNode.Parent> parents = new ArrayList<>();
        for (JsonElement pe : o.getAsJsonArray("parents")) {
            if (pe.isJsonPrimitive()) parents.add(new SkillNode.Parent(pe.getAsString(), 1));
            else parents.add(new SkillNode.Parent(str(pe.getAsJsonObject(), "node"), pe.getAsJsonObject().get("level").getAsInt()));
        }
        List<Integer> costs = new ArrayList<>();
        if (o.has("costs")) for (JsonElement c : o.getAsJsonArray("costs")) costs.add(c.getAsInt());
        List<String> passives = new ArrayList<>();
        for (JsonElement pe : o.getAsJsonArray("passives")) passives.add(pe.getAsString());
        Map<String, List<Double>> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("values").entrySet()) {
            List<Double> table = new ArrayList<>();
            JsonArray a = e.getValue().getAsJsonArray();
            for (JsonElement x : a) table.add(x.getAsDouble());
            values.put(e.getKey(), table);
        }
        return new SkillNode(str(o, "id"), str(o, "name"), str(o, "desc"), parents, o.get("cost").getAsInt(), costs, o.get("max_level").getAsInt(), str(o, "skill"), passives,
                values, o.get("x").getAsInt(), o.get("y").getAsInt(), str(o, "icon"));
    }
}
