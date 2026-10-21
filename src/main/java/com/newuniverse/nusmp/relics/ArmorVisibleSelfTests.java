package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.ArmorPainter;
import com.newuniverse.nusmp.client.relics.ArmorPainterV2;
import com.newuniverse.nusmp.client.relics.worn.ArmorSlotPlan;
import com.newuniverse.nusmp.client.relics.worn.ArmorVisible;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

/**
 * The self-test {@code relic.armor_visible} (run by the CI server smoke test, see {@link SelfTests}; pure checks only, no client class is loaded that touches Minecraft's client side, so it
 * runs on a dedicated server): a worn armour relic is never invisible.
 * <ul>
 *   <li>the decision table of {@link ArmorVisible#checkTable()}: for every slot, circumstance and option the plan leaves something on the body unless the player switched armour visuals off,
 *       and for every combination of "what was really drawn" the answer is a drawn thing, the player's choice, the distance, or the failsafe, never nothing;</li>
 *   <li>every HEAD, CHEST and FEET relic has a painted layer (both painters) that is not empty, and the plain layer used when a painter fails covers the head, torso, arm and leg areas;</li>
 *   <li>the two render options (models on or off) and the three views leave a verdict with a visible result for every armour relic.</li>
 * </ul>
 */
public final class ArmorVisibleSelfTests {
    private ArmorVisibleSelfTests() {}

    public static void register() { SelfTests.register("relic.armor_visible", server -> run()); }

    static SelfTests.Result run() {
        String table = ArmorVisible.checkTable();
        if (table != null) return SelfTests.Result.fail("decision table: " + table);
        int[] plain = ArmorVisible.solidLayer(0x6A2A7A, 0xD9A441);
        if (plain.length != ArmorPainter.W * ArmorPainter.H || !ArmorVisible.nonEmpty(plain)) return SelfTests.Result.fail("the plain fallback layer is empty or the wrong size");
        int[][] regions = {{0, 0, 32, 16}, {16, 16, 24, 16}, {40, 16, 16, 16}, {0, 16, 16, 16}};
        for (int[] r : regions) {
            boolean any = false;
            for (int y = r[1]; y < r[1] + r[3] && !any; y++) for (int x = r[0]; x < r[0] + r[2]; x++) if (plain[y * ArmorPainter.W + x] >>> 24 != 0) { any = true; break; }
            if (!any) return SelfTests.Result.fail("the plain fallback layer leaves the area " + r[0] + "," + r[1] + " empty");
        }
        int relics = 0;
        for (RelicDef d : Relics.all()) {
            if (!d.slot().isArmor() || d.isAdopted()) continue;
            relics++;
            long seed = d.id().hashCode();
            if (!ArmorVisible.nonEmpty(ArmorPainter.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), seed))) return SelfTests.Result.fail(d.id() + ": the older painter returns an empty layer");
            if (!ArmorVisible.nonEmpty(ArmorPainterV2.paint(d.look(), d.slot(), d.paletteMain(), d.paletteTrim(), d.paletteGlow(), seed))) return SelfTests.Result.fail(d.id() + ": the 1.110.0 painter returns an empty layer");
            for (boolean models : new boolean[]{true, false}) for (WornPlan.View view : WornPlan.View.values()) for (boolean has : new boolean[]{true, false}) {
                ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide(d.slot().name(), has, false, models, false, view, false);
                boolean cameraInHead = view == WornPlan.View.SELF_FIRST && d.slot() == RelicSlot.HEAD;
                if (!v.visible() && !cameraInHead) return SelfTests.Result.fail(d.id() + " is invisible: models=" + models + " view=" + view + " hasModel=" + has);
            }
        }
        if (relics < 100) return SelfTests.Result.fail("only " + relics + " armour-slot relics found");
        return SelfTests.Result.ok("decision table holds; " + relics + " armour relics have non-empty painted layers and a visible verdict in every circumstance");
    }
}
