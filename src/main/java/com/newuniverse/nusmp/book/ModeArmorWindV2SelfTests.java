package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Entry;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Wind Spirit Dive overlay v2 (Yuno's layered coat, crown and leaf wings, drawn by the client) cannot be seen by its author in a game, so the real game proves what can be proved without eyes
 * (the CI server smoke test runs it; key prefix {@code magic.mode_wind_v2}): (1) every mesh of every level of detail builds into finite quads with unit-ish normals and uv inside its texture; (2) the vertex
 * count of a wearer is under its cap at every level of detail (3,200 / 1,200 / 150, nothing beyond 64 blocks) and the combined calm steady Yuno dive (aura + overlay) is under 6,500; (3) the five textures are in
 * the mod jar; (4) the mode wire: the Wind Spirit Dive of a Yuno-tier wearer is sent as the Yuno variant and nothing else changes; (5) the option parses and defaults to V2. Pure checks: nothing touches a client
 * class, so it runs on the dedicated server.
 */
public final class ModeArmorWindV2SelfTests {
    private ModeArmorWindV2SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.mode_wind_v2", ModeArmorWindV2SelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
            for (ModeArmorWindV2Rules.ArmStyle arms : ModeArmorWindV2Rules.ArmStyle.values()) for (Entry e : ModeArmorWindV2Rules.entries(lod, arms)) {
                ModeArmorWindV2Mesh m;
                try { m = ModeArmorWindV2Mesh.get(e.piece(), e.detail()); } catch (RuntimeException ex) { bad.add("missing mesh " + e); continue; }
                if (m.verts <= 0 || m.verts % 4 != 0) bad.add(e + " has " + m.verts + " vertices");
                for (float f : m.pos) if (Float.isNaN(f) || Float.isInfinite(f)) { bad.add(e + " has a non-finite vertex"); break; }
                for (float f : m.uv) if (!(f >= 0f && f <= 1f)) { bad.add(e + " has a uv outside the texture"); break; }
                for (int i = 0; i < m.verts; i++) {
                    float l = (float) Math.sqrt(m.nrm[i * 3] * m.nrm[i * 3] + m.nrm[i * 3 + 1] * m.nrm[i * 3 + 1] + m.nrm[i * 3 + 2] * m.nrm[i * 3 + 2]);
                    if (!(l > 0.97f && l < 1.03f)) { bad.add(e + " has a normal of length " + l); break; }
                }
            }
            for (ModeArmorWindV2Rules.ArmStyle arms : ModeArmorWindV2Rules.ArmStyle.values()) {
                int v = ModeArmorWindV2Rules.vertices(lod, arms);
                if (v > ModeArmorWindV2Rules.cap(lod)) bad.add(lod + " " + arms + ": " + v + " vertices over the cap " + ModeArmorWindV2Rules.cap(lod));
            }
            if (ModeArmorWindV2Rules.vertices(lod, ModeArmorWindV2Rules.ArmStyle.BLOCKY) > ModeArmorWindV2Rules.vertices(lod, ModeArmorWindV2Rules.ArmStyle.SMOOTH)) bad.add(lod + ": the blocky arms grow the overlay");
        }
        if (ModeArmorWindV2Rules.vertices(Lod.CULLED) != 0) bad.add("something is drawn beyond 64 blocks");
        int combined = ModeArmorWindV2Rules.yunoCombinedSteady();
        if (combined > ModeArmorWindV2Rules.CAP_COMBINED_STEADY) bad.add("the combined calm Yuno dive draws " + combined + " vertices, over " + ModeArmorWindV2Rules.CAP_COMBINED_STEADY);
        for (String t : ModeArmorWindV2Rules.allTextures()) if (ModeArmorWindV2SelfTests.class.getClassLoader().getResource("assets/nusmp/" + t) == null) bad.add("missing texture " + t);
        if (ModeArmor.wire(ModeArmor.Mode.WIND_SPIRIT_DIVE, true) != ModeArmor.Mode.WIND_SPIRIT_DIVE_YUNO || ModeArmor.wire(ModeArmor.Mode.WIND_SPIRIT_DIVE, false) != ModeArmor.Mode.WIND_SPIRIT_DIVE
                || ModeArmor.wire(ModeArmor.Mode.FIRE_SPIRIT_DIVE, true) != ModeArmor.Mode.FIRE_SPIRIT_DIVE || ModeArmor.wire(ModeArmor.Mode.DEMON, true) != ModeArmor.Mode.DEMON)
            bad.add("the mode wire is wrong");
        if (ModeArmorWindV2Rules.DEFAULT_LOOK != ModeArmorWindV2Rules.Look.V2 || ModeArmorWindV2Rules.parse("garbage") != ModeArmorWindV2Rules.Look.V2 || ModeArmorWindV2Rules.parse("classic") != ModeArmorWindV2Rules.Look.CLASSIC)
            bad.add("the windLook option does not default to V2 / parse");
        if (ModeArmorWindV2Rules.DEFAULT_ARMS != ModeArmorWindV2Rules.ArmStyle.BLOCKY || ModeArmorWindV2Rules.parseArms("garbage") != ModeArmorWindV2Rules.ArmStyle.BLOCKY || ModeArmorWindV2Rules.parseArms("smooth") != ModeArmorWindV2Rules.ArmStyle.SMOOTH)
            bad.add("the windArms option does not default to BLOCKY / parse");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok("meshes finite; wearer vertices " + ModeArmorWindV2Rules.vertices(Lod.FULL, ModeArmorWindV2Rules.ArmStyle.BLOCKY) + " blocky arms (smooth " + ModeArmorWindV2Rules.vertices(Lod.FULL) + ") / " + ModeArmorWindV2Rules.vertices(Lod.MID) + " / " + ModeArmorWindV2Rules.vertices(Lod.FAR)
                + " (full / mid / far, caps 3200 / 1200 / 150); combined calm Yuno dive " + combined + " (cap " + ModeArmorWindV2Rules.CAP_COMBINED_STEADY + "); 5 textures; mode wire ok; default V2, arms BLOCKY");
    }
}
