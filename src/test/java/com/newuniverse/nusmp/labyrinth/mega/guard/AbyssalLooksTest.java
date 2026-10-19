package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the owner's complaint was that every Decennial boss looked like the same suit of armour. This pins the fix without a game: the table of looks (assets/nusmp/abyssal/looks.json) holds the
 * twenty-two creatures with three phases each, every SHIPPED boss profile that names a look names one the table has, every famous boss has its own distinct look (so no two of the seventeen can
 * look alike), an evil boss of an Abyss sector with no look of its own wears the Abyss demon of its tier while a holy one and a boss of any other sector keep the old look, a profile without a
 * {@code look} parses as it always did, and a bad look id is refused with a plain sentence. The files themselves (model, texture, glow, skin, extras, clips) are checked by AbyssalAssetsTest.
 */
class AbyssalLooksTest {
    private static final String DIR = "src/main/resources/data/nusmp/mega/dnd_bosses";
    private static final List<String> FAMOUS_LOOKS = List.of("beholder", "elder_brain", "strahd", "lolth", "zariel", "kraken", "demogorgon", "asmodeus", "orcus", "acererak", "tiamat", "vecna",
            "tarrasque", "bahamut", "solar", "dracolich", "lich_king");

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static List<File> profileFiles() {
        List<File> out = new ArrayList<>();
        File[] top = new File(DIR).listFiles((d, n) -> n.endsWith(".json"));
        File[] fam = new File(DIR, "famous").listFiles((d, n) -> n.endsWith(".json"));
        assertNotNull(top, "no profile folder");
        assertNotNull(fam, "no famous profile folder");
        out.addAll(List.of(top));
        out.addAll(List.of(fam));
        return out;
    }

    @Test
    void theTableHoldsTwentyTwoLooksOfThreePhases() {
        assertTrue(AbyssalLooks.all().size() >= 22, "the table has " + AbyssalLooks.all().size() + " looks");
        for (String id : FAMOUS_LOOKS) assertNotNull(AbyssalLooks.get(id), "no look " + id);
        for (String id : List.of("lich", "quasit", "vrock", "hezrou", "balor")) assertNotNull(AbyssalLooks.get(id), "no look " + id);
        for (AbyssalLooks.Look l : AbyssalLooks.all().values()) {
            assertEquals(3, l.phases().size(), l.id() + " phases");
            Set<String> models = new HashSet<>();
            for (AbyssalLooks.Phase p : l.phases()) models.add(p.model());
            assertEquals(3, models.size(), l.id() + " must wear a different model in each phase");
            assertTrue(l.height() > 0.5f && l.height() < 8f, l.id() + " height " + l.height());
            assertTrue(l.boost() > 0.3f && l.boost() < 2f, l.id() + " boost " + l.boost());
            assertTrue(l.phase(2).caption().length() > 10 && l.phase(3).caption().length() > 10, l.id() + " needs a caption for phases 2 and 3");
            assertEquals(l.phase(1).model(), l.model(0), "a phase below 1 is the first");
            assertEquals(l.phase(3).model(), l.model(9), "a phase above 3 is the last");
        }
    }

    @Test
    void theHumanoidsAreSkinsAndExtrasKeyedByLook() {
        List<String> humanoids = List.of("lich", "acererak", "vecna", "strahd", "zariel", "asmodeus", "orcus", "lich_king", "solar");
        Set<String> anchors = Set.of("root", "body", "head", "arm_r", "arm_l", "leg_r", "leg_l");
        for (AbyssalLooks.Look l : AbyssalLooks.all().values()) {
            assertEquals(humanoids.contains(l.id()), l.humanoid(), l.id() + " humanoid flag");
            if (!l.humanoid()) continue;
            for (AbyssalLooks.Phase p : l.phases()) {
                assertEquals("playermodel/" + AbyssalLooks.kitId(l.id(), l.phases().indexOf(p) + 1), p.skin(), l.id() + " skin path");
                assertEquals("pmx_" + AbyssalLooks.kitId(l.id(), l.phases().indexOf(p) + 1), p.extrasModel(), l.id() + " extras model");
                assertFalse(p.extras().isEmpty(), l.id() + " has no extras");
                for (AbyssalLooks.Extra x : p.extras()) assertTrue(anchors.contains(x.anchor()), l.id() + " extra " + x.id() + " hangs on " + x.anchor());
            }
        }
        assertTrue(AbyssalLooks.get("vecna").partScale().containsKey("arm_l"), "Vecna's left arm is short (he has lost the hand)");
        assertTrue(AbyssalLooks.get("zariel").slim(), "Zariel's skin has slim arms");
    }

