package com.newuniverse.nusmp.relics.vu;

import com.newuniverse.nusmp.client.relics.vu.VuOverrides;
import com.newuniverse.nusmp.client.relics.worn.lib.WornAnchor;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The self-test {@code visual.duel_disk} (run by the CI server smoke test): every deck relic (the Deck of Many Things and the Pact of the Deck) is a registered relic, has its lines in the
 * duel disk override table, its disk models (worn on the left arm, held), their textures, a lod of at most 15 percent, and the Deck of Many Things still has its floating cards (the groups
 * {@code fate_cards} and {@code fate_cards_far}); the older override table and the older models are still in the jar (the fallback). Reads only jar files and pure classes: runs on a dedicated server.
 */
public final class DuelDiskSelfTests {
    private DuelDiskSelfTests() {}

    private static final String ASSETS = "/assets/nusmp/";

    public static void register() { SelfTests.register("visual.duel_disk", server -> run()); }

    private static List<String> lines(String path) throws IOException {
        List<String> out = new ArrayList<>();
        try (InputStream in = DuelDiskSelfTests.class.getResourceAsStream(path)) {
            if (in == null) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String l;
                while ((l = r.readLine()) != null) out.add(l);
            }
        }
        return out;
    }

    private static String name(String piece) {
        String s = piece.endsWith("~") ? piece.substring(0, piece.length() - 1) : piece;
        int at = s.indexOf('@');
        return at < 0 ? s : s.substring(0, at);
    }

    static SelfTests.Result run() throws IOException {
        List<String> ov = lines(ASSETS + "visual_upgrade/overrides_duel_disk.txt");
        if (ov == null) return SelfTests.Result.fail("visual_upgrade/overrides_duel_disk.txt is not in the jar");
        VuOverrides table = VuOverrides.parse(ov);
        if (!table.problems().isEmpty()) return SelfTests.Result.fail("overrides_duel_disk.txt: " + table.problems().get(0));
        Set<String> known = new HashSet<>();
        for (RelicDef d : Relics.all()) known.add(d.id());
        int models = 0, relics = 0;
        for (String id : new String[]{"deck_of_many_things", "pact_of_the_deck"}) {
            relics++;
            if (!known.contains(id)) return SelfTests.Result.fail("the deck relic " + id + " is not registered");
            if (table.hand(id) == null || table.worn(id) == null) return SelfTests.Result.fail(id + " has no hand or worn line in the duel disk table");
            Set<String> names = new HashSet<>();
            for (String p : table.hand(id).pieces()) names.add(name(p));
            for (String p : table.worn(id).pieces()) names.add(name(p));
            boolean disk = false;
            for (String n : names) {
                if (!n.startsWith("vu/")) return SelfTests.Result.fail(id + " names a library model " + n);
                List<String> l = lines(ASSETS + "visual_upgrade/models/" + n.substring(3) + ".txt");
                if (l == null) return SelfTests.Result.fail("model " + n + " is not in the jar");
                WornMesh m = WornMesh.parse(n, l);
                if (!m.custom()) return SelfTests.Result.fail(n + " names no skin");
                for (String tex : new String[]{m.skin, m.glowTex, m.starTex}) {
                    if (tex == null) continue;
                    try (InputStream in = DuelDiskSelfTests.class.getResourceAsStream(ASSETS + "textures/" + tex)) {
                        if (in == null) return SelfTests.Result.fail(n + " names the missing texture " + tex);
                    }
                }
                if (n.contains("duel_disk")) {
                    disk = true;
                    if (n.endsWith("_worn") && m.anchor != WornAnchor.LEFT_ARM) return SelfTests.Result.fail(n + " is not anchored on the left arm");
                    if (m.lodName == null) return SelfTests.Result.fail(n + " has no lod");
                    List<String> ll = lines(ASSETS + "visual_upgrade/models/" + m.lodName.substring(3) + ".txt");
                    if (ll == null) return SelfTests.Result.fail(n + " names the missing lod " + m.lodName);
                    WornMesh lm = WornMesh.parse(m.lodName, ll);
                    if (lm.quads * 100 > m.quads * 15) return SelfTests.Result.fail(n + " lod has " + lm.quads + " of " + m.quads + " quads (over 15 percent)");
                }
                models++;
            }
            if (!disk) return SelfTests.Result.fail(id + " is not drawn as a duel disk");
        }
        List<String> cards = lines(ASSETS + "visual_upgrade/models/deck_of_many_things_cards_worn.txt");
        if (cards == null) return SelfTests.Result.fail("the floating cards of the Deck of Many Things (deck_of_many_things_cards_worn) are not in the jar");
        WornMesh cm = WornMesh.parse("vu/deck_of_many_things_cards_worn", cards);
        boolean near = false, far = false;
        for (WornMesh.Group g : cm.groups) {
            if (g == null) continue;
            if (g.name().equals("fate_cards")) near = true;
            if (g.name().equals("fate_cards_far")) far = true;
        }
        if (!near || !far || cm.quads != 108) return SelfTests.Result.fail("the floating cards lost their groups or quads (" + cm.quads + " quads)");
        // KEPT: the older table and the older models
        if (lines(ASSETS + "visual_upgrade/overrides.txt") == null || lines(ASSETS + "visual_upgrade/models/deck_of_many_things_worn.txt") == null
                || lines(ASSETS + "visual_upgrade/models/deck_of_many_things.txt") == null || lines(ASSETS + "relic_worn/charm_orbit.txt") == null) {
            return SelfTests.Result.fail("an older deck model or the older override table is gone");
        }
        return SelfTests.Result.ok(relics + " deck relics, " + models + " models, floating cards kept, older models kept");
    }
}
