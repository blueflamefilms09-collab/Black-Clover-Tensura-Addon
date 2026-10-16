package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.hand.HandCatalog;
import com.newuniverse.nusmp.client.relics.hand.HandTransforms;
import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner asked for a 3D model, seen in the hand, for every relic. The relics are added by data in the wave sources (about 370 defs in 31 waves), and nothing checked that
 * each of them has a held form: a weapon needs its mesh, any other relic needs a hand model of its own or a worn model of its slot and look (and every consumable a hand model,
 * because it has no worn one), and its item json must be the builtin/entity model that hands the drawing to the renderer with the old sprite kept as the inventory icon. This test
 * reads the wave sources and the asset files (no game needed) and fails when any relic of any wave misses a step, so a new relic cannot be added as a flat sprite by mistake.
 */
class RelicHandCoverageTest {
    private static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Pattern DEF = Pattern.compile("RelicDef\\s*\\.\\s*of\\s*\\(\\s*([^,]+?)\\s*,\\s*(?:\"(?:[^\"\\\\]|\\\\.)*\"|[^,]+),\\s*Grade\\.\\w+\\s*,\\s*RelicSlot\\.(\\w+)");
    private static final Pattern CONST = Pattern.compile("(\\w+)\\s*=\\s*\"([a-z0-9_]+)\"");
    private static final Pattern NOTEBOOK = Pattern.compile("notebook\\(out,\\s*\"([a-z0-9_]+)\"");

    private record Def(String id, String slot, String look, boolean adopted, boolean pair) {}

