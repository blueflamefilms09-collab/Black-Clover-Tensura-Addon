package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRole;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: nothing of this cast can be seen in a game by its author, so the facts that must hold are checked as data: sixteen distinct friendly records with no
 * boss data and no floor, gated only on the five opening champions (a typo there would make a character wait for nobody for ever), a squad that follows the
 * series, and name colours that can be read on a dark chat.
 */
class KnightsCastTest {
    private static final Set<String> OPENERS = Set.of(KnightsGates.REVCHI, KnightsGates.HEATH, KnightsGates.LOTUS, KnightsGates.MARS, KnightsGates.VETTO);

    @Test
    void sixteenDistinctFriendlyCharacters() {
        List<ArcCharacter> all = KnightsCast.ALL;
        assertEquals(16, all.size());
        Set<String> ids = new HashSet<>();
        for (ArcCharacter c : all) {
            assertTrue(ids.add(c.id()), c.id() + " listed twice");
            assertEquals(ArcRole.NPC, c.role(), c.id() + " is a friend, not a boss");
            assertEquals(ArcSaga.ALLIES, c.saga(), c.id());
            assertEquals("Black Clover", c.source(), c.id());
            assertFalse(c.title().isEmpty(), c.id() + " needs an epithet");
            assertFalse(c.magic().isEmpty(), c.id() + " names the magic it is known for");
        }
    }

    @Test
    void aFriendHasNoBossData() {
        for (ArcCharacter c : KnightsCast.ALL) {
            assertEquals(0, c.floorMax(), c.id() + " must not stand on a floor");
            assertFalse(c.standsOn(10, Polarity.HOLY), c.id());
            assertFalse(c.standsOn(10, Polarity.ABYSSAL), c.id());
            assertFalse(c.hasCharter(), c.id() + " has no Charter Grimoire");
            assertEquals(0.0, c.health(), c.id() + " has no boss health");
        }
    }

    @Test
    void theGatesNameOnlyTheFiveOpeningChampions() {
        int gated = 0;
        for (ArcCharacter c : KnightsCast.ALL) {
            assertTrue(c.prerequisites().size() <= 1, c.id() + " waits for at most one champion");
            for (String p : c.prerequisites()) assertTrue(OPENERS.contains(p), c.id() + " waits for '" + p + "', which is not one of the five openers");
            if (!c.prerequisites().isEmpty()) gated++;
        }
        assertEquals(10, gated, "ten of the sixteen arrive as the story moves");
    }

    @Test
    void theCourtAndTheJuniorsWaitForNobody() {
        for (ArcCharacter c : List.of(CourtCast.JULIUS_NOVACHRONO, CourtCast.MARX_FRANCOIS, GoldenDawnCast.YUNO, GoldenDawnCast.MIMOSA_VERMILLION,
                GoldenDawnCast.KLAUS_LUNETTES, CrimsonLionCast.LEOPOLD_VERMILLION)) {
            assertTrue(c.prerequisites().isEmpty(), c.id() + " is in the Sanctuary from the start");
        }
    }

    @Test
    void theSeabedSiblingsArriveTogetherAfterVetto() {
        assertEquals(List.of(KnightsGates.VETTO), SeabedTempleCast.KAHONO.prerequisites());
        assertEquals(SeabedTempleCast.KAHONO.prerequisites(), SeabedTempleCast.KIATO.prerequisites());
        assertEquals(CaptainsCast.JACK_THE_RIPPER.prerequisites(), SilverEagleCast.NOZEL_SILVA.prerequisites(), "the second champion brings the Silver Eagle and the Green Mantis");
        assertEquals(SilverEagleCast.NOZEL_SILVA.prerequisites(), SilverEagleCast.SOLID_SILVA.prerequisites(), "the two Silvas arrive together");
    }

    @Test
    void theSquadsFollowTheSeriesNotTheOwnersFirstGuess() {
        assertTrue(CaptainsCast.JACK_THE_RIPPER.title().contains("Green Mantis"), "Jack leads the Green Mantis");
        assertTrue(CaptainsCast.DOROTHY_UNSWORTH.title().contains("Coral Peacock"), "Dorothy leads the Coral Peacock");
        assertTrue(CaptainsCast.CHARLOTTE_ROSELEI.title().contains("Blue Rose"));
        assertTrue(SeabedTempleCast.KAHONO.title().contains("Seabed Temple"), "Kahono is of the Seabed Temple, not a Magic Knight");
        assertTrue(SeabedTempleCast.KIATO.title().contains("Seabed Temple"));
    }

    @Test
    void coloursAreReadableInChatAndDistinctFromEachOther() {
        Set<Integer> mains = new HashSet<>();
        for (ArcCharacter c : KnightsCast.ALL) {
            int main = c.color() & 0xFFFFFF, accent = c.accent() & 0xFFFFFF;
            assertTrue(brightest(main) >= 0x80, c.id() + " name colour is too dark to read on a dark chat");
            assertTrue(brightest(accent) >= 0x80, c.id() + " accent is too dark");
            assertNotEquals(main, accent);
            assertTrue(mains.add(main), c.id() + " shares its colour with somebody else");
        }
    }

    @Test
    void registerAllFillsTheRosterOnceAndIsHarmlessTwice() {
        ArcRoster.clearForTests();
        try {
            KnightsCast.registerAll();
            KnightsCast.registerAll();
            assertEquals(16, ArcRoster.all().size());
            assertEquals(16, ArcRoster.npcs().size());
            assertEquals(0, ArcRoster.bosses().size());
            assertSame(CourtCast.JULIUS_NOVACHRONO, ArcRoster.byId("julius_novachrono"));
            assertEquals(List.of(), ArcRoster.prerequisitesOf(SeabedTempleCast.KIATO), "a gate whose champion is not in the roster is skipped, not an error");
        } finally {
            ArcRoster.clearForTests();
        }
    }

    private static int brightest(int rgb) {
        return Math.max((rgb >> 16) & 0xFF, Math.max((rgb >> 8) & 0xFF, rgb & 0xFF));
    }
}
