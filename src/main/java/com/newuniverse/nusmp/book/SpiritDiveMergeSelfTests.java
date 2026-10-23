package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the merge of the diver with the spirit (the Sylph, Undine, Salamander and Gnome flowing into the chest) cannot be seen by its author in a game, so the real game proves what can be proved
 * without eyes (the CI server smoke test runs it; key prefix {@code magic.spirit_dive_merge}): (1) every mesh and texture of the merge is in the mod and every mesh parses into finite quads; (2) the clock
 * runs 0 to 22 forwards on the entry and backwards on the exit; the path of the spirit is finite and ends at the chest; the ghost is gone and the flash peaks at the contact; (3) the budgets hold: a ghost
 * mesh is at most 700 vertices, the inner silhouette at most 300 and at most 12 percent alpha, the whole merge of one diver stays under its worst case, a culled diver draws nothing, the steady state
 * is the silhouette alone and only at full detail; (4) the option default. Pure checks: nothing touches a client class, so it runs on the dedicated server.
 */
public final class SpiritDiveMergeSelfTests {
    private SpiritDiveMergeSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dive_merge", SpiritDiveMergeSelfTests::merge);
    }

    private static boolean asset(String path) {
        return SpiritDiveMergeSelfTests.class.getClassLoader().getResource("assets/nusmp/" + path) != null;
    }

    private static Result merge(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // ---- 1. the files
        for (String t : SpiritDiveMergeRules.textures()) if (!asset("textures/" + t + ".png")) bad.add("missing texture " + t);
        int ghostMax = 0, coreMax = 0;
        for (String n : SpiritDiveMergeRules.meshes()) {
            if (!asset("sdm_meshes/" + n + ".txt")) { bad.add("missing mesh " + n); continue; }
            SpiritDiveWind3Mesh m = SpiritDiveMergeMesh.get(n);
            if (m.quads(SpiritDiveWind3Mesh.GLOW) < 12) bad.add("mesh " + n + " has no body");
            if (m.quads(SpiritDiveWind3Mesh.SOLID) != 0 || m.quads(SpiritDiveWind3Mesh.ORNAMENT) != 0) bad.add("mesh " + n + " has solid quads (all are additive)");
            if (!(m.radius > 0f && m.radius < 80f)) bad.add("mesh " + n + " has a bad radius " + m.radius);
            for (int i = 0; i < m.pos[SpiritDiveWind3Mesh.GLOW].length; i++) {
                float v = m.pos[SpiritDiveWind3Mesh.GLOW][i];
                if (Float.isNaN(v) || Float.isInfinite(v)) { bad.add("mesh " + n + " has a non-finite vertex"); break; }
            }
            if (n.endsWith("_core")) coreMax = Math.max(coreMax, m.totalVerts()); else ghostMax = Math.max(ghostMax, m.totalVerts());
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- 2. the clock, the path, the strengths
        if (SpiritDiveMergeRules.clock(0f, 1000f) != 0f || SpiritDiveMergeRules.clock(100f, 1000f) != SpiritDiveMergeRules.ENTRY_TICKS || SpiritDiveMergeRules.clock(1000f, 0f) != 0f
                || SpiritDiveMergeRules.clock(1000f, SpiritDiveMergeRules.EXIT_TICKS) != SpiritDiveMergeRules.ENTRY_TICKS) bad.add("the clock does not run 0..22 forwards and backwards");
        float[] p = new float[4];
        for (float side : new float[]{1f, -1f})
            for (boolean ch : new boolean[]{false, true})
                for (float m = -3f; m <= 30f; m += 0.5f) {
                    SpiritDiveMergeRules.path(m, side, ch, p);
                    for (float v : p) if (Float.isNaN(v) || Float.isInfinite(v)) { bad.add("path not finite at " + m); break; }
                }
        SpiritDiveMergeRules.path(SpiritDiveMergeRules.FLOW_END, 1f, false, p);
        if (Math.hypot(p[0] - SpiritDiveMergeRules.CHEST_X, p[2] - SpiritDiveMergeRules.CHEST_Z) > 4f) bad.add("the spirit does not end at the chest");
        if (SpiritDiveMergeRules.ghostAlpha(SpiritDiveMergeRules.FLOW_END) > 0.01f || SpiritDiveMergeRules.ghostAlpha(0f) != 0f) bad.add("the ghost is not gone at the contact / absent at the start");
        if (SpiritDiveMergeRules.flash(SpiritDiveMergeRules.FLASH_AT) < 0.99f || SpiritDiveMergeRules.flash(8f) > 0.001f || SpiritDiveMergeRules.flash(20f) > 0.001f) bad.add("the flash does not peak at the contact");
        if (SpiritDiveMergeRules.INNER_ALPHA > 0.12f) bad.add("the inner silhouette is brighter than 12 percent");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- 3. the budgets
        if (ghostMax > SpiritDiveMergeRules.GHOST_VERTS) bad.add("a ghost mesh has " + ghostMax + " vertices (cap " + SpiritDiveMergeRules.GHOST_VERTS + ")");
        if (coreMax > SpiritDiveMergeRules.INNER_VERTS) bad.add("an inner silhouette has " + coreMax + " vertices (cap " + SpiritDiveMergeRules.INNER_VERTS + ")");
        int worst = 0;
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                worst = Math.max(worst, SpiritDiveMergeRules.worst(e, ch));
                if (SpiritDiveMergeRules.vertices(e, ch, Lod.CULLED, 12f, true) != 0) bad.add(e + ": a culled dive draws vertices");
                if (SpiritDiveMergeRules.vertices(e, ch, Lod.FULL, 12f, false) > coreMax) bad.add(e + ": the ghost is drawn beyond 32 blocks");
                if (SpiritDiveMergeRules.vertices(e, ch, Lod.MID, SpiritDiveMergeRules.ENTRY_TICKS, true) != 0) bad.add(e + ": the steady state is drawn beyond full detail");
                if (SpiritDiveMergeRules.vertices(e, ch, Lod.FULL, SpiritDiveMergeRules.ENTRY_TICKS, true) > SpiritDiveMergeRules.INNER_VERTS) bad.add(e + ": the steady state is more than the silhouette");
            }
        if (worst > SpiritDiveMergeRules.GHOST_VERTS + 5 * SpiritDiveMergeRules.STRIP_SEGS * 4 + SpiritDiveMergeRules.FLASH_SPRITES * 4 + SpiritDiveMergeRules.COVER_VERTS + SpiritDiveMergeRules.INNER_VERTS) bad.add("the worst merge is " + worst);
        // ---- 4. the option
        if (!SpiritDiveMergeRules.DEFAULT_MERGE) bad.add("the merge option is not on by default");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(SpiritDiveMergeRules.meshes().size() + " meshes load; ghost mesh at most " + ghostMax + " vertices, silhouette at most " + coreMax + "; worst merge of one diver " + worst + " vertices");
    }
}
