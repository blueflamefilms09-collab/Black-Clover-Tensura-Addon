package com.newuniverse.nusmp.client.relics.hand;

import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: most relics are held as the model they wear (the worn library already draws their ring, necklace or hood), but the most distinctive ones are a different object
 * in the hand than on the body (a lyre is worn as a charm orb, a spoon as a charm orb, a flask as a flask) and every consumable has no worn model at all. This is the
 * data side of that: it parses assets/nusmp/relic_hand/catalog.txt, whose {@code sig} lines name the hand models of a relic by its id (written by tools/relics/gen_hand_all.py).
 * Pure.
 * <pre>
 * sig &lt;relic id&gt; &lt;model[@anchor][~]&gt; ...        the hand models of that relic (files assets/nusmp/relic_hand/&lt;model&gt;.txt)
 * </pre>
 */
public final class HandCatalog {
    private final Map<String, List<WornCatalog.Piece>> sigs = new HashMap<>();

    public static HandCatalog parse(Iterable<String> lines) {
        HandCatalog c = new HandCatalog();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] v = line.split("\\s+");
            if (v[0].equals("sig") && v.length >= 3) {
                List<WornCatalog.Piece> ps = new ArrayList<>(v.length - 2);
                for (int i = 2; i < v.length; i++) ps.add(WornCatalog.piece(v[i]));
                c.sigs.put(v[1], List.copyOf(ps));
            }
        }
        return c;
    }

    /** The hand models of a relic, or an empty list if it has no signature model of its own. */
    public List<WornCatalog.Piece> sigFor(String relicId) {
        List<WornCatalog.Piece> p = sigs.get(relicId);
        return p == null ? List.of() : p;
    }

    public int sigCount() { return sigs.size(); }

    public Iterable<String> sigIds() { return sigs.keySet(); }
}
