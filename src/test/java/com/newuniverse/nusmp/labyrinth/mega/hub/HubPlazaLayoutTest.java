package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the plaza is written by a Python script and placed by Java, and the two must agree to the block: the template's size, the row that holds the
 * paving, and the cell where the home gate stands once the template's corner is put at {@link HubPlazaLayout#originX()}. If the gate were one block off,
 * {@code hub.plaza} would fail in the CI game, and a player would walk into an arch with a gate-shaped hole; so the test opens {@code plaza.nbt} (a
 * minimal NBT reader, no Minecraft classes) and checks every promise the Java makes about it: the size, that the 3 x 4 plane of {@code
 * nusmp:labyrinth_gate} is exactly where {@link HubLayout} says, that nothing else in the file is a mod block, that the paving is whole, that the arrival
 * column is free, that the data version is 1.21.1's. The arithmetic of {@link HubPlazaLayout} is pinned against {@link HubLayout} too.
 */
class HubPlazaLayoutTest {
    private static final Path FILE = HubTestIo.DATA.resolve("structure/hub/plaza.nbt");
    private static final String GATE = "nusmp:labyrinth_gate";

    @Test
    void theOriginPutsTheMiddleOfTheTemplateOnThePlazaCentreAndItsPavingOnTheGround() {
        assertEquals(HubLayout.PLAZA_X, HubPlazaLayout.originX() + HubPlazaLayout.SIZE_X / 2);
        assertEquals(HubLayout.PLAZA_Z, HubPlazaLayout.originZ() + HubPlazaLayout.SIZE_Z / 2);
        assertEquals(HubLayout.SURFACE_Y - 1, HubPlazaLayout.originY() + HubPlazaLayout.FLOOR_ROW, "the paving is the top block of the ground, so feet stand on SURFACE_Y");
        assertEquals(1, HubPlazaLayout.SIZE_X % 2);
        assertEquals(1, HubPlazaLayout.SIZE_Z % 2);
    }

    @Test
    void theHomeGatePlaneIsThreeWideFourHighAndContainsTheRecordedCentre() {
        int cells = 0;
        for (int x = HubLayout.HOME_GATE_X - 3; x <= HubLayout.HOME_GATE_X + 3; x++) {
            for (int y = HubLayout.HOME_GATE_Y - 4; y <= HubLayout.HOME_GATE_Y + 4; y++) {
                if (HubPlazaLayout.isHomeGateCell(x, y, HubLayout.HOME_GATE_Z)) cells++;
            }
        }
        assertEquals(12, cells, "3 x 4");
        assertTrue(HubPlazaLayout.isHomeGateCell(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z), "the centre is a gate cell");
        assertEquals(HubLayout.SURFACE_Y, HubPlazaLayout.gateBottomY(), "a walking player (feet on SURFACE_Y) enters the plane");
        assertFalse(HubPlazaLayout.isHomeGateCell(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z + 1), "one deep");
        assertTrue(HubPlazaLayout.inBox(HubLayout.HOME_GATE_X, HubPlazaLayout.gateBottomY(), HubLayout.HOME_GATE_Z) && HubPlazaLayout.inBox(HubLayout.HOME_GATE_X, HubPlazaLayout.gateTopY(), HubLayout.HOME_GATE_Z), "the plane is inside the template");
        for (int x = HubLayout.HOME_GATE_X - 1; x <= HubLayout.HOME_GATE_X + 1; x++) {
            assertEquals(HubLayout.GateKind.HOME, HubLayout.gateNear(x, HubPlazaLayout.gateBottomY(), HubLayout.HOME_GATE_Z), "every plane cell is the home gate");
            assertEquals(HubLayout.GateKind.HOME, HubLayout.gateNear(x, HubPlazaLayout.gateTopY(), HubLayout.HOME_GATE_Z));
        }
    }

    @Test
    void theTemplateCoversTheChunksTheStampLoads() {
        assertEquals(-1, HubPlazaLayout.minChunkX());
        assertEquals(1, HubPlazaLayout.maxChunkX());
        assertEquals(-1, HubPlazaLayout.minChunkZ());
        assertEquals(1, HubPlazaLayout.maxChunkZ());
        assertTrue(HubPlazaLayout.SIZE_Y + HubPlazaLayout.originY() <= 319, "the template fits under the build limit");
        assertTrue(HubPlazaLayout.originY() >= -64, "and above the bottom");
        assertTrue(HubPlazaLayout.maxY() > HubLayout.SURFACE_Y + 8, "tall enough for the gatehouse crown");
    }

