package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The grimoire's geometry and looks, headless: every quad wound outward, inside the item cube, on a texture that exists. */
class GrimoireBookPlanTest {
    private static final File TEXTURES = new File("src/main/resources/assets/nusmp/textures/item/grimoire_book");

    private static float[] normalOf(GrimoireBookPlan.Face f) {
        return switch (f) {
            case DOWN -> new float[]{0, -1, 0}; case UP -> new float[]{0, 1, 0};
            case NORTH -> new float[]{0, 0, -1}; case SOUTH -> new float[]{0, 0, 1};
            case WEST -> new float[]{-1, 0, 0}; case EAST -> new float[]{1, 0, 0};
        };
    }

    @Test
    void everyTextureExists() {
        for (String t : GrimoireBookPlan.textures()) assertTrue(new File(TEXTURES, t + ".png").isFile(), "missing texture " + t);
    }

    @Test
    void everyQuadOfEveryLookIsWellFormed() {
        int looks = 0;
        for (GrimoireCover c : GrimoireCover.values()) {
            for (BookMotif m : BookMotif.values()) {
                for (boolean held : new boolean[]{false, true}) {
                    BookLook.Key key = new BookLook.Key(c.name(), m, m == BookMotif.TATTERED, held);
                    List<GrimoireBookPlan.Quad> quads = GrimoireBookPlan.build(key);
                    assertTrue(quads.size() > 40 && quads.size() < 200, "quad count " + quads.size());
                    for (GrimoireBookPlan.Quad q : quads) {
                        float[] p = q.pos();
                        for (float v : p) assertTrue(v >= 0 && v <= 16, "outside the item cube: " + v);
                        float ax = p[3] - p[0], ay = p[4] - p[1], az = p[5] - p[2];
                        float bx = p[6] - p[0], by = p[7] - p[1], bz = p[8] - p[2];
                        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                        float[] want = normalOf(q.face());
                        assertTrue(nx * want[0] + ny * want[1] + nz * want[2] > 0, "inward winding on " + q.texture() + " " + q.face());
                        assertArrayEquals(want, q.normal(), 1e-6f);
                        assertTrue(GrimoireBookPlan.textures().contains(q.texture()), "unknown texture " + q.texture());
                        for (float u : q.uv()) assertTrue(u >= 0 && u <= 16);
                    }
                    long glowing = quads.stream().filter(GrimoireBookPlan.Quad::emissive).count();
                    assertEquals(held, glowing > 0, "only a held book glows");
                    looks++;
                }
            }
        }
        assertEquals(GrimoireCover.values().length * BookMotif.values().length * 2, looks);
    }

    @Test
    void openBookIsWellFormedAtEveryStep() {
        for (BookMotif m : BookMotif.values()) {
            for (int step = 1; step <= GrimoireBookPlan.OPEN_STEPS; step++) {
                for (boolean held : new boolean[]{false, true}) {
                    BookLook.Key key = new BookLook.Key("FOUR_LEAF", m, m == BookMotif.TATTERED, held, step);
                    List<GrimoireBookPlan.Quad> quads = GrimoireBookPlan.build(key);
                    assertTrue(quads.size() > 40 && quads.size() < 220, "quad count " + quads.size());
                    float maxZ = 0;
                    boolean pagesToReader = false, coverToOnlookers = false;
                    for (GrimoireBookPlan.Quad q : quads) {
                        float[] p = q.pos();
                        for (float v : p) assertTrue(v >= 0 && v <= 16, "outside the item cube: " + v + " at step " + step);
                        float ax = p[3] - p[0], ay = p[4] - p[1], az = p[5] - p[2];
                        float bx = p[6] - p[0], by = p[7] - p[1], bz = p[8] - p[2];
                        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                        float[] n = q.normal();
                        assertEquals(1f, (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]), 1e-4f);
                        assertTrue(nx * n[0] + ny * n[1] + nz * n[2] > 0, "inward winding on " + q.texture() + " step " + step);
                        float[] axis = normalOf(q.face());
                        assertTrue(n[0] * axis[0] + n[1] * axis[1] + n[2] * axis[2] > 0.5f, "face is the nearest axis");
                        for (int i = 2; i < 12; i += 3) maxZ = Math.max(maxZ, p[i]);
                        if (q.texture().equals("pages") && n[2] < -0.5f) pagesToReader = true;
                        if (q.texture().startsWith("emblem_") && n[2] > 0.4f) coverToOnlookers = true;
                    }
                    assertTrue(pagesToReader, "pages face the owner (-Z)");
                    assertTrue(coverToOnlookers, "front cover and emblem face onlookers (+Z)");
                    assertEquals(held, quads.stream().anyMatch(GrimoireBookPlan.Quad::emissive), "only a held book glows");
                }
            }
        }
        assertTrue(GrimoireBookPlan.OPEN_HINGE_Z < GrimoireBookPlan.OPEN_CZ - GrimoireBookPlan.OPEN_T - GrimoireBookPlan.OPEN_P, "flip hinge in front of the pages");
    }

    @Test
    void canonBooksUseRealCovers() {
        for (CanonBook b : CanonBook.values()) {
            assertNotNull(GrimoireCover.valueOf(b.cover), b.name());
            BookLook look = BookLook.resolve("THREE_LEAF", "FLAME", b.id(), 1234);
            assertEquals(b.cover, look.emblem());
            assertEquals(b.coverColor, look.coverColor(), "canon books are exact, never varied per owner");
            assertSame(b, CanonBook.byId(b.id().toUpperCase()));
        }
    }

    @Test
    void ownerSeedOnlyShadesTheCover() {
        BookLook a = BookLook.resolve("THREE_LEAF", "WIND", null, 0), b = BookLook.resolve("THREE_LEAF", "WIND", null, 12345);
        assertEquals(a.trimColor(), b.trimColor());
        assertEquals(a.emblem(), b.emblem());
        assertEquals(a.motif(), b.motif());
        assertNotEquals(a.coverColor(), b.coverColor());
    }

    @Test
    void kingdomsGetTheirMotifsAndAstaIsTattered() {
        assertEquals(BookMotif.WHEELS, BookLook.resolve("SPADE", "FLAME", null, 0).motif());
        assertEquals(BookMotif.FLORAL, BookLook.resolve("HEART", "WATER", null, 0).motif());
        assertEquals(BookMotif.LATTICE, BookLook.resolve("DIAMOND", "EARTH", null, 0).motif());
        assertEquals(BookMotif.ORNATE, BookLook.resolve("FOUR_LEAF", "WIND", null, 0).motif());
        BookLook asta = BookLook.resolve("FIVE_LEAF", "ANTI_MAGIC", null, 0);
        assertTrue(asta.tattered());
        assertEquals(BookPalette.emblem("FIVE_LEAF"), asta.emblemColor());
    }
}
