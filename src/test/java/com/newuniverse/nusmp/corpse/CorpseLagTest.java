package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner rule 11, no lag on a public server): the server work of Soul Corpse summons is bounded by numbers and by the shape of the code, and this pins both. The numbers (global cap on
 * top of the per-player cap, the shared foe scan, the re-path rate) are pure and tested here; the shapes (one foe scan per owner per half second shared by all of the owner's bodies, packets
 * through the effect budget, a probe around the scan, cleanup on logout and stop, draw range and a reused frame on the client) are pinned by reading the source, like PerfSourceScanTest does.
 */
class CorpseLagTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void theGlobalCapBoundsTheWholeServerWhateverTheNumberOfCasters() {
        // 30 casters each at their own cap of 6 could stand 180 zombies; the global cap stops it at 60
        int total = 0;
        for (int caster = 0; caster < 30; caster++) {
            int[] f = SoulZombieRules.fitGlobal(0, 6, 6, total, SoulZombieRules.GLOBAL_CAP_DEFAULT);
            total += f[0];
            assertTrue(total <= SoulZombieRules.GLOBAL_CAP_DEFAULT, "caster " + caster + " total " + total);
        }
        assertEquals(SoulZombieRules.GLOBAL_CAP_DEFAULT, total);
        // an owner at the global cap who summons again replaces their own oldest and never grows the total
        int[] again = SoulZombieRules.fitGlobal(6, 6, 6, SoulZombieRules.GLOBAL_CAP_DEFAULT, SoulZombieRules.GLOBAL_CAP_DEFAULT);
        assertEquals(6, again[0]);
        assertEquals(6, again[1]);
        // a newcomer when the server is full makes none
        assertEquals(0, SoulZombieRules.fitGlobal(0, 4, 6, SoulZombieRules.GLOBAL_CAP_DEFAULT, SoulZombieRules.GLOBAL_CAP_DEFAULT)[0]);
        for (int live = 0; live <= 8; live++)
            for (int want = 0; want <= 10; want++)
                for (int others = 0; others <= 70; others += 7) {
                    int[] f = SoulZombieRules.fitGlobal(live, want, 6, live + others, 60);
                    assertTrue(f[0] >= 0 && f[1] >= 0 && f[1] <= live);
                    assertTrue(live + others - f[1] + f[0] <= Math.max(60, live + others - f[1]), "never above the global cap by this summon");
                }
    }

    @Test
    void theRosterCountsEveryOwner() {
        CorpseRoster<Integer, Integer> r = new CorpseRoster<>();
        assertEquals(0, r.total());
        r.add(1, 10, 4);
        r.add(1, 11, 4);
        r.add(2, 20, 4);
        assertEquals(3, r.total());
        r.remove(1, 10);
        assertEquals(2, r.total());
        r.clear();
        assertEquals(0, r.total());
    }

    @Test
    void theScanAndPathRatesAreHalfASecondAndASecond() {
        assertEquals(10, SoulZombieRules.SCAN_EVERY_TICKS);
        assertTrue(SoulZombieRules.REPATH_TICKS >= 20, "a follower re-paths at most once per second");
        assertTrue(SoulZombieRules.MAX_FOES_KEPT <= 64);
        assertTrue(SoulZombieRules.GLOBAL_CAP_DEFAULT <= 100 && SoulZombieRules.NAMED_GLOBAL_CAP_DEFAULT <= 60);
    }

    @Test
    void theFoeScanIsSharedPerOwnerAndTimed() throws IOException {
        String c = read("corpse/Corpses.java");
        int pick = c.indexOf("static LivingEntity pickFoe(");
        assertTrue(pick > 0);
        String body = c.substring(pick, c.indexOf("// ================================================================ the sweep"));
        assertFalse(body.contains("getEntitiesOfClass"), "a body never scans the world itself: it reads the owner's shared scan");
        assertTrue(body.contains("foesOf(owner, sl)"));
        int scan = c.indexOf("private static List<LivingEntity> foesOf(");
        String scanBody = c.substring(scan, pick);
        assertTrue(scanBody.contains("SCAN_EVERY_TICKS") && scanBody.contains("MAX_FOES_KEPT") && scanBody.contains("PerfProbe.begin()") && scanBody.contains("PerfProbe.end("));
        assertEquals(1, count(c, "getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox()"), "one scan site");
        assertTrue(c.contains("FOES.remove(owner)") && c.contains("FOES.clear()") && c.contains("FOES.remove(player)"), "the cache is dropped on logout and on stop");
    }

    @Test
    void packetsGoThroughTheBudgetAndTheCapsAreEnforced() throws IOException {
        String c = read("corpse/Corpses.java");
        assertTrue(c.contains("PerfBudgets.effectPacket("), "the decay effect packet goes through the effect-packet budget");
        assertTrue(c.contains("NAMED.total() >= CorpseConfig.namedGlobalCap()"), "the numbered undead have a server-wide cap");
        assertTrue(read("corpse/SoulZombies.java").contains("fitGlobal("), "the zombies have a server-wide cap");
        assertTrue(read("corpse/CorpseConfig.java").contains("\"zombieGlobalCap\"") && read("corpse/CorpseConfig.java").contains("\"namedGlobalCap\""));
    }

    @Test
    void noBodyDoesWorldWorkEveryTick() throws IOException {
        String z = read("corpse/SoulZombieEntity.java");
        assertFalse(z.contains("getEntitiesOfClass") || z.contains("getEntities("), "the zombie never scans the world");
        assertTrue(z.contains("REPATH_TICKS"));
        String n = read("corpse/NamedCorpseEntity.java");
        // the signature moves scan only when their cooldown is over (barrier: every 10th tick of the ready state; slam: once per cast), never every tick
        int barrier = n.indexOf("case SOUL_BARRIER ->");
        assertTrue(barrier > 0 && n.substring(barrier, barrier + 160).contains("sigCd <= 0") && n.substring(barrier, barrier + 160).contains("tickCount % 10 == 0"));
        assertTrue(n.contains("sigCd = NamedCorpses.BARRIER_EVERY_TICKS") && n.contains("sigCd = NamedCorpses.SLAM_EVERY_TICKS") && n.contains("sigCd = NamedCorpses.BARRAGE_EVERY_TICKS"));
        // the sweep runs every 10th tick over the rosters only
        String c = read("corpse/Corpses.java");
        assertTrue(c.contains("server.getTickCount() % SWEEP != 7"));
    }

    @Test
    void theClientDrawIsBounded() throws IOException {
        String r = read("client/corpse/RadesUndeadRenderer.java");
        assertTrue(r.contains("DRAW_RANGE_SQ") && r.contains("GLOW_RANGE_SQ"), "a draw range and a glow range");
        assertTrue(r.contains("private final ZogratisPose.Frame scratch"), "one reused pose frame");
        assertEquals(1, count(r, "new ZogratisPose.Frame()"), "the only frame is the reused field");
        assertFalse(r.contains("frame(e, partial)"), "the base class's allocating frame() is not used");
        assertTrue(read("corpse/CorpseEntities.java").contains("clientTrackingRange(8)"), "bodies are tracked within 8 chunks only");
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + 1)) n++;
        return n;
    }
}
