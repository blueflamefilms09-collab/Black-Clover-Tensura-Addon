package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Living Dungeon's moods (fog, particles, ambient sounds) and the arithmetic that smooths and picks them. */
class DungeonAtmosphereTest {
    @Test
    void everyMoodBelongsToATierAndStandsAtItsOrdinal() {
        for (int i = 0; i < DungeonAtmosphere.moodCount(); i++) {
            DungeonAtmosphere.Mood m = DungeonAtmosphere.mood(i);
            DungeonTier t = DungeonTier.valueOf(m.tier());
            assertEquals(i, t.ordinal(), m.tier());
            assertSame(m, DungeonAtmosphere.mood(t.ordinal()), t.name());
            assertSame(m, DungeonAtmosphere.mood(t.name()), t.name());
        }
        for (DungeonTier t : new DungeonTier[]{DungeonTier.UPPER_CATACOMBS, DungeonTier.DEEP_WATERWAYS, DungeonTier.VOLCANIC_VENTS, DungeonTier.GOLDEN_CITY})
            assertEquals(t.name(), DungeonAtmosphere.mood(t.ordinal()).tier(), "the four tiers have a mood each");
        assertEquals(DungeonAtmosphere.UPPER_CATACOMBS, DungeonAtmosphere.mood(-1), "out of range falls back to the first mood");
        assertEquals(DungeonAtmosphere.UPPER_CATACOMBS, DungeonAtmosphere.mood(99));
        assertEquals(DungeonAtmosphere.UPPER_CATACOMBS, DungeonAtmosphere.mood("NOT_A_TIER"));
    }

    @Test
    void moodsAreDistinctAndWellFormed() {
        Set<String> colours = new HashSet<>();
        for (int i = 0; i < DungeonAtmosphere.moodCount(); i++) {
            DungeonAtmosphere.Mood m = DungeonAtmosphere.mood(i);
            for (float c : m.fog()) assertTrue(c >= 0f && c <= 1f, m.tier());
            assertTrue(colours.add(m.red() + "/" + m.green() + "/" + m.blue()), "each tier has its own fog colour: " + m.tier());
            assertTrue(m.fogNear() > 0.3f && m.fogNear() <= 1.5f, m.tier());
            assertTrue(m.fogFar() > 0.3f && m.fogFar() <= 1.5f, m.tier());
            assertTrue(m.soundMin() > 0 && m.soundMin() <= m.soundMax(), m.tier());
            assertFalse(m.particles().isEmpty(), m.tier());
            assertFalse(m.sounds().isEmpty(), m.tier());
            float perTick = 0f;
            for (DungeonAtmosphere.Drift d : m.particles()) {
                assertTrue(d.rate() > 0f, m.tier() + " " + d.kind());
                perTick += d.rate();
            }
            assertTrue(perTick <= 1.5f, "a few particles per tick at most: " + m.tier());
            assertSoundsAreWellFormed(m.sounds(), m.tier());
        }
    }

    @Test
    void growlsAreWellFormedAndWiderThanTheirDistance() {
        assertSoundsAreWellFormed(DungeonAtmosphere.DIREWOLF_GROWLS, "direwolf");
        assertSoundsAreWellFormed(DungeonAtmosphere.BASILISK_GROWLS, "basilisk");
        assertTrue(DungeonAtmosphere.GROWL_MIN_DISTANCE < DungeonAtmosphere.GROWL_RANGE);
        assertTrue(DungeonAtmosphere.GROWL_MIN_TICKS <= DungeonAtmosphere.GROWL_MAX_TICKS);
        assertTrue(DungeonAtmosphere.GROWL_RECHECK_MIN_TICKS <= DungeonAtmosphere.GROWL_RECHECK_MAX_TICKS);
        assertTrue(DungeonAtmosphere.FIRST_SOUND_MIN_TICKS <= DungeonAtmosphere.FIRST_SOUND_MAX_TICKS);
    }

    private static void assertSoundsAreWellFormed(List<DungeonAtmosphere.Sound> sounds, String who) {
        int total = 0;
        for (DungeonAtmosphere.Sound s : sounds) {
            assertTrue(s.id().matches("minecraft:[a-z0-9_]+(\\.[a-z0-9_]+)+"), who + ": a vanilla sound id: " + s.id());
            assertTrue(s.weight() > 0, who + " " + s.id());
            assertTrue(s.volumeMin() > 0f && s.volumeMin() <= s.volumeMax() && s.volumeMax() <= DungeonAtmosphere.MAX_VOLUME, who + " " + s.id());
            assertTrue(s.pitchMin() > 0f && s.pitchMin() <= s.pitchMax() && s.pitchMax() <= 2f, who + " " + s.id());
            total += s.weight();
        }
        assertTrue(total > 0, who);
    }

    @Test
    void smoothingApproachesItsTargetAboutThreeSecondsWithATauOfOne() {
        assertEquals(0f, DungeonAtmosphere.smoothing(0f, 1f));
        assertEquals(1f, DungeonAtmosphere.smoothing(1f, 0f));
        assertEquals(10f, DungeonAtmosphere.approach(10f, 20f, 0f, 1f));
        assertEquals(20f, DungeonAtmosphere.approach(10f, 20f, 0.5f, 0f));
        float v = 0f;
        for (int frame = 0; frame < 180; frame++) v = DungeonAtmosphere.approach(v, 1f, 1f / 60f, 1f);   // 3 seconds at 60 frames per second
        assertTrue(v > 0.94f && v < 0.96f, "about 95% after 3 seconds: " + v);
        float steps = 0f, once = DungeonAtmosphere.approach(0f, 1f, 3f, 1f);
        for (int frame = 0; frame < 3; frame++) steps = DungeonAtmosphere.approach(steps, 1f, 1f, 1f);
        assertEquals(once, steps, 1e-5f, "frame rate does not change the result");
    }

