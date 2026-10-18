package com.newuniverse.nusmp.skilltree.ft.kit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the route files are the data the abilities and the balance law are built on, so the tests read the SHIPPED files from the repository (not a copy) and turn them into the same pure records
 * the server builds from them ({@code SkillRoute}, {@code SkillNode}), following the schema of design.md section 2 (a parent is a node id or {@code {node, level}}; {@code costs}, {@code passives},
 * {@code values}, {@code skill} and {@code icon} are optional). That lets one test run the very function the in-game self-test runs ({@code problems(route)}) on the real file at build time.
 * This is a test helper, not a second loader: the server's loader is the core builder's, and a mismatch between the two on a shipped file would show as a self-test failure, which is the point of having both.
 */
public final class RouteFiles {
    private RouteFiles() {}

    private static final Path DIR = Path.of("src/main/resources/data/nusmp/skill_trees");

    /** The raw JSON of a shipped route file, for the tests that check the text itself. */
    public static JsonObject json(String fileName) throws Exception {
        return JsonParser.parseString(Files.readString(DIR.resolve(fileName))).getAsJsonObject();
    }

    /** The route a shipped file describes; the id is {@code nusmp:<file name without .json>}, like the server's loader makes it. */
    public static SkillRoute load(String fileName) throws Exception {
        JsonObject o = json(fileName);
        List<SkillNode> nodes = new ArrayList<>();
        for (JsonElement el : o.getAsJsonArray("nodes")) nodes.add(node(el.getAsJsonObject()));
        return new SkillRoute("nusmp:" + fileName.replace(".json", ""), str(o, "magic"), str(o, "name"), str(o, "desc"), str(o, "book"), str(o, "book_name"), str(o, "book_lore"),
                Integer.parseInt(str(o, "color").substring(1), 16), str(o, "icon"), str(o, "class_id"), o.get("duplicate_sp").getAsInt(), o.get("loot_weight").getAsInt(), nodes);
    }

    private static SkillNode node(JsonObject n) {
        List<SkillNode.Parent> parents = new ArrayList<>();
        for (JsonElement p : n.getAsJsonArray("parents")) {
            if (p.isJsonPrimitive()) parents.add(new SkillNode.Parent(p.getAsString(), 1));
            else parents.add(new SkillNode.Parent(p.getAsJsonObject().get("node").getAsString(), p.getAsJsonObject().get("level").getAsInt()));
        }
        List<Integer> costs = new ArrayList<>();
        if (n.has("costs")) for (JsonElement c : n.getAsJsonArray("costs")) costs.add(c.getAsInt());
        List<String> passives = new ArrayList<>();
        if (n.has("passives")) for (JsonElement c : n.getAsJsonArray("passives")) passives.add(c.getAsString());
        Map<String, List<Double>> values = new LinkedHashMap<>();
        if (n.has("values")) {
            for (Map.Entry<String, JsonElement> e : n.getAsJsonObject("values").entrySet()) {
                List<Double> table = new ArrayList<>();
                if (e.getValue().isJsonArray()) {
                    JsonArray a = e.getValue().getAsJsonArray();
                    for (JsonElement v : a) table.add(v.getAsDouble());
                } else {
                    table.add(e.getValue().getAsDouble());                      // the core's schema: a lone number is a one-entry table
                }
                values.put(e.getKey(), table);
            }
        }
        return new SkillNode(str(n, "id"), str(n, "name"), n.has("desc") ? str(n, "desc") : "", parents, n.get("cost").getAsInt(), costs, n.get("max_level").getAsInt(),
                n.has("skill") ? str(n, "skill") : "", passives, values, n.get("x").getAsInt(), n.get("y").getAsInt(), n.has("icon") ? str(n, "icon") : "");
    }

    private static String str(JsonObject o, String key) { return o.get(key).getAsString(); }
}
