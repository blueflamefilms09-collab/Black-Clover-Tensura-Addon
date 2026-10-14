package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Ambient;
import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Anchor;
import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Style;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pure decisions of the Djinn's look: which mesh set, the colour maths of the dark variants, the per-Djinn tables and the particle rates. */
class DjinnLookTest {
    private static final Set<String> PARTICLE_KEYS = Set.of("spark", "ember", "flame", "smoke", "bubble", "drip", "wind", "feather", "petal", "spore", "shard", "crit", "ash",
            "note", "enchant", "mote", "mote2", "snow", "ink", "rukh");

    @Test
    void aDarkDjinnPrefersItsDarkSetThenItsOwnThenTheOldRig() {
        assertArrayEquals(new int[]{DjinnLook.DARK, DjinnLook.UNIQUE, DjinnLook.BASE}, DjinnLook.chain(true));
        assertArrayEquals(new int[]{DjinnLook.UNIQUE, DjinnLook.BASE}, DjinnLook.chain(false));
        assertEquals(DjinnLook.DARK, DjinnLook.pickSet(true, k -> true));
        assertEquals(DjinnLook.UNIQUE, DjinnLook.pickSet(true, k -> k == DjinnLook.UNIQUE), "no dark set: the unique look");
        assertEquals(DjinnLook.BASE, DjinnLook.pickSet(true, k -> false), "no set at all: the shared rig");
        assertEquals(DjinnLook.UNIQUE, DjinnLook.pickSet(false, k -> true));
        assertEquals(DjinnLook.UNIQUE, DjinnLook.pickSet(false, k -> k != DjinnLook.DARK));
        assertEquals(DjinnLook.BASE, DjinnLook.pickSet(false, k -> k == DjinnLook.DARK), "a dark set is never used by a light Djinn");
        assertEquals(DjinnLook.BASE, DjinnLook.pickSet(false, k -> false));
    }

    @Test
    void theFileNamesFollowTheGeneratorsConvention() {
        assertEquals("baal", DjinnLook.dir("baal", DjinnLook.BASE));
        assertEquals("u_baal", DjinnLook.dir("baal", DjinnLook.UNIQUE));
        assertEquals("d_baal", DjinnLook.dir("baal", DjinnLook.DARK));
        assertEquals("textures/entity/djinn/baal_armor.png", DjinnLook.skinPath("baal", DjinnLook.BASE, false));
        assertEquals("textures/entity/djinn/baal_armor_glow.png", DjinnLook.skinPath("baal", DjinnLook.BASE, true));
        assertEquals("textures/entity/djinn/baal_u_armor.png", DjinnLook.skinPath("baal", DjinnLook.UNIQUE, false));
        assertEquals("textures/entity/djinn/baal_d_armor_glow.png", DjinnLook.skinPath("baal", DjinnLook.DARK, true));
        assertEquals("textures/entity/djinn/baal_rune.png", DjinnLook.runePath("baal", false, false));
        assertEquals("textures/entity/djinn/baal_u_rune.png", DjinnLook.runePath("baal", false, true));
        assertEquals("textures/entity/djinn/baal_d_rune.png", DjinnLook.runePath("baal", true, true));
        assertEquals("textures/entity/djinn/baal_d_bust.png", DjinnLook.bustPath("baal", true, true));
        assertEquals("textures/entity/djinn/baal_bust.png", DjinnLook.bustPath("baal", true, false), "no dark bust: the shared one");
        assertEquals("textures/entity/djinn/baal_bust.png", DjinnLook.bustPath("baal", false, true));
        assertEquals("textures/entity/djinn/baal_d_art.png", DjinnLook.artPath("baal"));
    }

