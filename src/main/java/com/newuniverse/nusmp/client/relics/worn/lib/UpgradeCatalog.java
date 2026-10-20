package com.newuniverse.nusmp.client.relics.worn.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The catalog of the visual upgrade (1.102.0), read from assets/nusmp/visual_upgrade/catalog.txt (written by tools/visual_upgrade/god_worn/build.py). Pure.
 * <pre>
 * up   &lt;relic id&gt; &lt;model[@anchor][~]&gt; ...   the worn models of an upgraded relic: they replace its {@code relic} / {@code look} entry of relic_worn/catalog.txt
 *                                             while the client option {@code visual.upgrade} is on
 * lod  &lt;relic id&gt; &lt;piece&gt; ...                the same relic's low detail models (at most 15% of the vertices): drawn beyond 12 blocks and at the low quality
 * held &lt;relic id&gt; &lt;piece&gt; ...                what the relic is held as in the hand, on the ground and in an item frame (at most 2500 vertices)
 * </pre>
 * The old files and the old catalog stay on disk untouched; turning the option off returns every relic to them.
 */
public final class UpgradeCatalog {
    public static final UpgradeCatalog EMPTY = new UpgradeCatalog();

    private final Map<String, List<WornCatalog.Piece>> up = new HashMap<>(), lod = new HashMap<>(), held = new HashMap<>();

    public static UpgradeCatalog parse(Iterable<String> lines) {
        UpgradeCatalog c = new UpgradeCatalog();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] v = line.split("\\s+");
            if (v.length < 3) continue;
            Map<String, List<WornCatalog.Piece>> into = switch (v[0]) {
                case "up" -> c.up;
                case "lod" -> c.lod;
                case "held" -> c.held;
                default -> null;
            };
            if (into == null) continue;
            List<WornCatalog.Piece> ps = new ArrayList<>(v.length - 2);
            for (int i = 2; i < v.length; i++) ps.add(WornCatalog.piece(v[i]));
            into.put(v[1], List.copyOf(ps));
        }
        return c;
    }

    /** Does this relic have an upgraded worn look? */
    public boolean has(String relicId) { return up.containsKey(relicId); }

    /** The upgraded worn pieces, or an empty list. */
    public List<WornCatalog.Piece> up(String relicId) { return up.getOrDefault(relicId, List.of()); }

    /** The low detail pieces, or an empty list. */
    public List<WornCatalog.Piece> lod(String relicId) { return lod.getOrDefault(relicId, List.of()); }

    /** The held pieces, or an empty list. */
    public List<WornCatalog.Piece> held(String relicId) { return held.getOrDefault(relicId, List.of()); }

    public int size() { return up.size(); }

    public Iterable<String> ids() { return up.keySet(); }
}
