package com.newuniverse.nusmp.greed;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the gauntlets and the dual blades of the Black Sword Greed are two-hand forms. The rules (which forms are twin, which mesh a hand draws, how big and how often the second blow is) are pure and checked
 * here; so is the data that makes the forms read as two hands: every form has its Better Combat file, the dual form is flagged `twin_blade` + two_handed (two blades in one weapon), the gauntlets are flagged
 * `fist` + two_handed with an `attacks` combo of Better Combat's own fist animations, and the meshes are one glove per hand (the left one the mirror of the right) and one sword per hand. Pure: java.nio only,
 * CWD = repo root.
 */
class GreedHandsTest {
    private static final Path RES = Path.of("src", "main", "resources");
    private static final Path MESHES = RES.resolve("assets/anime_multiverse/weapon_meshes");
    private static final Path SKINS = RES.resolve("assets/anime_multiverse/textures/entity/weapon");
    private static final Path ATTRS = RES.resolve("data/anime_multiverse/weapon_attributes");

    @Test
    void exactlyTheGauntletsAndTheDualBladesAreTwin() {
        for (GreedRules.Form f : GreedRules.Form.values())
            assertEquals(f == GreedRules.Form.GAUNTLETS || f == GreedRules.Form.DUAL_BLADES, GreedHands.isTwin(f.itemId()), f.itemId());
        assertFalse(GreedHands.isTwin(null));
        assertFalse(GreedHands.isTwin("black_sword_greed"));
        assertTrue(GreedHands.isGloves(GreedRules.Form.GAUNTLETS.itemId()));
        assertFalse(GreedHands.isGloves(GreedRules.Form.DUAL_BLADES.itemId()));
    }

    @Test
    void theLeftHandDrawsTheLeftGloveAndEveryOtherMeshStays() {
        assertEquals("black_sword_greed_gauntlets_left", GreedHands.meshFor("black_sword_greed_gauntlets", true));
        assertEquals("black_sword_greed_gauntlets", GreedHands.meshFor("black_sword_greed_gauntlets", false));
        assertEquals("black_sword_greed_dual_blades", GreedHands.meshFor("black_sword_greed_dual_blades", true));
        assertEquals("black_sword_greed_spear", GreedHands.meshFor("black_sword_greed_spear", true));
    }

    @Test
    void theSecondBlowIsSmallerCappedAndRateLimited() {
        assertTrue(GreedHands.offBlow(10f) > 0f && GreedHands.offBlow(10f) < 10f, "smaller than the main blow");
        assertEquals(0f, GreedHands.offBlow(0f));
        assertEquals(0f, GreedHands.offBlow(-5f));
        assertEquals(0f, GreedHands.offBlow(Float.NaN));
        assertEquals(0f, GreedHands.offBlow(Float.POSITIVE_INFINITY));
        assertTrue(GreedHands.offBlow(1e30f) <= 100f, "capped like a pair's");
        assertFalse(GreedHands.ready(100, GreedHands.next(100)), "not again in the same tick");
        assertTrue(GreedHands.ready(GreedHands.next(100), GreedHands.next(100)), "again when the cooldown is over");
        assertTrue(GreedHands.ready(5, 1_000_000L), "a clock set back does not lock it for ever");
        assertTrue(GreedHands.offHandFree(true, false));
        assertFalse(GreedHands.offHandFree(false, false), "a held item blocks it");
        assertFalse(GreedHands.offHandFree(true, true), "a creative or spectating wielder gets none");
    }

    @Test
    void everyFormHasABetterCombatFile() {
        for (GreedRules.Form f : GreedRules.Form.values()) {
            if (f == GreedRules.Form.BOW || f == GreedRules.Form.SHIELD) continue;                      // ranged and shield: no melee swing
            assertTrue(Files.exists(ATTRS.resolve(f.itemId() + ".json")), f.itemId() + " has no Better Combat file");
        }
    }

