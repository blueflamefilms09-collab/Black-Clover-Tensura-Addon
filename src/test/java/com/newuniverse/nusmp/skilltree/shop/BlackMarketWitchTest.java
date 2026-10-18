package com.newuniverse.nusmp.skilltree.shop;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRole;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the witch is one plain record plus four files that must agree with it by NAME (the dialogue file, the model's geo, animation and textures, the model spec), and a
 * mismatch is silent in the game (a mute NPC, a villager standing in her place, a shop that nobody can open). These tests pin the record to the owner's brief (an NPC of the saga
 * Allies who waits for the first boss of the roster), check her magic and her colours the way the Black Bulls' are checked, prove that registering her twice leaves one copy and that a
 * boss cannot take her id, and check that every asset the id promises exists.
 */
class BlackMarketWitchTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path DATA = Path.of("src/main/resources/data/nusmp");
    private static final Path MAGIC_TYPE = Path.of("src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java");

    @Test
    void sheIsAnNpcOfTheAlliesWhoWaitsForTheFirstBoss() {
        ArcCharacter c = BlackMarketWitch.CHARACTER;
        assertEquals("black_market_witch", c.id());
        assertEquals(BlackMarketWitch.ID, c.id());
        assertEquals(ArcRole.NPC, c.role());
        assertTrue(c.isNpc() && !c.isBoss());
        assertEquals(ArcSaga.ALLIES, c.saga());
        assertEquals(List.of("revchi_salik"), c.prerequisites(), "the shop is hidden until somebody has beaten the first boss of the roster");
        assertEquals(BlackMarketWitch.GATE, c.prerequisites().get(0));
        assertFalse(c.name().isBlank());
        assertFalse(c.title().isBlank());
        assertEquals("Old Witch", c.name(), "the owner's text gives her no name, so none is invented");
    }

    @Test
    void sheCarriesNoBossData() {
        ArcCharacter c = BlackMarketWitch.CHARACTER;
        assertEquals(0.0, c.health(), 0.0);
        assertEquals(0.0, c.attack(), 0.0);
        assertFalse(c.hasCharter());
        assertTrue(c.charterSkills().isEmpty() && c.themes().isEmpty());
        assertTrue(c.arena().isEmpty() && c.opening().isEmpty() && c.gates().isEmpty() && c.death().isEmpty());
        assertFalse(c.standsOn(10, Polarity.HOLY) || c.standsOn(-10, Polarity.ABYSSAL), "a friend is never a floor boss");
    }

    @Test
    void herMagicIsARealMagicType() throws IOException {
        String source = Files.readString(MAGIC_TYPE, StandardCharsets.UTF_8);
        Set<String> types = new HashSet<>();
        Matcher m = Pattern.compile("(?m)^\\s{4}([A-Z][A-Z_]+)\\(").matcher(source);
        while (m.find()) types.add(m.group(1));
        assertTrue(types.contains("CURSE") && types.contains("FLAME"), "the pattern reads the enum constants: " + types.size());
        assertEquals("curse", BlackMarketWitch.CHARACTER.magic());
        assertTrue(types.contains(BlackMarketWitch.CHARACTER.magic().toUpperCase(java.util.Locale.ROOT)));
    }

    @Test
    void herColoursReadOnADarkChat() {
        ArcCharacter c = BlackMarketWitch.CHARACTER;
        for (int rgb : new int[]{c.color(), c.accent()}) {
            assertTrue(rgb >= 0 && rgb <= 0xFFFFFF, "0xRRGGBB");
            double brightness = (0.2126 * ((rgb >> 16) & 255) + 0.7152 * ((rgb >> 8) & 255) + 0.0722 * (rgb & 255)) / 255.0;
            assertTrue(brightness >= 0.40, "too dark for the chat: " + Integer.toHexString(rgb));
        }
        assertTrue(c.color() != c.accent());
    }

    @Test
    void registeringTwiceLeavesOneCopyAndABossCannotTakeHerId() {
        ArcRoster.clearForTests();
        try {
            BlackMarketWitch.registerAll();
            BlackMarketWitch.registerAll();
            assertEquals(1, ArcRoster.all().size());
            assertSame(BlackMarketWitch.CHARACTER, ArcRoster.byId("BLACK_MARKET_WITCH"));
            assertEquals(1, ArcRoster.npcs().size());
            assertEquals(0, ArcRoster.prerequisitesOf(BlackMarketWitch.CHARACTER).size(), "a gate boss that is not registered is skipped, never a lock");
            ArcCharacter revchi = ArcRoster.register(ArcCharacter.boss("revchi_salik", "Revchi Salik").build());
            assertEquals(List.of(revchi), ArcRoster.prerequisitesOf(BlackMarketWitch.CHARACTER));
        } finally {
            ArcRoster.clearForTests();
        }
        ArcRoster.clearForTests();
        try {
            ArcRoster.register(ArcCharacter.boss("black_market_witch", "Someone Else").build());
            boolean threw = false;
            try {
                BlackMarketWitch.registerAll();
            } catch (IllegalStateException e) {
                threw = true;
            }
            assertTrue(threw, "a different character under her id must stop the boot, not replace her");
        } finally {
            ArcRoster.clearForTests();
        }
    }

    @Test
    void theGateNamesABossTheEarlySagaDefines() throws IOException {
        String early = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/early/EarlyRoster.java"), StandardCharsets.UTF_8);
        assertTrue(early.contains("ArcCharacter.boss(\"" + BlackMarketWitch.GATE + "\""), "the gate boss exists in the early roster");
    }

    @Test
    void everyAssetHerIdPromisesExists() {
        String id = BlackMarketWitch.ID;
        for (Path p : List.of(ASSETS.resolve("geo/entity/arc_" + id + ".geo.json"), ASSETS.resolve("animations/entity/arc_" + id + ".animation.json"),
                ASSETS.resolve("textures/entity/arc_" + id + ".png"), ASSETS.resolve("textures/entity/arc_" + id + "_glow.png"),
                DATA.resolve("arc/dialogue/" + id + ".json"), Path.of("tools/arc/char_" + id + ".py"), Path.of("tools/render_preview/scenes.d/arc_" + id + ".json"),
                ASSETS.resolve("textures/gui/black_market/witch.png"), ASSETS.resolve("textures/gui/black_market/cobwebs.png"))) {
            assertTrue(Files.isRegularFile(p), "missing " + p);
        }
    }

    @Test
    void theModelSpecIsForHerAndAnNpc() throws IOException {
        String spec = Files.readString(Path.of("tools/arc/char_black_market_witch.py"), StandardCharsets.UTF_8);
        assertTrue(spec.contains("\"id\": \"" + BlackMarketWitch.ID + "\""));
        assertTrue(spec.contains("\"role\": \"npc\""));
        assertTrue(spec.contains("\"name\": \"" + BlackMarketWitch.CHARACTER.name() + "\""));
        assertNotNull(spec);
    }
}
