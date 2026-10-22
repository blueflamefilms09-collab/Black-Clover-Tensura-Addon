package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (CLAUDE.md rule 11, no lag on a public server): nothing can be profiled here, so the lag guards of Recombination Magic are pinned by reading its source, like {@link PerfSourceScanTest} does for
 * the performance pass: the golem scans for foes on the shared interval and never every tick, every construct is counted in a ledger when it spawns and leaves it when it ends, a construct whose
 * caster is gone ends, Bulwark places its blocks in batches, the painters allocate nothing per frame, choose a low-poly model by distance, draw at most a few full golems a frame and cull far
 * constructs, and the particle helper goes through the client budget. If a guard is replaced by a better one on purpose, change the pin in the same commit and say why.
 */
class RecombinationPerfScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) throws IOException {
        String s = Files.readString(SRC.resolve(rel));
        s = s.replaceAll("(?s)/\\*.*?\\*/", " ");
        return s.replaceAll("(?m)//[^\\n]*", "");
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + needle.length())) n++;
        return n;
    }

    @Test
    void theGolemScansOnTheSharedIntervalNeverEveryTick() throws IOException {
        String s = code("prop/RecombinationProps.java");
        assertTrue(s.contains("PerfMath.dueFor(e.life(), PerfSettings.entityScanInterval(), e.getId())"), "the foe scan runs every entityScanInterval ticks, staggered by the entity id");
        assertFalse(s.contains("e.target() == null ? foes("), "a golem with no target must not scan every tick");
        assertFalse(Pattern.compile("% 6 == 0 \\|\\|").matcher(s).find());
        assertTrue(s.contains("FOE_SCAN_MAX"), "a crowd is looked at only as far as the nearest foes");
        assertTrue(s.contains("PerfProbe.section(\"s.recomb.golem\")"), "the golem's tick is timed by the performance probe");
        assertEquals(3, count(s, "foes(e, owner, sl,"), "the three scans: the interval one, the volley's, the ram's (a volley and a ram are rate limited by their cooldowns)");
    }

    @Test
    void everyConstructIsCountedInALedgerAndLeavesItWhenItEnds() throws IOException {
        String s = code("prop/RecombinationProps.java");
        for (String kind : new String[]{"GOLEM", "ROOM", "BOARDS", "DRILL"}) {
            assertTrue(s.contains("track(e, sl, RecombinationBudget." + kind + ")"), kind + " is counted when it spawns");
            assertTrue(s.contains("untrack(e, sl, RecombinationBudget." + kind + ")"), kind + " leaves the ledger when it ends");
        }
        assertTrue(s.contains("m.expire()"), "the oldest construct over a cap is ended");
        assertTrue(s.contains("Map<ServerLevel, RecombinationBudget.Ledger<UUID>[]> LEDGERS = new WeakHashMap<>()"), "the ledgers are per level and do not keep a level alive");
        assertTrue(count(s, "orphaned(e)") >= 2, "rooms and rings of boards end when their caster is gone");
        assertTrue(s.contains("if (owner == null) { e.expire(); return; }"), "the golem ends at once when its caster logs out, dies or leaves the dimension");
        assertTrue(s.contains("RecombinationBudget.OWNER_CHECK_TICKS"), "the caster check of the other constructs is every few ticks, not every tick");
    }

    @Test
    void bulwarkPlacesItsBlocksInBatches() throws IOException {
        String s = code("book/RecombinationArts.java");
        assertTrue(s.contains("BULWARK_MAX_BLOCKS") && s.contains("BULWARK_BLOCKS_PER_TICK"));
        assertFalse(s.contains("placed++"), "no block-by-block placing loop inside the cast tick");
        assertTrue(s.contains("BULWARK_BLOCKS.tryAdd("), "the standing blocks are capped per caster and on the server");
        assertTrue(s.contains("SpellRuntime.later(level, batch, place)"), "the later batches go over the following ticks");
    }

    @Test
    void thePaintersAllocateNothingPerFrame() throws IOException {
        String s = code("client/prop/RecombAnimePainter.java");
        assertTrue(s.contains("Int2ObjectOpenHashMap<St>"), "the per prop state is keyed by the int id without boxing");
        assertFalse(s.contains("Map<Integer"), "no boxed keys");
        assertFalse(s.contains("new HashMap"));
        Matcher m = Pattern.compile("\\bnew\\s+([A-Za-z0-9_.]+(?:<[^>]*>)?)\\s*[(\\[]").matcher(s);
        List<String> found = new ArrayList<>();
        while (m.find()) found.add(m.group(1));
        List<String> allowed = List.of("Int2ObjectOpenHashMap<>", "RecombAnimeRules.FrameBudget", "Quaternionf", "St", "Vec3");
        for (String f : found) assertTrue(allowed.contains(f), "the painter makes a new " + f + ": it may only build its tables once, its per tick state when a prop first appears, and a Vec3 for a camera shake on an event tick");
        assertEquals(1, count(s, "new Quaternionf"), "the one Quaternionf is the static scratch");
        assertTrue(s.contains("private static final Quaternionf TURN"));
        assertTrue(s.contains("TURN.rotationTo("));
        int events = s.indexOf("private static void events(");
        int house = s.indexOf("public static void house(");
        for (int i = s.indexOf("new Vec3("); i >= 0; i = s.indexOf("new Vec3(", i + 1))
            assertTrue(i > events && i < house, "every Vec3 (a camera shake) sits inside events(), which runs once per game tick and only when something happens, never per frame");
    }

    @Test
    void thePaintersChooseALevelOfDetailAndCullFarConstructs() throws IOException {
        String s = code("client/prop/RecombAnimePainter.java");
        assertTrue(s.contains("RecombAnimeRules.lod(dsq, PerfSettings.auraLodNear(), PerfSettings.vfxQuality().detail, sc)"), "the golem's model depends on the distance, the aura LOD distance and the video quality");
        assertTrue(s.contains("FULL_BUDGET.take(") && s.contains("FULL_GOLEMS_PER_FRAME"), "at most a few full golems a frame");
        assertTrue(s.contains("LOD_CULL) return"), "a golem beyond the culling distance is not drawn");
        assertTrue(s.contains("GOLEM_LOD"), "the low-poly model");
        assertTrue(count(s, "cullSmall(") >= 3, "houses, rings and drills are culled when far");
        assertTrue(s.contains("EVENT_RANGE_SQ"), "events (particles) are only worked out near the camera");
        assertTrue(s.contains("PerfProbe.section(\"c.recomb.paint\")"), "the golem's paint is timed by the performance probe");
    }

    @Test
    void theParticleHelperGoesThroughTheClientBudgetAndTheDistance() throws IOException {
        String s = code("vfx/client/RecombAnimeFx.java");
        assertTrue(s.contains("PerfBudgets.clientParticles("), "every spawn is counted against the client particle budget");
        assertTrue(s.contains("FxClientLod.distanceDetail("), "and thinned by the distance to the camera");
        assertTrue(s.contains("FxConfig.clientCustom()") && s.contains("RecombAnimeOptions.particles()"), "and can be switched off");
        assertFalse(s.contains("Minecraft.getInstance().level"), "it spawns into the level it was given");
        assertFalse(s.contains("sendParticles"), "nothing is sent from a server");
    }

    @Test
    void theNumbersOfTheCapsAreWhatTheDocSays() {
        assertEquals(1, com.newuniverse.nusmp.book.RecombinationBudget.GOLEMS_PER_OWNER);
        assertEquals(12, com.newuniverse.nusmp.book.RecombinationBudget.GOLEMS_PER_LEVEL);
        assertEquals(32, com.newuniverse.nusmp.book.RecombinationBudget.BULWARK_MAX_BLOCKS);
        assertEquals(8, com.newuniverse.nusmp.book.RecombinationBudget.BULWARK_BLOCKS_PER_TICK);
    }
}
