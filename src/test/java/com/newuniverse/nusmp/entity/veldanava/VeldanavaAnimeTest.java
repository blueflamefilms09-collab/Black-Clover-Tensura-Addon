package com.newuniverse.nusmp.entity.veldanava;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (3.30.0, the anime Veldanava, docs/veldanava.md): the numbers of the god-tier stats, the five million health with its damage-divisor fallback, Ultraspeed Regeneration and its counter,
 * the per-body animated boss bar and the geometry of the seven Tensura-themed moves are pure, so they are pinned here; the wiring (attributes, immunities, the classic switch, the effects) is pinned by
 * reading the real files, since nothing of the fight can be played in a unit test.
 */
class VeldanavaAnimeTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static String read(String rel) throws IOException { return code(Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8)); }

    @Test
    void theStatsAreGodTier() {
        assertEquals(5_000_000.0, VeldanavaStats.HEALTH, 0.0, "exactly five million");
        assertTrue(VeldanavaStats.oneShots(VeldanavaStats.meleeDamage(VeldanavaStats.ATTACK_DAMAGE, 1f), 20.0), "one blow kills an unarmoured player");
        assertTrue(VeldanavaStats.meleeDamage(VeldanavaStats.ATTACK_DAMAGE, 1.3f) > VeldanavaStats.meleeDamage(VeldanavaStats.ATTACK_DAMAGE, 1f), "the Dark Avatar hits harder");
        assertTrue(VeldanavaStats.MOVEMENT_SPEED > 2 * VeldanavaStats.CLASSIC_SPEED * 0.7 && VeldanavaStats.MOVEMENT_SPEED > 0.23, "faster than any ordinary mob");
        assertEquals(30.0, VeldanavaStats.ARMOR, 0.0, "vanilla's armour cap");
        assertEquals(20.0, VeldanavaStats.ARMOR_TOUGHNESS, 0.0, "vanilla's toughness cap");
        assertEquals(1.0, VeldanavaStats.KNOCKBACK_RESISTANCE, 0.0);
        assertTrue(VeldanavaStats.ATTACK_DAMAGE > VeldanavaStats.CLASSIC_ATTACK && VeldanavaStats.ARMOR > VeldanavaStats.CLASSIC_ARMOR);
        assertEquals(4000.0, VeldanavaStats.CLASSIC_HEALTH, 0.0, "the classic numbers are kept");
    }

    @Test
    void healthIsClampedAndTheDivisorKeepsTheFightTheSameSizeWhenTheCapCannotBeLifted() {
        assertEquals(5_000_000.0, VeldanavaStats.health(false, 4000.0, 5_000_000.0), 0.0);
        assertEquals(4000.0, VeldanavaStats.health(true, 4000.0, 5_000_000.0), 0.0, "classic mode uses its own setting");
        assertEquals(VeldanavaStats.HEALTH_MAX, VeldanavaStats.clampHealth(1.0E12), 0.0);
        assertEquals(100.0, VeldanavaStats.clampHealth(-3.0), 0.0);
        assertEquals(VeldanavaStats.HEALTH, VeldanavaStats.clampHealth(Double.NaN), 0.0);
        assertEquals(1.0, VeldanavaStats.healthDivisor(5_000_000.0, 5_000_000.0), 0.0, "the cap was lifted: no divisor");
        assertEquals(1.0, VeldanavaStats.healthDivisor(1000.0, 1024.0), 0.0);
        assertEquals(5_000_000.0 / 1024.0, VeldanavaStats.healthDivisor(5_000_000.0, 1024.0), 1.0E-9, "the missing part becomes a damage divisor");
        assertEquals(1.0, VeldanavaStats.healthDivisor(5_000_000.0, 0.0), 0.0, "a bad maximum never divides by zero");
        double d = VeldanavaStats.healthDivisor(5_000_000.0, 1024.0);
        assertEquals(5_000_000L, VeldanavaStats.shown(1024.0, d), "the players are shown the whole pool");
        assertEquals(100f, VeldanavaStats.scaledDamage(100f, 1.0), 0f);
        assertTrue(VeldanavaStats.scaledDamage(100f, d) < 0.03f && VeldanavaStats.scaledDamage(100f, d) > 0f);
    }

    @Test
    void regenerationHealsWhileFightingAndStopsAfterACounterHit() {
        float step = VeldanavaStats.regenStep(5_000_000.0, VeldanavaStats.REGEN_PERCENT_PER_SECOND);
        assertEquals(2500f, step, 0.01f, "0.1% of five million a second, in steps of half a second");
        assertEquals(0f, VeldanavaStats.regenStep(5_000_000.0, 0.0), 0f, "0 = off");
        assertEquals(0f, VeldanavaStats.regenStep(-1.0, 1.0), 0f);
        assertTrue(VeldanavaStats.regenStep(5_000_000.0, 1.0E9) <= 5_000_000f, "a silly setting heals at most the whole bar a second");
        assertTrue(VeldanavaStats.regenerating(true, false, 100, 0, 10f, 100f));
        assertFalse(VeldanavaStats.regenerating(false, false, 100, 0, 10f, 100f), "out of combat there is no regeneration step (the fight reset refills him)");
        assertFalse(VeldanavaStats.regenerating(true, true, 100, 0, 10f, 100f), "not while he changes body");
        assertFalse(VeldanavaStats.regenerating(true, false, 100, 0, 100f, 100f), "nothing to heal at full health");
        assertFalse(VeldanavaStats.regenerating(true, false, 100, 0, 0f, 100f), "a dead boss does not heal");
        long broken = VeldanavaStats.breakRegen(100, 0);
        assertEquals(100 + VeldanavaStats.REGEN_BROKEN_TICKS, broken);
        assertFalse(VeldanavaStats.regenerating(true, false, 150, broken, 10f, 100f), "a counter hit stops it");
        assertTrue(VeldanavaStats.regenerating(true, false, broken, broken, 10f, 100f), "and it resumes after 6 seconds");
        assertEquals(broken + 50, VeldanavaStats.breakRegen(100, broken + 50), "a break never shortens an older one");
    }

    @Test
    void fallFireAndTheWitherAreNullified() {
        for (String t : new String[]{"fall", "in_fire", "on_fire", "lava", "hot_floor", "wither"}) assertTrue(VeldanavaStats.nullifiesDamage(t), t);
        for (String t : new String[]{"magic", "indirect_magic", "player_attack", "generic", "out_of_world"}) assertFalse(VeldanavaStats.nullifiesDamage(t), t);
    }

    @Test
    void everyBodyHasItsOwnBarAndItPulses() {
        Set<String> looks = new HashSet<>();
        for (int f = 0; f < VeldanavaRules.FORMS; f++) {
            VeldanavaBar.Style s = VeldanavaBar.style(f);
            assertTrue(looks.add(s.color() + "/" + s.overlay()), "two bodies share a bar look");
            assertEquals(VeldanavaRules.formName(f), s.title(), "the bar names the body");
            assertEquals(s.color(), VeldanavaBar.color(f, VeldanavaBar.CALM, 0));
            assertEquals("RED", VeldanavaBar.color(f, VeldanavaBar.BROKEN, 0));
            assertEquals("YELLOW", VeldanavaBar.color(f, VeldanavaBar.EXPOSED, 0));
            assertFalse(VeldanavaBar.color(f, VeldanavaBar.TRANSFORMING, 0).equals(VeldanavaBar.color(f, VeldanavaBar.TRANSFORMING, 5)) && !s.color().equals("WHITE"), "the bar flashes while he changes body");
            Set<Integer> frames = new HashSet<>();
            for (long t = 0; t < VeldanavaBar.PULSE_PERIOD; t += VeldanavaBar.REFRESH) frames.add(VeldanavaBar.pulseColor(f, VeldanavaBar.CALM, t, 1f));
            assertTrue(frames.size() >= 4, "the title colour pulses through at least four steps");
            assertEquals(s.base(), VeldanavaBar.pulseColor(f, VeldanavaBar.CALM, 0, 1f), "a pulse starts at the base colour");
        }
        assertTrue(Set.of("BLUE", "WHITE", "PURPLE").containsAll(Set.of(VeldanavaBar.style(0).color(), VeldanavaBar.style(1).color(), VeldanavaBar.style(2).color())));
        assertEquals(VeldanavaBar.DEAD, VeldanavaBar.state(true, true, true, true, true, true, true), "the loudest state wins");
        assertEquals(VeldanavaBar.TRANSFORMING, VeldanavaBar.state(false, true, true, true, true, true, true));
        assertEquals(VeldanavaBar.REGENERATING, VeldanavaBar.state(false, false, false, false, false, false, true));
        assertEquals("Ultraspeed Regeneration", VeldanavaBar.status(VeldanavaBar.REGENERATING, ""));
        assertEquals("Megiddo", VeldanavaBar.status(VeldanavaBar.CASTING, "Megiddo"));
        assertEquals("4,999,200 / 5,000,000", VeldanavaBar.healthText(4_999_200L, 5_000_000L));
        assertEquals("0", VeldanavaBar.group(0));
        assertEquals("999", VeldanavaBar.group(999));
        assertEquals("1,000", VeldanavaBar.group(1000));
        assertEquals(0xFFFFFF, VeldanavaBar.mix(0, 0xFFFFFF, 1.0));
        assertEquals(0, VeldanavaBar.mix(0, 0xFFFFFF, -4.0), "mix clamps");
    }

    @Test
    void theNewMovesAreAppendedBoundedAndGeometryHolds() {
        assertEquals(18, VeldanavaRules.MOVES);
        assertEquals(11, VeldanavaRules.CLASSIC_MOVES);
        assertEquals(VeldanavaRules.GENESIS + 1, VeldanavaRules.ABSOLUTE_SEVERANCE, "the new ids are appended after the old ones");
        String[] names = {"absolute_severance", "black_lightning", "black_lightning_blast", "death_tornado", "disintegration", "hell_flare", "megiddo"};
        for (int i = 0; i < names.length; i++) assertEquals(names[i], VeldanavaRules.spec(VeldanavaRules.CLASSIC_MOVES + i).name());
        assertEquals(3, VeldanavaRules.bolts(50));
        assertEquals(0, VeldanavaRules.bolts(0));
        assertEquals(1, VeldanavaRules.bolts(1));
        assertEquals(VeldanavaRules.MAX_BEAMS, VeldanavaRules.beams(100));
        assertEquals(3, VeldanavaRules.beams(1));
        assertEquals(0, VeldanavaRules.beams(0));
        VeldanavaRules.Spec m = VeldanavaRules.spec(VeldanavaRules.MEGIDDO);
        int last = -1;
        for (int i = 0; i < VeldanavaRules.MAX_BEAMS; i++) {
            int t = VeldanavaRules.strikeTick(i, VeldanavaRules.MAX_BEAMS, m.active());
            assertTrue(t >= last && t < m.active(), "the strikes are spread inside the active part, in order");
            last = t;
        }
        assertEquals(0, VeldanavaRules.hellRingRadius(0, 0, 18.0), 0.0);
        assertEquals(18.0, VeldanavaRules.hellRingRadius(0, 12, 18.0), 1.0E-9, "the first ring reaches the edge in 12 ticks");
        assertEquals(0.0, VeldanavaRules.hellRingRadius(2, 8, 18.0), 0.0, "the third ring starts 8 ticks late");
        assertEquals(18.0, VeldanavaRules.hellRingRadius(2, 99, 18.0), 0.0, "and never passes the edge");
        assertTrue(VeldanavaRules.inRingBand(10.0, 9.0, 2.2) && !VeldanavaRules.inRingBand(14.0, 9.0, 2.2) && !VeldanavaRules.inRingBand(1.0, 0.0, 2.2));
        assertTrue(VeldanavaRules.withinHeight(-4.9, 5.0) && !VeldanavaRules.withinHeight(5.1, 5.0));
        assertEquals(0.0, VeldanavaRules.tornadoRemaining(0.1, 0.22), 0.0);
        assertTrue(VeldanavaRules.TORNADO_SPEED < 0.28, "a sprinting player (about 0.28 blocks a tick) outruns the tornado");
        assertTrue(VeldanavaRules.bigCast(VeldanavaRules.MEGIDDO) && VeldanavaRules.bigCast(VeldanavaRules.GENESIS) && !VeldanavaRules.bigCast(VeldanavaRules.BITE));
        for (int id = VeldanavaRules.CLASSIC_MOVES; id < VeldanavaRules.MOVES; id++) assertTrue(VeldanavaRules.spec(id).windup() >= 16, "every new move has a readable telegraph");
    }

    @Test
    void theWiringIsThere() throws IOException {
        String boss = read("entity/veldanava/VeldanavaBoss.java");
        for (String must : new String[]{"HealthCap.raise()", "fireImmune()", "if (anime()) return false;", "removeAllEffects()", "VeldanavaStats.regenerating(", "source.is(COUNTER)", "VeldanavaStats.scaledDamage(", "animateBar(", "DamageTypeTags.IS_FIRE",
                "VeldanavaRules.CLASSIC_MOVES", "applyStats(", "Attributes.KNOCKBACK_RESISTANCE"})
            assertTrue(boss.contains(must), "the boss lost: " + must);
        assertTrue(boss.contains("dpsBudget.bypass(() -> setHealth(Math.min(getMaxHealth(), getHealth() + heal)))"), "the regeneration is his own health: it goes around the heal guard");
        String moves = read("entity/veldanava/VeldanavaMoves.java");
        for (String shape : new String[]{"VELD_SEVERANCE", "VELD_BLACK_LIGHTNING", "VELD_BLACK_BLAST", "VELD_TORNADO", "VELD_DISINTEGRATION", "VELD_HELL_FLARE", "VELD_MEGIDDO"})
            assertTrue(moves.contains("VfxShape." + shape), "no move sends " + shape);
        String cfg = read("entity/veldanava/VeldanavaConfig.java");
        assertTrue(cfg.contains("\"classicMode\"") && cfg.contains("false"), "the classic switch exists and is off by default");
        assertTrue(Files.readString(Path.of("src/main/resources/data/nusmp/tags/damage_type/veldanava_counter.json")).contains("nusmp:conceptual_severance"), "the counter-element tag lists the Conceptual Severance damage");
        assertTrue(read("client/veldanava/VeldanavaRenderer.java").contains("VeldanavaRules.bigCast("), "the renderer plays the big cast clip for the new moves");
        String vm = Files.readString(SRC.resolve("vfx/client/VfxManager.java"), StandardCharsets.UTF_8);
        for (String layer : new String[]{"VeldSeveranceLayer", "VeldBlackLightningLayer", "VeldBlackBlastLayer", "VeldTornadoLayer", "VeldDisintegrationLayer", "VeldHellFlareLayer", "VeldMegiddoLayer"})
            assertTrue(vm.contains("register(new " + layer + "()"), layer + " is not registered");
    }
}
