package com.newuniverse.nusmp.client.relics.worn;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The last-resort rule for worn armour relics: whatever was or was not drawn, a worn piece is never invisible unless the player chose it. */
class ArmorVisibleTest {
    private static final String[] SLOTS = {"HEAD", "CHEST", "FEET"};

    @Test
    void theWholeDecisionTableHolds() {
        assertNull(ArmorVisible.checkTable());
    }

    @Test
    void nothingDrawnMeansTheFailsafeDraws() {
        for (String slot : SLOTS) {
            ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide(slot, true, false, true, false, WornPlan.View.SELF_THIRD, false);
            ArmorVisible.Facts nothing = new ArmorVisible.Facts(0, false, false, true, slot.equals("CHEST"));
            assertEquals(ArmorVisible.Outcome.FAILSAFE, ArmorVisible.resolve(v, nothing), slot);
            assertTrue(ArmorVisible.needsFailsafe(v, nothing));
        }
    }

    @Test
    void aPaintedLayerWhoseTextureIsMissingIsNotACoverUp() {
        ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide("FEET", false, false, true, false, WornPlan.View.OTHER, false);
        assertEquals(ArmorSlotPlan.Layer.PAINTED, v.layer());
        assertEquals(ArmorVisible.Outcome.FAILSAFE, ArmorVisible.resolve(v, new ArmorVisible.Facts(0, false, true, false, false)));
        assertEquals(ArmorVisible.Outcome.PAINTED, ArmorVisible.resolve(v, new ArmorVisible.Facts(0, false, true, true, false)));
    }

    @Test
    void aModelThatDrewNothingDoesNotCount() {
        ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide("HEAD", true, false, true, false, WornPlan.View.OTHER, false);
        assertEquals(ArmorSlotPlan.Layer.EMPTY, v.layer());                                   // the vanilla layer drew nothing, the plan relied on the model
        assertEquals(ArmorVisible.Outcome.FAILSAFE, ArmorVisible.resolve(v, new ArmorVisible.Facts(0, false, false, true, false)));
        assertEquals(ArmorVisible.Outcome.MODEL, ArmorVisible.resolve(v, new ArmorVisible.Facts(3, false, false, true, false)));
    }

    @Test
    void ornamentsAloneDoNotMakeAChestPieceVisible() {
        ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide("CHEST", true, false, true, false, WornPlan.View.OTHER, false);
        assertEquals(ArmorVisible.Outcome.FAILSAFE, ArmorVisible.resolve(v, new ArmorVisible.Facts(4, false, false, true, true)));
        assertEquals(ArmorVisible.Outcome.MODEL, ArmorVisible.resolve(v, new ArmorVisible.Facts(4, false, true, true, true)));
    }

    @Test
    void theTwoChoicesOfThePlayerAreObeyed() {
        ArmorSlotPlan.Verdict hidden = ArmorSlotPlan.decide("CHEST", true, false, true, true, WornPlan.View.OTHER, false);
        assertEquals(ArmorVisible.Outcome.OFF_ON_PURPOSE, ArmorVisible.resolve(hidden, new ArmorVisible.Facts(0, false, false, false, true)));
        ArmorSlotPlan.Verdict first = ArmorSlotPlan.decide("HEAD", true, false, true, false, WornPlan.View.SELF_FIRST, false);
        assertEquals(ArmorVisible.Outcome.OFF_ON_PURPOSE, ArmorVisible.resolve(first, new ArmorVisible.Facts(0, false, false, false, false)));
    }

    @Test
    void theFallbackLayerIsAFullImageThatCoversEveryBodyPart() {
        int[] px = ArmorVisible.solidLayer(0x6A2A7A, 0xD9A441);
        assertEquals(64 * 32, px.length);
        assertTrue(ArmorVisible.nonEmpty(px));
        assertFalse(ArmorVisible.nonEmpty(new int[64 * 32]));
        assertFalse(ArmorVisible.nonEmpty(null));
        int[][] areas = {{0, 0, 32, 16}, {16, 16, 24, 16}, {40, 16, 16, 16}, {0, 16, 16, 16}};
        for (int[] a : areas) {
            boolean any = false;
            for (int y = a[1]; y < a[1] + a[3]; y++) for (int x = a[0]; x < a[0] + a[2]; x++) if (px[y * 64 + x] >>> 24 != 0) any = true;
            assertTrue(any, "area " + a[0] + "," + a[1]);
        }
    }

    @Test
    void theTraceKeepsTheVanillaLayersWitnessUntilItIsRead() {
        UUID id = UUID.randomUUID();
        assertFalse(ArmorTrace.takePainted(id, "HEAD"));
        ArmorTrace.markPainted(id, "HEAD");
        ArmorTrace.markPainted(id, "FEET");
        assertFalse(ArmorTrace.takePainted(id, "CHEST"));
        assertTrue(ArmorTrace.takePainted(id, "HEAD"));
        assertFalse(ArmorTrace.takePainted(id, "HEAD"));
        assertTrue(ArmorTrace.takePainted(id, "FEET"));
        assertTrue(ArmorTrace.firstWarning("some_relic"));
        assertFalse(ArmorTrace.firstWarning("some_relic"));
        ArmorTrace.clear();
        assertTrue(ArmorTrace.firstWarning("some_relic"));
        ArmorTrace.clear();
    }

    @Test
    void theWornLayerIsWiredToTheFailsafeAndTheTexturesAreHealed() throws IOException {
        String layer = read("src/main/java/com/newuniverse/nusmp/client/relics/RelicWornLayer.java");
        assertTrue(layer.contains("armourFailsafe(") && layer.contains("ArmorVisible.resolve("), "the failsafe is wired into the worn layer");
        assertTrue(layer.contains("WornLibDraw.resetDrawRecord()") && layer.contains("WornLibDraw.piecesDrawn("), "the layer reads what the library really drew");
        String hook = read("src/main/java/com/newuniverse/nusmp/client/relics/worn/lib/ArmorSlotClient.java");
        assertTrue(hook.contains("ArmorTrace.markPainted("), "the armour-model hook records that the painted layer was asked for");
        String client = read("src/main/java/com/newuniverse/nusmp/client/relics/RelicClient.java");
        assertTrue(client.contains("RelicTextures.onClientTick()"), "the first-frame texture check is hooked to the client tick");
        String tex = read("src/main/java/com/newuniverse/nusmp/client/relics/RelicTextures.java");
        assertTrue(tex.contains("public static int verify()") && tex.contains("PAINTED_OK"), "the texture check exists");
        assertTrue(tex.indexOf("try {", tex.indexOf("for (RelicDef d : Relics.all())")) < tex.indexOf("ArmorPainterV2.paint(", tex.indexOf("for (RelicDef d : Relics.all())")),
                "every relic is painted inside its own try block");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Path.of(path)), StandardCharsets.UTF_8);
    }
}
