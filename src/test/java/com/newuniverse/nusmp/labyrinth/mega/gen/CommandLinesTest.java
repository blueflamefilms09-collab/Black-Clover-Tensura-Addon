package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void noLineContainsANewlineSoOneLineIsOneMessage() {
        List<String> all = List.of(CommandLines.status(1, 1, 0.5f), CommandLines.sector(0, "ACTIVE", "x", 1, 1, 1, 1, 1, 0, 0, 1, 1), CommandLines.audit(0, List.of("a", "b")),
                CommandLines.queued("expand", 1), CommandLines.refused("expand", "x"), CommandLines.plan("p"), CommandLines.roomKind(RoomKind.HALL, 3));
        for (String s : all) assertFalse(s.contains("\n"), s);
    }
}
