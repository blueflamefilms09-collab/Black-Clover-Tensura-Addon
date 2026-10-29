package com.newuniverse.nusmp.boss.bars;

import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialProfile;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialRoster;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "every dungeon boss gets a bar of its own family" is a promise about 200 shipped files, the arcs and the named bosses, so it is checked against them: every one of the 200 Decennial bosses
 * resolves to a style that exists, every hall theme and archetype of the roster is decided by the theme or archetype table (not by the side fallback that would make 200 bars look like two), every saga and every named boss has one, and the families the
 * owner listed all show up in the roster's answers.
 */
class BarStyleResolverTest {
    private static List<DecennialProfile> roster() {
        DecennialRoster.Loaded l = DecennialRoster.loadShipped();
        assertTrue(l.problems().isEmpty(), "the shipped roster must parse: " + l.problems());
        return DecennialRoster.all();
    }

    private static String styleOf(DecennialProfile p) {
        return BarStyleResolver.forDecennial(p.side(), p.archetype(), p.theme(), p.creature(), p.devil());
    }

    @Test
    void everyDecennialBossResolvesToAStyleThatExists() {
        List<DecennialProfile> all = roster();
        assertEquals(200, all.size());
        for (DecennialProfile p : all) {
            String id = styleOf(p);
            assertTrue(BarStyleIds.known(id), p.id() + " (" + p.name() + ") resolves to '" + id + "', which is not a style");
        }
    }

    @Test
    void everyHallThemeOfTheRosterIsKnownToTheTable() {
        Set<String> missing = new HashSet<>();
        for (DecennialProfile p : roster()) if (!BarStyleResolver.knownThemes().contains(p.theme())) missing.add(p.theme());
        assertTrue(missing.isEmpty(), "halls the resolver has no style for (they would fall back to the side's style): " + missing);
        for (int g = 1; g <= 7; g++) assertEquals(BarStyleIds.QLIPHOTH_GATE, BarStyleResolver.forDecennial("evil", "qliphoth", "qliphoth_gate_" + g, "devil", "WALGNER"));
    }

    @Test
    void theRosterIsNotJustHolyAndEvil() {
        Map<String, Integer> per = new HashMap<>();
        for (DecennialProfile p : roster()) per.merge(styleOf(p), 1, Integer::sum);
        assertTrue(per.size() >= 22, "the 200 bosses should wear many different bars, they wear " + per.size() + ": " + per);
        for (String must : List.of(BarStyleIds.HOLY, BarStyleIds.EVIL, BarStyleIds.UNDEAD, BarStyleIds.FIRE, BarStyleIds.ICE, BarStyleIds.STORM, BarStyleIds.VOID, BarStyleIds.FOREST, BarStyleIds.WATER,
                BarStyleIds.BLOOD, BarStyleIds.CLOCKWORK, BarStyleIds.MIRROR, BarStyleIds.QLIPHOTH_GATE, BarStyleIds.DEVIL, BarStyleIds.SUPREME_DEVIL, BarStyleIds.DRAGON, BarStyleIds.ANGEL,
                BarStyleIds.ARCHON, BarStyleIds.LICH, BarStyleIds.FALLEN_KING, BarStyleIds.PLAGUE))
            assertTrue(per.containsKey(must), "no Decennial boss wears the '" + must + "' bar: " + per);
        int sideOnly = per.getOrDefault(BarStyleIds.HOLY, 0) + per.getOrDefault(BarStyleIds.EVIL, 0);
        assertTrue(sideOnly < 60, "too many bosses fall back to the plain Holy / Evil frame: " + sideOnly);
    }

