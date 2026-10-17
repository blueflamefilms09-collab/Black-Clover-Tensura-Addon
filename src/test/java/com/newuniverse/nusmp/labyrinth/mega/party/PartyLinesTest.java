package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.gen.CommandLines;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the headless CI smoke test READS what {@code /multiverse mega party ...} and the party-aware {@code status} and {@code expand} print, so the exact words are pinned here,
 * together with copies of the regular expressions the script (tools/ci/server_smoke.py, {@code phase_party}) uses; a drift of one word would turn a working feature into a failed
 * pipeline. Party 0 lines must stay what they always were, because the older phases of the script read them.
 */
public class PartyLinesTest {
    // copies of the script's regexes (Python and Java agree on these constructs)
    private static final Pattern HEAD = Pattern.compile("^party: mode=(\\w+) instanced=(true|false) maxParties=(\\d+) known=(\\d+) sectors=(\\d+)\\s*$", Pattern.MULTILINE);
    private static final Pattern ROW = Pattern.compile("^party (\\d+) (team|solo|unowned) key=(\\S+) name=(.*?) holy=(\\d+) abyssal=(\\d+) online=(\\d+)\\s*$", Pattern.MULTILINE);
    private static final Pattern QUEUED = Pattern.compile("queued sector (\\d+) on the holy road");
    private static final Pattern FLOOR_SECTOR = Pattern.compile("sector (\\d+) (\\w+) theme=(\\w+) .*? road=(\\w+) chain=(\\d+) floors=([+-]?\\d+)\\.\\.([+-]?\\d+)");

    @Test
    public void theHeaderIsTheLineTheScriptReads() {
        String line = PartyLines.header("party", true, 64, 3, 41);
        assertEquals("party: mode=party instanced=true maxParties=64 known=3 sectors=41", line);
        Matcher m = HEAD.matcher(line);
        assertTrue(m.find());
        assertEquals("party", m.group(1));
        assertEquals("true", m.group(2));
        assertEquals("64", m.group(3));
        assertEquals("3", m.group(4));
        assertEquals("41", m.group(5));
        assertTrue(HEAD.matcher(PartyLines.header("shared", false, 1, 0, 0)).find());
    }

    @Test
    public void aPartyRowCarriesEverythingTheScriptChecks() {
        String line = PartyLines.row(1, "unowned", null, null, 2, 1, 0);
        assertEquals("party 1 unowned key=none name=- holy=2 abyssal=1 online=0", line);
        Matcher m = ROW.matcher(line);
        assertTrue(m.find());
        assertEquals("1", m.group(1));
        assertEquals("unowned", m.group(2));
        assertEquals("2", m.group(5));
        assertEquals("1", m.group(6));
        String team = PartyLines.row(4, "team", "123e4567-e89b-12d3-a456-426614174000", "The Delvers Of Old", 12, 3, 2);
        Matcher t = ROW.matcher(team);
        assertTrue(t.find());
        assertEquals("The Delvers Of Old", t.group(4), "a name with spaces is read whole");
        assertEquals("123e4567-e89b-12d3-a456-426614174000", t.group(3));
        assertEquals("12", t.group(5));
        assertEquals("2", t.group(7));
    }

    @Test
    public void aNameNeverBreaksTheLine() {
        String line = PartyLines.row(2, "solo", "k", "evil\nname\r", 0, 0, 0);
        assertFalse(line.contains("\n") || line.contains("\r"));
        assertTrue(ROW.matcher(line).find());
        assertEquals("-", PartyLines.clean("   "));
        assertEquals("-", PartyLines.clean(null));
    }

    @Test
    public void theOtherLinesHaveTheirWords() {
        assertEquals("party of Dev: index=3 kind=solo key=k name=Dev where=party 3 floor +4", PartyLines.of("Dev", 3, "solo", "k", "Dev", "party 3 floor +4"));
        assertEquals("party of Dev: index=none kind=team key=k name=- where=outside", PartyLines.of("Dev", -1, "team", "k", "", "outside"));
        assertEquals("party set: Dev is in party 5 now (was 0, key k, solo)", PartyLines.set("Dev", 5, 0, "k", "solo"));
        assertEquals("party set: Dev is in party 5 now (was new, key k, team)", PartyLines.set("Dev", 5, -1, "k", "team"));
        assertEquals("party tp: party 2 abyssal floor -1 (sector 17)", PartyLines.tp(2, Polarity.ABYSSAL, -1, 17));
        assertEquals("party set: refused (party 99 is outside 0..63)", PartyLines.refused("set", "party 99 is outside 0..63"));
    }

    @Test
    public void partyZeroLinesAreUnchangedAndOtherPartiesAreMarked() {
        String base = "sector 3 ACTIVE theme=frost threat=1 rooms=28 edges=30 pieces=60 relics=3 region=0,-85,170,85 road=holy chain=0 floors=+1..+1";
        assertEquals(base, CommandLines.withParty(base, 0), "the shared chain's line is what it always was");
        assertEquals(base + " party=2", CommandLines.withParty(base, 2));
        Matcher m = FLOOR_SECTOR.matcher(CommandLines.withParty(base, 2));
        assertTrue(m.find(), "the script's sector-line regex still matches a line with the party at the end");
        assertEquals("3", m.group(1));
        assertEquals("holy", m.group(4));
        String queued = CommandLines.withParty(CommandLines.queuedOn("expand", 9, Polarity.HOLY, 1, 1), 1);
        assertEquals("expand: queued sector 9 on the holy road (floors +1..+1) party=1", queued);
        Matcher q = QUEUED.matcher(queued);
        assertTrue(q.find());
        assertEquals("9", q.group(1));
        assertEquals("expand: queued sector 9 on the holy road (floors +1..+1)", CommandLines.withParty(CommandLines.queuedOn("expand", 9, Polarity.HOLY, 1, 1), 0));
    }

    @Test
    public void theViewLineSaysWhichPartyIsShown() {
        assertEquals("party: showing party 1 (parties with floors: 0,1,4; status party <n> shows another)", CommandLines.partyView(1, List.of(0, 1, 4)));
        assertEquals("party: showing party 0 (parties with floors: none; status party <n> shows another)", CommandLines.partyView(0, List.of()));
        // the line must never be mistaken for a road header or a sector line by the older phases of the script
        String view = CommandLines.partyView(1, List.of(0, 1));
        assertFalse(Pattern.compile("road (holy|abyssal): sectors=").matcher(view).find());
        assertFalse(Pattern.compile("sector (\\d+) (\\w+) theme=").matcher(view).find());
        assertTrue(Pattern.compile("^party: showing party 1 ", Pattern.MULTILINE).matcher(view).find());
    }
}
