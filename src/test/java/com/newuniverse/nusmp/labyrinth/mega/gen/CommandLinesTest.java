package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the CI smoke test parses these lines, so the exact text is a contract with a script nobody can run here. Each test writes the expected line out
 * longhand (never built from the same pieces as the code) and compares it character for character.
 */
class CommandLinesTest {
    @Test
    void theStatusHeaderNamesTheLevelTheSectorCountAndTheJob() {
        assertEquals("mega: expansionLevel=2 sectors=3 job=idle", CommandLines.status(2, 3, -1f));
        assertEquals("mega: expansionLevel=0 sectors=1 job=0%", CommandLines.status(0, 1, 0f));
        assertEquals("mega: expansionLevel=0 sectors=1 job=43%", CommandLines.status(0, 1, 0.43f));
        assertEquals("mega: expansionLevel=0 sectors=1 job=100%", CommandLines.status(0, 1, 1f));
    }

    @Test
    void aSectorLineHasEveryFieldInTheAgreedOrder() {
        assertEquals("sector 4 ACTIVE theme=gravity threat=5 rooms=61 edges=70 pieces=118 relics=2 region=-90,0,80,170",
                CommandLines.sector(4, "ACTIVE", "gravity", 5, 61, 70, 118, 2, -90, 0, 80, 170));
    }

    @Test
    void theAuditLineIsOkOrAFailListJoinedWithSemicolons() {
        assertEquals("audit 3 OK", CommandLines.audit(3, List.of()));
        assertEquals("audit 3 FAIL: the sector has no rooms", CommandLines.audit(3, List.of("the sector has no rooms")));
        assertEquals("audit 0 FAIL: a b; c d; e", CommandLines.audit(0, List.of("a b", "c d", "e")));
    }

    @Test
    void queuedAndRefusedLinesAreOneLineEach() {
        assertEquals("expand: queued sector 7", CommandLines.queued("expand", 7));
        assertEquals("expand: refused (sector 2 is still generating)", CommandLines.refused("expand", "sector 2 is still generating"));
        assertEquals("regen: queued sector 0", CommandLines.queued("regen", 0));
        assertEquals("plan: /srv/mega_plan_1.json", CommandLines.plan("/srv/mega_plan_1.json"));
    }

    @Test
    void roomKindsAreLowerCaseWithTheEnumSpelling() {
        assertEquals("landing 1", CommandLines.roomKind(RoomKind.LANDING, 1));
        assertEquals("set_piece 0", CommandLines.roomKind(RoomKind.SET_PIECE, 0));
        assertEquals("antechamber 1", CommandLines.roomKind(RoomKind.ANTECHAMBER, 1));
    }

    @Test
    void percentIsRoundedAndClamped() {
        assertEquals(0, CommandLines.percent(-0.5f));
        assertEquals(50, CommandLines.percent(0.499f));
        assertEquals(100, CommandLines.percent(7f));
    }

    /** The regex of tools/ci/server_smoke.py that reads a sector line (a copy: the script is the contract). */
    private static final Pattern SMOKE_SECTOR = Pattern.compile("sector (\\d+) (\\w+) theme=(\\w+) threat=(\\d+) rooms=(\\d+) edges=(\\d+) pieces=(\\d+) relics=(\\d+) region=(-?\\d+),(-?\\d+),(-?\\d+),(-?\\d+)");

    @Test
    void aSectorLineCarriesItsRoadAndFloorsAfterTheWordsTheScriptReads() {
        String base = CommandLines.sector(4, "ACTIVE", "gravity", 5, 61, 70, 118, 2, -90, 0, 80, 170);
        assertEquals(base + " road=holy chain=0 floors=+1..+10", CommandLines.sectorOnRoad(base, Polarity.HOLY, 0, 1, 10));
        assertEquals("sector 7 ACTIVE theme=abyss threat=21 rooms=80 edges=90 pieces=120 relics=3 region=0,299915,170,300085 road=abyssal chain=2 floors=-21..-30",
                CommandLines.sectorOnRoad(CommandLines.sector(7, "ACTIVE", "abyss", 21, 80, 90, 120, 3, 0, 299915, 170, 300085), Polarity.ABYSSAL, 2, -21, -30));
        Matcher m = SMOKE_SECTOR.matcher(CommandLines.sectorOnRoad(base, Polarity.ABYSSAL, 3, -31, -40));
        assertTrue(m.find(), "the 1.54.0 regex of the smoke script still reads the line");
        assertEquals("4", m.group(1));
        assertEquals("80", m.group(11));
    }

    @Test
    void aRoadHeaderListsItsSectorsFloorsAndTheStateOfItsNewestSector() {
        assertEquals("road holy: sectors=3 ids=0,2,4 floors=+1..+30 state=ACTIVE", CommandLines.road(Polarity.HOLY, List.of(0, 2, 4), 1, 30, "ACTIVE"));
        assertEquals("road abyssal: sectors=1 ids=1 floors=-1..-10 state=GENERATING", CommandLines.road(Polarity.ABYSSAL, List.of(1), -1, -10, "GENERATING"));
        assertEquals("road abyssal: not opened", CommandLines.road(Polarity.ABYSSAL, List.of(), 0, 0, ""));
        for (String header : List.of(CommandLines.road(Polarity.HOLY, List.of(0, 2), 1, 20, "ACTIVE"), CommandLines.road(Polarity.ABYSSAL, List.of(), 0, 0, ""))) {
            assertFalse(SMOKE_SECTOR.matcher(header).find(), "a road header must not be read as a sector line: " + header);
        }
    }

    @Test
    void anExpansionNamesTheRoadAndFloorsItGrows() {
        assertEquals("expand: queued sector 3 on the abyssal road (floors -1..-10)", CommandLines.queuedOn("expand", 3, Polarity.ABYSSAL, -1, -10));
        assertEquals("expand: queued sector 2 on the holy road (floors +11..+20)", CommandLines.queuedOn("expand", 2, Polarity.HOLY, 11, 20));
        assertTrue(CommandLines.queuedOn("expand", 3, Polarity.HOLY, 1, 10).startsWith(CommandLines.queued("expand", 3)), "the script still finds 'queued sector 3'");
    }

    @Test
    void noLineContainsANewlineSoOneLineIsOneMessage() {
        List<String> all = List.of(CommandLines.status(1, 1, 0.5f), CommandLines.sector(0, "ACTIVE", "x", 1, 1, 1, 1, 1, 0, 0, 1, 1), CommandLines.audit(0, List.of("a", "b")),
                CommandLines.queued("expand", 1), CommandLines.refused("expand", "x"), CommandLines.plan("p"), CommandLines.roomKind(RoomKind.HALL, 3),
                CommandLines.road(Polarity.HOLY, List.of(0, 1), 1, 20, "ACTIVE"), CommandLines.queuedOn("expand", 1, Polarity.ABYSSAL, -1, -10),
                CommandLines.sectorOnRoad("sector 0 ACTIVE", Polarity.HOLY, 0, 1, 10));
        for (String s : all) assertFalse(s.contains("\n"), s);
    }
}
