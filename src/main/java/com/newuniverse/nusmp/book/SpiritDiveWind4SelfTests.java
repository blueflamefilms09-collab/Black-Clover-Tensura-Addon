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
 * Why: the final Wind A (the default Wind, a real 3D crown layer, the calm steady state) cannot be seen by its author in a game, so the real game proves what can be proved without eyes (the CI server
 * smoke test runs it; key prefix {@code magic.spirit_dive_windfinal}): (1) the four crown meshes are in the mod and parse into finite quads, Yuno's with the lower second ring; (2) the default of
 * {@code spiritDive.windLook} is A_ANIME and the older looks are still values of the option; (3) the vertex count of design A is under its cap at every tier, stage and level of detail and the quiet steady
 * state is never dearer than the loud one; (4) the energy function is loud in the entry, quiet in the steady state, loud after a cast and in the exit, always in 0..1; (5) the quiet set has no ribbons and
 * no circle behind the back. Pure checks: nothing touches a client class, so it runs on the dedicated server.
 */
public final class SpiritDiveWind4SelfTests {
    private SpiritDiveWind4SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dive_windfinal", SpiritDiveWind4SelfTests::windFinal);
    }

    private static Result windFinal(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (String n : SpiritDiveWind4Rules.NAMES) {
            if (SpiritDiveWind4SelfTests.class.getClassLoader().getResource("assets/nusmp/sdw4_meshes/" + n + ".txt") == null) { bad.add("missing mesh " + n); continue; }
            SpiritDiveWind3Mesh m = SpiritDiveWind3Mesh.getIn(SpiritDiveWind3Mesh.FOLDER4, n);
            if (m.quads(SpiritDiveWind3Mesh.SOLID) < 20 || m.quads(SpiritDiveWind3Mesh.GLOW) < 8) bad.add("mesh " + n + " has no body or no glow shell");
            if (!(m.radius > 5f && m.radius < 25f)) bad.add("mesh " + n + " has a bad radius " + m.radius);
            for (int l = 0; l < SpiritDiveWind3Mesh.LAYERS; l++)
                for (float f : m.pos[l]) if (Float.isNaN(f) || Float.isInfinite(f)) { bad.add("mesh " + n + " has a non-finite vertex"); break; }
        }
        if (SpiritDiveWind4Rules.crown(true, false).quads(SpiritDiveWind3Mesh.ORNAMENT) == 0) bad.add("the Yuno crown lost its second ring");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        if (SpiritDiveWindRules.DEFAULT_LOOK != SpiritDiveWindRules.WindLook.A_ANIME || SpiritDiveWindRules.parse("garbage") != SpiritDiveWindRules.WindLook.A_ANIME
                || SpiritDiveWindRules.parse("pack") != SpiritDiveWindRules.WindLook.PACK || SpiritDiveWindRules.parse("b_clip") != SpiritDiveWindRules.WindLook.B_CLIP) bad.add("the windLook default is not A_ANIME or an older look no longer parses");

        for (boolean ch : new boolean[]{false, true})
            for (Stage st : Stage.values())
                for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
                    int worst = SpiritDiveWind3Rules.vertices(Ver.A, ch, lod, st), calm = SpiritDiveWind4Rules.steadyVertices(ch, lod, st, true), loud = SpiritDiveWind4Rules.steadyVertices(ch, lod, st, false);
                    if (worst > SpiritDiveWind3Rules.cap(Ver.A, ch)) bad.add("A " + (ch ? "Yuno " : "standard ") + st + " " + lod + ": " + worst + " vertices over the cap");
                    if (calm > loud) bad.add("the quiet set is dearer than the loud one at " + st + " " + lod);
                }

        float never = 1e9f;
        if (SpiritDiveWind4Rules.energy(5f, 100f, never) < 0.99f) bad.add("the entry is not loud");
        if (SpiritDiveWind4Rules.energy(80f, 5000f, never) > 0.01f) bad.add("the steady state is not quiet");
        if (SpiritDiveWind4Rules.energy(80f, 5000f, 10f) < 0.99f) bad.add("a cast does not make the dive loud");
        if (SpiritDiveWind4Rules.energy(80f, 2f, never) < 0.99f) bad.add("the exit is not loud");
        for (float t = 0f; t < 300f; t += 3f) { float e = SpiritDiveWind4Rules.energy(t, 300f - t, t % 50f); if (!(e >= 0f && e <= 1f)) bad.add("energy out of range at " + t); }

        SpiritDiveWindRules.Spec calm = SpiritDiveWind4Rules.calmSpec(SpiritDiveWindRules.spec(true, Lod.FULL));
        if (calm.ribbons() != 0 || calm.backCircle()) bad.add("the quiet set still has ribbons or the back circle");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok("4 crown meshes load (Yuno " + SpiritDiveWind4Rules.crownVerts(true, false) + " / standard " + SpiritDiveWind4Rules.crownVerts(false, false) + " vertices); A_ANIME is the default Wind; worst entry A "
                + SpiritDiveWind3Rules.worst(Ver.A, true) + " / " + SpiritDiveWind3Rules.worst(Ver.A, false) + " vertices (Yuno / standard), quiet steady state " + SpiritDiveWind4Rules.worstSteady(true, true) + " / "
                + SpiritDiveWind4Rules.worstSteady(false, true) + ", caps " + SpiritDiveWind3Rules.CAP_CHARACTER_A + " / " + SpiritDiveWind3Rules.CAP_STANDARD);
    }
}
