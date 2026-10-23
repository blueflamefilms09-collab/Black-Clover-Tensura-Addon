package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules.Ver;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the three designs of the Wind remake (A anime-true, B clip-style, C saint spirit) with REAL 3D weapons cannot be seen by their author in a game, so the real game proves what can be proved without
 * eyes (the CI server smoke test runs it; key prefix {@code magic.spirit_dive_wind3}): (1) every mesh file and the two textures are in the mod, every mesh parses into quads with finite coordinates;
 * (2) the vertex count of every design, tier, level of detail and stage is under its cap (4000 character, 2500 standard) and a culled dive draws nothing; (3) the option {@code spiritDive.windLook}
 * keeps PACK and REEL first and appends the three designs, the default is one of them and garbage parses to it; (4) the geometry (crown cubes, feathers, halo) is finite. Pure checks: nothing touches a
 * client class, so it runs on the dedicated server.
 */
public final class SpiritDiveWind3SelfTests {
    private SpiritDiveWind3SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dive_wind3", SpiritDiveWind3SelfTests::wind3);
    }

    private static boolean asset(String path) {
        return SpiritDiveWind3SelfTests.class.getClassLoader().getResource("assets/nusmp/" + path) != null;
    }

    private static Result wind3(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // ---- 1. the files
        for (String t : SpiritDiveWind3Rules.allTextures()) if (!asset("textures/" + t + ".png")) bad.add("missing texture " + t);
        for (String n : SpiritDiveWind3Mesh.NAMES) {
            if (!asset("sdw3_meshes/" + n + ".txt")) { bad.add("missing mesh " + n); continue; }
            SpiritDiveWind3Mesh m = SpiritDiveWind3Mesh.get(n);
            if (m.quads(SpiritDiveWind3Mesh.SOLID) < 4) bad.add("mesh " + n + " has no body");
            if (!(m.radius > 0f && m.radius < 80f)) bad.add("mesh " + n + " has a bad radius " + m.radius);
            for (int l = 0; l < SpiritDiveWind3Mesh.LAYERS; l++)
                for (int i = 0; i < m.pos[l].length; i++) if (Float.isNaN(m.pos[l][i]) || Float.isInfinite(m.pos[l][i])) { bad.add("mesh " + n + " has a non-finite vertex"); break; }
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- 2. the budgets
        for (Ver v : Ver.values())
            for (boolean ch : new boolean[]{false, true})
                for (Stage st : Stage.values())
                    for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
                        int n = SpiritDiveWind3Rules.vertices(v, ch, lod, st);
                        if (n > SpiritDiveWind3Rules.cap(ch)) bad.add(v + (ch ? " Yuno " : " standard ") + st + " " + lod + ": " + n + " vertices");
                    }
        for (Ver v : Ver.values()) if (SpiritDiveWind3Rules.vertices(v, true, Lod.CULLED, Stage.FULL) != 0) bad.add(v + ": a culled dive draws vertices");

        // ---- 3. the option
        SpiritDiveWindRules.WindLook[] look = SpiritDiveWindRules.WindLook.values();
        if (look.length != 5 || look[0] != SpiritDiveWindRules.WindLook.PACK || look[1] != SpiritDiveWindRules.WindLook.REEL) bad.add("the windLook values are not PACK, REEL + three designs");
        if (SpiritDiveWind3Rules.ver(SpiritDiveWindRules.DEFAULT_LOOK) == null || SpiritDiveWindRules.parse("garbage") != SpiritDiveWindRules.DEFAULT_LOOK
                || SpiritDiveWind3Rules.ver(SpiritDiveWindRules.WindLook.PACK) != null || SpiritDiveWind3Rules.ver(SpiritDiveWindRules.WindLook.REEL) != null) bad.add("the windLook default or mapping is wrong");
        for (Ver v : Ver.values()) if (SpiritDiveWind3Rules.ver(SpiritDiveWind3Rules.look(v)) != v) bad.add("the option value of " + v + " does not map back");

        // ---- 4. the geometry
        float[] g = new float[6];
        for (int i = 0; i < SpiritDiveWind3Geometry.CROWN_PLACES; i++) { SpiritDiveWind3Geometry.crownCube(g, i); if (!finite(g)) bad.add("crown cube " + i); }
        for (int i = 0; i < 8; i++) { SpiritDiveWind3Geometry.orbiter(g, i, 8, 37f); if (!finite(g)) bad.add("orbiter " + i); }
        for (int i = 0; i < 5; i++) { SpiritDiveWind3Geometry.featherDir(g, i, 5, 1f, 0f); if (!finite(g) || Math.abs(g[0] * g[0] + g[1] * g[1] + g[2] * g[2] - 1f) > 1e-3f) bad.add("feather " + i); }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(SpiritDiveWind3Mesh.NAMES.size() + " meshes load; worst full dive: A " + SpiritDiveWind3Rules.worst(Ver.A, true) + " / " + SpiritDiveWind3Rules.worst(Ver.A, false) + ", B "
                + SpiritDiveWind3Rules.worst(Ver.B, true) + " / " + SpiritDiveWind3Rules.worst(Ver.B, false) + ", C " + SpiritDiveWind3Rules.worst(Ver.C, true) + " / " + SpiritDiveWind3Rules.worst(Ver.C, false)
                + " vertices (Yuno / standard), caps " + SpiritDiveWind3Rules.CAP_CHARACTER + " / " + SpiritDiveWind3Rules.CAP_STANDARD);
    }

    private static boolean finite(float[] a) {
        for (float v : a) if (Float.isNaN(v) || Float.isInfinite(v)) return false;
        return true;
    }
}
