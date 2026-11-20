package com.newuniverse.nusmp.tooltip;

import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.beastwill.BeastRank;
import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.labyrinth.loot.LootRarity;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.RiftTier;
import com.newuniverse.nusmp.multiverse.KnightRank;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Rankings;
import com.newuniverse.nusmp.saiyan.SaiyanRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner wants every rank or rarity of the mod shown the way the relics show theirs (stars, grade name, place; docs/rank_audit.md). The old enums stay (saves), so this checks the one table that
 * maps each old rank to a relic grade ({@code rankings.json "legacy"}): every constant of every old enum has an entry, a higher old rank never maps to a lower grade, the card text is the relic card's
 * (stars, two spaces, label), the classic switch exists and is registered with its own file name, and every place that draws one of these ranks goes through {@link RankCards}. Reads the repo's files.
 */
class RankCardsTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static void everyConstantIsMappedAndOrdered(String kind, Enum<?>[] all, String skip) {
        int last = -1;
        for (Enum<?> c : all) {
            if (c.name().equals(skip)) continue;
            Rankings.Rank r = Rankings.legacy(kind, c.name());
            assertNotNull(r, kind + "." + c.name() + " has no relic grade in rankings.json legacy");
            assertTrue(r.grade().ordinal() >= last, kind + "." + c.name() + " maps below the rank before it");
            last = r.grade().ordinal();
        }
    }

    @Test
    void everyOldRankMapsToARelicGradeInOrder() {
        everyConstantIsMappedAndOrdered("beast", BeastRank.values(), null);
        everyConstantIsMappedAndOrdered("devil", DevilTier.values(), "NONE");
        everyConstantIsMappedAndOrdered("saiyan", SaiyanRules.Rank.values(), null);
        everyConstantIsMappedAndOrdered("knight", KnightRank.values(), null);
        everyConstantIsMappedAndOrdered("loot", LootRarity.values(), null);
        everyConstantIsMappedAndOrdered("aptitude", Aptitude.Rarity.values(), null);
        everyConstantIsMappedAndOrdered("rift", RiftTier.values(), null);
    }

    @Test
    void everyManaCoreRankLetterIsMappedInOrder() {
        int last = -1;
        for (String letter : com.newuniverse.nusmp.manacore.ManaCoreRules.RANK_LABEL) {
            Rankings.Rank r = Rankings.legacy("mana", letter);
            assertNotNull(r, "mana." + letter);
            assertTrue(r.grade().ordinal() >= last, "mana." + letter + " maps below the rank before it");
            last = r.grade().ordinal();
        }
        assertEquals(Rankings.legacy("beast", "SS").grade(), Rankings.legacy("mana", "SS").grade(), "the same letter is the same grade in both systems");
        assertEquals(Rankings.legacy("beast", "A").grade(), Rankings.legacy("mana", "A").grade());
    }

    @Test
    void theCeremonyTiersAreMappedToo() throws IOException {
        String src = Files.readString(SRC.resolve("multiverse/Ceremony.java"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("enum Tier \\{([^}]*)\\}").matcher(src);
        assertTrue(m.find(), "Ceremony.Tier moved");
        for (String name : m.group(1).split(",")) assertNotNull(Rankings.legacy("ceremony", name.trim()), "ceremony." + name.trim());
    }

    @Test
    void theNamedExamplesAreRight() {
        assertEquals(Grade.GOD, Rankings.legacy("beast", "SS").grade(), "an SS beast is God Grade");
        assertEquals(Grade.UNIQUE, Rankings.legacy("beast", "A").grade());
        assertEquals(Grade.GOD, Rankings.legacy("devil", "SUPREME").grade());
        assertEquals(Grade.GENESIS, Rankings.legacy("knight", "WIZARD_KING").grade());
        assertEquals(Grade.GOD, Rankings.legacy("BEAST", "ss").grade(), "the lookup ignores case");
    }

    @Test
    void theCardIsTheStarsAndTheLabelLikeTheRelicCard() {
        Rankings.Rank r = new Rankings.Rank(Grade.GOD, 4, 9, "item");
        assertEquals(Grade.GOD.stars() + "  God Grade #4", r.card());
        assertEquals(Grade.GOD.stars() + "  " + Grade.GOD.label(), Rankings.legacy("beast", "SS").card());
        assertNotNull(Rankings.table().legacy());
        assertFalse(Rankings.table().legacy().isEmpty());
    }

    @Test
    void anUnknownOrBrokenLegacyEntryIsSkippedNeverThrown() {
        Rankings.Table t = Rankings.parse("{\"legacy\":{\"x\":{\"A\":\"nope\",\"B\":\"rare:2\"},\"_doc\":\"text\",\"y\":5}}");
        assertEquals(null, t.legacy("x", "A"));
        assertEquals(Grade.RARE, t.legacy("x", "B").grade());
        assertEquals(2, t.legacy("x", "B").place());
    }

    @Test
    void theClassicSwitchIsOffByDefaultAndRegisteredUnderItsOwnFile() throws IOException {
        assertFalse(RankDisplayConfig.classic(), "an unloaded config is the new look");
        String main = Files.readString(SRC.resolve("NUSMP.java"), StandardCharsets.UTF_8);
        assertTrue(main.contains("RankDisplayConfig.SPEC") && main.contains("nusmp-rank-display-client.toml"));
        String cfg = Files.readString(SRC.resolve("tooltip/RankDisplayConfig.java"), StandardCharsets.UTF_8);
        assertTrue(cfg.contains("rankDisplayClassic") && cfg.contains("define(\"rankDisplayClassic\", false)"));
    }

    @Test
    void everyPlaceThatDrawsAnOldRankAsksTheCardHelper() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String f : new String[]{"beastwill/BeastWillCoreItem.java", "client/devil/DevilClient.java", "client/saiyan/SaiyanClient.java", "labyrinth/loot/GearTooltip.java",
                "client/avatar/AptitudesPage.java", "client/multiverse/MultiverseStatusScreen.java", "manacore/RankCoreItem.java"}) {
            if (!Files.readString(SRC.resolve(f), StandardCharsets.UTF_8).contains("RankCards.")) missing.add(f);
        }
        assertTrue(missing.isEmpty(), "these still show an old rank as plain text only: " + missing);
        String tip = Files.readString(SRC.resolve("tooltip/RankTooltip.java"), StandardCharsets.UTF_8);
        assertTrue(tip.contains("r.card()") && tip.contains("RankDisplayConfig.classic()"), "the item rank line is the relic card unless classic");
    }

    @Test
    void everyOldRankDisplayedInTheAuditExists() throws IOException {
        String audit = Files.readString(Path.of("docs/rank_audit.md"), StandardCharsets.UTF_8);
        for (String must : new String[]{"BeastRank", "DevilTier", "SaiyanRules.Rank", "KnightRank", "LootRarity", "Aptitude.Rarity", "RiftTier", "Ceremony.Tier", "SpiritDiveTier"}) {
            assertTrue(audit.contains(must), "docs/rank_audit.md does not list " + must);
        }
    }
}
