package com.newuniverse.nusmp.elite.nations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.nations.contract.ContractTemplate;
import com.newuniverse.nusmp.elite.nations.contract.ContractType;
import com.newuniverse.nusmp.elite.nations.core.Perk;
import com.newuniverse.nusmp.elite.nations.core.PerkTable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Why: the shipped datapack files (perks, 30+ contract templates, guard tags) are what a real server loads; they must parse with the same code the game uses, and the built-in perk table must equal perks.json. */
class ShippedDataTest {
    private static final Path DATA = Paths.get("src/main/resources/data/nusmp");

    private static JsonObject read(Path p) throws IOException { return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject(); }

    @Test
    void everyShippedContractParses() throws IOException {
        Set<ContractType> types = EnumSet.noneOf(ContractType.class);
        Set<Integer> tiers = new HashSet<>();
        int n = 0;
        try (Stream<Path> files = Files.list(DATA.resolve("elite/nations/contracts"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                String id = p.getFileName().toString().replace(".json", "");
                ContractTemplate t = ContractTemplate.parse(id, read(p));
                types.add(t.type());
                tiers.add(t.tier());
                assertFalse(t.title().isEmpty(), id + " has no title");
                assertTrue(t.reward().coins() > 0 && t.reward().nationXp() > 0 && t.reward().reputation() > 0, id + " reward");
                assertTrue(t.minNationLevel() >= 1, id);
                n++;
            }
        }
        assertTrue(n >= 30, "only " + n + " templates");
        assertTrue(types.containsAll(EnumSet.of(ContractType.KILL_MOB, ContractType.MINE_BLOCK, ContractType.CRAFT_ITEM, ContractType.EXPLORE_BIOME, ContractType.DELIVER_ITEM)), "types: " + types);
        assertEquals(Set.of(1, 2, 3, 4, 5), tiers, "all five tiers have templates");
    }

    @Test
    void higherTiersPayMore() throws IOException {
        double[] best = new double[6], worst = {0, 1e18, 1e18, 1e18, 1e18, 1e18};
        try (Stream<Path> files = Files.list(DATA.resolve("elite/nations/contracts"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                ContractTemplate t = ContractTemplate.parse("x", read(p));
                best[t.tier()] = Math.max(best[t.tier()], t.reward().coins());
                worst[t.tier()] = Math.min(worst[t.tier()], t.reward().coins());
            }
        }
        for (int t = 2; t <= 5; t++) assertTrue(worst[t] > worst[t - 1], "tier " + t + " pays no more than tier " + (t - 1));
    }

    @Test
    void perksJsonEqualsTheBuiltInTable() throws IOException {
        PerkTable file = PerkTable.parse(read(DATA.resolve("elite/nations/perks.json")));
        assertTrue(file.warnings().isEmpty(), "warnings: " + file.warnings());
        PerkTable builtIn = PerkTable.defaults();
        assertEquals(builtIn.perkCount(), file.perkCount());
        for (int level = 1; level <= 12; level++) {
            for (String type : new String[]{Perk.TREASURY_CAP, Perk.MEMBER_CAP, Perk.SHOP_DISCOUNT, Perk.GUARD_COUNT, Perk.CONTRACT_BONUS, Perk.BOARD_SLOTS, Perk.ATTRIBUTE}) {
                assertEquals(builtIn.sum(type, level), file.sum(type, level), 1e-9, type + " at " + level);
            }
        }
        assertEquals(10, file.maxLevelWithPerks());
    }

    @Test
    void guardTagsExistAndNameVanillaOrOptionalIds() throws IOException {
        for (int t = 1; t <= 5; t++) {
            JsonObject tag = read(DATA.resolve("tags/entity_type/nation_guard_tier_" + t + ".json"));
            assertTrue(tag.getAsJsonArray("values").size() >= 1, "tier " + t + " tag is empty");
        }
        JsonObject all = read(DATA.resolve("tags/entity_type/nation_guard.json"));
        assertEquals(5, all.getAsJsonArray("values").size());
    }

    @Test
    void blockAssetsExist() {
        for (String f : new String[]{"assets/nusmp/blockstates/nation_banner.json", "assets/nusmp/blockstates/contract_board.json", "assets/nusmp/models/block/nation_banner.json",
                "assets/nusmp/models/block/contract_board.json", "assets/nusmp/models/item/nation_banner.json", "assets/nusmp/models/item/contract_board.json",
                "assets/nusmp/textures/block/nation_banner_cloth.png", "assets/nusmp/textures/block/nation_banner_pole.png", "assets/nusmp/textures/block/contract_board_side.png",
                "data/nusmp/loot_table/blocks/nation_banner.json", "data/nusmp/loot_table/blocks/contract_board.json"}) {
            assertTrue(Files.exists(Paths.get("src/main/resources").resolve(f)), "missing " + f);
        }
    }
}