    @Test
    void colourMathsStaysInRange() {
        assertEquals(0x000000, DjinnLook.mix(0x000000, 0xFFFFFF, 0f));
        assertEquals(0xFFFFFF, DjinnLook.mix(0x000000, 0xFFFFFF, 1f));
        assertEquals(0x808080, DjinnLook.mix(0x000000, 0xFFFFFF, 0.5f));
        assertEquals(0xFFFFFF, DjinnLook.mix(0x000000, 0xFFFFFF, 7f), "t is clamped");
        assertEquals(0x102030, DjinnLook.mix(0x102030, 0xFFFFFF, -3f));
        assertEquals(0x204060, DjinnLook.scale(0x102030, 2f));
        assertEquals(0xFFFFFF, DjinnLook.scale(0xC0C0C0, 4f), "clamped at 255");
        assertEquals(0x000000, DjinnLook.scale(0xC0C0C0, -1f), "never negative");
        assertEquals(0xFFFFFE, DjinnLook.tintSafe(0xFFFFFF), "a pure white is the shader's 'default colours' value");
        assertEquals(0x123456, DjinnLook.tintSafe(0x123456));
        assertEquals(0x123456, DjinnLook.tintSafe(0xFF123456), "the alpha byte is dropped");
    }

    @Test
    void theDarkAuraIsRedVioletAndStillTellsTheDjinnApart() {
        Set<Integer> glows = new HashSet<>(), auras = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            int a = DjinnLook.darkAura(d.glow());
            int r = (a >> 16) & 255, g = (a >> 8) & 255, b = a & 255;
            assertTrue(r > g && b > g, d.name() + " aura is red-violet: " + Integer.toHexString(a));
            assertTrue(r < 200 && b < 200, d.name() + " aura stays dark");
            assertEquals(0, a >>> 24, "no alpha byte");
            assertNotEquals(0xFFFFFF, a);
            glows.add(d.glow() & 0xFFFFFF);
            auras.add(a);
            int v = DjinnLook.darkVignette(d.glow());
            assertTrue(((v >> 16) & 255) < 90 && ((v >> 8) & 255) < 60 && (v & 255) < 90, d.name() + " vignette is near black");
        }
        assertEquals(glows.size(), auras.size(), "every Djinn's own colour still shows through in its dark aura");
    }

    @Test
    void theDarkGlowKeepsTheSaturatedColour() {
        for (Djinn d : Djinn.values()) {
            int g = DjinnLook.darkGlow(d.color(), d.glow());
            assertTrue(dist(g, d.color()) < dist(g, d.glow()) || dist(d.color(), d.glow()) < 30, d.name());
        }
    }

    private static int dist(int a, int b) {
        return Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) + Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) + Math.abs((a & 255) - (b & 255));
    }

    @Test
    void everyDjinnHasItsOwnMotionAndItsTailAgreesWithTheTable() {
        Set<Style> seen = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            Style s = DjinnLook.style(d.ordinal());
            assertTrue(s.flap() >= 0.4f && s.flap() <= 2f, d.name());
            assertTrue(s.tailSegs() >= 0 && s.tailSegs() <= 8, d.name());
            assertTrue(s.weaponScale() >= 0.5f && s.weaponScale() <= 1.2f, d.name());
            assertTrue(Math.abs(s.backSpin()) <= 3f && s.backPulse() >= 0f && s.backPulse() <= 0.12f, d.name());
            assertEquals(d.look().tail() == Djinn.Look.Tail.NONE, s.tailSegs() == 0, d.name() + ": a tail is drawn exactly when the table says it has one");
            assertTrue(seen.add(s), d.name() + " shares its motion with another Djinn");
        }
        assertEquals(DjinnLook.style(-1), DjinnLook.style(99), "out of range falls back to one style");
        assertEquals(1.2f, DjinnLook.haloSpin(DjinnLook.style(Djinn.BAAL.ordinal()), true), 1e-6, "a dark halo always turns");
        assertEquals(0f, DjinnLook.haloSpin(DjinnLook.style(Djinn.BAAL.ordinal()), false), 1e-6);
    }

    @Test
    void everyDjinnShedsItsOwnParticles() {
        Set<String> pairs = new HashSet<>();
        Set<String> mains = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            Ambient a = DjinnLook.ambient(d.ordinal());
            assertTrue(PARTICLE_KEYS.contains(a.main()), d.name() + " " + a.main());
            assertTrue(a.mainRate() >= 1 && a.mainRate() <= 20, d.name());
            assertTrue(a.secondRate() >= 0 && a.secondRate() <= 20, d.name());
            if (a.secondRate() > 0) assertTrue(PARTICLE_KEYS.contains(a.second()), d.name() + " " + a.second());
            assertNotNull(a.anchor());
            assertTrue(pairs.add(a.main() + "/" + a.second() + "/" + a.anchor()), d.name() + " sheds exactly what another Djinn sheds");
            mains.add(a.main());
        }
        assertTrue(mains.size() >= 9, "twelve Djinn, at least nine different main particles: " + mains);
        assertTrue(PARTICLE_KEYS.contains(DjinnLook.RUKH_KEY));
        assertTrue(DjinnLook.falls(DjinnLook.RUKH_KEY), "the black rukh sinks as it streams");
        assertFalse(DjinnLook.falls("ember"));
        assertFalse(DjinnLook.falls("bubble"));
        assertTrue(DjinnLook.falls("feather"));
    }

    @Test
    void spawnCountsFollowTheRateAndNeverFlood() {
        assertEquals(0, DjinnLook.spawnCount(0f, 1f, 0f));
        assertEquals(0, DjinnLook.spawnCount(10f, 0f, 0f));
        assertEquals(0, DjinnLook.spawnCount(-5f, 1f, 0f));
        assertEquals(1, DjinnLook.spawnCount(10f, 1f, 0.3f), "half a particle a tick: a low roll spawns one");
        assertEquals(0, DjinnLook.spawnCount(10f, 1f, 0.7f));
        assertEquals(2, DjinnLook.spawnCount(40f, 1f, 0.99f), "exactly two a tick, whatever the roll");
        assertEquals(2, DjinnLook.spawnCount(40f, 1f, 0.0f), "no fractional part: no extra");
        assertEquals(6, DjinnLook.spawnCount(5000f, 1f, 0f), "capped");
        float sum = 0;
        for (int i = 0; i < 1000; i++) sum += DjinnLook.spawnCount(13f, 1f, i / 1000f);
        assertEquals(13f / 20f * 1000, sum, 2f, "the expectation holds over many ticks");
        float half = 0;
        for (int i = 0; i < 1000; i++) half += DjinnLook.spawnCount(13f, DjinnLook.HALF_SHARE, i / 1000f);
        assertEquals(13f / 20f * 1000 * DjinnLook.HALF_SHARE, half, 2f);
    }

    @Test
    void thePhasesPickShareAndAnchor() {
        assertEquals(0f, DjinnLook.phaseShare(0));
        assertEquals(0f, DjinnLook.phaseShare(1));
        assertEquals(DjinnLook.HALF_SHARE, DjinnLook.phaseShare(2));
        assertEquals(1f, DjinnLook.phaseShare(3));
        assertEquals(1f, DjinnLook.phaseShare(4));
        assertEquals(Anchor.BODY, DjinnLook.anchorAt(Anchor.WINGS, 2), "a Half Equip has no wings: it sheds from the body");
        assertEquals(Anchor.BODY, DjinnLook.anchorAt(Anchor.ABOVE, 2));
        assertEquals(Anchor.WINGS, DjinnLook.anchorAt(Anchor.WINGS, 3));
        assertEquals(Anchor.FEET, DjinnLook.anchorAt(Anchor.FEET, 2));
    }

    @Test
    void thePiecesComeInAtTheirPhase() {
        assertEquals(2, DjinnLook.minPhase("crest", false));
        assertEquals(2, DjinnLook.minPhase("eye", false));
        assertEquals(2, DjinnLook.minPhase("weapon", false));
        assertEquals(2, DjinnLook.minPhase("pauldron_r", true));
        assertEquals(3, DjinnLook.minPhase("pauldron_r", false), "the off-hand arm waits for the Full Equip");
        assertEquals(2, DjinnLook.minPhase("gauntlet_l", true));
        assertEquals(3, DjinnLook.minPhase("vambrace_l", false));
        for (String p : new String[]{"helm", "chest", "belt", "wing_in_l", "halo", "backdrop", "mantle", "tail_seg", "greave_l", "horn_r"}) assertEquals(3, DjinnLook.minPhase(p, true), p);
        assertTrue(DjinnLook.drawsEye(true));
        assertFalse(DjinnLook.drawsEye(false), "only a Dark Djinn wears the eye crest");
    }
}
