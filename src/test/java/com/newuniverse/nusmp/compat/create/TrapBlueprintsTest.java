package com.newuniverse.nusmp.compat.create;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the drawings are the only thing standing between the Create traps and a corridor that locks a party in, so every family at every length, height, trigger and seed is
 * proved safe, and a walker who waits for the quiet crosses every one of them.
 */
class TrapBlueprintsTest {
    private static List<Blueprint> all() {
        List<Blueprint> out = new ArrayList<>();
        for (TrapFamily f : TrapFamily.values()) {
            for (int core = TrapRules.MIN_CORE; core <= TrapRules.MAX_CORE; core++) {
                for (int h = TrapRules.MIN_HEADROOM; h <= TrapRules.MAX_HEADROOM; h += 2) {
                    for (TriggerKind t : TriggerKind.values()) {
                        for (long seed = 1; seed <= 4; seed++) out.add(TrapBlueprints.build(f, core, h, t, seed * 7919L));
                    }
                }
            }
        }
        return out;
    }

    @Test
    void everyDrawingIsSafe() {
        for (Blueprint b : all()) {
            List<String> p = TrapBlueprints.problems(b);
            assertTrue(p.isEmpty(), b.family() + " core " + (b.length() - 2) + " h " + b.headroom() + " " + b.trigger() + ": " + p);
        }
    }

    @Test
    void theValidatorSeesABlockInTheWalkway() {
        Blueprint good = TrapBlueprints.build(TrapFamily.SAW_RUN, 7, 4, TriggerKind.PLATE, 1L);
        List<Blueprint.Placed> bad = new ArrayList<>(good.parts());
        bad.add(new Blueprint.Placed(0, 1, 4, Part.GEARBOX, null, null, null));
        Blueprint broken = new Blueprint(good.family(), good.trigger(), good.length(), good.headroom(), bad, good.bands(), good.power(), good.rams(), good.conveyor(), 1L);
        assertFalse(TrapBlueprints.problems(broken).isEmpty());
        // a ram in the middle column, a band that hurts too long, a belt too strong to walk against
        Blueprint ram = new Blueprint(good.family(), good.trigger(), good.length(), good.headroom(), good.parts(), good.bands(), good.power(), List.of(new int[] {0, 1, 3}), null, 1L);
        assertFalse(TrapBlueprints.problems(ram).isEmpty());
        Blueprint.Band b0 = good.bands().get(0);
        List<Blueprint.Band> long1 = List.of(new Blueprint.Band(b0.xMin(), b0.xMax(), b0.yMax(), b0.zMin(), b0.zMax(), 0, 40, 10, 30, b0.kind(), 1.0, -1));
        assertFalse(TrapBlueprints.problems(new Blueprint(good.family(), good.trigger(), good.length(), good.headroom(), good.parts(), long1, good.power(), good.rams(), null, 1L)).isEmpty());
        Blueprint belt = new Blueprint(good.family(), good.trigger(), good.length(), good.headroom(), good.parts(), good.bands(), good.power(), good.rams(), new Blueprint.Conveyor(2, 4, 0.2), 1L);
        assertFalse(TrapBlueprints.problems(belt).isEmpty());
        // no lever at one end
        List<Blueprint.Placed> noLever = new ArrayList<>(good.parts());
        noLever.removeIf(p -> p.part() == Part.LEVER && p.dz() == good.length() - 1);
        assertFalse(TrapBlueprints.problems(new Blueprint(good.family(), good.trigger(), good.length(), good.headroom(), noLever, good.bands(), good.power(), good.rams(), null, 1L)).isEmpty());
    }

    @Test
    void theSameSeedGivesTheSameDrawing() {
        for (TrapFamily f : TrapFamily.values()) {
            Blueprint a = TrapBlueprints.build(f, 7, 5, TriggerKind.TRIPWIRE, 42L), b = TrapBlueprints.build(f, 7, 5, TriggerKind.TRIPWIRE, 42L);
            assertEquals(a.parts(), b.parts(), f.id());
            assertEquals(a.bands(), b.bands(), f.id());
            assertEquals(a.power(), b.power(), f.id());
        }
    }

    @Test
    void everyFamilyUsesCreateParts() {
        for (TrapFamily f : TrapFamily.values()) {
            Blueprint b = TrapBlueprints.build(f, 8, 5, TriggerKind.PLATE, 3L);
            assertTrue(TrapBlueprints.createPartCount(b) >= 8, f.id() + " has too few Create parts: " + TrapBlueprints.createPartCount(b));
            assertFalse(b.bands().isEmpty(), f.id());
        }
        assertFalse(TrapBlueprints.build(TrapFamily.SAW_RUN, 8, 5, TriggerKind.PLATE, 3L).of(Part.SAW).isEmpty());
        assertFalse(TrapBlueprints.build(TrapFamily.PRESS, 8, 5, TriggerKind.PLATE, 3L).of(Part.PISTON).isEmpty());
        assertFalse(TrapBlueprints.build(TrapFamily.DRILL_WALL, 8, 5, TriggerKind.PLATE, 3L).of(Part.DRILL).isEmpty());
        assertFalse(TrapBlueprints.build(TrapFamily.DRILL_WALL, 8, 5, TriggerKind.PLATE, 3L).of(Part.GEARBOX).isEmpty());
        assertFalse(TrapBlueprints.build(TrapFamily.TREADMILL, 8, 5, TriggerKind.PLATE, 3L).of(Part.BELT).isEmpty());
        assertFalse(TrapBlueprints.build(TrapFamily.POUR, 8, 5, TriggerKind.PLATE, 3L).of(Part.SPOUT).isEmpty());
        assertFalse(TrapBlueprints.build(TrapFamily.POUR, 8, 5, TriggerKind.PLATE, 3L).of(Part.PIPE).isEmpty());
        assertFalse(TrapBlueprints.build(TrapFamily.SEQUENCE, 8, 5, TriggerKind.PLATE, 3L).of(Part.NIXIE).isEmpty());
        assertFalse(TrapBlueprints.build(TrapFamily.SEQUENCE, 8, 5, TriggerKind.PLATE, 3L).of(Part.LINK).isEmpty());
    }