    @Test
    void specificBossesWearTheFramesTheOwnerDescribed() {
        assertEquals(BarStyleIds.LICH, BarStyleResolver.forDecennial("evil", "lich", "graveyard", "undead", ""));
        assertEquals(BarStyleIds.FALLEN_KING, BarStyleResolver.forDecennial("evil", "fallen_king", "ruined_throne_room", "undead", ""));
        assertEquals(BarStyleIds.SUPREME_DEVIL, BarStyleResolver.forDecennial("evil", "qliphoth", "qliphoth_root_throne", "devil", "LUCIFERO"));
        assertEquals(BarStyleIds.ICE, BarStyleResolver.forDecennial("holy", "frost_sentinel", "frozen_keep", "construct", ""));
        assertEquals(BarStyleIds.FIRE, BarStyleResolver.forDecennial("holy", "forge_saint", "volcanic_forge", "humanoid", ""));
        assertEquals(BarStyleIds.UNDEAD, BarStyleResolver.forDecennial("evil", "wraith_king", "graveyard", "spirit", ""));
        assertEquals(BarStyleIds.HOLY, BarStyleResolver.forDecennial("holy", "paladin", "cathedral", "humanoid", ""));
        assertEquals(BarStyleIds.DRAGON, BarStyleResolver.forDecennial("holy", "order_dragon", "dragon_hoard_cavern", "dragon", ""));
        // unknown things never break the bar: the theme, then the creature, then the side decide
        assertEquals(BarStyleIds.UNDEAD, BarStyleResolver.forDecennial("evil", "never_heard_of", "no_such_hall", "undead", ""));
        assertEquals(BarStyleIds.HOLY, BarStyleResolver.forDecennial("holy", "x", "y", "z", ""));
        assertEquals(BarStyleIds.EVIL, BarStyleResolver.forDecennial("anything", null, null, null, null));
        assertEquals(BarStyleIds.EVIL, BarStyleResolver.bySide(null));
    }

    @Test
    void everySagaAndEveryNamedBossHasAStyle() {
        for (ArcSaga s : ArcSaga.values()) assertTrue(BarStyleIds.known(BarStyleResolver.forArc(s.id(), "someone")), s.id());
        assertEquals(BarStyleIds.MIDNIGHT_SUN, BarStyleResolver.forArc("elf_reincarnation", "fana"));
        assertEquals(BarStyleIds.MAGIC_KNIGHTS, BarStyleResolver.forArc("elf_reincarnation", "heath_grice"));
        assertEquals(BarStyleIds.SPADE_KINGDOM, BarStyleResolver.forArc("spade_raid", "morris"));
        assertEquals(BarStyleIds.SEVEN_SINS, BarStyleResolver.forArc("seven_deadly_sins", "meliodas"));
        assertEquals(BarStyleIds.DANMACHI, BarStyleResolver.forArc("danmachi", "minotaur"));
        assertEquals(BarStyleIds.ZAGRED, BarStyleResolver.forArc("elf_reincarnation", "zagred"));
        assertEquals(BarStyleIds.MAGIC_KNIGHTS, BarStyleResolver.forArc(null, null), "an unknown saga falls to the Magic Knights");
        for (String path : List.of("dante_zogratis", "vanica_zogratis", "zenon_zogratis", "devil_avatar", "lucius_zogratis", "goliath", "charybdis_spawn", "god_of_order", "zagred",
                "floor_warden", "labyrinth_guardian")) {
            assertTrue(BarStyleIds.known(BarStyleResolver.forEntityPath(path)), path);
        }
        assertEquals(BarStyleIds.GOLIATH, BarStyleResolver.forEntityPath("goliath"));
        assertEquals(BarStyleIds.CHARYBDIS, BarStyleResolver.forEntityPath("charybdis_spawn"));
        assertEquals(BarStyleIds.ZOGRATIS, BarStyleResolver.forEntityPath("dante_zogratis"));
        assertNull(BarStyleResolver.forEntityPath("zombie"));
        assertEquals(BarStyleIds.POCKET_TOMB, BarStyleResolver.forGuardian(true));
        assertEquals(BarStyleIds.WARDEN, BarStyleResolver.forGuardian(false));
    }

    @Test
    void everyStyleIsUsedBySomething() {
        Set<String> used = new HashSet<>();
        for (DecennialProfile p : roster()) used.add(styleOf(p));
        for (ArcSaga s : ArcSaga.values()) used.add(BarStyleResolver.forArc(s.id(), ""));
        for (String path : List.of("dante_zogratis", "lucius_zogratis", "goliath", "charybdis_spawn", "god_of_order", "zagred", "floor_warden")) used.add(BarStyleResolver.forEntityPath(path));
        used.add(BarStyleResolver.forGuardian(true));
        used.add(BarStyleResolver.forArc("elf_reincarnation", "fana"));
        used.add(BarStyleResolver.forArc("x", "morris"));
        used.add(BarStyleResolver.forDecennial("evil", "x", "abyss_rift_ring", "", ""));               // the Underworld frame: the rift ring and a root throne without a devil
        used.add(BarStyleResolver.forDecennial("evil", "qliphoth", "qliphoth_root_throne", "devil", ""));
        Set<String> unused = new HashSet<>(BarStyleIds.ALL);
        unused.removeAll(used);
        assertTrue(unused.isEmpty(), "styles that nothing resolves to: " + unused);
    }
}
