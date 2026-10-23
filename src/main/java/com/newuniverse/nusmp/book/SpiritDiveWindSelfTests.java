package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing of the PACK Wind of the Spirit Dive can be seen by its author, so the real game proves what can be proved without eyes (the CI server smoke test runs it; key prefix
 * {@code magic.spirit_dive_wind}): (1) every texture the look names is in the mod, and the REEL textures it shares; (2) the tables hold: the vertex count of every tier, level of detail and stage is
 * under the REEL Wind's count and under the cap, the leaf, ribbon and weapon geometry is finite and bounded, the crown has its bars; (3) the option {@code spiritDive.windLook} has PACK and REEL (PACK the
 * default, garbage parses to PACK) and the weapon rule picks the sword at rest and the bow in a cast. Pure checks: nothing touches a client class, so it runs on the dedicated server.
 */
public final class SpiritDiveWindSelfTests {
    private SpiritDiveWindSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dive_wind", SpiritDiveWindSelfTests::wind);
    }

    private static boolean asset(String path) {
        return SpiritDiveWindSelfTests.class.getClassLoader().getResource("assets/nusmp/" + path) != null;
    }

    private static boolean finite(float[] a, int n) {
        for (int i = 0; i < n; i++) if (Float.isNaN(a[i]) || Float.isInfinite(a[i])) return false;
        return true;
    }

    private static Result wind(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // ---- 1. the files
        for (String t : SpiritDiveWindRules.allTextures()) if (!asset("textures/" + t + ".png")) bad.add("missing texture " + t);
        for (String t : SpiritDiveWindRules.sharedTextures()) if (!asset("textures/" + t + ".png")) bad.add("missing shared texture " + t);
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- 2. the tables and the budgets
        for (boolean ch : new boolean[]{false, true})
            for (Stage st : Stage.values())
                for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
                    int v = SpiritDiveWindRules.vertices(ch, lod, st), reel = SpiritDiveReelRules.vertices(Element.WIND, ch, lod, st);
                    int cap = ch ? SpiritDiveReelRules.CAP_CHARACTER : SpiritDiveReelRules.CAP_STANDARD;
                    if (v > reel || v > cap || v >= SpiritDiveReelRules.V2_WORST_VERTICES) bad.add((ch ? "Yuno " : "standard ") + st + " " + lod + ": " + v + " vertices (REEL Wind " + reel + ")");
                }
        if (SpiritDiveWindRules.vertices(true, Lod.CULLED, Stage.FULL) != 0) bad.add("a culled dive draws vertices");
        float[] spine = new float[(SpiritDiveReelGeometry.MAX_SEGS + 1) * 3];
        for (int r = 0; r < SpiritDiveWindGeometry.leafRows(); r++)
            for (float open : new float[]{0f, 0.5f, 1f, 1.12f}) {
                SpiritDiveReelGeometry.leafSpine(spine, 10, SpiritDiveWindGeometry.leaf(r), 1f, SpiritDiveWindGeometry.LEAF_CHARACTER, open, 50f, 1f);
                if (!finite(spine, 33) || Math.abs(spine[30]) > 90f) bad.add("a leaf is out of range");
            }
        for (int i = 0; i < 3; i++) {
            SpiritDiveWindGeometry.ribbonSpine(spine, 12, i, 3, 123f);
            if (!finite(spine, 39)) bad.add("a ribbon is not finite");
        }
        if (SpiritDiveWindGeometry.reach() > 120f) bad.add("the look reaches too far");
        if (SpiritDiveWindGeometry.barCount(SpiritDiveWindGeometry.BARS_STANDARD, false) != SpiritDiveWindGeometry.BARS_STANDARD) bad.add("the crown lost bars");
        for (Lod lod : Lod.values())
            if (lod != Lod.CULLED && (SpiritDiveWindRules.spec(true, lod).leafSegs() > SpiritDiveReelGeometry.MAX_SEGS || SpiritDiveWindRules.spec(true, lod).ribbonSegs() > SpiritDiveReelGeometry.MAX_SEGS))
                bad.add("too many segments at " + lod);

        // ---- 3. the option and the weapon
        SpiritDiveWindRules.WindLook[] look = SpiritDiveWindRules.WindLook.values();
        if (look.length < 2 || look[0] != SpiritDiveWindRules.WindLook.PACK || look[1] != SpiritDiveWindRules.WindLook.REEL) bad.add("the windLook values changed (PACK, REEL)");
        if (SpiritDiveWindRules.DEFAULT_LOOK != SpiritDiveWindRules.WindLook.A_ANIME || SpiritDiveWindRules.parse("garbage") != SpiritDiveWindRules.DEFAULT_LOOK || SpiritDiveWindRules.parse(null) != SpiritDiveWindRules.DEFAULT_LOOK
                || SpiritDiveWindRules.parse("pack") != SpiritDiveWindRules.WindLook.PACK
                || SpiritDiveWindRules.parse("reel") != SpiritDiveWindRules.WindLook.REEL) bad.add("the windLook option does not parse");
        if (SpiritDiveWindRules.weapon(false) != SpiritDiveWindRules.Weapon.SWORD || SpiritDiveWindRules.weapon(true) != SpiritDiveWindRules.Weapon.BOW) bad.add("the weapon rule is wrong");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(SpiritDiveWindRules.allTextures().size() + " textures are in place; the tables hold; the worst full dive draws " + SpiritDiveWindRules.worstVertices(true) + " vertices (Yuno), "
                + SpiritDiveWindRules.worstVertices(false) + " (standard) against the REEL look's worst of " + SpiritDiveReelRules.worstVertices(true) + " / " + SpiritDiveReelRules.worstVertices(false)
                + "; A_ANIME is the default of windLook; PACK and REEL bring the older Winds back");
    }
}
