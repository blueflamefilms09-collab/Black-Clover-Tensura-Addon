package com.newuniverse.nusmp.client.relics.vu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the visual upgrade of the charms, the gauntlet, the crowns and the Gate of Babylon crest replaces the models some relics are held and worn as, but the older models and
 * the hand and worn catalogs (written by tools/relics/gen_hand_all.py and gen_worn_all.py, "do not edit") must stay exactly as they were, so that the owner can switch back
 * with {@code visual.upgrade=false} and so nothing is deleted. This is the documented override table that sits beside them: assets/nusmp/visual_upgrade/overrides.txt.
 * Pure (no Minecraft types), so the file is parsed and checked in tests.
 * <pre>
 * hand &lt;relic id&gt; &lt;piece&gt; ... [| size=0.62 style=palm]    the models the relic is held as in the hand, on the ground and in a frame
 * worn &lt;relic id&gt; &lt;piece&gt; ...                          the models it wears on the body
 * </pre>
 * A piece is {@code model[@anchor][~]} exactly as in the catalogs; a model named {@code vu/<name>} is read from assets/nusmp/visual_upgrade/models/&lt;name&gt;.txt.
 * Options after the bar apply to the hold style of a hand entry: {@code size} (the longest side of the object in blocks) and {@code style} (a style of transforms.txt).
 */
public final class VuOverrides {
    /** One override line. */
    public record Entry(String kind, String id, List<String> pieces, Map<String, String> options) {
        /** The option as a float, or {@code fallback}. */
        public float number(String key, float fallback) {
            String v = options.get(key);
            if (v == null) return fallback;
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException bad) {
                return fallback;
            }
        }
    }

    private final Map<String, Entry> hand = new LinkedHashMap<>(), worn = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public static VuOverrides parse(Iterable<String> lines) {
        VuOverrides o = new VuOverrides();
        int ln = 0;
        for (String raw : lines) {
            ln++;
            String line = raw;
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);
            line = line.trim();
            if (line.isEmpty()) continue;
            String opts = "";
            int bar = line.indexOf('|');
            if (bar >= 0) {
                opts = line.substring(bar + 1).trim();
                line = line.substring(0, bar).trim();
            }
            String[] v = line.split("\\s+");
            if (v.length < 3 || !(v[0].equals("hand") || v[0].equals("worn"))) {
                o.problems.add("line " + ln + ": expected 'hand|worn <id> <piece...>'");
                continue;
            }
            Map<String, String> map = new HashMap<>();
            if (!opts.isEmpty()) {
                for (String kv : opts.split("\\s+")) {
                    int eq = kv.indexOf('=');
                    if (eq <= 0) {
                        o.problems.add("line " + ln + ": option without '=': " + kv);
                        continue;
                    }
                    map.put(kv.substring(0, eq), kv.substring(eq + 1));
                }
            }
            List<String> pieces = new ArrayList<>(v.length - 2);
            for (int i = 2; i < v.length; i++) pieces.add(v[i]);
            Entry e = new Entry(v[0], v[1], List.copyOf(pieces), Collections.unmodifiableMap(map));
            (v[0].equals("hand") ? o.hand : o.worn).put(v[1], e);
        }
        return o;
    }

    /** The hand entry of a relic, or null. */
    public Entry hand(String id) { return hand.get(id); }

    /** The worn entry of a relic, or null. */
    public Entry worn(String id) { return worn.get(id); }

    public Iterable<String> handIds() { return hand.keySet(); }

    public Iterable<String> wornIds() { return worn.keySet(); }

    public int handCount() { return hand.size(); }

    public int wornCount() { return worn.size(); }

    /** Lines that could not be read. */
    public List<String> problems() { return problems; }
}
