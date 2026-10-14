package com.newuniverse.nusmp.charter;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The seven Charter Grimoires as data: complete, capped, only real Tensura skills, nothing destructive, and the isolation guard. */
class CharterTableTest {
    private static Set<String> tensuraSkills() throws IOException {
        Set<String> out = new HashSet<>();
        try (InputStream in = CharterTableTest.class.getResourceAsStream("tensura_skill_ids.txt")) {
            assertNotNull(in, "the id list is a test resource");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) if (!line.isBlank() && !line.startsWith("#")) out.add("tensura:" + line.trim());
        }
        return out;
    }

    @Test
    void sevenBossesWithTheStableIds() {
        assertEquals(List.of("dante", "vanica", "zenon", "lucius", "devil_avatar", "goliath", "charybdis_spawn"), CharterTable.ids());
        assertEquals(7, CharterTable.all().size());
        for (CharterTable.Boss b : CharterTable.all()) {
            assertEquals("charter_grimoire_" + b.id(), b.itemId());
            assertEquals("has_dropped_charter_grimoire_" + b.id(), b.flag());
            assertSame(b, CharterTable.byItemPath(b.itemId()));
            assertSame(b, CharterTable.byId(b.id()));
        }
    }

    @Test
    void everySkillSetIsCappedAndMadeOfRealTensuraSkills() throws IOException {
        Set<String> real = tensuraSkills();
        assertTrue(real.size() > 300, "the reference list was read");
        for (CharterTable.Boss b : CharterTable.all()) {
            assertFalse(b.skills().isEmpty(), b.id());
            assertTrue(b.skills().size() <= CharterTable.MAX_SKILLS, b.id() + " has " + b.skills().size());
            assertEquals(b.skills().size(), new HashSet<>(b.skills()).size(), b.id() + " lists a skill twice");
            for (String s : b.skills()) assertTrue(real.contains(s), b.id() + ": " + s + " is not in the Tensura id reference");
        }
    }

    @Test
    void noGrimoireLendsAThiefsSkill() {
        assertTrue(CharterTable.NEVER_LENT.contains("tensura:usurper"));
        for (CharterTable.Boss b : CharterTable.all())
            for (String s : b.skills()) assertFalse(CharterTable.NEVER_LENT.contains(s), b.id() + " lends " + s);
        assertTrue(CharterTable.byId("lucius").skills().contains("tensura:anti_skill"));
        assertTrue(CharterTable.byId("lucius").skills().contains("tensura:megiddo"));
    }

    @Test
    void chancesAndGrades() {
        for (String z : List.of("dante", "vanica", "zenon", "lucius")) assertEquals(0.25, CharterTable.byId(z).chance(), 1e-9, z);
        for (String f : List.of("devil_avatar", "goliath", "charybdis_spawn")) assertEquals(0.40, CharterTable.byId(f).chance(), 1e-9, f);
        for (CharterTable.Boss b : CharterTable.all()) assertTrue(b.grade().equals("LEGEND") || b.grade().equals("GOD") || b.grade().equals("GENESIS"), b.id());
        Set<Integer> covers = new HashSet<>();
        for (CharterTable.Boss b : CharterTable.all()) covers.add(b.cover());
        assertEquals(7, covers.size(), "every boss has its own cover colour");
    }

    @Test
    void onlyEntitiesOfThisModsNamespaceAreBosses() {
        for (CharterTable.Boss b : CharterTable.all()) {
            assertSame(b, CharterTable.byEntity("nusmp", b.entityPath()), b.id());
            assertNull(CharterTable.byEntity("minecraft", b.entityPath()), "vanilla " + b.entityPath());
            assertNull(CharterTable.byEntity("tensura", b.entityPath()), "base Tensura " + b.entityPath());
            assertNull(CharterTable.byEntity("NUSMP", b.entityPath()));
            assertNull(CharterTable.byEntity(null, b.entityPath()));
        }
        assertNull(CharterTable.byEntity("nusmp", "zombie"));
        assertNull(CharterTable.byEntity("nusmp", null));
        assertNull(CharterTable.byEntity("tensura", "goblin"));
        // the four Zogratis entity paths and the three floor bosses map one to one
        assertEquals("zenon", CharterTable.byEntity("nusmp", "zenon_zogratis").id());
        assertEquals("charybdis_spawn", CharterTable.byEntity("nusmp", "charybdis_spawn").id());
        assertEquals("devil_avatar", CharterTable.byEntity("nusmp", "devil_avatar").id());
        // the dark disciples and clones are not bosses
        assertNull(CharterTable.byEntity("nusmp", "dark_disciple"));
        assertNull(CharterTable.byEntity("nusmp", "zogratis_clone"));
    }

    @Test
    void prettyNames() {
        assertEquals("Gravity Field", CharterTable.pretty("tensura:gravity_field"));
        assertEquals("Anti Skill", CharterTable.pretty("anti_skill"));
    }
}
