package com.newuniverse.nusmp.client.relics.djinn;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The white bone-dragon set of the canon Belial (docs/belial/models.md) is all there, has the pieces the layer draws, and stays under its own frame budget. */
class DjinnBoneSetTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "anime_multiverse");
    private static final Path DIR = ASSETS.resolve("djinn").resolve(DjinnLook.dir("belial", DjinnLook.BONE));
    private static final List<String> CORE = List.of("helm", "chest", "belt", "pauldron_l", "pauldron_r", "vambrace_l", "vambrace_r", "gauntlet_l", "gauntlet_r", "greave_l",
            "greave_r", "crest", "wing_in_l", "wing_in_r", "wing_out_l", "wing_out_r", "weapon", "backdrop", "tail_seg", "tail_tip");
    /** The most quads (body + glow) the set may draw in a Full Equip at close range, the 3 tail segments counted as the layer draws them. Far players skip the detail pieces. */
    private static final int BUDGET = 6400;

    private static int quads(String piece) throws IOException {
        return (int) Files.readAllLines(DIR.resolve(piece + ".txt")).stream().filter(s -> s.startsWith("qn ")).count();
    }

    @Test
    void theBoneSetHasEveryPieceTheLayerDraws() throws IOException {
        assertTrue(Files.exists(DIR.resolve(DjinnLook.MARKER_PIECE + ".txt")), "the marker piece decides the set");
        for (String piece : CORE) assertTrue(quads(piece) > 0, "empty or missing bone piece " + piece);
        for (String glow : new String[]{"", "_glow"})
            assertTrue(Files.exists(ASSETS.resolve(DjinnLook.skinPath("belial", DjinnLook.BONE, !glow.isEmpty()))), "skin " + glow);
    }

    @Test
    void theBoneSetStaysUnderTheFrameBudget() throws IOException {
        int total = 0;
        for (String piece : CORE) total += piece.equals("tail_seg") ? quads(piece) * 3 : quads(piece);
        for (String piece : List.of("mantle", "horn_l", "horn_r")) total += quads(piece);
        assertTrue(total <= BUDGET, "the bone set draws " + total + " quads in a Full Equip; the budget is " + BUDGET);
    }
}
