package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the drop tables are data ({@code data/nusmp/mega/gear/tables.json}) so the owner or a pack can retune what drops and how the grades shift with
 * depth without a mod update, and a data reload ({@code /reload}, a world load) must pick the change up. A plain {@code ResourceManagerReloadListener}
 * (the pattern of the faction files and the avatar races) reads the file through the resource manager, so a data pack's file of the same path replaces
 * the shipped one, hands the parsed tree to the pure {@link GearTablesParser}, logs every problem once with the file it came from, and installs the
 * result in one volatile write so a reload in progress never exposes a half-built table. Two safety nets, because a death event must never meet an empty
 * table by accident: a pack file that is unreadable or yields nothing usable is replaced by the file inside the mod jar (read straight from the class
 * path), and until the first reload has run (or if a caller asks very early) the same class-path copy is used. If even that fails the table is
 * {@link GearTables#EMPTY} and the log says so: nothing drops, nothing crashes. The Gson tree is converted to plain maps and lists here, and only here, so the
 * parser stays library-free and testable.
 */
public final class GearTablesReload implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();

    /** The data file (namespace nusmp, path under data/nusmp/). */
    static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "mega/gear/tables.json");
    /** The same file inside the mod jar, for the fallbacks. */
    private static final String CLASSPATH = "data/nusmp/mega/gear/tables.json";

    /** Where the tables in force came from: {@link #FROM_DATA} (a data reload read the file through the resource manager), {@link #FROM_JAR} (the reload gave nothing usable) or {@link #BEFORE_RELOAD} (asked before any reload ran). */
    public static final String FROM_DATA = "data", FROM_JAR = "mod jar (the pack file was unusable)", BEFORE_RELOAD = "mod jar (before the first data reload)";

    private static volatile GearTables current;
    private static volatile List<String> problems = List.of();
    private static volatile String source = "none";

    /** The tables in force; never null. The class-path copy answers until a data reload has installed one. */
    public static GearTables tables() {
        GearTables t = current;
        if (t != null) return t;
        synchronized (GearTablesReload.class) {
            if (current == null) {
                GearTablesParser.Result r = fromClassPath();
                for (String p : r.problems()) LOG.warn("[nusmp] Gear tables (mod file): {}", p);
                install(r, BEFORE_RELOAD);
            }
            return current;
        }
    }

    /** What the parser complained about when the tables in force were read (empty when the file is clean). */
    public static List<String> problems() {
        tables();
        return problems;
    }

    /** Where the tables in force were read from (one of the constants above, or "none"). */
    public static String source() {
        tables();
        return source;
    }

    private static void install(GearTablesParser.Result r, String from) {
        problems = r.problems();
        source = from;
        current = r.tables();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        GearTablesParser.Result result = null;
        String from = FROM_DATA;
        try {
            Optional<Resource> res = manager.getResource(FILE);
            if (res.isPresent()) {
                try (Reader reader = res.get().openAsReader()) {
                    result = parse(reader);
                }
                for (String p : result.problems()) LOG.warn("[nusmp] Gear tables file {}: {}", FILE, p);
            } else {
                LOG.warn("[nusmp] Gear tables file {} is missing from the loaded packs", FILE);
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("[nusmp] Gear tables file {} could not be read: {}", FILE, e.toString());
            result = null;
        }
        if (result == null || !result.tables().isUsable()) {
            LOG.warn("[nusmp] Gear tables: the pack file gave nothing usable, using the one inside the mod jar instead");
            result = fromClassPath();
            from = FROM_JAR;
            for (String p : result.problems()) LOG.warn("[nusmp] Gear tables (mod file): {}", p);
        }
        install(result, from);
        LOG.info("[nusmp] Loaded the Mega-Dungeon gear tables: {}", GearTablesParser.summary(result.tables()));
    }

    // ---------------------------------------------------------------- reading
    private static GearTablesParser.Result fromClassPath() {
        try (InputStream in = GearTablesReload.class.getClassLoader().getResourceAsStream(CLASSPATH)) {
            if (in == null) {
                LOG.error("[nusmp] Gear tables: {} is not inside the mod jar; no standard gear will drop", CLASSPATH);
                return new GearTablesParser.Result(GearTables.EMPTY, List.of("the mod jar has no " + CLASSPATH));
            }
            return parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            LOG.error("[nusmp] Gear tables: the copy inside the mod jar could not be read; no standard gear will drop", e);
            return new GearTablesParser.Result(GearTables.EMPTY, List.of("the mod jar's table could not be read: " + e));
        }
    }

    private static GearTablesParser.Result parse(Reader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        return GearTablesParser.parse(toTree(root));
    }

    /** A Gson element as plain Java: objects to insertion-ordered maps, arrays to lists, numbers to Double, booleans, strings, null. */
    static Object toTree(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) m.put(e.getKey(), toTree(e.getValue()));
            return m;
        }
        if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            List<Object> l = new ArrayList<>();
            for (JsonElement x : a) l.add(toTree(x));
            return l;
        }
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isBoolean()) return p.getAsBoolean();
        if (p.isNumber()) return p.getAsDouble();
        return p.getAsString();
    }
}
