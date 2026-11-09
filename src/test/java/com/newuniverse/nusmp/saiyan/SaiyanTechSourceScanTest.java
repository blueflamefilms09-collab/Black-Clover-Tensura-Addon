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

/** Why: the four techniques of 3.66.0 (Ki Barrier, Instant Transmission, Spirit Bomb, Meteor Combination) must stay server-decided, gated, bounded and wired; nothing can be profiled here, so it is pinned by reading the source. */
class SaiyanTechSourceScanTest {
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
        String t = read(SAIYAN + "SaiyanTechniques.java");
        assertFalse(t.contains("net.minecraft.client") || t.contains("com.newuniverse.nusmp.client") || t.contains("Minecraft.getInstance"), "no client class");
        assertFalse(t.contains("addFreshEntity") || t.contains("EntityType."), "no technique spawns an entity");
        assertFalse(Pattern.compile("static\\s+[^;=(]*Map<").matcher(t).find(), "per-player state lives in the player's run, keyed by the run, never in a static map");
        assertTrue(count(t, "getEntitiesOfClass") <= 5, "a technique makes at most the five bounded queries it documents");
        assertTrue(t.contains("MAX_BLAST_TARGETS") && t.contains("MAX_SOURCES") && t.contains("MAX_CANDIDATES"), "every query is capped");
    }

    @Test
    void aTeleportNeverLoadsAChunkNorGoesToAPlayerWhoIsNotAnAlly() throws IOException {
        String t = read(SAIYAN + "SaiyanTechniques.java");
        assertTrue(t.contains("hasChunkAt"), "the landing spot must be in a loaded chunk");
        assertTrue(t.contains("isAlly"), "a player is only a target of a transmission when an ally");
        assertTrue(t.contains("fallDistance = 0f") && t.contains("safeFallUntil"), "the arrival is safe");
    }

    @Test
    void theGokuTechniquesAreGatedAndTheBarrierIsOpenToEveryAwakenedSaiyan() throws IOException {
        String t = read(SAIYAN + "SaiyanTechniques.java");
        assertEquals(3, count(t, "!open(p, s, true)"), "Instant Transmission, Spirit Bomb and Meteor Combination need the Son Goku Ultimate");
        assertEquals(1, count(t, "!open(p, s, false)"), "the Ki Barrier needs only an awakened Saiyan");
        assertTrue(t.contains("SaiyanConfig.techEnabled()"), "one server switch turns the four off");
    }

    @Test
    void theAsksAreValidatedAndAReleaseIsNeverDropped() throws IOException {
        String n = read(SAIYAN + "SaiyanNet.java");
        assertTrue(n.contains("op >= OP_BLAST && op <= OP_ENERGY"), "the range check covers the new ops");
        String m = read(SAIYAN + "SaiyanManager.java");
        assertTrue(m.indexOf("OP_SPIRIT_RELEASE") < m.indexOf("MIN_GAP_TICKS"), "the Spirit Bomb release is handled before the rate limit, so a gather can not stay stuck");
        for (String op : new String[] {"OP_TRANSMIT", "OP_BARRIER", "OP_SPIRIT_START", "OP_METEOR"}) assertTrue(m.contains("case SaiyanNet." + op), op + " must be routed");
        assertTrue(m.contains("SaiyanTechniques.cancel(r)"), "everything running stops when the system is switched off");
        assertTrue(read(SAIYAN + "SaiyanCombat.java").contains("SaiyanTechniques.absorb("), "the barrier is read where a blow lands");
    }

    @Test
    void everyMessageAndKeyNameHasItsText() throws IOException {
        String lang = read("src/main/resources/assets/nusmp/lang/en_us.json");
        Matcher m = Pattern.compile("say\\((?:p|v), \"([a-z_]+)\"\\)").matcher(read(SAIYAN + "SaiyanTechniques.java"));
        int seen = 0;
        while (m.find()) {
            seen++;
            String k = m.group(1);
            if (k.equals("no_ki") || k.equals("not_awakened") || k.equals("goku_locked")) continue;       // shared with the older techniques
            assertTrue(lang.contains("\"nusmp.saiyan.msg." + k + "\""), "missing text for " + k);
        }
        assertTrue(seen >= 8, "the scan must see the messages");
        String c = read("src/main/java/com/newuniverse/nusmp/client/saiyan/SaiyanClient.java");
        Matcher key = Pattern.compile("\"(key\\.nusmp\\.saiyan_[a-z_]+)\"").matcher(c);
        int keys = 0;
        while (key.find()) {
            keys++;
            assertTrue(lang.contains("\"" + key.group(1) + "\""), "missing key name " + key.group(1));
        }
        assertTrue(keys >= 15, "all key mappings are scanned");
    }

    @Test
    void theSelfTestIsRegisteredAndInTheSmokeList() throws IOException {
        assertTrue(read(SAIYAN + "SaiyanBoot.java").contains("SaiyanTechSelfTests.register()"));
        String y = read(".github/workflows/smoke.yml");
        assertTrue(y.contains(",saiyan.tech,"), "the smoke list must expect the self-tests");
        assertTrue(y.contains(",tier\""), "tier must stay last");
    }
}
