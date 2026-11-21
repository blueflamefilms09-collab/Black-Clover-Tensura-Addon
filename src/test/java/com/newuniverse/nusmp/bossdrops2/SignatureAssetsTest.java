package com.newuniverse.nusmp.bossdrops2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/boss_drops_worthwhile.md): the shipped files of the worthwhile boss drops must agree with the Java catalog. Reads the real files: the 55 tables (valid, named after their boss, tier and one-of-one
 * equal to the catalog, only items and effects that can exist), the icon and model of every boss, the model overrides (the order is the custom model data), the lang keys the code and the tooltips read.
 * Pure file reads; the working directory is the project root.
 */
class SignatureAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path DATA = Path.of("src/main/resources/data/anime_multiverse/boss_signature");
    private static final Path JAVA = Path.of("src/main/java");
    private static final Set<String> NAMESPACES = Set.of("minecraft", "anime_multiverse", "tensura");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    private static JsonArray index() throws IOException { return json(Path.of("tools/boss_drops/signature_index.json")).getAsJsonArray("bosses"); }

    @Test
    void theCatalogTheIndexAndTheDataFilesListTheSameBossesInTheSameOrder() throws IOException {
        List<String> ids = new ArrayList<>();
        for (JsonElement e : index()) ids.add(e.getAsJsonObject().get("id").getAsString());
        assertEquals(ids, BossSigCatalog.BOSSES, "append-only order: the position is the custom model data");
        assertEquals(new HashSet<>(ids).size(), ids.size(), "no boss twice");
        assertEquals("default", ids.get(ids.size() - 1));
        Set<String> files = new HashSet<>();
        try (Stream<Path> s = Files.list(DATA)) {
            s.forEach(p -> files.add(p.getFileName().toString().replace(".json", "")));
        }
        assertEquals(new HashSet<>(ids), files, "one data file per catalog boss and no stray file");
    }

    @Test
    void everyTableIsValidNamedAfterItsBossAndAgreesWithTheCatalog() throws IOException {
        for (JsonElement e : index()) {
            JsonObject row = e.getAsJsonObject();
            String boss = row.get("id").getAsString();
            SignatureTable.Table t = SignatureTable.parse(json(DATA.resolve(boss + ".json")));
            assertEquals(boss, t.boss());
            assertEquals(row.get("tier").getAsString(), t.tier().id(), boss + ": tier in the file and in the index");
            assertEquals(t.tier(), BossSigCatalog.tier(boss), boss + ": tier in the Java catalog");
            String relic = row.get("relic").getAsString();
            assertEquals(relic, BossSigCatalog.relic(boss), boss + ": relic in the Java catalog");
            assertEquals(!relic.isEmpty(), t.hasUnique(), boss + ": a one-of-one exactly when the catalog says so");
            if (t.hasUnique()) assertEquals(relic, t.unique().relic());
            assertFalse(t.extras().isEmpty(), boss + ": a boss pays more than its signature");
            assertEquals(1.0, t.signature().chance(), boss + ": the signature is always paid");
        }
    }

    @Test
    void theOneOfOnesAreDistinctAndSomeBossesHaveNone() {
        Set<String> relics = new HashSet<>();
        int n = 0;
        for (String b : BossSigCatalog.BOSSES) {
            String r = BossSigCatalog.relic(b);
            if (r.isEmpty()) continue;
            n++;
            assertTrue(relics.add(r), "a relic is the one-of-one of one boss only: " + r);
        }
        assertEquals(19, n);
    }

    @Test
    void everyItemAndEffectOfEveryTableCanExist() throws IOException {
        for (String boss : BossSigCatalog.BOSSES) {
            SignatureTable.Table t = SignatureTable.parse(json(DATA.resolve(boss + ".json")));
            for (SignatureTable.Row r : t.extras()) {
                String ns = r.item().substring(0, r.item().indexOf(':'));
                assertTrue(NAMESPACES.contains(ns), boss + " names an item of an unknown mod: " + r.item());
                if (ns.equals("tensura")) assertTrue(r.item().equals("tensura:gold_coin") || r.item().equals("tensura:silver_coin"), "only the two coins the mod already uses: " + r.item());
                if (ns.equals("anime_multiverse")) assertTrue(sourceMentions("\"" + r.item().substring(r.item().indexOf(':') + 1) + "\""), boss + ": no item " + r.item() + " in the sources");
            }
            List<SignatureTable.Boon> boons = new ArrayList<>(t.signature().boon());
            if (t.hasUnique()) boons.addAll(t.unique().echoBoon());
            for (SignatureTable.Boon b : boons) assertTrue(b.effect().startsWith("minecraft:"), boss + ": only vanilla effects: " + b.effect());
        }
    }

    private static List<String> sources;

    private static boolean sourceMentions(String quoted) throws IOException {
        if (sources == null) {
            sources = new ArrayList<>();
            try (Stream<Path> s = Files.walk(JAVA)) {
                for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) sources.add(Files.readString(p));
            }
        }
        for (String src : sources) if (src.contains(quoted)) return true;
        return false;
    }

    @Test
    void everyBossHasAnIconAndAModelAndTheOverridesFollowTheCatalogOrder() throws IOException {
        JsonArray over = json(ASSETS.resolve("models/item/boss_signature.json")).getAsJsonArray("overrides");
        assertEquals(BossSigCatalog.BOSSES.size(), over.size());
        for (int i = 0; i < BossSigCatalog.BOSSES.size(); i++) {
            String boss = BossSigCatalog.BOSSES.get(i);
            JsonObject o = over.get(i).getAsJsonObject();
            assertEquals(i + 1, o.getAsJsonObject("predicate").get("custom_model_data").getAsInt(), boss + ": the override index is the position in the catalog");
            assertEquals("anime_multiverse:item/sig/" + boss, o.get("model").getAsString());
            JsonObject model = json(ASSETS.resolve("models/item/sig/" + boss + ".json"));
            assertEquals("anime_multiverse:item/sig/" + boss, model.getAsJsonObject("textures").get("layer0").getAsString());
            BufferedImage im = ImageIO.read(ASSETS.resolve("textures/item/sig/" + boss + ".png").toFile());
            assertNotNull(im, boss);
            assertEquals(im.getWidth(), im.getHeight(), boss + ": a square icon");
            boolean visible = false;
            for (int y = 0; y < im.getHeight() && !visible; y++) for (int x = 0; x < im.getWidth(); x++) if ((im.getRGB(x, y) >>> 24) > 0) { visible = true; break; }
            assertTrue(visible, boss + ": the icon is not empty");
        }
        for (String base : List.of("boss_signature", "boss_echo")) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("textures/item/" + base + ".png")), base + " icon");
            assertTrue(Files.isRegularFile(ASSETS.resolve("models/item/" + base + ".json")), base + " model");
        }
    }

    @Test
    void theLangFragmentHasEveryKeyTheItemsAndTheMessagesRead() throws IOException {
        JsonObject lang = json(Path.of("tools/lang/bossdrops2.json"));
        for (String k : List.of("item.anime_multiverse.boss_signature", "item.anime_multiverse.boss_echo", "bossdrops2.tooltip.echo", "bossdrops2.msg.unique", "bossdrops2.msg.echo",
                "bossdrops2.msg.share", "bossdrops2.msg.used", "bossdrops2.msg.faded", "bossdrops2.cmd.given", "bossdrops2.cmd.unknown")) {
            assertTrue(lang.has(k), "missing lang key " + k);
        }
        for (String boss : BossSigCatalog.BOSSES) {
            for (String part : List.of("name", "lore", "boon")) assertTrue(lang.has("bossdrops2.sig." + boss + "." + part), boss + " lacks " + part);
            if (!BossSigCatalog.relic(boss).isEmpty()) assertTrue(lang.has("bossdrops2.sig." + boss + ".echo_boon"), boss + " lacks the echo line");
        }
    }

    @Test
    void theRelicsOfTheOneOfOnesHaveALangNameAsEveryRelicDoes() throws IOException {
        JsonObject en = json(ASSETS.resolve("lang/en_us.json"));
        for (String boss : BossSigCatalog.BOSSES) {
            String r = BossSigCatalog.relic(boss);
            if (!r.isEmpty()) assertTrue(en.has("item.anime_multiverse." + r), "no relic named " + r);
        }
    }
}
