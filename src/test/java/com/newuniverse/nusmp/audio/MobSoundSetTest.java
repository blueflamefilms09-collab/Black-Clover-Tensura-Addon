package com.newuniverse.nusmp.audio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the catalogue of mob voices is pure data and pure rules, so what it promises (families, cues, pitch ranges, which family a mob gets, which vanilla sounds the remap reads) is pinned here. */
class MobSoundSetTest {
    @Test
    void theCatalogueHasAtLeastTwentyFamiliesOfEightCues() {
        assertTrue(MobSoundSet.families().size() >= 20, "families: " + MobSoundSet.families().size());
        assertEquals(8, Cue.values().length);
        assertEquals(MobSoundSet.families().size() * 8, MobSoundSet.allEventIds().size());
        assertEquals(MobSoundSet.allEventIds().size(), Set.copyOf(MobSoundSet.allEventIds()).size(), "no event twice");
        for (String must : new String[]{"goblin", "orc", "kobold", "skeleton", "ghoul", "lich", "wraith", "armor", "golem", "dragon", "drake", "slime", "demon", "imp", "harpy", "troll", "ogre", "beast", "insect",
                "elemental", "aquatic", "cultist", "vampire", "celestial", "npc"}) {
            assertTrue(MobSoundSet.has(must), "family " + must);
        }
        assertTrue(MobSoundSet.has(MobSoundSet.DEFAULT_FAMILY));
    }

    @Test
    void everyCueHasASaneRangeAndVolume() {
        for (MobSoundSet.Family f : MobSoundSet.families()) {
            assertFalse(f.label().isBlank(), f.id());
            assertFalse(f.trope().isBlank(), f.id());
            assertTrue(f.id().matches("[a-z]+"), f.id());
            for (Cue c : Cue.values()) {
                assertTrue(f.pitchMin(c) >= 0.5f && f.pitchMax(c) <= 2.0f && f.pitchMin(c) <= f.pitchMax(c), f.id() + "." + c.id());
                assertTrue(f.volume(c) > 0.2f && f.volume(c) <= 1.5f, f.id() + "." + c.id() + " volume " + f.volume(c));
                float p = MobSoundSet.pitch(f.id(), c, 0.5);
                assertTrue(p >= f.pitchMin(c) - 1e-6f && p <= f.pitchMax(c) + 1e-6f);
            }
            assertTrue(f.pitchMax(Cue.STEP) - f.pitchMin(Cue.STEP) > f.pitchMax(Cue.DEATH) - f.pitchMin(Cue.DEATH), "feet stray further than a death");
        }
        assertEquals("mob.goblin.taunt", MobSoundSet.eventId("goblin", Cue.TAUNT));
        assertEquals("subtitles.nusmp.mob.lich.ambient", MobSoundSet.subtitleKey("lich", Cue.AMBIENT));
        assertEquals(Cue.HURT, Cue.byId("hurt"));
        assertNull(Cue.byId("nope"));
    }

    @Test
    void aFoundryMobIsFamilyByItsFirstKnownWordThenByFaction() {
        assertEquals("skeleton", MobSoundSet.forFoundry("undead", "bone_archer_of_the_barrow"));
        assertEquals("wraith", MobSoundSet.forFoundry("undead", "banshee_tower_mourner"));
        assertEquals("dragon", MobSoundSet.forFoundry("aquatic", "ancient_black_dragon_of_the_drowned_deep"), "the first word decides, not the faction");
        assertEquals("kobold", MobSoundSet.forFoundry("goblin", "kobold_trapsmith"));
        assertEquals("orc", MobSoundSet.forFoundry("goblin", "hobgoblin_warlord"), "a hobgoblin is an orc");
        assertEquals("goblin", MobSoundSet.forFoundry("goblin", "goblin_slayer_bait"));
        assertEquals("lich", MobSoundSet.forFoundry("undead", "crypt_lich_apprentice"));
        assertEquals("ghoul", MobSoundSet.forFoundry("undead", "grave_mourner_of_the_pit"), "no word: the undead faction");
        assertEquals("armor", MobSoundSet.forFoundry("construct", "admiral_of_the_gilded_fleet"));
        assertEquals("cultist", MobSoundSet.forFoundry("cultist", "admiral_of_the_gilded_fleet"));
        assertEquals(MobSoundSet.DEFAULT_FAMILY, MobSoundSet.forFoundry("nope", "admiral_of_the_gilded_fleet"));
        assertEquals(MobSoundSet.DEFAULT_FAMILY, MobSoundSet.forFoundry(null, null));
        for (String faction : new String[]{"undead", "goblin", "ogre", "beast", "slime", "construct", "cultist", "demon", "elemental", "aquatic", "insect"}) {
            assertTrue(MobSoundSet.has(MobSoundSet.forFoundry(faction, "x_y")), faction);
        }
    }

