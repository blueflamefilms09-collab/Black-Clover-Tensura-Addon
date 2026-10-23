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
 * Why: nothing of the ANIME Spirit Dive can be seen by its author, so the real game proves what can be proved without eyes (the CI server smoke test runs it; key prefix
 * {@code magic.spirit_dive_anime}): (1) every texture the look names is in the mod and every spirit model of the backdrop has its model, texture and glow map; (2) the tables hold: every element has
 * plume rows and hair locks with sane numbers, the specs of the tiers are within the ceiling, the vertex counts of every element, stage, tier and level of detail are under the 3.0.0 V2 dive's worst
 * case and under the cap of the look; (3) the style number of every element, stage and grade survives the round trip the server sends it through (the look only changes what is drawn, never what
 * is sent); (4) the look option has its value and its default, the old looks are all still there. Pure checks: nothing touches a client class, so it runs on the dedicated server.
 */
public final class SpiritDiveAnimeSelfTests {
    private SpiritDiveAnimeSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dive_anime", SpiritDiveAnimeSelfTests::anime);
    }

    private static boolean asset(String path) {
        return SpiritDiveAnimeSelfTests.class.getClassLoader().getResource("assets/nusmp/" + path) != null;
    }

    private static Result anime(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        // ---- 1. the files
        for (String t : SpiritDiveAnimeRules.allTextures()) if (!asset("textures/" + t + ".png")) bad.add("missing texture " + t);
        for (Element e : Element.values())
            for (String id : SpiritDiveAnimeRules.BACKDROP_MODEL[e.ordinal()]) {
                String stem = id.replace(':', '_');
                for (String rel : new String[]{"geo/entity/" + stem + ".geo.json", "textures/entity/" + stem + ".png", "textures/entity/" + stem + "_glow.png"})
                    if (!asset(rel)) bad.add("missing " + rel);
            }
        for (String s : new String[]{"sdv_pillar", "sdv_sigil_wind", "sdv_sigil_fire", "sdv_sigil_water", "sdv_sigil_earth"})
            if (!asset("textures/aura/" + s + ".png")) bad.add("missing aura/" + s);
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- 2. the tables and the budgets
        int worst = 0;
        for (Element e : Element.values()) {
            if (SpiritDiveAnimeGeometry.rows(e, false) < 3) bad.add(e + " has fewer than 3 plume rows");
            if (SpiritDiveAnimeGeometry.lockRows(e) < 8) bad.add(e + " has fewer than 8 locks");
            for (int i = 0; i < SpiritDiveAnimeGeometry.rows(e, false); i++) {
                float[] r = SpiritDiveAnimeGeometry.row(e, i);
                if (r.length != SpiritDiveAnimeGeometry.ROW || r[12] <= 0f || r[13] <= 0f || SpiritDiveAnimeGeometry.reach(r, 1f) > 90f) bad.add(e + " plume row " + i + " is out of range");
            }
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && SpiritDiveTier.gradeOfElement(e) == Grade.STANDARD) continue;
                for (Stage st : Stage.values())
                    for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
                        int v = SpiritDiveAnimeRules.vertices(e, ch, lod, st);
                        if (lod == Lod.FULL && st == Stage.FULL) worst = Math.max(worst, v);
                        if (v > (ch ? SpiritDiveAnimeRules.CAP_CHARACTER : SpiritDiveAnimeRules.CAP_STANDARD) || v >= SpiritDiveAnimeRules.V2_WORST_VERTICES) bad.add(e + " " + st + " " + lod + (ch ? " character" : " standard") + ": " + v + " vertices");
                    }
            }
        }
        for (Lod lod : Lod.values()) if (lod != Lod.CULLED && SpiritDiveAnimeRules.spec(true, lod).plumeSegs() > SpiritDiveAnimeGeometry.MAX_SEGS) bad.add("too many segments at " + lod);
        if (SpiritDiveAnimeRules.vertices(Element.WIND, true, Lod.CULLED, Stage.FULL) > 0) bad.add("a culled dive draws vertices");
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
        if (SpiritDiveDjinnPieces.Look.values().length != 4 || SpiritDiveDjinnPieces.Look.valueOf("ANIME").ordinal() != 3) bad.add("the look values changed (they are append only: DJINN, V2, CLASSIC, ANIME)");
        if (SpiritDiveDjinnPieces.DEFAULT_LOOK != SpiritDiveDjinnPieces.Look.ANIME || SpiritDiveAnimeRules.parse("garbage") != SpiritDiveDjinnPieces.Look.ANIME) bad.add("the default look is not ANIME");
        for (String old : new String[]{"DJINN", "V2", "CLASSIC"}) if (!SpiritDiveAnimeRules.parse(old).name().equals(old)) bad.add("the old look " + old + " cannot be chosen any more");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(SpiritDiveAnimeRules.allTextures().size() + " textures and the spirit models are in place; the tables hold; the worst full dive draws " + worst + " vertices (V2: "
                + SpiritDiveAnimeRules.V2_WORST_VERTICES + "); the style numbers round trip; ANIME is the default and the old looks are still choosable");
    }
}
