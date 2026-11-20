package com.newuniverse.nusmp.devil.phase2;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.WorldDevil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the EP table and the per-devil data of phase 2 are a data file (assets/anime_multiverse/devil_phase2.json); this pins that it parses, that it says what the doc says, and that a damaged file can not break the rules. Pure; the working directory is the project root. */
class Phase2TableTest {
    private static final Path FILE = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/anime_multiverse/devil_phase2.json");

    private static Phase2Table shipped() throws IOException {
        return Phase2Table.parse(JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject());
    }

    @Test
    void theShippedTableSaysSupremeIsAboutOneBillionAndEachRankIsATenthLess() throws IOException {
        Phase2Table t = shipped();
        assertEquals(1.0E9, t.tierEp(DevilTier.SUPREME), 1);
        assertEquals(1.0E8, t.tierEp(DevilTier.HIGH), 1);
        assertEquals(1.0E7, t.tierEp(DevilTier.MID), 1);
        assertEquals(1.0E6, t.tierEp(DevilTier.LOW), 1);
        assertTrue(t.tierChance(DevilTier.SUPREME) < t.tierChance(DevilTier.HIGH) && t.tierChance(DevilTier.HIGH) < t.tierChance(DevilTier.MID) && t.tierChance(DevilTier.MID) < t.tierChance(DevilTier.LOW), "a higher rank is harder to call");
        assertEquals(0.35, t.relicBonus(), 1e-9);
        assertTrue(t.summonChance(DevilTier.SUPREME, true) > t.summonChance(DevilTier.SUPREME, false), "holding a relic raises the chance");
    }

    @Test
    void theShippedTableAgreesWithTheDefaultsInCode() throws IOException {
        Phase2Table file = shipped(), code = Phase2Table.defaults();
        for (DevilTier t : DevilTier.values()) {
            assertEquals(code.tierEp(t), file.tierEp(t), 1, t + " EP");
            assertEquals(code.tierChance(t), file.tierChance(t), 1e-9, t + " chance");
        }
    }

    @Test
    void everyDevilNamedInTheFileIsInTheRosterAndTheFightersAreTheRightOnes() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
        for (String id : root.getAsJsonObject("devils").keySet()) assertTrue(WorldDevil.byId(id) != null, id + " is not in the roster");
        Phase2Table t = shipped();
        assertTrue(t.devil("liebe").trial(), "Liebe fought Asta before the contract");
        assertTrue(t.devil("lucifero").betray() > 0, "Lucifero turned on the circle in the anime");
        assertFalse(t.devil("sebastian").trial());
        assertFalse(t.devil("not_a_devil").trial());
        assertEquals(0.0, t.devil("not_a_devil").betray(), 0);
    }

    @Test
    void aDamagedFileCanNotBreakTheRules() {
        assertEquals(1.0E9, Phase2Table.parse(null).tierEp(DevilTier.SUPREME), 1, "no file reads as the defaults");
        JsonObject bad = JsonParser.parseString("{\"ep\":{\"supreme\":-5,\"high\":\"many\"},\"chance\":{\"low\":7},\"devils\":{\"x\":{\"trial\":\"yes\",\"betray\":9}},\"relicBonus\":5}").getAsJsonObject();
        Phase2Table t = Phase2Table.parse(bad);
        assertEquals(0.0, t.tierEp(DevilTier.SUPREME), 0, "a negative EP clamps to 0");
        assertEquals(1.0E8, t.tierEp(DevilTier.HIGH), 1, "a text where a number belongs keeps the default");
        assertEquals(1.0, t.tierChance(DevilTier.LOW), 0, "a chance over 1 clamps to 1");
        assertEquals(1.0, t.devil("x").betray(), 0);
        assertFalse(t.devil("x").trial(), "only a real true is a trial");
        assertEquals(1.0, t.relicBonus(), 0);
    }
}
