package com.newuniverse.nusmp.relics.upgrade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The ambient and act effects of one upgraded relic (1.102.0), read from {@code assets/nusmp/visual_upgrade/<relic id>.json} (written by tools/visual_upgrade/god_worn/fx.py). Pure data and
 * parsing, no game classes, so the tables are unit-tested and the server (act effects) and the client (ambient layers) read the very same file from the jar.
 * <pre>
 * { "id": "...", "layers": [ { "name": "idle_motes", "kind": "motes", "particle": "minecraft:end_rod" | "dust", "color": "FFF0C0", "size": 1.0, "per_second": 6,
 *                              "box": [x0,y0,z0,x1,y1,z1] (motes) | "center": [x,y,z], "radius": r, "spin": turns a second, "plane": "flat" | "vertical" (ring, orbit, ground)
 *                              | "from": [..], "to": [..] (line), "speed": rise blocks a tick, "max_per_tick": n }, ... ],
 *   "acts": { "<act key>": [ { "shape": "MAGIC_CIRCLE", "color": "FFF0C0", "duration": 24, "power": 3.0, "at": "feet" | "eye" | "aim",
 *                              "particle": "minecraft:end_rod", "count": 40, "spread": 1.2, "radius": 2.0, "height": 1.0 } ] } }
 * </pre>
 * Positions of a layer are in the wearer's local frame in blocks: x to the wearer's left, y up from the feet, z behind the wearer.
 */
public final class UpgradeFxData {
    /** The layer kinds the client knows. */
    public static final List<String> KINDS = List.of("motes", "ring", "line", "orbit", "trail", "ground");
    public static final String DIR = "/assets/nusmp/visual_upgrade/";

    /** One ambient layer. Arrays are never null (zeros when the file leaves them out). */
    public record Layer(String name, String kind, String particle, int color, float size, float perSecond, double[] box, double[] center, double radius, double spin, String plane, double[] from,
                        double[] to, double speed, int maxPerTick) {}

    /** One visual of an act. */
    public record ActFx(String shape, int color, int duration, float power, String at, String particle, int count, double spread, double radius, double height) {}

    private final String id;
    private final List<Layer> layers;
    private final Map<String, List<ActFx>> acts;

    private UpgradeFxData(String id, List<Layer> layers, Map<String, List<ActFx>> acts) {
        this.id = id;
        this.layers = List.copyOf(layers);
        this.acts = acts;
    }

    public String id() { return id; }

    public List<Layer> layers() { return layers; }

    public Map<String, List<ActFx>> acts() { return Collections.unmodifiableMap(acts); }

    /** The act visuals of one act key (empty when it has none). */
    public List<ActFx> actFx(String actKey) { return acts.getOrDefault(actKey, List.of()); }

    // ---------------------------------------------------------------- parsing
    public static UpgradeFxData parse(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String id = str(o, "id", "");
        List<Layer> layers = new ArrayList<>();
        JsonArray la = o.has("layers") && o.get("layers").isJsonArray() ? o.getAsJsonArray("layers") : new JsonArray();
        for (JsonElement e : la) {
            JsonObject l = e.getAsJsonObject();
            layers.add(new Layer(str(l, "name", "layer"), str(l, "kind", "motes"), str(l, "particle", "dust"), color(str(l, "color", "FFFFFF")), (float) num(l, "size", 1.0),
                    (float) num(l, "per_second", 4.0), arr(l, "box", 6), arr(l, "center", 3), num(l, "radius", 0.5), num(l, "spin", 0.0), str(l, "plane", "flat"), arr(l, "from", 3), arr(l, "to", 3),
                    num(l, "speed", 0.0), (int) num(l, "max_per_tick", 4)));
        }
        Map<String, List<ActFx>> acts = new LinkedHashMap<>();
        if (o.has("acts") && o.get("acts").isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : o.getAsJsonObject("acts").entrySet()) {
                List<ActFx> list = new ArrayList<>();
                for (JsonElement e : en.getValue().getAsJsonArray()) {
                    JsonObject a = e.getAsJsonObject();
                    list.add(new ActFx(str(a, "shape", ""), color(str(a, "color", "FFFFFF")), (int) num(a, "duration", 20), (float) num(a, "power", 2.0), str(a, "at", "feet"), str(a, "particle", ""),
                            (int) num(a, "count", 0), num(a, "spread", 1.0), num(a, "radius", 2.0), num(a, "height", 1.0)));
                }
                acts.put(en.getKey(), List.copyOf(list));
            }
        }
        return new UpgradeFxData(id, layers, acts);
    }

    private static String str(JsonObject o, String k, String d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d; }

    private static double num(JsonObject o, String k, double d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : d; }

    private static double[] arr(JsonObject o, String k, int n) {
        double[] out = new double[n];
        if (o.has(k) && o.get(k).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(k);
            for (int i = 0; i < n && i < a.size(); i++) out[i] = a.get(i).getAsDouble();
        }
        return out;
    }

    static int color(String hex) {
        try {
            return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    // ---------------------------------------------------------------- the jar
    private static final Map<String, UpgradeFxData> CACHE = new HashMap<>();
    private static final UpgradeFxData NONE = new UpgradeFxData("", List.of(), new LinkedHashMap<>());

    /** The table of a relic from the jar, or null if it has none (cached either way; read once, both sides). */
    public static synchronized UpgradeFxData forId(String relicId) {
        UpgradeFxData d = CACHE.get(relicId);
        if (d == null) {
            d = NONE;
            try (InputStream in = UpgradeFxData.class.getResourceAsStream(DIR + relicId + ".json")) {
                if (in != null) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        d = parse(r.lines().collect(Collectors.joining("\n")));
                    }
                }
            } catch (IOException | RuntimeException bad) {
                org.slf4j.LoggerFactory.getLogger("nusmp").error("[nusmp] the visual upgrade table of {} could not be read", relicId, bad);
                d = NONE;
            }
            CACHE.put(relicId, d);
        }
        return d == NONE ? null : d;
    }

    /** What is wrong with this table (empty = fine): unknown kinds, missing geometry, absurd rates. */
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        if (layers.isEmpty()) out.add("no layers");
        for (Layer l : layers) {
            if (!KINDS.contains(l.kind())) out.add(l.name() + ": unknown kind " + l.kind());
            if (l.perSecond() <= 0f || l.perSecond() > 80f) out.add(l.name() + ": per_second " + l.perSecond());
            if (l.maxPerTick() < 1 || l.maxPerTick() > 8) out.add(l.name() + ": max_per_tick " + l.maxPerTick());
            if (l.kind().equals("motes") && l.box()[3] <= l.box()[0] && l.box()[4] <= l.box()[1] && l.box()[5] <= l.box()[2]) out.add(l.name() + ": empty box");
            if ((l.kind().equals("ring") || l.kind().equals("orbit") || l.kind().equals("ground")) && l.radius() <= 0.0) out.add(l.name() + ": no radius");
        }
        return out;
    }
}
