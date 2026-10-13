package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.DungeonAtmosphere;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The fog and air of the four zones of the Variable Core. */
class NexusMoodTest {
    @Test
    void everyZoneHasAMoodOfItsOwn() {
        Set<String> colours = new HashSet<>();
        for (NexusZone z : NexusZone.coreZones()) {
            DungeonAtmosphere.Mood m = DungeonAtmosphere.zoneMood(z);
            assertNotNull(m, z.name());
            assertEquals("GOLDEN_CITY", m.tier(), "all four belong to the deepest tier");
            assertTrue(colours.add(m.red() + "/" + m.green() + "/" + m.blue()), "each zone has its own fog colour: " + z);
        }
        assertSame(DungeonAtmosphere.GOLDEN_CITY, DungeonAtmosphere.zoneMood(NexusZone.GOLDEN_CITY), "the Golden City keeps its warm gold");
    }

    @Test
    void theFloorBossArenasKeepTheMoodOfTheTierTheyStandIn() {
        assertSame(DungeonAtmosphere.mood(0), DungeonAtmosphere.zoneMood(NexusZone.GOLIATH_PIT), "the pit is in the Upper Catacombs");
        assertSame(DungeonAtmosphere.mood(1), DungeonAtmosphere.zoneMood(NexusZone.CHARYBDIS_BASIN), "the basin is in the Deep Waterways");
        for (NexusZone z : NexusZone.values()) assertNotNull(DungeonAtmosphere.zoneMood(z), z.name());
    }

    @Test
    void theZoneMoodsAreWellFormed() {
        for (NexusZone z : NexusZone.values()) {
            DungeonAtmosphere.Mood m = DungeonAtmosphere.zoneMood(z);
            for (float c : m.fog()) assertTrue(c >= 0f && c <= 1f, z.name());
            assertTrue(m.fogNear() > 0.3f && m.fogNear() <= 1.5f, z.name());
            assertTrue(m.fogFar() > 0.3f && m.fogFar() <= 1.5f, z.name());
            assertTrue(m.soundMin() > 0 && m.soundMin() <= m.soundMax(), z.name());
            assertFalse(m.particles().isEmpty());
            float perTick = 0f;
            for (DungeonAtmosphere.Drift d : m.particles()) {
                assertTrue(d.rate() > 0f, z + " " + d.kind());
                perTick += d.rate();
            }
            assertTrue(perTick <= 1.5f, "a few particles per tick at most: " + z);
            int total = 0;
            for (DungeonAtmosphere.Sound s : m.sounds()) {
                assertTrue(s.id().matches("minecraft:[a-z0-9_]+(\\.[a-z0-9_]+)+"), "a vanilla sound id: " + s.id());
                assertTrue(s.weight() > 0);
                assertTrue(s.volumeMin() > 0f && s.volumeMin() <= s.volumeMax() && s.volumeMax() <= DungeonAtmosphere.MAX_VOLUME, s.id());
                assertTrue(s.pitchMin() > 0f && s.pitchMin() <= s.pitchMax() && s.pitchMax() <= 2f, s.id());
                total += s.weight();
            }
            assertTrue(total > 0);
        }
    }

    @Test
    void theNewZonesAreDarkerAndTheirColoursTellThemApart() {
        DungeonAtmosphere.Mood monarch = DungeonAtmosphere.zoneMood(NexusZone.MONARCH_VAULT), underworld = DungeonAtmosphere.zoneMood(NexusZone.UNDERWORLD_GATE), mirage = DungeonAtmosphere.zoneMood(NexusZone.TEMPEST_MIRAGE);
        assertTrue(monarch.blue() > monarch.green() && monarch.red() < 0.1f, "violet-black");
        assertTrue(underworld.red() > underworld.green() * 3 && underworld.blue() > underworld.green(), "crimson-violet");
        assertTrue(mirage.green() > mirage.red() * 3 && mirage.blue() > mirage.red(), "green-teal");
        assertTrue(mirage.fogFar() < monarch.fogFar(), "the haze of the Mirage closes in; the hall of the Vault is open");
    }

    @Test
    void onlyTheDeepestTierIsTintedByZone() {
        for (int x = -300; x <= 300; x += 37) {
            for (int z = -300; z <= 300; z += 41) {
                for (DungeonTier t : DungeonTier.values()) {
                    DungeonAtmosphere.Mood m = DungeonAtmosphere.mood(t.ordinal(), x, z);
                    if (t == DungeonTier.GOLDEN_CITY) assertSame(DungeonAtmosphere.zoneMood(NexusZone.at(x, z)), m, x + "," + z);
                    else assertSame(DungeonAtmosphere.mood(t.ordinal()), m, t.name());
                }
            }
        }
        assertEquals(3, DungeonTier.GOLDEN_CITY.ordinal(), "the mood table counts the deepest tier as the fourth");
        assertEquals("The Variable Core", DungeonTier.GOLDEN_CITY.label);
    }

    @Test
    void theOldTierMoodsAreUntouched() {
        assertEquals(4, DungeonAtmosphere.moodCount());
        assertSame(DungeonAtmosphere.GOLDEN_CITY, DungeonAtmosphere.mood(3));
        assertSame(DungeonAtmosphere.GOLDEN_CITY, DungeonAtmosphere.mood("GOLDEN_CITY"));
    }
}
