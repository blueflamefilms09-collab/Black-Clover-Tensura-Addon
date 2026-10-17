package com.newuniverse.nusmp.arc.npc;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the fourteen Black Bulls are plain records, and everything that goes wrong with them is a typo that no compiler sees: an id that does not match its
 * dialogue file, a magic id that is not a magic, a gate that names a boss nobody wrote, a colour that vanishes on the dark chat. These tests pin the list, the
 * order they arrive in, the colours, and the magic ids (read from the source of {@code MagicType}, which cannot be loaded here because it imports the game).
 */
class BlackBullsNpcsTest {
    private static final List<String> IDS = List.of("yami_sukehiro", "asta", "noelle_silva", "finral_roulacase", "magna_swing", "luck_voltia", "charmy_pappitson",
            "vanessa_enoteca", "gordon_agrippa", "zora_ideale", "gauche_adlai", "grey", "secre_swallowtail", "nacht_faust");
    private static final Set<String> GATES = Set.of(BlackBullsNpcs.GATE_1, BlackBullsNpcs.GATE_2, BlackBullsNpcs.GATE_3, BlackBullsNpcs.GATE_4);
    private static final Path MAGIC_TYPE = Path.of("src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java");

    @Test
    void theFourteenAreListedInTheOrderTheyArrive() {
        List<String> got = new ArrayList<>();
        for (ArcCharacter c : BlackBullsNpcs.all()) got.add(c.id());
        assertEquals(IDS, got);
        assertEquals(14, new HashSet<>(got).size(), "every id once");
    }

    @Test
    void everyRecordIsAFriendWithoutBossData() {
        for (ArcCharacter c : BlackBullsNpcs.all()) {
            assertEquals(ArcRole.NPC, c.role(), c.id());
            assertTrue(c.isNpc() && !c.isBoss(), c.id());
            assertEquals(ArcSaga.ALLIES, c.saga(), c.id());
            assertEquals("Black Clover", c.source(), c.id());
            assertFalse(c.title().isEmpty(), c.id() + " has a title");
            assertFalse(c.magic().isEmpty(), c.id() + " has a magic");
            assertEquals(0.0, c.health(), c.id() + " health");
            assertEquals(0.0, c.attack(), c.id() + " attack");
            assertFalse(c.hasCharter(), c.id() + " has no Charter Grimoire");
            assertTrue(c.charterSkills().isEmpty() && c.themes().isEmpty(), c.id());
            assertTrue(c.arena().isEmpty() && c.opening().isEmpty() && c.gates().isEmpty() && c.death().isEmpty(), c.id() + " has no boss lines");
            assertFalse(c.standsOn(10, Polarity.HOLY) || c.standsOn(-10, Polarity.ABYSSAL), c.id() + " never stands on a floor");
        }
    }

    @Test
    void theMagicIdsAreRealMagicTypes() throws IOException {
        String source = Files.readString(MAGIC_TYPE, StandardCharsets.UTF_8);
        Set<String> types = new HashSet<>();
        Matcher m = Pattern.compile("(?m)^\\s{4}([A-Z][A-Z_]+)\\(").matcher(source);
        while (m.find()) types.add(m.group(1));
        assertTrue(types.contains("FLAME") && types.contains("ANTI_MAGIC") && types.contains("WING"), "the pattern reads the enum constants: " + types.size());
        for (ArcCharacter c : BlackBullsNpcs.all()) {
            assertEquals(c.magic(), c.magic().toLowerCase(java.util.Locale.ROOT), c.id() + " magic id is lower case");
            assertTrue(types.contains(c.magic().toUpperCase(java.util.Locale.ROOT)), c.id() + " names a magic that does not exist: " + c.magic());
        }
    }

