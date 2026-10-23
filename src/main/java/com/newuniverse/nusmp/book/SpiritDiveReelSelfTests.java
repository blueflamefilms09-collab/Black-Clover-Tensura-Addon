package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing of the REEL Spirit Dive can be seen by its author, so the real game proves what can be proved without eyes (the CI server smoke test runs it; key prefix
 * {@code magic.spirit_dive_reel}): (1) every texture the look names is in the mod, and the two textures it shares with the ANIME look; (2) the tables hold: the clock of the transformation runs 0 to 22
 * forwards on the entry and backwards on the exit, every effect that is not steady is gone at the end of it, the specs of the tiers are within the ceiling, the vertex counts of every element, stage, tier and level of
 * detail are under the 3.0.0 V2 dive's worst case and under the cap of the look, the geometry of the wings, the crown and the ribbons is finite and bounded; (3) the style number of every element, stage and grade
 * survives the round trip the server sends it through (the look only changes what is drawn, never what is sent); (4) the look option has its value (appended last), its default and every old look is still
 * choosable. Pure checks: nothing touches a client class, so it runs on the dedicated server.
 */
public final class SpiritDiveReelSelfTests {
    private SpiritDiveReelSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dive_reel", SpiritDiveReelSelfTests::reel);
    }

    private static boolean asset(String path) {
        return SpiritDiveReelSelfTests.class.getClassLoader().getResource("assets/nusmp/" + path) != null;
    }

    private static boolean finite(float[] a, int n) {
        for (int i = 0; i < n; i++) if (Float.isNaN(a[i]) || Float.isInfinite(a[i])) return false;
        return true;
    }

    private static Result reel(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // ---- 1. the files
        for (String t : SpiritDiveReelRules.allTextures()) if (!asset("textures/" + t + ".png")) bad.add("missing texture " + t);
        for (String t : SpiritDiveReelRules.sharedTextures()) if (!asset("textures/" + t + ".png")) bad.add("missing shared texture " + t);
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- 2. the timeline, the tables and the budgets
        if (SpiritDiveReelRules.clock(0f, 100f) != 0f || SpiritDiveReelRules.clock(100f, 100f) != SpiritDiveReelRules.ENTRY_TICKS || SpiritDiveReelRules.clock(100f, 0f) != 0f) bad.add("the clock does not run 0 to 22");
        if (Math.abs(SpiritDiveReelRules.clock(10f, 1e4f) - 10f) > 1e-4f || Math.abs(SpiritDiveReelRules.clock(1e4f, 10f) - 11f) > 1e-3f) bad.add("the clock of the exit is not the entry backwards");
        float u = SpiritDiveReelRules.ENTRY_TICKS;
        if (SpiritDiveReelRules.flash(u) != 0f || SpiritDiveReelRules.ribbonAlpha(u) > 0.01f || SpiritDiveReelRules.cage(u) > 0.01f || SpiritDiveReelRules.groundPulse(u) != 0f || SpiritDiveReelRules.reveal(u) != 0f)
            bad.add("an effect of the entry is still on when the transformation is complete");
        if (Math.abs(SpiritDiveReelRules.wingScale(u) - 1f) > 1e-4f || Math.abs(SpiritDiveReelRules.gauntlet(u) - 1f) > 1e-4f || SpiritDiveReelRules.weapon(u) < 0.999f) bad.add("the final form is not at full size");
        for (Element e : Element.values()) {
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && SpiritDiveTier.gradeOfElement(e) == Grade.STANDARD) continue;
                for (Stage st : Stage.values())
                    for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
                        int v = SpiritDiveReelRules.vertices(e, ch, lod, st);
                        int cap = ch ? SpiritDiveReelRules.CAP_CHARACTER : SpiritDiveReelRules.CAP_STANDARD;
                        if (v > cap || v >= SpiritDiveReelRules.V2_WORST_VERTICES) bad.add(e + " " + st + " " + lod + (ch ? " character" : " standard") + ": " + v + " vertices");
                    }
            }
            float[] spine = new float[(SpiritDiveReelGeometry.MAX_SEGS + 1) * 3];
            for (int r = 0; r < SpiritDiveReelGeometry.leafRows(); r++)
                for (float open : new float[]{0f, 0.5f, 1f, 1.12f}) {
                    SpiritDiveReelGeometry.leafSpine(spine, 8, SpiritDiveReelGeometry.leaf(r), 1f, 1.18f, open, 50f, 1f);
                    if (!finite(spine, 27) || Math.abs(spine[24]) > 90f) bad.add("a leaf of " + e + " is out of range");
                }
        }
        if (SpiritDiveReelRules.vertices(Element.WIND, true, Lod.CULLED, Stage.FULL) > 0) bad.add("a culled dive draws vertices");
        for (Lod lod : Lod.values())
            for (Element e : Element.values())
                if (lod != Lod.CULLED && (SpiritDiveReelRules.spec(e, true, lod).ribbonSegs() > SpiritDiveReelGeometry.MAX_SEGS || SpiritDiveReelRules.spec(e, true, lod).leafSegs() > SpiritDiveReelGeometry.MAX_SEGS))
                    bad.add("too many segments at " + lod);
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- 3. the style number round trip (what the server sends)
        for (Element e : Element.values())
            for (Stage st : Stage.values())
                for (Grade g : Grade.values()) {
                    int style = SpiritDivePieces.style(e, st, g);
                    if (!SpiritDivePieces.valid(style) || SpiritDivePieces.element(style) != e || SpiritDivePieces.stage(style) != st || SpiritDivePieces.grade(style) != g)
                        bad.add("the style " + style + " of " + e + "/" + st + "/" + g + " does not round trip");
                }

        // ---- 4. the option
        SpiritDiveDjinnPieces.Look[] look = SpiritDiveDjinnPieces.Look.values();
        if (look.length < 5 || look[4] != SpiritDiveDjinnPieces.Look.REEL || look[3] != SpiritDiveDjinnPieces.Look.ANIME) bad.add("the look values changed (they are append only: DJINN, V2, CLASSIC, ANIME, REEL)");
        if (SpiritDiveDjinnPieces.DEFAULT_LOOK != SpiritDiveDjinnPieces.Look.REEL || SpiritDiveReelRules.parse("garbage") != SpiritDiveDjinnPieces.Look.REEL || SpiritDiveReelRules.parse(null) != SpiritDiveDjinnPieces.Look.REEL)
            bad.add("the default look is not REEL");
        for (String old : new String[]{"DJINN", "V2", "CLASSIC", "ANIME"}) if (!SpiritDiveReelRules.parse(old).name().equals(old)) bad.add("the old look " + old + " cannot be chosen any more");
        if (SpiritDiveReelRules.CAMERA_PUSH) bad.add("the camera must not be touched");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(SpiritDiveReelRules.allTextures().size() + " textures are in place; the clock and the tables hold; the worst full dive draws " + SpiritDiveReelRules.worstVertices(true) + " vertices (character), "
                + SpiritDiveReelRules.worstVertices(false) + " (standard) (V2: " + SpiritDiveReelRules.V2_WORST_VERTICES + "); the style numbers round trip; REEL is the default and the old looks are still choosable");
    }
}
