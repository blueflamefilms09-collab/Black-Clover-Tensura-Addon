package com.newuniverse.nusmp.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner's crash report (4.25.0, "IllegalStateException: Not building!" in WeaponUpgrade.emitQuads): the orbit loop kept one cached body buffer and asked for the
 * additive glow buffer inside the same loop; asking for another render type ends the buffer before it, so the next orbit wrote into an ended builder. A draw buffer must be
 * fetched right where it is used. Source scan: no cached body buffer, and the orbit's body draw fetches its buffer inline.
 */
class WeaponUpgradeBufferScanTest {
    @Test
    void orbitBodyBufferIsFetchedPerUseNotCached() throws IOException {
        String s = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/WeaponUpgrade.java"));
        assertFalse(s.contains("VertexConsumer body"), "a cached body buffer is ended by the glow buffer request");
        assertTrue(s.contains("emitQuads(pose.last(), buffers.getBuffer(RenderType.entityCutout(mesh.tex)), o.verts"));
    }
}