    @Test
    void everyMobOfTheShippedFoundryRosterGetsAKnownFamilyAndTheRosterSpreadsOverManyFamilies() throws IOException {
        Path dir = Path.of("src/main/resources/data/nusmp/mega/foundry");
        Map<String, Integer> count = new TreeMap<>();
        int mobs = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.getFileName().toString().endsWith(".json") && !x.getFileName().toString().startsWith("_"))::iterator) {
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                String fam = MobSoundSet.forFoundry(o.get("faction").getAsString(), o.get("id").getAsString());
                assertTrue(MobSoundSet.has(fam), f + " -> " + fam);
                count.merge(fam, 1, Integer::sum);
                mobs++;
            }
        }
        assertTrue(mobs >= 400, "mobs: " + mobs);
        assertTrue(count.size() >= 18, "the roster should use many voices, it uses " + count);
        for (Map.Entry<String, Integer> e : count.entrySet()) assertTrue(e.getValue() < mobs / 2, e.getKey() + " has " + e.getValue() + " of " + mobs + " mobs: one voice for half the roster");
    }

    @Test
    void entityTypesAndBossLooksMapToFamilies() {
        assertEquals("ghoul", MobSoundSet.forEntityType("minecraft:zombie"));
        assertEquals("skeleton", MobSoundSet.forEntityType("minecraft:wither_skeleton"));
        assertEquals("slime", MobSoundSet.forEntityType("nusmp:dungeon_slime"));
        assertEquals("armor", MobSoundSet.forEntityType("nusmp:living_armor"));
        assertEquals("beast", MobSoundSet.forEntityType("nusmp:dungeon_direwolf"));
        assertNull(MobSoundSet.forEntityType("nusmp:foundry_mob"), "the Foundry mob needs its faction and id");
        assertNull(MobSoundSet.forEntityType("minecraft:cow"));
        assertEquals("ogre", MobSoundSet.forEntity("nusmp:foundry_mob", "ogre", "x_y"));
        assertEquals("lich", MobSoundSet.forEntity("nusmp:foundry_mob", "undead", "lich_acolyte"));
        assertEquals("lich", MobSoundSet.forBoss(null, "vecna", "Hand of Vecna", List.of(), "evil"));
        assertEquals("aquatic", MobSoundSet.forBoss(null, "kraken", "Kraken", List.of(), "evil"));
        assertEquals("dragon", MobSoundSet.forBoss("dragon", "lich", "x", List.of(), "evil"), "an explicit sound_set wins");
        assertEquals("lich", MobSoundSet.forBoss("not_a_family", "lich", "x", List.of(), "evil"), "an unknown explicit one is ignored here (the loader refuses it)");
        assertEquals("lich", MobSoundSet.forBoss(null, "", "Umbral Catacomb Lich-Djinn", List.of("Magi: a dark Djinn"), "evil"));
        assertEquals("celestial", MobSoundSet.forBoss(null, null, "A Plain Name", List.of(), "holy"));
        assertEquals("demon", MobSoundSet.forBoss(null, null, "A Plain Name", List.of(), "evil"));
        assertEquals("armor", MobSoundSet.forBoss(null, null, "A Plain Name", List.of(), null));
    }

    @Test
    void aBigCreatureSoundsLowerAndASmallOneHigher() {
        assertEquals(1.0f, MobSoundSet.sizeShift(1.0f), 1e-6f);
        assertTrue(MobSoundSet.sizeShift(2.0f) < 0.8f && MobSoundSet.sizeShift(2.0f) > 0.7f);
        assertTrue(MobSoundSet.sizeShift(0.7f) > 1.1f && MobSoundSet.sizeShift(0.7f) < 1.2f);
        assertEquals(MobSoundSet.sizeShift(100.0f), MobSoundSet.sizeShift(2.5f), 1e-6f, "clamped at 2.5");
        assertEquals(MobSoundSet.sizeShift(0.01f), MobSoundSet.sizeShift(0.6f), 1e-6f, "clamped at 0.6");
        assertEquals(1.0f, MobSoundSet.sizeShift(Float.NaN), 1e-6f);
    }

    @Test
    void theVanillaVoiceReader() {
        MobSoundSet.Voice v = MobSoundSet.parseVanillaVoice("entity.zombie.ambient");
        assertNotNull(v);
        assertEquals("zombie", v.mob());
        assertEquals(Cue.AMBIENT, v.cue());
        assertEquals(Cue.HURT, MobSoundSet.parseVanillaVoice("entity.skeleton.hurt").cue());
        assertEquals(Cue.DEATH, MobSoundSet.parseVanillaVoice("entity.husk.death").cue());
        assertEquals(Cue.STEP, MobSoundSet.parseVanillaVoice("entity.zombie.step").cue());
        assertEquals(Cue.AGGRO, MobSoundSet.parseVanillaVoice("entity.ravager.roar").cue());
        assertEquals(Cue.TAUNT, MobSoundSet.parseVanillaVoice("entity.wolf.howl").cue());
        assertEquals(Cue.CAST, MobSoundSet.parseVanillaVoice("entity.evoker.cast_spell").cue());
        assertEquals(Cue.ATTACK, MobSoundSet.parseVanillaVoice("entity.phantom.swoop").cue());
        assertNull(MobSoundSet.parseVanillaVoice("entity.skeleton.shoot"), "a bow's twang is not a voice");
        assertNull(MobSoundSet.parseVanillaVoice("entity.arrow.shoot"));
        assertNull(MobSoundSet.parseVanillaVoice("entity.cow.ambient"), "not a stock hostile mob");
        assertNull(MobSoundSet.parseVanillaVoice("block.wooden_door.open"));
        assertNull(MobSoundSet.parseVanillaVoice("entity.zombie"));
        assertNull(MobSoundSet.parseVanillaVoice(null));
        List<String> bad = new ArrayList<>();
        for (String s : new String[]{"entity.zombie.ambient", "entity.zombie.hurt", "entity.zombie.death", "entity.zombie.step", "entity.skeleton.ambient", "entity.skeleton.hurt", "entity.skeleton.death",
                "entity.skeleton.step", "entity.spider.ambient", "entity.slime.squish", "entity.pillager.ambient", "entity.vindicator.ambient", "entity.ravager.step"}) {
            if (MobSoundSet.parseVanillaVoice(s) == null) bad.add(s);
        }
        assertTrue(bad.isEmpty(), "the generic vanilla voices the remap must read: " + bad);
    }
}
