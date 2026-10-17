package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MarkerSpec;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the {@link PoolSource} of the tests and the simulator: it reads {@code src/test/resources/mega/catalog.json}, the dump of the REAL template and
 * pool files that {@code tools/labyrinth/mega_catalog.py} writes, so the assembler is proved against the templates that ship and not against shapes
 * someone invented for a test. (The synthetic catalog of {@link SyntheticCatalog} writes the very same format, which is how this reader is tested
 * before the real templates exist.) A missing or empty catalog is an empty source, not an error: the catalog test then reports that there is nothing to
 * assemble yet instead of failing the build while the templates are still being drawn.
 */
final class CatalogSource implements PoolSource {
    private final Map<String, Shape> shapes = new LinkedHashMap<>();
    private final Map<String, PoolDef> pools = new LinkedHashMap<>();

    /**
     * The default location, relative to the project root (the working directory of Gradle's test task). The system property {@code mega.catalog} points the
     * tests at another file, which is how the "real templates" branch of {@code MegaCatalogTest} is exercised before the real templates exist.
     */
    static final Path DEFAULT = Path.of(System.getProperty("mega.catalog", "src/test/resources/mega/catalog.json"));

    static CatalogSource load(Path file) throws IOException {
        if (!Files.exists(file)) return new CatalogSource();
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    static CatalogSource parse(String json) {
        CatalogSource c = new CatalogSource();
        Map<String, Object> root = MiniJson.object(MiniJson.parse(json));
        Map<String, Object> templates = MiniJson.object(root.getOrDefault("templates", Map.of()));
        for (Map.Entry<String, Object> e : templates.entrySet()) c.shapes.put(e.getKey(), shape(e.getKey(), MiniJson.object(e.getValue())));
        Map<String, Object> pools = MiniJson.object(root.getOrDefault("pools", Map.of()));
        for (Map.Entry<String, Object> e : pools.entrySet()) c.pools.put(e.getKey(), pool(e.getKey(), MiniJson.object(e.getValue())));
        return c;
    }

    private static Shape shape(String id, Map<String, Object> t) {
        List<Object> size = MiniJson.array(t.get("size"));
        RoomKind kind;
        try { kind = RoomKind.valueOf(String.valueOf(t.getOrDefault("kind", "OTHER"))); } catch (IllegalArgumentException ex) { kind = RoomKind.OTHER; }
        List<Socket> sockets = new ArrayList<>();
        for (Object o : MiniJson.array(t.getOrDefault("sockets", List.of()))) {
            Map<String, Object> s = MiniJson.object(o);
            Facing f = Facing.byName(String.valueOf(s.get("facing")));
            if (f == null) continue;                                          // a vertical jigsaw: the game-side reader drops those too
            sockets.add(new Socket(new Vec(i(s.get("x")), i(s.get("y")), i(s.get("z"))), f, String.valueOf(s.get("name")), String.valueOf(s.get("target")),
                    String.valueOf(s.get("pool")), i(s.getOrDefault("sel", 0L)), i(s.getOrDefault("place", 0L)), String.valueOf(s.getOrDefault("final", "minecraft:air"))));
        }
        List<ShapeMarker> markers = new ArrayList<>();
        for (Object o : MiniJson.array(t.getOrDefault("markers", List.of()))) {
            Map<String, Object> m = MiniJson.object(o);
            MarkerKind mk = MarkerKind.byId(String.valueOf(m.get("kind")));
            if (mk == null) continue;
            Map<String, String> args = new LinkedHashMap<>();
            for (Map.Entry<String, Object> a : MiniJson.object(m.getOrDefault("args", Map.of())).entrySet()) args.put(a.getKey(), String.valueOf(a.getValue()));
            markers.add(new ShapeMarker(new MarkerSpec(mk, args), new Vec(i(m.get("x")), i(m.get("y")), i(m.get("z")))));
        }
        return new Shape(id, i(size.get(0)), i(size.get(1)), i(size.get(2)), kind, sockets, markers, 0);
    }

    private static PoolDef pool(String id, Map<String, Object> p) {
        List<PoolEntry> entries = new ArrayList<>();
        for (Object o : MiniJson.array(p.getOrDefault("elements", List.of()))) {
            Map<String, Object> e = MiniJson.object(o);
            entries.add(new PoolEntry(String.valueOf(e.get("template")), i(e.getOrDefault("weight", 1L)), "nusmp:mega_interior"));
        }
        return new PoolDef(id, entries, i(p.getOrDefault("emptyWeight", 0L)), String.valueOf(p.getOrDefault("fallback", "minecraft:empty")));
    }

    private static int i(Object o) { return ((Number) o).intValue(); }

    boolean isEmpty() { return shapes.isEmpty(); }

    /**
     * The pools a sector cannot be built without that hold no readable template yet: the Landings, the ordinary door pool and the door caps, the Guardian's hall, the
     * antechamber, the vault for {@code relicCount} relics and the three Way Onward variants. Empty when the catalog is complete. The boss chain is drawn by other builders than the
     * rooms, so the real catalog is incomplete for a while after the rooms arrive; a test that needs whole sectors uses this to say "not yet" instead of failing a build for
     * content that has not been merged.
     */
    List<String> missingForSectors(int relicCount) {
        String[] needed = {MegaGrid.P_LANDING, MegaGrid.P_SIDE, MegaGrid.P_CAP, MegaGrid.P_ARENA, MegaGrid.P_ANTECHAMBER, MegaGrid.P_VAULT + "_" + relicCount, SectorPlanner.POOL_STRAIGHT,
                SectorPlanner.POOL_LEFT, SectorPlanner.POOL_RIGHT};
        List<String> missing = new ArrayList<>();
        for (String id : needed) {
            PoolDef p = pools.get(id);
            boolean stocked = false;
            if (p != null) for (PoolEntry e : p.entries()) if (shapes.containsKey(e.shape())) stocked = true;
            if (!stocked) missing.add(id);
        }
        return missing;
    }

    int shapeCount() { return shapes.size(); }

    int poolCount() { return pools.size(); }

    @Override
    public PoolDef pool(String id) { return pools.get(id); }

    @Override
    public Shape shape(String id) { return shapes.get(id); }
}
