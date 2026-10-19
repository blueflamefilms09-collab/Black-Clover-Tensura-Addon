package com.newuniverse.nusmp.client.relics.worn.lib;

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

import static org.junit.jupiter.api.Assertions.*;

/** Every relic worn on the body has a worn model (or the default look of its slot), read from the wave sources and the catalog. */
class WornCoverageTest {
    static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    static final Path WORN = Path.of("src/main/resources/assets/nusmp/relic_worn");
    static final Pattern DEF = Pattern.compile("RelicDef\\s*\\.\\s*of\\s*\\(\\s*([^,]+?)\\s*,\\s*\"(?:[^\"\\\\]|\\\\.)*\"\\s*,\\s*Grade\\s*\\.\\s*(\\w+)\\s*,\\s*RelicSlot\\s*\\.\\s*(\\w+)\\s*\\)", Pattern.DOTALL);
    static final Pattern CONST_DECL = Pattern.compile("static\\s+final\\s+String\\s+([^;]+);");
    static final Pattern CONST_PAIR = Pattern.compile("(\\w+)\\s*=\\s*\"([a-z0-9_]+)\"");
    static final Pattern LOOK = Pattern.compile("\\.look\\(\\s*Look\\.(\\w+)");

    static WornCatalog catalog() throws IOException {
        return WornCatalog.parse(Files.readAllLines(WORN.resolve("catalog.txt")));
    }

    static List<WornCoverage.Entry> relicsFromSources() throws IOException {
        Map<String, String> sources = new HashMap<>();
        try (Stream<Path> s = Files.list(WAVES)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                String name = p.getFileName().toString();
                sources.put(name.substring(0, name.length() - 5), Files.readString(p));
            }
        }
        Map<String, String> consts = new HashMap<>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Matcher d = CONST_DECL.matcher(e.getValue());
            while (d.find()) {
                Matcher pr = CONST_PAIR.matcher(d.group(1));
                while (pr.find()) {
                    consts.put(e.getKey() + "." + pr.group(1), pr.group(2));
                    consts.putIfAbsent(pr.group(1), pr.group(2));
                }
            }
        }
        List<WornCoverage.Entry> out = new ArrayList<>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            String text = e.getValue();
            Matcher m = DEF.matcher(text);
            while (m.find()) {
                String arg = m.group(1).trim(), slot = m.group(3);
                String id;
                if (arg.startsWith("\"") && arg.endsWith("\"")) id = arg.substring(1, arg.length() - 1);
                else {
                    id = !arg.contains(".") ? consts.get(e.getKey() + "." + arg) : null;
                    if (id == null) id = consts.get(arg);
                    if (id == null) id = consts.get(arg.substring(arg.lastIndexOf('.') + 1));
                }
                if (id == null) continue;
                String tail = text.substring(m.end(), Math.min(text.length(), m.end() + 1500));
                String chain = tail.contains(");") ? tail.substring(0, tail.indexOf(");")) : tail;
                if (chain.contains(".adopt(")) continue;
                Matcher lk = LOOK.matcher(chain);
                String look = lk.find() ? lk.group(1) : WornCatalog.defaultLook(slot);
                out.add(new WornCoverage.Entry(id, slot, look));
            }
        }
        return out;
    }

    @Test
    void everyRelicWornOnTheBodyHasAWornModelOrTheDefaultLookOfItsSlot() throws IOException {
        List<WornCoverage.Entry> relics = relicsFromSources();
        assertTrue(relics.size() > 300, "the wave sources should list hundreds of relics, found " + relics.size());
        WornCoverage.Report rep = WornCoverage.check(relics, catalog(), name -> Files.exists(WORN.resolve(name + ".txt")));
        assertTrue(rep.worn() > 300, rep.summary());
        assertTrue(rep.ok(), rep.summary() + ": " + rep.problems());
    }

    @Test
    void aSlotAndLookTheCatalogDoesNotKnowWearsTheDefaultLookOfItsSlot() {
        WornCatalog c = WornCatalog.parse(List.of("look NECK AMULET amulet_medallion", "relic special_ring pendant_fang"));
        assertEquals(WornCatalog.Source.SIGNATURE, c.sourceOf("special_ring", "NECK", "CHARM"));
        assertEquals(WornCatalog.Source.LOOK, c.sourceOf("x", "NECK", "AMULET"));
        assertEquals(WornCatalog.Source.DEFAULT_LOOK, c.sourceOf("x", "NECK", "POTION"));
        assertEquals("amulet_medallion", c.piecesFor("x", "NECK", "POTION").get(0).model());
        assertEquals(WornCatalog.Source.NONE, c.sourceOf("x", "BELT", "BELT"));
        assertTrue(c.piecesFor("x", "BELT", "BELT").isEmpty());
    }

    @Test
    void theCheckNamesAMissingModelFileAndASlotWithoutAnyLook() {
        WornCatalog c = WornCatalog.parse(List.of("look NECK AMULET amulet_medallion", "relic broken pendant_gone"));
        List<WornCoverage.Entry> relics = List.of(new WornCoverage.Entry("a", "NECK", "AMULET"), new WornCoverage.Entry("broken", "NECK", "AMULET"),
                new WornCoverage.Entry("b", "BELT", "BELT"), new WornCoverage.Entry("sword", "MAIN_HAND", "SWORD"));
        WornCoverage.Report rep = WornCoverage.check(relics, c, n -> n.equals("amulet_medallion"));
        assertEquals(3, rep.worn(), "the hand relic needs no worn model");
        assertFalse(rep.ok());
        assertTrue(rep.problems().stream().anyMatch(p -> p.startsWith("broken") && p.contains("pendant_gone")));
        assertTrue(rep.problems().stream().anyMatch(p -> p.startsWith("b:")));
        assertFalse(rep.problems().stream().anyMatch(p -> p.startsWith("a:")));
    }
}
