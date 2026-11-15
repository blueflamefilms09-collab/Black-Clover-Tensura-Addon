package com.newuniverse.nusmp.labyrinth.props;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner wants the armour stand back only as a RARE prop, "about 4-8 percent of armoury / barracks / vault-type rooms, 0-1 per room, at most 1-2 per floor, never in corridors, never
 * twice in neighbouring rooms". Those are numbers, so they are measured here over many seeds on made-up floors (a row of rooms, the eligible ones among them) and on the real names of the
 * shipped templates: the mean share of eligible rooms that get a stand is the configured chance (a little less, where the neighbour rule and the cap take rooms away), the cap is never
 * exceeded, no two chosen rooms touch, a room is never chosen twice, a corridor or a stair or a relic vault is never chosen, 0 percent means none, and the choice is the same for the same seed.
 */
class ArmourStandRarityTest {
    private static List<ArmourStandRarity.Piece> floor(int rooms, int eligibleEvery) {
        List<ArmourStandRarity.Piece> out = new ArrayList<>();
        int x = 0;
        for (int i = 0; i < rooms; i++) {
            boolean eligible = i % eligibleEvery == 0;
            String t = eligible ? (i % 2 == 0 ? "anime_multiverse:mega/hall/armory_forge_a" : "anime_multiverse:mega/treasure/closet_b") : (i % 3 == 0 ? "anime_multiverse:mega/corridor/straight_1x3_a" : "anime_multiverse:mega/hall/library_a");
            int w = 27;
            out.add(new ArmourStandRarity.Piece(i, t, x, 0, x + w - 1, 17));
            x += w;                                 // face to face: every room touches its neighbours
        }
        return out;
    }

    @Test
    void theMeanShareOfEligibleRoomsIsTheConfiguredChance() {
        // eligible rooms are every third room of a row (so no two eligible rooms touch): the neighbour rule never fires, the cap rarely does
        List<ArmourStandRarity.Piece> floor = floor(30, 3);
        long eligible = floor.stream().filter(p -> ArmourStandRarity.eligibleTemplate(p.template())).count();
        assertEquals(10, eligible);
        for (double percent : new double[] {4.0, 6.0, 8.0}) {
            int chosen = 0, seeds = 20000, over = 0;
            for (long s = 0; s < seeds; s++) {
                List<Integer> plan = ArmourStandRarity.plan(floor, s, percent, 8);
                chosen += plan.size();
                if (plan.size() > 8) over++;
            }
            double share = chosen / (double) (seeds * eligible);
            assertTrue(Math.abs(share - percent / 100.0) < 0.006, String.format(Locale.ROOT, "at %.0f%% the share of eligible rooms was %.4f", percent, share));
            assertEquals(0, over);
        }
    }

    @Test
    void theFloorCapIsNeverExceededAndItCutsTheTailNotTheStart() {
        List<ArmourStandRarity.Piece> floor = floor(60, 3);
        for (int cap : new int[] {0, 1, 2, 3}) {
            int most = 0;
            for (long s = 0; s < 3000; s++) {
                List<Integer> plan = ArmourStandRarity.plan(floor, s, 100.0, cap);
                assertTrue(plan.size() <= cap, "cap " + cap + " seed " + s + ": " + plan);
                most = Math.max(most, plan.size());
            }
            assertEquals(cap, most, "100 percent fills the cap");
        }
        for (long s = 0; s < 200; s++) assertTrue(ArmourStandRarity.plan(floor, s, 100.0, 100).size() <= ArmourStandRarity.HARD_CAP, "the hard cap");
        // with 100 percent and cap 2 the first two eligible rooms get the stands
        assertEquals(List.of(0, 3), ArmourStandRarity.plan(floor, 1L, 100.0, 2));
    }

    @Test
    void neverTwoStandsInRoomsThatTouch() {
        List<ArmourStandRarity.Piece> floor = floor(24, 1);               // every room eligible, every room touches the next
        for (long s = 0; s < 4000; s++) {
            List<Integer> plan = ArmourStandRarity.plan(floor, s, 40.0, 8);
            for (int i = 0; i < plan.size(); i++) {
                for (int j = i + 1; j < plan.size(); j++) {
                    assertFalse(floor.get(plan.get(i)).touches(floor.get(plan.get(j))), "seed " + s + ": rooms " + plan.get(i) + " and " + plan.get(j) + " touch");
                }
            }
            assertEquals(plan.size(), plan.stream().distinct().count(), "a room is chosen once");
        }
    }