    @Test
    void colourLerpGoesChannelByChannel() {
        float[] a = {0f, 0.5f, 1f}, b = {1f, 0.5f, 0f};
        assertArrayEquals(a, DungeonAtmosphere.lerpColor(a, b, 0f));
        assertArrayEquals(b, DungeonAtmosphere.lerpColor(a, b, 1f));
        assertArrayEquals(new float[]{0.5f, 0.5f, 0.5f}, DungeonAtmosphere.lerpColor(a, b, 0.5f), 1e-6f);
        assertEquals(5f, DungeonAtmosphere.lerp(0f, 10f, 0.5f));
    }

    @Test
    void picksAreDeterministicAndFollowTheWeights() {
        for (int i = 0; i < DungeonAtmosphere.moodCount(); i++) {
            DungeonAtmosphere.Mood m = DungeonAtmosphere.mood(i);
            int total = 0;
            for (DungeonAtmosphere.Sound s : m.sounds()) total += s.weight();
            int[] hits = new int[m.sounds().size()];
            int n = 20000;
            for (int seed = 0; seed < n; seed++) {
                DungeonAtmosphere.Sound a = DungeonAtmosphere.pick(m.sounds(), seed);
                assertSame(a, DungeonAtmosphere.pick(m.sounds(), seed), "same seed, same sound");
                hits[m.sounds().indexOf(a)]++;
            }
            for (int k = 0; k < hits.length; k++) {
                double expected = m.sounds().get(k).weight() / (double) total;
                assertEquals(expected, hits[k] / (double) n, 0.03, m.tier() + " " + m.sounds().get(k).id());
            }
        }
        assertNull(DungeonAtmosphere.pick(List.of(), 1L));
    }

    @Test
    void intervalsRingsAndVolumesStayInTheirRanges() {
        for (int i = 0; i < DungeonAtmosphere.moodCount(); i++) {
            DungeonAtmosphere.Mood m = DungeonAtmosphere.mood(i);
            for (long seed = 0; seed < 2000; seed++) {
                int gap = DungeonAtmosphere.soundInterval(m, seed);
                assertTrue(gap >= m.soundMin() && gap <= m.soundMax(), m.tier() + " " + gap);
            }
        }
        boolean[] quadrant = new boolean[4];
        for (long seed = 0; seed < 2000; seed++) {
            double[] r = DungeonAtmosphere.ring(seed, DungeonAtmosphere.SOUND_MIN_DISTANCE, DungeonAtmosphere.SOUND_MAX_DISTANCE);
            assertTrue(r[2] >= DungeonAtmosphere.SOUND_MIN_DISTANCE && r[2] <= DungeonAtmosphere.SOUND_MAX_DISTANCE, "distance " + r[2]);
            assertEquals(r[2], Math.hypot(r[0], r[1]), 1e-9, "the offset has the distance's length");
            quadrant[(r[0] >= 0 ? 0 : 1) + (r[1] >= 0 ? 0 : 2)] = true;
        }
        for (boolean q : quadrant) assertTrue(q, "every direction is used");
        for (double d = 12; d <= 40; d += 0.5) {
            float v = DungeonAtmosphere.audibleVolume(1.0f, d);
            assertTrue(16.0 * v > d + 5.0, "a sound " + d + " blocks away carries that far: " + v);
            assertTrue(v <= DungeonAtmosphere.MAX_VOLUME);
        }
        assertEquals(2.5f, DungeonAtmosphere.audibleVolume(2.5f, 12.0), "a loud sound is not made quieter");
        assertEquals(DungeonAtmosphere.MAX_VOLUME, DungeonAtmosphere.audibleVolume(1.0f, 1000.0));
    }

    @Test
    void particleCountsFollowTheRateAndTheCap() {
        assertEquals(0, DungeonAtmosphere.particleCount(0f, 0f));
        assertEquals(1, DungeonAtmosphere.particleCount(0.35f, 0.1f));
        assertEquals(0, DungeonAtmosphere.particleCount(0.35f, 0.9f));
        assertEquals(3, DungeonAtmosphere.particleCount(2.5f, 0.4f));
        assertEquals(2, DungeonAtmosphere.particleCount(2.5f, 0.6f));
        assertEquals(DungeonAtmosphere.MAX_PARTICLES_PER_KIND, DungeonAtmosphere.particleCount(9f, 0f));
        int total = 0, n = 10000;
        for (int seed = 0; seed < n; seed++) total += DungeonAtmosphere.particleCount(0.35f, DungeonAtmosphere.unit(seed));
        assertEquals(0.35, total / (double) n, 0.03, "on average the rate");
    }

    @Test
    void seededValuesStayInRange() {
        for (long seed = -500; seed < 500; seed++) {
            float u = DungeonAtmosphere.unit(seed);
            assertTrue(u >= 0f && u < 1f, "unit " + u);
            int b = DungeonAtmosphere.between(5, 9, seed);
            assertTrue(b >= 5 && b <= 9, "between " + b);
        }
        assertEquals(7, DungeonAtmosphere.between(7, 7, 123L));
        assertEquals(7, DungeonAtmosphere.between(7, 3, 123L));
    }
}
