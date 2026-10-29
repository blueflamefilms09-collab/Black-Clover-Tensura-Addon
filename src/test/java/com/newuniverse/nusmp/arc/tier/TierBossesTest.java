package com.newuniverse.nusmp.arc.tier;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 3.33.0: the Tier List bosses of tiers F and D obey the house rules, stand on their floors, are all different and have their skins and names. */
class TierBossesTest {
    private static final Path RES = Path.of("src/main/resources/assets/nusmp");

    private static List<TierRow> all() {
        List<TierRow> out = new java.util.ArrayList<>(TierRosterF.rows());
        out.addAll(TierRosterD.rows());
        return out;
    }

    @Test
    void sixteenCharactersTheFirstBatch() {
        List<TierRow> all = all();
        assertEquals(6, TierRosterF.rows().size());
        assertEquals(10, TierRosterD.rows().size());
        assertEquals(16, all.size());
    }

    @Test
    void everyRowObeysTheHouseRules() {
        for (TierRow row : all()) assertEquals(List.of(), TierRules.problems(row), row.id());
    }

    @Test
    void idsAreUniqueAndTheNamesToo() {
        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (TierRow row : all()) {
            assertTrue(ids.add(row.id()), "repeated id " + row.id());
            assertTrue(names.add(row.character().name()), "repeated name " + row.character().name());
            assertTrue(row.id().matches("[a-z][a-z0-9_]*"), row.id());
        }
    }

    @Test
    void lowTiersStandOnTheEarlyFloors() {
        for (TierRow row : all()) {
            assertTrue(row.character().floorMin() >= 1 && row.character().floorMax() <= 20, row.id());
        }
    }

    @Test
    void theLadderOnlyGoesUp() {
        TierLetter[] l = TierLetter.values();
        for (int i = 1; i < l.length; i++) {
            assertTrue(l[i].health() > l[i - 1].health(), l[i].label());
            assertTrue(l[i].attack() >= l[i - 1].attack(), l[i].label());
            assertTrue(l[i].floorMax() >= l[i - 1].floorMax(), l[i].label());
        }
        assertEquals(TierLetter.TOP, TierLetter.fromLabel("SSS+++"));
        assertTrue(TierLetter.A.light() && !TierLetter.S.light());
    }

    /** 3.33.0: the tier bosses wear the mod's player-model kit like the other humanoids: a painted skin and a look in looks_tier.json. */
    @Test
    void everyCharacterHasItsPlayerModelSkinAndLook() throws Exception {
        String looks = Files.readString(RES.resolve("playermodel/looks_tier.json"));
        for (TierRow row : all()) {
            assertTrue(Files.isRegularFile(RES.resolve("textures/entity/playermodel/" + row.id() + ".png")), "missing player-model skin " + row.id() + ".png");
            assertTrue(looks.contains("\"" + row.id() + "\""), "no look for " + row.id());
        }
    }

    @Test
    void thePlayerModelKitDrawsExactlyTheseBosses() {
        assertEquals(TierRoster.ids().size(), all().size());
        for (String id : TierRoster.ids()) assertTrue(com.newuniverse.nusmp.client.playermodel.PlayerModelTable.ARC_BOSSES.contains(id), id);
        assertTrue(com.newuniverse.nusmp.client.playermodel.PlayerLookTables.FILES.contains("playermodel/looks_tier.json"));
    }

    /** 3.33.1: every tier boss, now and in every later tier, is in the anime skin style: the hair is painted on the skin, so a look never names a kit haircut. */
    @Test
    void everyTierLookIsInTheAnimeStyleWithPaintedHair() throws Exception {
        com.google.gson.JsonObject looks = com.google.gson.JsonParser.parseString(Files.readString(RES.resolve("playermodel/looks_tier.json"))).getAsJsonObject().getAsJsonObject("looks");
        for (TierRow row : all()) {
            assertTrue(looks.has(row.id()), "no look for " + row.id());
            assertFalse(looks.getAsJsonObject(row.id()).has("hair"), row.id() + " names a kit haircut; the hair is painted on the skin");
        }
    }

    /** The old shared-body recolours stay in the jar as the fallback if the kit is switched off. */
    @Test
    void theLegacySharedBodySkinsAreKept() {
        for (TierRow row : all()) assertTrue(Files.isRegularFile(RES.resolve("textures/entity/arc_" + row.id() + ".png")), "kept skin missing " + row.id());
    }

    @Test
    void everyCharacterHasANameInTheLanguageFile() throws Exception {
        String lang = Files.readString(RES.resolve("lang/en_us.json"));
        for (TierRow row : all()) assertTrue(lang.contains("\"entity.nusmp." + row.id() + "\""), "no name for " + row.id());
    }

    @Test
    void theRulesCatchABrokenRow() {
        TierMove cheap = TierMove.of("tap", "Tap", TierMove.Kind.CLEAVE).wind(4).cd(10).range(0, 3).dmg(9).build();
        assertFalse(TierRules.moveProblems("x", List.of(cheap)).isEmpty());
    }
}