    // ------------------------------------------------------------------------------------------------ the file
    @SuppressWarnings("unchecked")
    @Test
    void thePlazaTemplateIsExactlyWhatTheJavaPromises() throws IOException {
        Map<String, Object> root = HubTestIo.nbt(FILE);
        assertEquals(3955L, root.get("DataVersion"), "Minecraft 1.21.1");
        List<Object> size = (List<Object>) root.get("size");
        assertEquals((long) HubPlazaLayout.SIZE_X, size.get(0));
        assertEquals((long) HubPlazaLayout.SIZE_Y, size.get(1));
        assertEquals((long) HubPlazaLayout.SIZE_Z, size.get(2));
        List<Object> palette = (List<Object>) root.get("palette");
        String[] names = new String[palette.size()];
        for (int i = 0; i < names.length; i++) names[i] = (String) ((Map<String, Object>) palette.get(i)).get("Name");
        Map<Long, String> byPos = new HashMap<>();
        int gates = 0;
        Set<String> foreign = new HashSet<>();
        for (Object o : (List<Object>) root.get("blocks")) {
            Map<String, Object> b = (Map<String, Object>) o;
            List<Object> pos = (List<Object>) b.get("pos");
            long x = (Long) pos.get(0), y = (Long) pos.get(1), z = (Long) pos.get(2);
            assertTrue(x >= 0 && x < HubPlazaLayout.SIZE_X && y >= 0 && y < HubPlazaLayout.SIZE_Y && z >= 0 && z < HubPlazaLayout.SIZE_Z, "a block outside the template at " + x + "," + y + "," + z);
            String name = names[((Long) b.get("state")).intValue()];
            assertNotNull(name);
            byPos.put(key((int) x, (int) y, (int) z), name);
            if (name.equals(GATE)) gates++;
            else if (!name.startsWith("minecraft:")) foreign.add(name);
        }
        assertTrue(foreign.isEmpty(), "only vanilla blocks and the gate are allowed, found " + foreign);
        assertEquals(12, gates, "the gate block appears exactly in the 3 x 4 plane");
        for (int x = HubLayout.HOME_GATE_X - 1; x <= HubLayout.HOME_GATE_X + 1; x++) {
            for (int y = HubPlazaLayout.gateBottomY(); y <= HubPlazaLayout.gateTopY(); y++) {
                int lx = x - HubPlazaLayout.originX(), ly = y - HubPlazaLayout.originY(), lz = HubLayout.HOME_GATE_Z - HubPlazaLayout.originZ();
                assertEquals(GATE, byPos.get(key(lx, ly, lz)), "the gate plane at world " + x + "," + y + "," + HubLayout.HOME_GATE_Z);
            }
        }
        assertEquals(HubPlazaLayout.gateLocalX(), HubLayout.HOME_GATE_X - HubPlazaLayout.originX());
        assertEquals(GATE, byPos.get(key(HubPlazaLayout.gateLocalX(), HubPlazaLayout.gateLocalY(), HubPlazaLayout.gateLocalZ())), "the recorded centre is a gate");
        // the paving is whole inside the disc: a solid block on the floor row everywhere with r <= 16, and a foundation under it
        int cx = HubPlazaLayout.SIZE_X / 2, cz = HubPlazaLayout.SIZE_Z / 2;
        for (int x = 0; x < HubPlazaLayout.SIZE_X; x++) {
            for (int z = 0; z < HubPlazaLayout.SIZE_Z; z++) {
                if (Math.hypot(x - cx, z - cz) > 16.0) continue;
                String floor = byPos.get(key(x, HubPlazaLayout.FLOOR_ROW, z));
                assertNotNull(floor, "no paving at local " + x + "," + z);
                assertFalse(floor.equals("minecraft:air") || floor.equals("minecraft:water"), "a hole in the paving at local " + x + "," + z + ": " + floor);
                assertNotNull(byPos.get(key(x, 0, z)), "no foundation at local " + x + "," + z);
            }
        }
        // the arrival cell: solid paving under it, three free cells above it
        int sx = HubLayout.SPAWN_X - HubPlazaLayout.originX(), sz = HubLayout.SPAWN_Z - HubPlazaLayout.originZ();
        String under = byPos.get(key(sx, HubPlazaLayout.FLOOR_ROW, sz));
        assertNotNull(under);
        assertFalse(under.equals("minecraft:air"), "solid under the arrival cell");
        for (int k = 1; k <= 3; k++) {
            String above = byPos.get(key(sx, HubPlazaLayout.FLOOR_ROW + k, sz));
            assertTrue(above == null || above.equals("minecraft:air"), "the arrival column is free at +" + k + ", found " + above);
        }
    }

    @Test
    void theTemplateLivesOutsideTheMegaFolderSoTheMegaCheckerLeavesItAlone() {
        assertTrue(HubPlazaLayout.TEMPLATE.startsWith("hub/"));
        assertTrue(FILE.toString().replace('\\', '/').contains("structure/hub/plaza.nbt"));
    }

    private static long key(int x, int y, int z) { return ((long) x << 40) | ((long) y << 20) | (long) z; }
}
