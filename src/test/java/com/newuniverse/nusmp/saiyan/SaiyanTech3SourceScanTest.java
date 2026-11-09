package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the four techniques of 3.71.0 (Dragon Fist, God Bind, Ki Deflection, Energy Transfer) must stay server-decided, gated, bounded and wired; nothing can be profiled here, so it is pinned by reading the source. */
class SaiyanTech3SourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String SAIYAN = "src/main/java/com/newuniverse/nusmp/saiyan/";

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) n++;
        return n;
    }

    @Test
    void theTechniquesAreServerCodeThatSpawnsNothingAndKeepsNoStaticPlayerState() throws IOException {
        String t = read(SAIYAN + "SaiyanTechniques3.java");
        assertFalse(t.contains("net.minecraft.client") || t.contains("com.newuniverse.nusmp.client") || t.contains("Minecraft.getInstance"), "no client class");
        assertFalse(t.contains("addFreshEntity") || t.contains("EntityType."), "no technique spawns an entity");
        assertFalse(Pattern.compile("static\\s+[^;=(]*Map<").matcher(t).find(), "per-player state lives in the player's run");
        assertEquals(3, count(t, "getEntitiesOfClass"), "exactly the three bounded queries: a foe in front, the projectile pulse, the allies");
        assertTrue(t.contains("MAX_CANDIDATES") && t.contains("MAX_DEFLECTS"), "every query is capped");
        assertTrue(t.contains("pulseTick"), "the deflection query runs every second tick only");
    }

    @Test
    void theGatesAreRight() throws IOException {
        String t = read(SAIYAN + "SaiyanTechniques3.java");
        assertEquals(1, count(t, "!open(p, s, true)"), "only God Bind needs the Son Goku Ultimate");
        assertEquals(3, count(t, "!open(p, s, false)"), "the other three need an awakened Saiyan");
        assertTrue(t.contains("SaiyanConfig.tech3Enabled()"), "one server switch turns the four off");
        assertTrue(t.contains("dragonUnlocked"), "Dragon Fist needs the martial training");
        assertTrue(t.contains("SaiyanAbilities.allowed("), "foes (and players only where the server allows) are the only targets");
        assertTrue(t.contains("isAlly"), "Energy Transfer goes to allies only");
        assertTrue(t.contains("BalanceLaw.isBoss"), "a boss resists the bind and is not thrown");
    }

    @Test
    void theAsksAreValidatedAndRouted() throws IOException {
        String n = read(SAIYAN + "SaiyanNet.java");
        assertTrue(n.contains("op >= OP_BLAST && op <= OP_ENERGY"), "the range check covers the new ops");
        assertTrue(n.contains("OP_DRAGON = 18") && n.contains("OP_ENERGY = 21"), "the new ops are appended after the old ones");
        String m = read(SAIYAN + "SaiyanManager.java");
        for (String op : new String[] {"OP_DRAGON", "OP_BIND", "OP_DEFLECT", "OP_ENERGY"}) assertTrue(m.contains("case SaiyanNet." + op), op + " must be routed");
        assertTrue(m.contains("SaiyanTechniques3.cancel(r)"), "everything running stops when the system is switched off");
        assertTrue(m.contains("r.tech3.busy()"), "the per-tick call is skipped when nothing runs");
    }

    @Test
    void everyMessageAndKeyNameHasItsText() throws IOException {
        String lang = read("src/main/resources/assets/nusmp/lang/en_us.json");
        Matcher m = Pattern.compile("say\\((?:p|to), \"([a-z_]+)\"\\)").matcher(read(SAIYAN + "SaiyanTechniques3.java"));
        int seen = 0;
        while (m.find()) {
            seen++;
            String k = m.group(1);
            if (k.equals("no_ki") || k.equals("not_awakened") || k.equals("goku_locked")) continue;
            assertTrue(lang.contains("\"nusmp.saiyan.msg." + k + "\""), "missing text for " + k);
        }
        assertTrue(seen >= 10, "the scan must see the messages");
        for (String key : new String[] {"saiyan_dragon_fist", "saiyan_god_bind", "saiyan_ki_deflection", "saiyan_energy_transfer"}) {
            assertTrue(lang.contains("\"key.nusmp." + key + "\""), "missing key name " + key);
            assertTrue(read("src/main/java/com/newuniverse/nusmp/client/saiyan/SaiyanClient.java").contains("\"key.nusmp." + key + "\""), "key not mapped: " + key);
        }
    }

    @Test
    void theSelfTestIsRegisteredAndInTheSmokeList() throws IOException {
        assertTrue(read(SAIYAN + "SaiyanBoot.java").contains("SaiyanTech3SelfTests.register()"));
        assertTrue(read(".github/workflows/smoke.yml").contains("saiyan.tech3"), "the smoke list must expect the self-tests");
    }
}
