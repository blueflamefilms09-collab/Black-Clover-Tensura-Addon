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
 * Why (3.31.0, CLAUDE.md rules 9 and 11, docs/veldanava.md): the base Tensura entities that draw Veldanava's seven Tensura moves must stay only a look and a sound, bounded,
 * switchable and cleaned up. The numbers (beam angles, lives, the cap) are pure; the rest is read from the real files because the game is not run here.
 */
class VeldanavaTensuraTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp/entity/veldanava");

    private static String code(String name) throws IOException {
        return Files.readString(SRC.resolve(name + ".java"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    @Test
    void theSevenIdsAreDistinctAndTheseAreTheTensuraPaths() {
        Set<String> ids = new HashSet<>(java.util.List.of(VeldanavaTensura.IDS));
        assertEquals(7, VeldanavaTensura.IDS.length);
        assertEquals(7, ids.size());
        for (String id : new String[]{"severance_cutter", "black_lightning_bolt", "black_lightning_blast", "death_tornado", "disintegration", "hell_flare", "megiddo_bubble"})
            assertTrue(ids.contains(id), "missing " + id);
    }

    @Test
    void theBeamAnglesMatchTensurasConventionAndTheBeamsSpreadEvenly() {
        assertEquals(Math.PI / 2, VeldanavaTensura.beamYaw(0f), 1.0E-6);                       // Minecraft yaw 0 looks along +Z, Tensura's beam angle pi/2 is +Z
        assertEquals(Math.PI, VeldanavaTensura.beamYaw(90f), 1.0E-6);                          // Minecraft yaw 90 looks along -X, angle pi is -X
        assertEquals(-0.5, VeldanavaTensura.beamPitch((float) Math.toDegrees(0.5)), 1.0E-5);
        assertEquals(0f, VeldanavaTensura.beamStep(0, 6));
        assertEquals(60f, VeldanavaTensura.beamStep(1, 6));
        assertEquals(300f, VeldanavaTensura.beamStep(5, 6));
        assertEquals(0f, VeldanavaTensura.beamStep(3, 0));
    }

    @Test
    void everyLifeIsShortAndTheCountsAreCapped() {
        assertEquals(1, VeldanavaTensura.clampLife(-5));
        assertEquals(1, VeldanavaTensura.clampLife(0));
        assertEquals(VeldanavaTensura.MAX_LIFE, VeldanavaTensura.clampLife(100000));
        assertTrue(VeldanavaTensura.MAX_LIFE <= 120 && VeldanavaTensura.MAX_LIVE <= 20 && VeldanavaTensura.MAX_BEAMS <= 6, "the worst case grew");
        assertTrue(VeldanavaTensura.CUTTER_SPEED > 0f && VeldanavaTensura.CUTTER_SPEED <= 4f);
    }

    @Test
    void theTensuraEntitiesAreOnlyALookAndAreSwitchableAndCleanedUp() throws IOException {
        String t = code("VeldanavaTensura");
        for (String off : new String[]{"setDamage(0f)", "setSecondaryDamage(0f)", "setKnockForce(0f)", "setBurnTicks(0)", "setMobEffect(null)", "setTensuraDamage(0f)", "setShouldPlaceFire(false)", "setExplosionRadius(0f)", "setVisualOnly(true)", "setRadius(0f)", "setTickEachHit(NEVER_HIT)"})
            assertTrue(t.contains(off), "a Tensura entity keeps its own harm: " + off);
        assertTrue(t.contains("MAX_LIVE") && t.contains("live.remove(0).discard()") && t.contains("clampLife(life)"), "the live count or the life is not capped");
        assertFalse(t.contains("getEntities") || t.contains("getChunk(") || t.contains("setBlock(") || t.contains("Minecraft"), "the helper scans, loads or changes the world on its own");
        assertTrue(t.contains("catch (Throwable") && t.contains("return null;"), "a failed spawn must fall back instead of throwing");
        assertTrue(t.contains("!VeldanavaConfig.classic()") && t.contains("VeldanavaConfig.tensuraMoves()"), "the setting or the classic mode is not honoured");
        String boss = code("VeldanavaBoss");
        assertTrue(boss.split("tensura\\.clear\\(\\)", -1).length - 1 >= 3, "the boss must clear the Tensura entities on death, removal and reset");
        String cfg = code("VeldanavaConfig");
        assertTrue(cfg.contains("\"tensuraMoves\"") && cfg.contains("define(\"tensuraMoves\", true)"), "the setting is missing or off by default");
    }

    @Test
    void everyMoveKeepsItsOldEffectAsTheFallbackAndItsBudget() throws IOException {
        String m = code("VeldanavaMoves");
        for (String shape : new String[]{"VELD_SEVERANCE", "VELD_BLACK_LIGHTNING", "VELD_BLACK_BLAST", "VELD_TORNADO", "VELD_DISINTEGRATION", "VELD_HELL_FLARE", "VELD_MEGIDDO"})
            assertTrue(m.contains(shape), "the 3.30.0 effect " + shape + " is gone (it is the fallback)");
        for (String id : new String[]{"SEVERANCE", "BOLT", "BLAST", "TORNADO", "DISINTEGRATION", "HELL_FLARE", "MEGIDDO"})
            assertTrue(m.contains("VeldanavaTensura." + id), "the move does not use the Tensura " + id);
        assertTrue(m.contains("b.tensura()."), "the moves must spawn through the boss's own tracked helper");
        assertFalse(m.contains("addFreshEntity"), "a move adds an entity itself");
    }
}
