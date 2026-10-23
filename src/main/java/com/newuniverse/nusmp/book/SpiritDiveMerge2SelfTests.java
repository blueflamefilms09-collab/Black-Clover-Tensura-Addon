package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules.Models;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the second merge (slim spirits, calm entry, first person merge, small crowns) cannot be seen by its author in a game, so the real game proves what can be proved without eyes (the CI server smoke test
 * runs it; key prefix {@code magic.spirit_dive_merge2}): (1) every slim spirit mesh and every crown mesh is in the mod, parses to finite quads with a solid body and, for the spirits, a glow shell; (2) the vertex
 * budgets hold (slim ghost at most 900 at the character tier and 500 at the standard tier, the inner silhouette at most 300, a crown at most 480 / 620 and the Wind crown smaller than the old one); (3) the calm entry
 * is a third in the merge window and full at the start, with a burst after the contact; (4) the first person merge never covers more than 35 percent of the screen, at any clock, field of view or aspect, and draws
 * no inner silhouette; (5) the crowns are about 62 percent of the old ones; (6) the option defaults. Pure checks: no client class, so it runs on the dedicated server.
 */
public final class SpiritDiveMerge2SelfTests {
    private SpiritDiveMerge2SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dive_merge2", SpiritDiveMerge2SelfTests::merge2);
    }

    private static boolean asset(String path) { return SpiritDiveMerge2SelfTests.class.getClassLoader().getResource("assets/nusmp/" + path) != null; }

    private static Result merge2(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // ---- 1. the files
        int ghostMax = 0, coreMax = 0, crownMax = 0;
        for (String n : SpiritDiveMerge2Rules.meshes()) {
            if (!asset("sdm2_meshes/" + n + ".txt")) { bad.add("missing mesh " + n); continue; }
            SpiritDiveWind3Mesh m = SpiritDiveMerge2Rules.mesh(n);
            if (m.quads(SpiritDiveWind3Mesh.SOLID) < 12) bad.add("mesh " + n + " has no solid body");
            if (!n.endsWith("_core") && m.quads(SpiritDiveWind3Mesh.GLOW) < 1) bad.add("mesh " + n + " has no glow shell");
            if (!(m.radius > 0f && m.radius < 80f)) bad.add("mesh " + n + " has a bad radius " + m.radius);
            if (!finite(m)) bad.add("mesh " + n + " has a non-finite vertex");
            if (n.endsWith("_core")) coreMax = Math.max(coreMax, m.totalVerts()); else ghostMax = Math.max(ghostMax, m.totalVerts());
        }
        for (String n : SpiritDiveMerge2Rules.crownMeshes()) {
            if (!asset("sdm2_meshes/" + n + ".txt")) { bad.add("missing crown " + n); continue; }
            SpiritDiveWind3Mesh m = SpiritDiveMerge2Rules.mesh(n);
            if (m.quads(SpiritDiveWind3Mesh.SOLID) < 8) bad.add("crown " + n + " has no solid body");
            if (!finite(m)) bad.add("crown " + n + " has a non-finite vertex");
            crownMax = Math.max(crownMax, m.totalVerts());
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        // ---- 2. the budgets
        for (Element e : Element.values()) {
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && !SpiritDiveMergeRules.hasCharacterMesh(e)) continue;
                int v = SpiritDiveMerge2Rules.ghostVerts(e, ch, Models.SLIM);
                if (v > SpiritDiveMerge2Rules.ghostBudget(ch)) bad.add(e + (ch ? " char" : " std") + " ghost has " + v + " vertices");
                if (SpiritDiveMerge2Rules.crownVerts(e, ch, false) > SpiritDiveMerge2Rules.crownBudget(ch)) bad.add(e + " crown over budget");
            }
            if (SpiritDiveMerge2Rules.coreVerts(e, Models.SLIM) > SpiritDiveMerge2Rules.CORE_VERTS) bad.add(e + " inner silhouette over " + SpiritDiveMerge2Rules.CORE_VERTS);
        }
        for (boolean ch : new boolean[]{false, true})
            for (boolean half : new boolean[]{false, true})
                if (SpiritDiveMerge2Rules.windCrownDelta(ch, half) >= 0) bad.add("the new Wind crown is not smaller (" + ch + ", " + half + ")");
        // ---- 3. the calm entry
        for (float u = SpiritDiveMerge2Rules.WINDOW_FROM; u <= SpiritDiveMerge2Rules.WINDOW_TO; u += 0.25f) {
            if (SpiritDiveMerge2Rules.flashAlpha(u, SpiritDiveReelRules.flash(u), SpiritDiveReelRules.reveal(u)) > 0.351f || SpiritDiveMerge2Rules.flashSizeK(u) > 0.351f
                    || SpiritDiveMerge2Rules.ribbonK(u) > 0.351f) { bad.add("the entry is not calm at " + u); break; }
        }
        if (SpiritDiveMerge2Rules.flashAlpha(1.2f, 1f, 0f) < 0.99f || SpiritDiveMerge2Rules.burst(18.4f) < 0.99f) bad.add("the start flash or the final burst is missing");
        // ---- 4. the first person merge
        for (float fov : new float[]{30f, 70f, 110f})
            for (float aspect : new float[]{1f, 16f / 9f, 32f / 9f})
                for (float m = 0f; m <= SpiritDiveMergeRules.ENTRY_TICKS; m += 0.25f) {
                    float[] p = new float[3];
                    SpiritDiveMerge2Rules.fpPath(m, 1f, p);
                    float d = (float) Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
                    if (Float.isNaN(d)) bad.add("first person path not finite");
                    if (SpiritDiveMerge2Rules.coverage(SpiritDiveMerge2Rules.fpExtent(m, d, fov, aspect, true), d, fov, aspect) > SpiritDiveMerge2Rules.FP_MAX_COVERAGE + 0.001f) { bad.add("first person covers too much at " + m); break; }
                }
        // ---- 5. the crowns
        if (SpiritDiveMerge2Rules.CROWN_K < 0.55f || SpiritDiveMerge2Rules.CROWN_K > 0.7f || SpiritDiveMerge2Rules.NEW_RING > SpiritDiveMerge2Rules.OLD_RING * 0.7f || SpiritDiveMerge2Rules.NEW_TALL > SpiritDiveMerge2Rules.OLD_TALL * 0.75f)
            bad.add("the crowns are not about 62 percent of the old ones");
        // ---- 6. the options
        if (SpiritDiveMerge2Rules.DEFAULT_MODELS != Models.SLIM || !SpiritDiveMerge2Rules.DEFAULT_CALM_ENTRY || !SpiritDiveMerge2Rules.DEFAULT_FIRST_PERSON || !SpiritDiveMerge2Rules.DEFAULT_CROWN) bad.add("an option default is wrong");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(SpiritDiveMerge2Rules.meshes().size() + " slim meshes (ghost at most " + ghostMax + " vertices, silhouette at most " + coreMax + ") and " + SpiritDiveMerge2Rules.crownMeshes().size() + " crowns (at most " + crownMax + ") load");
    }

    private static boolean finite(SpiritDiveWind3Mesh m) {
        for (float[] a : m.pos) for (float v : a) if (Float.isNaN(v) || Float.isInfinite(v)) return false;
        return true;
    }
}