    private static List<Def> defs() throws IOException {
        Map<String, String> consts = new HashMap<>();
        Map<Path, String> texts = new HashMap<>();
        try (Stream<Path> s = Files.list(WAVES)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                String t = Files.readString(p);
                texts.put(p, t);
                Matcher m = CONST.matcher(t);
                String cls = p.getFileName().toString().replace(".java", "");
                while (m.find()) consts.putIfAbsent(cls + "." + m.group(1), m.group(2));
            }
        }
        List<Def> out = new ArrayList<>();
        for (Map.Entry<Path, String> e : texts.entrySet()) {
            String t = e.getValue();
            String own = e.getKey().getFileName().toString().replace(".java", "");
            Matcher m = DEF.matcher(t);
            while (m.find()) {
                String arg = m.group(1).trim();
                String id = arg.startsWith("\"") ? arg.substring(1, arg.length() - 1) : consts.get(arg.contains(".") ? arg : own + "." + arg);
                if (id == null) continue;                                           // the notebook helper builds its defs from a variable: found below
                int end = t.indexOf(";", m.end());
                String chain = t.substring(m.start(), end < 0 ? t.length() : end);
                Matcher lk = Pattern.compile("\\.look\\(\\s*Look\\.(\\w+)").matcher(chain);
                String slot = m.group(2);
                String look = lk.find() ? lk.group(1) : Look.defaultFor(RelicSlot.valueOf(slot)).name();
                out.add(new Def(id, slot, look, chain.contains(".adopt("), chain.contains(".pair(")));
            }
            Matcher nb = NOTEBOOK.matcher(t);
            while (nb.find()) out.add(new Def(nb.group(1), "CHARM", "CHARM", false, false));
        }
        List<Def> lefts = new ArrayList<>();
        for (Def d : out) if (d.pair()) lefts.add(new Def(d.id() + "_left", d.slot(), d.look(), false, false));
        out.addAll(lefts);
        return out;
    }

    private static String json(String path) throws IOException { return Files.readString(ASSETS.resolve(path)); }

    @Test
    void sourcesAreFound() throws IOException {
        List<Def> all = defs();
        assertTrue(all.size() >= 360, "the parser should see about 370 relics, saw " + all.size());
        assertTrue(all.stream().anyMatch(d -> d.id().equals("arcanists_notebook")), "the notebooks of wave 17 are found");
        assertTrue(all.stream().anyMatch(d -> d.id().equals("the_infinity_gauntlet")), "the stones and the gauntlet of wave 30 are found");
        assertTrue(all.stream().filter(d -> d.slot().equals("CONSUMABLE")).count() >= 22, "the consumables are found");
    }

    @Test
    void everyRelicIsHeldAsAnObject() throws IOException {
        HandCatalog hand = HandCatalog.parse(Files.readAllLines(ASSETS.resolve("relic_hand/catalog.txt")));
        WornCatalog worn = WornCatalog.parse(Files.readAllLines(ASSETS.resolve("relic_worn/catalog.txt")));
        List<String> missing = new ArrayList<>();
        int sig = 0, wornForm = 0, weapon = 0;
        for (Def d : defs()) {
            if (d.slot().equals("MAIN_HAND")) {                                      // a weapon (an adopted older item too): its own mesh, drawn by the weapon renderer
                if (!Files.exists(ASSETS.resolve("weapon_meshes/" + d.id() + ".txt"))) missing.add(d.id() + " has no weapon mesh");
                else weapon++;
                continue;
            }
            if (d.adopted()) continue;                                               // an older item with its own model (the Hand of Providence, the Devil Contract)
            if (!hand.sigFor(d.id()).isEmpty()) {
                sig++;
                continue;
            }
            if (d.slot().equals("CONSUMABLE")) {
                missing.add(d.id() + " is a consumable with no hand model (add one in tools/relics/gen_hand_consumables.py)");
                continue;
            }
            if (worn.piecesFor(d.id(), d.slot(), d.look()).isEmpty()) missing.add(d.id() + " (" + d.slot() + " " + d.look() + ") has neither a hand model nor a worn model");
            else wornForm++;
        }
        assertTrue(missing.isEmpty(), "no held form for: " + missing);
        assertTrue(sig >= 120, "relics with a signature hand model: " + sig);
        assertTrue(wornForm >= 100, "relics held as their worn model: " + wornForm);
        assertTrue(weapon >= 130, "weapons: " + weapon);
    }

    @Test
    void everyNonWeaponItemJsonHandsOverToTheRendererAndKeepsItsSpriteAsTheIcon() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Def d : defs()) {
            if (d.slot().equals("MAIN_HAND") || d.adopted()) continue;
            if (!Files.exists(ASSETS.resolve("models/item/" + d.id() + ".json"))) {
                bad.add(d.id() + " has no item json");
                continue;
            }
            String model = json("models/item/" + d.id() + ".json");
            if (!model.contains("\"minecraft:builtin/entity\"")) bad.add(d.id() + " is not builtin/entity (run tools/relics/gen_hand_item_models.py)");
            if (model.contains("\"display\"")) bad.add(d.id() + " still has a display block: the style in relic_hand/transforms.txt places it");
            if (!Files.exists(ASSETS.resolve("models/item/" + d.id() + "_icon.json"))) bad.add(d.id() + " has no _icon json (its sprite for the inventory)");
            else if (!json("models/item/" + d.id() + "_icon.json").contains("\"nusmp:item/" + d.id() + "\"")) bad.add(d.id() + "_icon.json does not show nusmp:item/" + d.id());
            if (!Files.exists(ASSETS.resolve("textures/item/" + d.id() + ".png"))) bad.add(d.id() + " has no sprite png");
        }
        assertTrue(bad.isEmpty(), "item models: " + bad);
    }

    @Test
    void everyUseLineOfTheStylesNamesARelicThatExists() throws IOException {
        HandTransforms t = HandTransforms.parse(Files.readAllLines(ASSETS.resolve("relic_hand/transforms.txt")));
        List<String> ids = new ArrayList<>();
        for (Def d : defs()) ids.add(d.id());
        List<String> bad = new ArrayList<>();
        for (String key : t.useKeys()) {
            if (key.startsWith("id:") && !ids.contains(key.substring(3))) bad.add(key);
            if (key.startsWith("slot:") && RelicSlot.byName(key.substring(5)) == null) bad.add(key);
            if (key.startsWith("look:")) {
                String[] p = key.split(":");
                try {
                    RelicSlot.valueOf(p[1]);
                    Look.valueOf(p[2]);
                } catch (RuntimeException e) {
                    bad.add(key);
                }
            }
        }
        assertTrue(bad.isEmpty(), "use lines for relics, slots or looks that do not exist: " + bad);
    }

    @Test
    void theOlderWeaponsThatAdoptAnExistingItemAreTheFullyModelledToo() throws IOException {
        // the 14 hand relics without .model3d() are the older items the relic list adopts: each already has its mesh, its item json, its class row and its engravings
        String engravings = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/item/WeaponEngravings.java"));
        String classesA = Files.readString(ASSETS.resolve("weapon_classes.json")), classesB = Files.readString(Path.of("tools/relics/weapon_classes.json"));
        List<String> bad = new ArrayList<>();
        int n = 0;
        for (Def d : defs()) {
            if (!d.adopted() || !d.slot().equals("MAIN_HAND")) continue;
            n++;
            if (!Files.exists(ASSETS.resolve("weapon_meshes/" + d.id() + ".txt"))) bad.add(d.id() + " mesh");
            if (!json("models/item/" + d.id() + ".json").contains("builtin/entity")) bad.add(d.id() + " item json");
            if (!classesA.contains("\"" + d.id() + "\"")) bad.add(d.id() + " class row (assets)");
            if (!classesB.contains("\"" + d.id() + "\"")) bad.add(d.id() + " class row (tools)");
            if (!engravings.contains(d.id())) bad.add(d.id() + " engravings");
        }
        assertEquals(14, n, "the adopted hand relics");
        assertTrue(bad.isEmpty(), "adopted weapons missing a step: " + bad);
    }
}