    @Test
    void everyFamousBossHasItsOwnLook() throws Exception {
        Set<String> seen = new HashSet<>();
        int famous = 0;
        for (File f : profileFiles()) {
            JsonObject o = json(f);
            if (!o.has("famous") || !o.get("famous").getAsBoolean()) continue;
            famous++;
            assertTrue(o.has("look"), f.getName() + " has no look: it would wear the Floor Warden's armour");
            String look = o.get("look").getAsString();
            assertNotNull(AbyssalLooks.get(look), f.getName() + " names the look " + look + " which the table does not have");
            assertTrue(seen.add(look), "two famous bosses share the look " + look);
        }
        assertEquals(17, famous, "famous bosses");
        assertEquals(new HashSet<>(FAMOUS_LOOKS), seen, "the looks the famous bosses wear");
    }

    @Test
    void everyShippedProfileParsesAndItsLookExists() throws Exception {
        for (File f : profileFiles()) {
            String id = f.getName().replace(".json", "");
            DndProfile p = DndProfile.parse(id, json(f));
            if (!p.look().isEmpty()) assertNotNull(AbyssalLooks.get(p.look()), id + " names an unknown look " + p.look());
        }
        DndProfile lich = DndProfile.parse("evil_t1a", json(new File(DIR, "evil_t1a.json")));
        assertEquals("lich", lich.look(), "the Lich-Djinn is a lich");
        DndProfile seraph = DndProfile.parse("holy_t1a", json(new File(DIR, "holy_t1a.json")));
        assertEquals("", seraph.look(), "a profile without a look keeps the old one");
    }

    @Test
    void anAbyssBossWithNoLookWearsTheDemonOfItsTier() {
        assertEquals("quasit", AbyssalLooks.resolve("", "abyss", "evil", 1));
        assertEquals("vrock", AbyssalLooks.resolve("", "abyss", "evil", 2));
        assertEquals("hezrou", AbyssalLooks.resolve("", "abyss", "evil", 3));
        assertEquals("balor", AbyssalLooks.resolve("", "abyss", "evil", 4));
        assertEquals("balor", AbyssalLooks.resolve("", "abyss", "evil", 5));
        assertEquals("balor", AbyssalLooks.resolve("", "abyss", "evil", 9), "a tier past the list is the last");
        for (String theme : new String[]{"blood", "flame", "infernal_foundry"}) assertEquals("hezrou", AbyssalLooks.resolve("", theme, "evil", 3), theme + " is a demon sector too");
        assertEquals("", AbyssalLooks.resolve("", "abyss", "holy", 3), "a holy boss is never a demon");
        assertEquals("", AbyssalLooks.resolve("", "gravity", "evil", 3), "another sector keeps the old look");
        assertEquals("", AbyssalLooks.resolve(null, null, null, 1));
        assertEquals("lich", AbyssalLooks.resolve("lich", "abyss", "evil", 3), "a profile's own look wins over the sector's default");
        assertEquals("", AbyssalLooks.resolve("no_such_creature", "gravity", "holy", 1), "an unknown look falls back to the old one");
    }

    @Test
    void aProfileWithoutALookIsAsItWas() throws Exception {
        DndProfile old = new DndProfile("x", "evil", 1, 0, "X", List.of(), List.of(), List.of("a", "b", "c"), 3, 3, List.of(), List.of(), List.of(), false, false, false, true, false, false, 1, 5, "fire", false);
        assertEquals("", old.look());
        JsonObject o = json(new File(DIR, "holy_t2a.json"));
        o.addProperty("look", "Bad Look!");
        boolean refused = false;
        try {
            DndProfile.parse("holy_t2a", o);
        } catch (IllegalArgumentException e) {
            refused = e.getMessage().contains("look");
        }
        assertTrue(refused, "a bad look id was accepted");
        o.addProperty("look", "beholder");
        assertEquals("beholder", DndProfile.parse("holy_t2a", o).look());
    }

    @Test
    void everyLookIsUsedByABossOrAnAbyssTier() throws Exception {
        Set<String> used = new HashSet<>();
        for (File f : profileFiles()) {
            JsonObject o = json(f);
            if (o.has("look")) used.add(o.get("look").getAsString());
        }
        for (int tier = 1; tier <= 5; tier++) used.add(AbyssalLooks.resolve("", "abyss", "evil", tier));
        for (String id : AbyssalLooks.all().keySet()) assertTrue(used.contains(id), "the look " + id + " is worn by no boss");
    }
}
