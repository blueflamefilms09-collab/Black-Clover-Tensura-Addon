package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the barrier is drawn by resource files and spoken for by a lang fragment that no Java compiler checks, and a missing model shows in the game as the
 * purple-and-black cube. This test reads the files (no game needed) and fails when the block lacks its blockstate, model, item model or animated texture, when the
 * lang fragment and the English map of the code drift apart, when the block is not in the creative tab list and the registry call is not in {@code MegaSystem}, when the
 * doc is missing, and, because the owner banned it outright, when anything in the lockdown package so much as mentions food or exhaustion.
 */
class LockdownAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/mega/lockdown");

    private static JsonObject json(String path) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void theBarrierHasItsBlockstateModelItemModelAndAnimatedTexture() throws Exception {
        assertEquals("nusmp:block/lockdown_barrier", json(ASSETS + "blockstates/lockdown_barrier.json").getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        JsonObject model = json(ASSETS + "models/block/lockdown_barrier.json");
        assertEquals("minecraft:translucent", model.get("render_type").getAsString(), "a sheet of light must be translucent");
        assertEquals("nusmp:block/lockdown_barrier", model.getAsJsonObject("textures").get("all").getAsString());
        assertEquals("nusmp:block/lockdown_barrier", json(ASSETS + "models/item/lockdown_barrier.json").get("parent").getAsString());
        BufferedImage tex = ImageIO.read(new File(ASSETS + "textures/block/lockdown_barrier.png"));
        assertEquals(16, tex.getWidth());
        assertEquals(64, tex.getHeight(), "four frames of 16 x 16");
        assertTrue(json(ASSETS + "textures/block/lockdown_barrier.png.mcmeta").has("animation"));
    }

    @Test
    void theBarrierIsNotTheTrialsCyan() throws Exception {
        BufferedImage a = ImageIO.read(new File(ASSETS + "textures/block/lockdown_barrier.png")), b = ImageIO.read(new File(ASSETS + "textures/block/magic_barrier.png"));
        long warm = 0, cool = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int pa = a.getRGB(x, y), pb = b.getRGB(x, y);
                if (((pa >> 16) & 255) > (pa & 255)) warm++;
                if ((pb & 255) > ((pb >> 16) & 255)) cool++;
            }
        }
        assertTrue(warm > 200, "the lockdown barrier is amber (red over blue): " + warm);
        assertTrue(cool > 200, "the trial barrier is cyan (blue over red): " + cool);
    }

    @Test
    void theLangFragmentIsExactlyTheEnglishMapOfTheCode() throws Exception {
        JsonObject frag = json("tools/lang/lockdown.json");
        Map<String, String> en = LockdownText.all();
        assertEquals(en.size(), frag.size(), "the fragment and LockdownText have a different number of keys");
        for (Map.Entry<String, String> e : en.entrySet()) {
            assertTrue(frag.has(e.getKey()), "the fragment lacks " + e.getKey());
            assertEquals(e.getValue(), frag.get(e.getKey()).getAsString(), "the text of " + e.getKey() + " drifted");
        }
        assertTrue(frag.has("block.nusmp.lockdown_barrier"));
    }

    @Test
    void everyFormatArgumentOfAMessageIsNumberedSoTranslationsCanReorder() {
        for (Map.Entry<String, String> e : LockdownText.all().entrySet()) {
            String t = e.getValue();
            assertFalse(t.matches("(?s).*%[sd].*"), e.getKey() + " uses an unnumbered %s or %d: translations cannot reorder it");
        }
    }

    @Test
    void theBlockIsInTheCreativeTabAndTheRegistryIsCalledFromMegaSystem() throws Exception {
        String creative = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/LabyrinthCreative.java"), StandardCharsets.UTF_8);
        assertTrue(creative.contains("\"lockdown_barrier\""));
        String system = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/mega/MegaSystem.java"), StandardCharsets.UTF_8);
        assertTrue(system.contains("lockdown.LockdownRegistry.init(modBus)"), "MegaSystem must call LockdownRegistry.init before the item list is handed to the bus");
        assertTrue(system.contains("lockdown.LockdownRegistry.register(bus)"));
        String commands = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/mega/MegaCommands.java"), StandardCharsets.UTF_8);
        assertTrue(commands.contains("lockdown.LockdownRegistry.command()"));
    }

    @Test
    void theDocExistsAndNamesNewReplacedAndKept() throws Exception {
        String doc = Files.readString(Path.of("docs/labyrinth/lockdown_ambush.md"), StandardCharsets.UTF_8);
        for (String must : new String[] {"NEW", "REPLACED", "KEPT", "WHY", "could not be verified"}) assertTrue(doc.contains(must), "the doc does not mention " + must);
    }

    @Test
    void nothingInTheLockdownPackageTouchesHunger() throws Exception {
        try (Stream<Path> files = Files.list(SRC)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String code = Files.readString(f, StandardCharsets.UTF_8).toLowerCase();
                for (String banned : new String[] {"fooddata", "exhaustion", "setfoodlevel", "hunger", "saturation", "mobeffects.hunger"}) {
                    assertFalse(code.contains(banned), f.getFileName() + " mentions " + banned + ": the dungeon bans hunger, exhaustion and eating pressure altogether");
                }
            }
        }
    }

    @Test
    void noMegaDungeonLockdownClassTouchesTheTimeMagicVfx() throws Exception {
        try (Stream<Path> files = Files.list(SRC)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String code = Files.readString(f, StandardCharsets.UTF_8);
                assertFalse(code.contains("TimeMagicLayer") || code.contains("TimeBook"), f.getFileName() + " touches the Time Magic VFX, which is off limits");
            }
        }
    }
}