    @Test
    void zeroPercentMeansNoStandAndTheChoiceIsStable() {
        List<ArmourStandRarity.Piece> floor = floor(30, 3);
        for (long s = 0; s < 500; s++) {
            assertTrue(ArmourStandRarity.plan(floor, s, 0.0, 8).isEmpty());
            assertTrue(ArmourStandRarity.plan(floor, s, -5.0, 8).isEmpty());
            assertTrue(ArmourStandRarity.plan(floor, s, Double.NaN, 8).isEmpty());
            assertTrue(ArmourStandRarity.plan(floor, s, 50.0, 0).isEmpty());
            assertEquals(ArmourStandRarity.plan(floor, s, 6.0, 2), ArmourStandRarity.plan(floor, s, 6.0, 2));
        }
        assertEquals(0.0, ArmourStandRarity.clampPercent(-1.0));
        assertEquals(0.0, ArmourStandRarity.clampPercent(Double.NaN));
        assertEquals(100.0, ArmourStandRarity.clampPercent(1000.0));
        assertEquals(6.0, ArmourStandRarity.clampPercent(6.0));
        assertTrue(ArmourStandRarity.plan(null, 1L, 50.0, 2).isEmpty());
    }

    @Test
    void onlyArmouryBarracksGuardTrainingAndTreasureTypeRoomsAreEligible() {
        for (String yes : List.of("anime_multiverse:mega/hall/armory_forge_a", "anime_multiverse:mega/hall/barracks_b", "anime_multiverse:mega/hall/guard_post_a", "anime_multiverse:mega/hall/training_hall_a", "mega/treasure/closet_c",
                "anime_multiverse:mega/treasure/dwarven_forgehold_hoard_nook", "anime_multiverse:mega/treasure/infernal_foundry_counting_house", "anime_multiverse:mega/treasure/sky_citadel_pantry", "anime_multiverse:mega/hall/drowned_crypt_sarcophagus_vault",
                "anime_multiverse:mega_r/hall/armory_forge_b_v3", "anime_multiverse:mega_r/treasure/closet_a_v1")) {
            assertTrue(ArmourStandRarity.eligibleTemplate(yes), yes);
        }
        for (String no : List.of("anime_multiverse:mega/corridor/straight_1x2_a", "anime_multiverse:mega/corridor/bend_1x1_a", "anime_multiverse:mega/junction/plaza_2x2_a", "anime_multiverse:mega/link/waystation", "anime_multiverse:mega/stairwell/up_a",
                "anime_multiverse:mega/vault/one_a", "anime_multiverse:mega/landing/landing_a", "anime_multiverse:mega/antechamber/antechamber_a", "anime_multiverse:mega/arena/arena_a", "anime_multiverse:mega/trap/pit_hall_a", "anime_multiverse:mega/puzzle/sigil_hall_a",
                "anime_multiverse:mega/trial/trial_hall_a", "anime_multiverse:mega/safe/hearth_hall", "anime_multiverse:mega/hall/library_a", "anime_multiverse:mega/hall/crypt_a", "anime_multiverse:mega/set/cathedral_rotunda", "anime_multiverse:mega/cap/plain_wall_a",
                "anime_multiverse:mega_r/corridor/straight_1x2_a_v1", "", "anime_multiverse:hub/plaza")) {
            assertFalse(ArmourStandRarity.eligibleTemplate(no), no);
        }
        assertFalse(ArmourStandRarity.eligibleTemplate(null));
    }

    @Test
    void ofTheRealShippedTemplatesOnlyAMinorityIsEligible() throws IOException {
        int all = 0, eligible = 0;
        for (String folder : List.of("hall", "treasure", "corridor", "junction", "link", "stairwell", "landing", "vault", "arena", "antechamber", "trap", "puzzle", "trial", "set", "cavern", "safe", "onward")) {
            Path dir = Path.of("src/main/resources/data/anime_multiverse/structure/mega/" + folder);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> w = Files.list(dir)) {
                for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".nbt"))::iterator) {
                    all++;
                    String id = "anime_multiverse:mega/" + folder + "/" + p.getFileName().toString().replace(".nbt", "");
                    if (ArmourStandRarity.eligibleTemplate(id)) {
                        eligible++;
                        assertTrue(folder.equals("hall") || folder.equals("treasure"), id + " is not a hall or a treasure room");
                    }
                }
            }
        }
        assertTrue(all > 200, "the shipped templates are found: " + all);
        assertTrue(eligible >= 12 && eligible <= 40, "eligible shipped rooms: " + eligible + " of " + all);
    }

    @Test
    void thePerFloorExpectationIsAFractionOfAStand() {
        // a realistic floor: 28 rooms, about a fifth of them halls or treasure rooms where a stand makes sense and spread out: the mean number of stands per floor at the defaults
        List<ArmourStandRarity.Piece> floor = floor(28, 4);
        double total = 0;
        int seeds = 20000, empty = 0;
        for (long s = 0; s < seeds; s++) {
            int n = ArmourStandRarity.plan(floor, s, 6.0, 2).size();
            total += n;
            if (n == 0) empty++;
        }
        double mean = total / seeds;
        System.out.println(String.format(Locale.ROOT, "ARMOUR STANDS: %.3f stands per floor at 6 percent with 7 eligible rooms (cap 2), %.0f%% of the floors have none", mean, 100.0 * empty / seeds));
        assertTrue(mean > 0.3 && mean < 0.6, "mean stands per floor " + mean);
        assertTrue(empty > seeds * 0.6, "most floors have no stand at all");
    }
}
