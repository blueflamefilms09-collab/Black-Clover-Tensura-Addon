package com.newuniverse.nusmp.visual.grip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.relics.item.WeaponClasses;
import com.newuniverse.nusmp.visual.shields.ShieldSet;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 4.45.0 (docs/weapon_hold.md): the numeric hold audit of every hero weapon mesh (Legendary, Seven Star, Vassal, every form), the Java twin of tools/weapon_grip/hold_audit.py.
 * Third person, both hands, idle arm: the weapon floats no further than {@link HoldFit#FLOAT_MAX} from the hand, no part behind the grip reaches past
 * {@link HoldFit#ARM_REACH} into the forearm, none passes through the torso; the numbers also match tools/weapon_grip/hold_golden.json. No game client is involved.
 */
class HoldAuditTest {
    private static final java.nio.file.Path GOLDEN = java.nio.file.Path.of("tools", "weapon_grip", "hold_golden.json");

    /** A hero mesh as the game holds it: the sz copy shadows the base one, a lod / orbit / vu / up mesh is not a held one. */
    private static List<GripTableTest.Mesh> heroMeshes() throws IOException {
        List<GripTableTest.Mesh> all = GripTableTest.meshes();
        Set<String> sz = new HashSet<>();
        for (GripTableTest.Mesh m : all) if (m.key().startsWith("sz/")) sz.add(m.id());
        List<GripTableTest.Mesh> out = new ArrayList<>();
        for (GripTableTest.Mesh m : all) {
            boolean base = !m.key().contains("/") && !sz.contains(m.id());
            if (m.vu() || !(base || m.key().startsWith("sz/") && m.key().indexOf('/') == m.key().lastIndexOf('/'))) continue;
            if (m.key().endsWith("_lod") || m.key().endsWith("_orbit") || !HoldFit.applies(m.id())) continue;
            out.add(m);
        }
        return out;
    }

    /** The third-person numbers of a mesh in one hand: {float, arm, body, move} with the rule on (hold) or off. */
    private static float[] numbers(GripTableTest.Mesh m, int ctx, boolean hold) throws IOException {
        GripTable t = GripTableTest.table();
        WeaponClasses classes = GripTableTest.classes();
        String cls = t.gripClass(m.id(), classes);
        WeaponClasses.Cls row = classes.named(cls);
        GripFinder.Grip g = GripState.find(m.body(), cls, row.grip(), t.fraction(m.id()), m.gripLine(), false, m.explicit());
        assertNotNull(g, m.key());
        GripState st = new GripState(g, GripTableTest.displays(m.id()), GripState.box(m.body()));
        float[] p = m.place();
        if (hold) st.withHold(HoldFit.surfaceSamples(m.body(), p[3]));
        float[] s = st.shift(ctx, p[0], p[1], p[2], p[3], p[4]);
        float[] samples = HoldFit.surfaceSamples(m.body(), p[3]);
        int n = samples.length / 3;
        float[] cube = new float[n * 3];
        for (int i = 0; i < n; i++) {
            float[] c = GripMath.cubePoint(p[0], p[1], p[2], p[3], p[4], samples[i * 3], samples[i * 3 + 1], samples[i * 3 + 2]);
            cube[i * 3] = c[0] + s[0];
            cube[i * 3 + 1] = c[1] + s[1];
            cube[i * 3 + 2] = c[2] + s[2];
        }
        boolean left = ctx == GripState.TP_LEFT;
        GripMath.Display d = st.display(ctx);
        float[] q = HoldFit.toHand(cube, n, d, left);
        float[] zero = {0f, 0f, 0f};
        float[] g0 = GripMath.cubePoint(p[0], p[1], p[2], p[3], p[4], g.x(), g.y(), g.z());
        double[] w = GripMathTest.applyEntry(d, left, g0[0] + s[0], g0[1] + s[1], g0[2] + s[2]);
        return new float[]{HoldFit.floatDistance(q, n, zero), HoldFit.armReach(q, n, zero, left, HoldFit.ARM_BOX), HoldFit.torsoDepth(q, n, zero, left),
                (float) Math.sqrt(w[0] * w[0] + w[1] * w[1] + w[2] * w[2])};
    }

    private static boolean judged(GripTableTest.Mesh m) {
        return !ShieldSet.isShieldMesh(m.id());                  // a shield sits on ShieldFrame's frame (ShieldFrameTest), not on the grip
    }

    @Test
    void everyHeroFormFindsTheDisplayOfItsItem() throws IOException {
        List<String> none = new ArrayList<>();
        for (GripTableTest.Mesh m : heroMeshes()) {
            GripMath.Display[] d = GripTableTest.displays(m.id());
            if (d[GripState.TP_RIGHT] == GripMath.Display.NONE || d[GripState.FP_RIGHT] == GripMath.Display.NONE) none.add(m.id());
        }
        assertTrue(none.isEmpty(), "a hero mesh with no display block in its model json (the hand point would be the cube's centre): " + none);
        assertEquals("vassal_katana__siderite_katana", GripTable.modelIds("vassal_katana__siderite_katana")[0]);
        assertEquals("vassal_katana", GripTable.modelIds("vassal_katana__siderite_katana")[2], "a form takes the json of its item");
    }

    @Test
    void everyHeroWeaponIsHeldWithoutFloatingAndWithoutPassingThroughTheArmOrBody() throws IOException {
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (GripTableTest.Mesh m : heroMeshes()) {
            if (!judged(m)) continue;
            boolean idle = !m.id().matches(".*_pull[123]$");
            for (int ctx = GripState.TP_RIGHT; ctx <= GripState.TP_LEFT; ctx++) {
                float[] r = numbers(m, ctx, true);
                if (r[0] > HoldFit.FLOAT_MAX) bad.add(m.key() + " ctx " + ctx + " floats " + r[0]);
                if (idle && r[1] > HoldFit.ARM_REACH) bad.add(m.key() + " ctx " + ctx + " arm " + r[1]);
                if (idle && r[2] > 0f) bad.add(m.key() + " ctx " + ctx + " body " + r[2]);
                assertTrue(r[3] <= HoldFit.MAX_MOVE + 1e-3f, m.key() + " ctx " + ctx + ": the grip moved " + r[3] + " from the hand");
                checked++;
            }
        }
        assertTrue(checked > 150, "the audit ran on the shipped hero meshes: " + checked);
        assertTrue(bad.isEmpty(), "hero weapons held badly (tools/weapon_grip/hold_audit.py lists the same): " + bad);
    }

    @Test
    void theHoldSwitchOffLandsTheGripExactlyOnTheHandAsBefore() throws IOException {
        for (GripTableTest.Mesh m : heroMeshes()) {
            if (!judged(m)) continue;
            assertEquals(0f, numbers(m, GripState.TP_RIGHT, false)[3], 2e-4f, m.key());
        }
    }

    @Test
    void theJavaNumbersMatchTheGoldenFileOfThePythonAudit() throws IOException {
        JsonObject golden = JsonParser.parseString(Files.readString(GOLDEN)).getAsJsonObject();
        List<String> off = new ArrayList<>();
        int compared = 0;
        for (GripTableTest.Mesh m : heroMeshes()) {
            JsonElement e = golden.get(m.key());
            if (e == null) {
                off.add(m.key() + " (not in hold_golden.json: run tools/weapon_grip/hold_audit.py --write)");
                continue;
            }
            if (!judged(m)) continue;
            for (int ctx = GripState.TP_RIGHT; ctx <= GripState.TP_LEFT; ctx++) {
                JsonObject o = e.getAsJsonObject().getAsJsonObject(ctx == GripState.TP_LEFT ? "tp_left" : "tp_right");
                float[] r = numbers(m, ctx, true);
                if (Math.abs(o.get("float").getAsFloat() - r[0]) > 0.02f || Math.abs(o.get("arm").getAsFloat() - r[1]) > 0.03f || Math.abs(o.get("body").getAsFloat() - r[2]) > 0.03f) {
                    off.add(m.key() + " ctx " + ctx + ": java {float " + r[0] + ", arm " + r[1] + ", body " + r[2] + "} vs golden " + o);
                }
                compared++;
            }
        }
        assertTrue(compared > 150, "compared " + compared);
        assertTrue(off.isEmpty(), "the Java hold differs from tools/weapon_grip/hold_golden.json: " + off);
    }

    @Test
    void aHeroGripSitsWhereTheMeshsOwnGripLineSaysItDoes() throws IOException {
        GripTable t = GripTableTest.table();
        WeaponClasses classes = GripTableTest.classes();
        List<String> bad = new ArrayList<>();
        for (GripTableTest.Mesh m : heroMeshes()) {
            if (Float.isNaN(m.gripLine()) || !judged(m)) continue;
            String cls = t.gripClass(m.id(), classes);
            if ("bow".equals(cls) || "crossbow".equals(cls)) continue;
            GripFinder.Grip g = GripState.find(m.body(), cls, classes.named(cls).grip(), t.fraction(m.id()), m.gripLine(), false, m.explicit());
            if (g.source() != GripFinder.Source.DIRECTIVE && g.source() != GripFinder.Source.GRIPXYZ && Math.abs(g.fraction() - m.gripLine()) > 0.05f) {
                bad.add(m.key() + " grip " + g.fraction() + " vs its line " + m.gripLine());
            }
        }
        assertTrue(bad.isEmpty(), "the finder ignored a hero mesh's own grip line (class table? add a holdFractions entry in tools/weapon_grip/grip_table.py): " + bad);
        assertFalse(GripTable.parse("{\"holdFractions\":{\"x\":0.3}}").hold(false).fraction("x") == 0.3f, "the setting off drops the per-form fractions");
        assertEquals(0.3f, GripTable.parse("{\"holdFractions\":{\"x\":0.3}}").fraction("x"), 1e-6f);
    }

    @Test
    void theSurfaceSamplesStayWithinTheirBudget() throws IOException {
        int worst = 0;
        for (GripTableTest.Mesh m : heroMeshes()) worst = Math.max(worst, HoldFit.surfaceSamples(m.body(), m.place()[3]).length / 3);
        assertTrue(worst > 0 && worst <= HoldFit.MAX_SAMPLES * 1.3, "the largest hero mesh has " + worst + " samples");
    }
}