    @Test
    void theDualFormIsTwoBladesAndTheGauntletsAreFists() throws IOException {
        String dual = Files.readString(ATTRS.resolve("black_sword_greed_dual_blades.json"));
        assertTrue(dual.contains("\"bettercombat:twin_blade\"") && dual.contains("\"two_handed\": true"), "dual blades: Better Combat's two-blades-in-one preset, two-handed");
        String fist = Files.readString(ATTRS.resolve("black_sword_greed_gauntlets.json"));
        assertTrue(fist.contains("\"bettercombat:fist\"") && fist.contains("\"two_handed\": true"), "gauntlets: the fist preset, two-handed");
        assertTrue(fist.contains("bettercombat:one_handed_punch") && fist.contains("bettercombat:dual_handed_"), "gauntlets: Better Combat's own punch and two-fist animations");
        assertFalse(fist.contains("anime_multiverse:"), "only Better Combat's own animations");
    }

    @Test
    void oneGlovePerHandAndTheLeftIsTheMirrorOfTheRight() throws IOException {
        float[] r = bbox(MESHES.resolve("black_sword_greed_gauntlets.txt")), l = bbox(MESHES.resolve("black_sword_greed_gauntlets_left.txt"));
        assertEquals(-l[3], r[0], 0.02f, "x min of the right glove is minus x max of the left");
        assertEquals(-l[0], r[3], 0.02f, "x max of the right glove is minus x min of the left");
        assertEquals(l[1], r[1], 0.02f);
        assertEquals(l[4], r[4], 0.02f);
        assertEquals(l[2], r[2], 0.02f);
        assertEquals(l[5], r[5], 0.02f);
        assertTrue(r[3] - r[0] < 30f, "one glove, not the pair (the pair was 50 units wide)");
        assertTrue(Files.exists(SKINS.resolve("black_sword_greed_gauntlets_left.png")) && Files.exists(SKINS.resolve("black_sword_greed_gauntlets_left_glow.png")), "the left glove's skins");
    }

    @Test
    void oneSwordPerHandAndTheOldMeshesAreKept() throws IOException {
        float[] one = bbox(MESHES.resolve("black_sword_greed_dual_blades.txt"));
        assertTrue(quads(MESHES.resolve("black_sword_greed_dual_blades.txt")) < quads(MESHES.resolve("classic/black_sword_greed_dual_blades.txt")) - 100, "the new mesh holds one blade, the old one two");
        assertEquals(-one[0], one[3], 0.1f, "the single sword is on its own axis");
        for (String id : new String[]{"black_sword_greed_dual_blades", "black_sword_greed_gauntlets"}) {
            assertTrue(Files.exists(MESHES.resolve("classic/" + id + ".txt")), "the old mesh of " + id + " is kept");
            assertTrue(Files.exists(SKINS.resolve("classic/" + id + ".png")) && Files.exists(SKINS.resolve("classic/" + id + "_glow.png")), "the old skins of " + id + " are kept");
        }
    }

    private static int quads(Path file) throws IOException {
        int n = 0;
        for (String line : Files.readAllLines(file)) if (line.startsWith("qn ")) n++;
        return n;
    }

    /** {minX, minY, minZ, maxX, maxY, maxZ} of every `qn` vertex of a mesh file. */
    private static float[] bbox(Path file) throws IOException {
        float[] b = {1e9f, 1e9f, 1e9f, -1e9f, -1e9f, -1e9f};
        List<String> lines = Files.readAllLines(file);
        int quads = 0;
        for (String line : lines) {
            String[] v = line.trim().split("\\s+");
            if (!v[0].equals("qn")) continue;
            quads++;
            for (int c = 0; c < 4; c++) {
                int o = 2 + c * 8;
                for (int a = 0; a < 3; a++) {
                    float x = Float.parseFloat(v[o + a]);
                    b[a] = Math.min(b[a], x);
                    b[3 + a] = Math.max(b[3 + a], x);
                }
            }
        }
        assertTrue(quads > 100, file + " has quads");
        return b;
    }
}
