package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.MarkerSpec;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Why: this is the seam between the pure assembler and the game. The assembler asks two questions ("what is in this pool?", "what does this template look
 * like?") and the answers come from the data pack in the live game: the pool JSON files of {@code data/nusmp/worldgen/template_pool/mega} read with the
 * resource manager (not through the vanilla pool registry, whose element lists are private, so the order of the elements and their weights are exactly the
 * file's and the draw is the assembler's own), and the structure templates read with the structure template manager, reduced to what the planner needs: size,
 * kind (from the folder), the Jigsaw sockets and the DATA structure blocks (markers). Everything is read at template-local coordinates with no rotation, and
 * every position in a plan is then computed by the pure maths of {@link Rot}; this class never asks the game to rotate anything, which is what keeps the plan
 * independent of how vanilla would transform a marker. It is also a reload listener (a {@code /reload} re-reads the pools, the Sector Recipe and drops the
 * cached templates) and warms its template cache a few templates per tick after the server starts, so the first sector does not stall the server on disk reads.
 * Server thread only; the loaded pools are published with a volatile write.
 */
public final class VanillaPools implements PoolSource, ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final VanillaPools INSTANCE = new VanillaPools();
    private static final String POOL_DIR = "worldgen/template_pool/mega";
    private static final String RECIPE_DIR = "mega/recipe";
    private static final ResourceLocation RECIPE_FILE = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "mega/recipe/default.json");
    private static final ResourceLocation FLOOR_RECIPE_FILE = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "mega/recipe/floor.json");
    private static final String THEME_DIR = "mega/template_themes";
    private static final String DEFAULT_PROCESSORS = "nusmp:mega_interior";

    private volatile Map<String, PoolDef> pools = Map.of();
    private volatile Recipe recipe = new Recipe();
    private volatile Recipe floorRecipe = Recipe.forFloors();
    private volatile TemplateThemes themes = TemplateThemes.none();
    private final Map<String, Shape> shapes = new HashMap<>();
    private final Set<String> unreadable = new HashSet<>();
    private final ArrayDeque<String> warmQueue = new ArrayDeque<>();
    private StructureTemplateManager templates;

    private VanillaPools() {}

    public static VanillaPools get() { return INSTANCE; }

    /** The Sector Recipe as loaded from the data pack (the defaults until the first reload). */
    public Recipe recipe() { return recipe; }

    /** 1.58.0: the recipe of a floor of the floor model ({@code data/nusmp/mega/recipe/floor.json} laid over {@link Recipe#forFloors}; the built-in numbers until the first reload). */
    public Recipe floorRecipe() { return floorRecipe; }

    /** 1.58.0: which templates are reserved for which themes ({@code data/nusmp/mega/template_themes/*.json}); nothing is reserved until a theme ships a file. */
    public TemplateThemes themes() { return themes; }

    /** The ids of the pools read from the data pack, sorted. */
    public List<String> poolIds() { return new ArrayList<>(new TreeMap<>(pools).keySet()); }

    /** Points the adapter at the structure template manager of a running server; call it before planning (cheap when nothing changed). */
    public void bind(StructureTemplateManager manager) {
        if (manager != templates) {
            templates = manager;
            shapes.clear();
            unreadable.clear();
            rebuildWarmQueue();
        }
    }

    /** Forgets the server (on shutdown), so a new server starts with a clean template cache. */
    public void unbind() {
        templates = null;
        shapes.clear();
        unreadable.clear();
        warmQueue.clear();
    }

    // ---------------------------------------------------------------- PoolSource

    @Override
    public PoolDef pool(String id) { return pools.get(id); }

    @Override
    public Shape shape(String id) {
        Shape s = shapes.get(id);
        if (s != null) return s;
        if (templates == null || unreadable.contains(id)) return null;
        s = load(id);
        if (s == null) {
            unreadable.add(id);
            LOG.warn("[nusmp] Mega template {} cannot be read (missing or not a structure); pools that name it skip it.", id);
            return null;
        }
        shapes.put(id, s);
        return s;
    }

    /** Loads up to {@code budget} templates named by the pools that are not cached yet; returns how many remain. Spread over server ticks after start-up. */
    public int warm(int budget) {
        while (budget-- > 0 && !warmQueue.isEmpty()) shape(warmQueue.poll());
        return warmQueue.size();
    }

    private void rebuildWarmQueue() {
        warmQueue.clear();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, PoolDef> e : new TreeMap<>(pools).entrySet()) {
            for (PoolEntry p : e.getValue().entries()) if (seen.add(p.shape())) warmQueue.add(p.shape());
        }
    }

    private Shape load(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        Optional<StructureTemplate> ot = templates.get(rl);
        if (ot.isEmpty()) return null;
        StructureTemplate t = ot.get();
        Vec3i size = t.getSize();
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) return null;
        RoomKind kind = RoomKind.of(id);
        if (kind != RoomKind.CAP && (size.getX() % MegaGrid.MODULE != 0 || size.getZ() % MegaGrid.MODULE != 0 || size.getY() != MegaGrid.HEIGHT)) {
            LOG.warn("[nusmp] Mega template {} is {}x{}x{}: pieces must be whole modules of {} and {} high; the plan will be wrong for it.", id, size.getX(), size.getY(), size.getZ(), MegaGrid.MODULE, MegaGrid.HEIGHT);
        }
        StructurePlaceSettings settings = new StructurePlaceSettings();
        List<Socket> sockets = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW)) {
            Direction front = JigsawBlock.getFrontFacing(info.state());
            Facing f = front.getAxis().isHorizontal() ? Facing.byName(front.getName()) : null;
            CompoundTag n = info.nbt();
            if (f == null || n == null) {
                LOG.warn("[nusmp] Mega template {} has a jigsaw at {} that is vertical or has no data; it is ignored.", id, info.pos());
                continue;
            }
            sockets.add(new Socket(new Vec(info.pos().getX(), info.pos().getY(), info.pos().getZ()), f, n.getString("name"), n.getString("target"), n.getString("pool"),
                    n.getInt("selection_priority"), n.getInt("placement_priority"), n.getString("final_state")));
        }
        sockets.sort((a, b) -> {                                  // filterBlocks returns palette order; a fixed order keeps plans identical across game versions
            int c = Integer.compare(a.cell().y(), b.cell().y());
            if (c == 0) c = Integer.compare(a.cell().z(), b.cell().z());
            return c != 0 ? c : Integer.compare(a.cell().x(), b.cell().x());
        });
        List<ShapeMarker> markers = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(BlockPos.ZERO, settings, Blocks.STRUCTURE_BLOCK)) {
            CompoundTag n = info.nbt();
            if (n == null || !"DATA".equals(n.getString("mode"))) continue;
            MarkerSpec spec = MarkerSpec.parse(n.getString("metadata"));
            if (spec == null) {
                if (n.getString("metadata").startsWith("nusmp:")) LOG.warn("[nusmp] Mega template {} has an unknown marker '{}' at {}; it is skipped.", id, n.getString("metadata"), info.pos());
                continue;
            }
            markers.add(new ShapeMarker(spec, new Vec(info.pos().getX(), info.pos().getY(), info.pos().getZ())));
        }
        markers.sort((a, b) -> {
            int c = Integer.compare(a.cell().y(), b.cell().y());
            if (c == 0) c = Integer.compare(a.cell().z(), b.cell().z());
            return c != 0 ? c : Integer.compare(a.cell().x(), b.cell().x());
        });
        return new Shape(id, size.getX(), size.getY(), size.getZ(), kind, sockets, markers, recipe.shapeMax(id));
    }

    // ---------------------------------------------------------------- reload

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, PoolDef> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<Resource>> e : manager.listResourceStacks(POOL_DIR, rl -> rl.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation key = e.getKey();
            if (!NUSMP.MODID.equals(key.getNamespace()) || e.getValue().isEmpty()) continue;
            String path = key.getPath();
            String name = path.substring(POOL_DIR.length() + 1, path.length() - ".json".length());
            String id = key.getNamespace() + ":mega/" + name;
            Resource top = e.getValue().get(e.getValue().size() - 1);                       // the stack runs lowest priority first: the last pack wins, as for any data file
            try (Reader reader = new InputStreamReader(top.open(), StandardCharsets.UTF_8)) {
                loaded.put(id, parsePool(id, JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception ex) {
                LOG.warn("[nusmp] Skipping Mega pool {}: {}", key, ex.toString());
            }
        }
        Recipe r = new Recipe();
        List<Resource> stack = manager.listResourceStacks(RECIPE_DIR, rl -> rl.equals(RECIPE_FILE)).get(RECIPE_FILE);
        if (stack != null && !stack.isEmpty()) {
            try (Reader reader = new InputStreamReader(stack.get(stack.size() - 1).open(), StandardCharsets.UTF_8)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> values = (Map<String, Object>) toJava(JsonParser.parseReader(reader));
                r = Recipe.fromMap(values);
                for (String k : r.unknownKeys()) LOG.warn("[nusmp] Mega recipe: unknown key '{}' (a typo keeps the default)", k);
            } catch (Exception ex) {
                LOG.warn("[nusmp] The Mega recipe {} cannot be read ({}); the built-in defaults are used.", RECIPE_FILE, ex.toString());
                r = new Recipe();
            }
        } else {
            LOG.info("[nusmp] No Mega recipe file ({}); the built-in defaults are used.", RECIPE_FILE);
        }
        recipe = r;
        floorRecipe = readFloorRecipe(manager);
        themes = readThemes(manager);
        pools = loaded;
        shapes.clear();
        unreadable.clear();
        rebuildWarmQueue();
        LOG.info("[nusmp] Loaded {} Mega pools and the Sector Recipe.", loaded.size());
    }

    /** The floor recipe: the shipped file laid over the built-in floor numbers; a missing or unreadable file keeps the built-in ones (logged). */
    @SuppressWarnings("unchecked")
    private static Recipe readFloorRecipe(ResourceManager manager) {
        List<Resource> stack = manager.listResourceStacks(RECIPE_DIR, rl -> rl.equals(FLOOR_RECIPE_FILE)).get(FLOOR_RECIPE_FILE);
        if (stack == null || stack.isEmpty()) {
            LOG.info("[nusmp] No floor recipe file ({}); the built-in floor numbers are used.", FLOOR_RECIPE_FILE);
            return Recipe.forFloors();
        }
        try (Reader reader = new InputStreamReader(stack.get(stack.size() - 1).open(), StandardCharsets.UTF_8)) {
            Recipe r = Recipe.fromMap((Map<String, Object>) toJava(JsonParser.parseReader(reader)), Recipe.forFloors());
            for (String k : r.unknownKeys()) LOG.warn("[nusmp] Mega floor recipe: unknown key '{}' (a typo keeps the default)", k);
            return r;
        } catch (Exception ex) {
            LOG.warn("[nusmp] The Mega floor recipe {} cannot be read ({}); the built-in floor numbers are used.", FLOOR_RECIPE_FILE, ex.toString());
            return Recipe.forFloors();
        }
    }

    /** The theme rules of every {@code data/nusmp/mega/template_themes/*.json} (the top file of each stack); what is not understood is logged and skipped. */
    private static TemplateThemes readThemes(ResourceManager manager) {
        List<Object> docs = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<Resource>> e : new TreeMap<>(manager.listResourceStacks(THEME_DIR, rl -> rl.getPath().endsWith(".json"))).entrySet()) {
            if (!NUSMP.MODID.equals(e.getKey().getNamespace()) || e.getValue().isEmpty()) continue;
            try (Reader reader = new InputStreamReader(e.getValue().get(e.getValue().size() - 1).open(), StandardCharsets.UTF_8)) {
                docs.add(toJava(JsonParser.parseReader(reader)));
            } catch (Exception ex) {
                LOG.warn("[nusmp] Skipping Mega theme rooms file {}: {}", e.getKey(), ex.toString());
            }
        }
        List<String> problems = new ArrayList<>();
        TemplateThemes rules = TemplateThemeFiles.parse(docs, problems);
        for (String p : problems) LOG.warn("[nusmp] Mega theme rooms: {}", p);
        LOG.info("[nusmp] Loaded {} theme room file(s): {} template prefix(es) reserved for a theme.", docs.size(), rules.size());
        return rules;
    }

    /** One pool file: {@code {"fallback": ..., "elements": [{"weight": n, "element": {"element_type": ..., "location": ..., "processors": ...}}]}}. */
    static PoolDef parsePool(String id, JsonObject root) {
        String fallback = root.has("fallback") ? root.get("fallback").getAsString() : MegaGrid.P_EMPTY;
        List<PoolEntry> entries = new ArrayList<>();
        int empty = 0;
        JsonArray els = root.has("elements") ? root.getAsJsonArray("elements") : new JsonArray();
        for (JsonElement je : els) {
            JsonObject o = je.getAsJsonObject();
            int weight = o.has("weight") ? o.get("weight").getAsInt() : 1;
            JsonObject el = o.getAsJsonObject("element");
            String type = el.has("element_type") ? el.get("element_type").getAsString() : "";
            if (type.equals("minecraft:empty_pool_element")) {
                empty += Math.max(1, weight);
                continue;
            }
            if (!el.has("location")) continue;                      // a feature or list element: not a template, and the Mega-Dungeon uses none
            String processors = DEFAULT_PROCESSORS;
            if (el.has("processors")) {
                JsonElement p = el.get("processors");
                processors = p.isJsonPrimitive() ? p.getAsString() : "minecraft:empty";
            }
            entries.add(new PoolEntry(el.get("location").getAsString(), Math.max(1, weight), processors));
        }
        return new PoolDef(id, entries, empty, fallback);
    }

    /** Gson to plain Java (objects to maps, arrays to lists, numbers, strings, booleans), the shape {@link Recipe#fromMap} reads. */
    static Object toJava(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonObject()) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) m.put(en.getKey(), toJava(en.getValue()));
            return m;
        }
        if (e.isJsonArray()) {
            List<Object> l = new ArrayList<>();
            for (JsonElement x : e.getAsJsonArray()) l.add(toJava(x));
            return l;
        }
        if (e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean();
        if (e.getAsJsonPrimitive().isNumber()) return e.getAsNumber();
        return e.getAsString();
    }
}