    @Test
    void coloursStayReadableOnADarkChat() {
        for (ArcCharacter c : BlackBullsNpcs.all()) {
            assertTrue(brightness(c.color()) >= 0.40, c.id() + " main colour is too dark for the chat: " + Integer.toHexString(c.color()));
            assertTrue(brightness(c.accent()) >= 0.40, c.id() + " accent is too dark for the chat: " + Integer.toHexString(c.accent()));
            assertTrue(c.color() >= 0 && c.color() <= 0xFFFFFF && c.accent() >= 0 && c.accent() <= 0xFFFFFF, c.id() + " colours are 0xRRGGBB");
            assertNotEquals(c.color(), c.accent());
        }
    }

    private static double brightness(int rgb) {
        return (0.2126 * ((rgb >> 16) & 255) + 0.7152 * ((rgb >> 8) & 255) + 0.0722 * (rgb & 255)) / 255.0;
    }

    @Test
    void theSquadGathersInFourWaves() {
        int[] perWave = new int[5];
        int lastWave = 0;
        for (ArcCharacter c : BlackBullsNpcs.all()) {
            int wave = waveOf(c);
            assertTrue(wave >= lastWave, c.id() + " arrives before a friend listed earlier");
            lastWave = wave;
            perWave[wave]++;
            assertFalse(c.prerequisites().contains(c.id()), c.id() + " cannot wait for itself");
            for (String p : c.prerequisites()) {
                assertTrue(GATES.contains(p), c.id() + " waits for " + p + ", which is not an early boss of the gate list");
                assertFalse(IDS.contains(p), c.id() + " waits for a friend");
            }
        }
        assertEquals(4, perWave[0], "Yami, Asta, Noelle and Finral stand there from the first day");
        assertEquals(3, perWave[1], "Magna, Luck, Charmy");
        assertEquals(3, perWave[2], "Vanessa, Gordon, Zora");
        assertEquals(2, perWave[3], "Gauche, Grey");
        assertEquals(2, perWave[4], "Secre, Nacht");
    }

    private static int waveOf(ArcCharacter c) {
        if (c.prerequisites().isEmpty()) return 0;
        assertEquals(1, c.prerequisites().size(), c.id() + " waits for exactly one boss");
        String gate = c.prerequisites().get(0);
        if (gate.equals(BlackBullsNpcs.GATE_1)) return 1;
        if (gate.equals(BlackBullsNpcs.GATE_2)) return 2;
        if (gate.equals(BlackBullsNpcs.GATE_3)) return 3;
        return 4;
    }

    @Test
    void registeringTwiceLeavesOneCopyOfEach() {
        ArcRoster.clearForTests();
        try {
            BlackBullsNpcs.registerAll();
            BlackBullsNpcs.registerAll();
            assertEquals(14, ArcRoster.all().size());
            assertEquals(14, ArcRoster.npcs().size());
            assertEquals(0, ArcRoster.bosses().size());
            assertSame(BlackBullsNpcs.ASTA, ArcRoster.byId("ASTA"));
            assertEquals(IDS.get(0), ArcRoster.all().get(0).id(), "the roster keeps the arrival order");
            assertEquals(0, ArcRoster.floorBosses(10, Polarity.HOLY).size(), "friends are never floor bosses");
            assertEquals(0, ArcRoster.prerequisitesOf(BlackBullsNpcs.SECRE).size(), "a gate that is not in the roster is skipped");
            ArcCharacter vetto = ArcRoster.register(ArcCharacter.boss("vetto", "Vetto").build());
            assertEquals(List.of(vetto), ArcRoster.prerequisitesOf(BlackBullsNpcs.SECRE), "once the boss exists the gate is real");
        } finally {
            ArcRoster.clearForTests();
        }
    }

    @Test
    void aBossOfTheSameIdIsRefusedNotOverwritten() {
        ArcRoster.clearForTests();
        try {
            ArcRoster.register(ArcCharacter.boss("asta", "Someone Else").build());
            boolean threw = false;
            try {
                BlackBullsNpcs.registerAll();
            } catch (IllegalStateException e) {
                threw = true;
            }
            assertTrue(threw, "a different character under a taken id must stop the boot, not replace it");
        } finally {
            ArcRoster.clearForTests();
        }
    }
}