    @Test
    void noPartIsSchematicannonAndEveryCreatePartHasAFallback() {
        for (Part p : Part.values()) {
            for (String id : CreateIds.candidates(p)) assertFalse(CreateIds.FORBIDDEN.contains(id), id);
            assertTrue(p.fallback().startsWith("minecraft:"), p + " needs a vanilla stand-in");
            if (!p.vanilla()) assertFalse(p.createPaths().isEmpty(), p + " has no Create id");
            else assertTrue(p.createPaths().isEmpty(), p + " is vanilla and lists Create ids");
        }
    }

    @Test
    void everyClutchHasItsRedstoneBlockAndEveryNixieItsLamp() {
        for (Blueprint b : all()) {
            for (Blueprint.Placed c : b.of(Part.CLUTCH)) {
                boolean fed = false;
                for (Blueprint.PowerCell p : b.power()) {
                    if (p.role() == Blueprint.Role.CLUTCH && Math.abs(p.dx() - c.dx()) + Math.abs(p.dy() - c.dy()) + Math.abs(p.dz() - c.dz()) == 1) fed = true;
                }
                assertTrue(fed, b.family() + ": clutch at " + c.key() + " has no power cell beside it");
            }
            for (Blueprint.Placed c : b.of(Part.NIXIE)) {
                boolean fed = false;
                for (Blueprint.PowerCell p : b.power()) {
                    if (p.role() == Blueprint.Role.LIGHT && Math.abs(p.dx() - c.dx()) + Math.abs(p.dy() - c.dy()) + Math.abs(p.dz() - c.dz()) == 1) fed = true;
                }
                assertTrue(fed, b.family() + ": nixie tube at " + c.key() + " has no power cell beside it");
            }
            // a CLUTCH power cell must not touch a clutch of ANOTHER line it was not meant for: it is beside exactly one clutch
            for (Blueprint.PowerCell p : b.power()) {
                if (p.role() != Blueprint.Role.CLUTCH) continue;
                int touching = 0;
                for (Blueprint.Placed c : b.of(Part.CLUTCH)) if (Math.abs(p.dx() - c.dx()) + Math.abs(p.dy() - c.dy()) + Math.abs(p.dz() - c.dz()) == 1) touching++;
                assertEquals(1, touching, b.family() + ": power cell " + p.key() + " touches " + touching + " clutches");
            }
            for (Blueprint.Band k : b.bands()) {
                if (b.family() == TrapFamily.SEQUENCE) assertTrue(k.lightIndex() >= 0);
            }
        }
    }

    @Test
    void machineryFacesTheLane() {
        for (Blueprint b : all()) {
            for (Part part : new Part[] {Part.SAW, Part.DRILL, Part.PISTON}) {
                for (Blueprint.Placed p : b.of(part)) {
                    assertEquals(p.dx() < 0 ? Dir6.RIGHT : Dir6.LEFT, p.facing(), b.family() + " " + part + " at " + p.key());
                }
            }
            for (Blueprint.Placed p : b.of(Part.MOTOR)) assertTrue(p.facing() != null);
        }
    }

    /**
     * A walker crosses the lane at 0.22 blocks per tick and waits in a free cell before a band until the band will stay quiet for long enough to cross it. Every drawing must
     * be crossable from every start time, in both directions, within a minute: nothing in the corridor can pin a party.
     */
    @Test
    void aWalkerWhoWaitsAlwaysCrosses() {
        for (Blueprint b : all()) {
            for (int dir = 0; dir < 2; dir++) {
                for (long start = 0; start < 160; start += 13) {
                    long t = start;
                    double z = dir == 0 ? 0 : b.length() - 1;
                    int step = dir == 0 ? 1 : -1;
                    long deadline = t + 1200;
                    while ((dir == 0 ? z < b.length() - 1 : z > 0) && t < deadline) {
                        double nz = z + step * 0.22;
                        int cell = (int) Math.round(nz);
                        boolean blocked = false;
                        for (Blueprint.Band k : b.bands()) {
                            if (cell >= k.zMin() && cell <= k.zMax()) {
                                // enter only when the band is not striking now and will not strike for the next 8 ticks
                                for (int ahead = 0; ahead <= 8; ahead++) {
                                    if (TrapClock.phase(t + ahead, k) == TrapClock.Phase.STRIKE) blocked = true;
                                }
                            }
                        }
                        if (!blocked) z = nz;
                        t++;
                    }
                    assertTrue(t < deadline, b.family() + " dir " + dir + " start " + start + " never crossed");
                }
            }
        }
    }

    @Test
    void theBeltCanBeWalkedAgainst() {
        assertTrue(TrapRules.BELT_STRENGTH < 0.22 - 0.05);
        assertTrue(TrapRules.MAX_BELT_STRENGTH < 0.22);
    }
}
