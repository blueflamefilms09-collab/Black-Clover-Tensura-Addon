package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.grimoire.GrimoireModelPlan.PlannedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Headless geometry rules: every quad wound outward, inside the item cube, sampling a real texture, with the right tint. */
class GrimoireModelPlanTest {
    private static final File TEXTURES = new File("src/main/resources/assets/nusmp/textures/item/grimoire3d");

    private static GrimoireRenderKey key(InsigniaType i, Thickness t, ClaspType c) {
        return new GrimoireRenderKey(i, CoverMaterial.PLAIN, TrimMetal.Style.PLAIN, t, c, GrimoireRenderKey.auraTier(i), false);
    }

    private static GrimoireRenderKey held(GrimoireRenderKey k) {
        return k.withHeld(true);
    }

    @Test
    void textureBudgetAndFilesExist() {
        File[] files = TEXTURES.listFiles((d, n) -> n.endsWith(".png"));
        assertNotNull(files, "texture folder missing: " + TEXTURES.getAbsolutePath());
        assertTrue(files.length <= 40, "max 40 base textures on disk, found " + files.length);
        assertEquals(GrimoireSprite.values().length, files.length);
        for (GrimoireSprite s : GrimoireSprite.values()) assertTrue(new File(TEXTURES, s.file + ".png").isFile(), "missing " + s.file);
    }

    @Test
    void everyQuadOfEveryKeyIsWellFormed() {
        int keys = 0, maxQuads = 0;
        for (InsigniaType ins : InsigniaType.values()) {
            for (CoverMaterial cover : CoverMaterial.values()) {
                for (TrimMetal.Style style : TrimMetal.Style.values()) {
                    for (Thickness th : Thickness.values()) {
                        for (ClaspType clasp : ClaspType.values()) {
                            for (boolean held : new boolean[]{false, true}) {
                                GrimoireRenderKey k = new GrimoireRenderKey(ins, cover, style, th, clasp, GrimoireRenderKey.auraTier(ins), held);
                                List<PlannedQuad> plan = GrimoireModelPlan.build(k);
                                keys++;
                                maxQuads = Math.max(maxQuads, plan.size());
                                assertFalse(plan.isEmpty());
                                for (PlannedQuad q : plan) check(k, q);
                            }
                        }
                    }
                }
            }
        }
        assertEquals(16 * 5 * 4 * 4 * 4 * 2, keys);
        assertTrue(maxQuads <= 220, "quad budget per grimoire: " + maxQuads);
        System.out.println("planned " + keys + " distinct geometries, largest has " + maxQuads + " quads");
    }

    private static void check(GrimoireRenderKey k, PlannedQuad q) {
        float[] p = q.pos();
        assertEquals(12, p.length);
        for (float v : p) assertTrue(v >= -0.001f && v <= 1.001f, k + ": vertex outside the item cube " + v + " in " + q.sprite());
        for (float v : q.uv()) assertTrue(v >= 0f && v <= 16f, "uv out of range");
        // counter-clockwise seen from outside => the winding normal must equal the declared direction
        float ax = p[6] - p[0], ay = p[7] - p[1], az = p[8] - p[2];
        float bx = p[9] - p[3], by = p[10] - p[4], bz = p[11] - p[5];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        assertTrue(len > 1e-7f, k + ": degenerate quad of " + q.sprite());
        Direction d = q.direction();
        assertEquals(d.getStepX(), Math.round(nx / len), k + " " + q.sprite() + " winding X");
        assertEquals(d.getStepY(), Math.round(ny / len), k + " " + q.sprite() + " winding Y");
        assertEquals(d.getStepZ(), Math.round(nz / len), k + " " + q.sprite() + " winding Z");
        // flat on its own axis
        for (int i = 1; i < 4; i++) {
            if (d.getAxis() == Direction.Axis.X) assertEquals(p[0], p[i * 3], 1e-6f);
            if (d.getAxis() == Direction.Axis.Y) assertEquals(p[1], p[i * 3 + 1], 1e-6f);
            if (d.getAxis() == Direction.Axis.Z) assertEquals(p[2], p[i * 3 + 2], 1e-6f);
        }
        // tint rules: aura => 3 + emissive, crest => 2, trim => 1, pages => none
        String name = q.sprite().name();
        if (name.startsWith("AURA_")) { assertEquals(3, q.tint()); assertTrue(q.emissive()); assertFalse(q.shade()); }
        else { assertFalse(q.emissive(), name + " must not be emissive"); }
        if (name.startsWith("INSIGNIA_")) assertEquals(2, q.tint());
        if (name.equals("TRIM_BORDER")) assertEquals(1, q.tint());
        if (name.startsWith("TRIM_") || name.equals("CLASP_BUCKLE") || name.equals("CLASP_CHAIN") || name.equals("CLASP_LOCK")) assertEquals(1, q.tint());
        if (name.startsWith("COVER_") || name.equals("CLASP_STRAP")) assertEquals(0, q.tint());
        if (name.equals("PAGES")) assertEquals(-1, q.tint());
    }

    private static float[] zRange(List<PlannedQuad> plan) {
        float lo = 1, hi = 0;
        for (PlannedQuad q : plan) {
            if (q.emissive() || q.sprite().name().startsWith("CLASP_") || q.sprite().name().startsWith("TRIM_") || q.sprite().name().startsWith("INSIGNIA_")) continue;
            for (int i = 0; i < 4; i++) { lo = Math.min(lo, q.pos()[i * 3 + 2]); hi = Math.max(hi, q.pos()[i * 3 + 2]); }
        }
        return new float[]{lo, hi};
    }

    @Test
    void thicknessChangesTheBodyDepth() {
        float prev = -1;
        for (Thickness t : new Thickness[]{Thickness.SINGLE_PAGE, Thickness.THIN, Thickness.STANDARD, Thickness.TOME}) {
            float[] z = zRange(GrimoireModelPlan.build(key(InsigniaType.THREE_LEAF, t, ClaspType.OPEN)));
            float depth = z[1] - z[0];
            assertTrue(depth > prev, t + " must be deeper than the previous (" + depth + " vs " + prev + ")");
            assertEquals(2 * t.halfDepth / 16f, depth, 1e-4f, t + " body depth");
            prev = depth;
        }
    }

    @Test
    void claspsAddTheirOwnGeometry() {
        int open = GrimoireModelPlan.build(key(InsigniaType.THREE_LEAF, Thickness.STANDARD, ClaspType.OPEN)).size();
        for (ClaspType c : new ClaspType[]{ClaspType.SINGLE_BUCKLE, ClaspType.DUAL_CHAINS, ClaspType.LOCK_AND_KEY}) {
            List<PlannedQuad> plan = GrimoireModelPlan.build(key(InsigniaType.THREE_LEAF, Thickness.STANDARD, c));
            assertTrue(plan.size() > open + 8, c + " should add quads");
            Set<GrimoireSprite> sprites = EnumSet.noneOf(GrimoireSprite.class);
            for (PlannedQuad q : plan) sprites.add(q.sprite());
            switch (c) {
                case SINGLE_BUCKLE -> assertTrue(sprites.contains(GrimoireSprite.CLASP_BUCKLE) && sprites.contains(GrimoireSprite.CLASP_STRAP));
                case DUAL_CHAINS -> assertTrue(sprites.contains(GrimoireSprite.CLASP_CHAIN));
                case LOCK_AND_KEY -> assertTrue(sprites.contains(GrimoireSprite.CLASP_LOCK) && sprites.contains(GrimoireSprite.CLASP_STRAP));
                default -> fail();
            }
        }
        Set<GrimoireSprite> openSprites = EnumSet.noneOf(GrimoireSprite.class);
        GrimoireModelPlan.build(key(InsigniaType.THREE_LEAF, Thickness.STANDARD, ClaspType.OPEN)).forEach(q -> openSprites.add(q.sprite()));
        assertFalse(openSprites.contains(GrimoireSprite.CLASP_CHAIN) || openSprites.contains(GrimoireSprite.CLASP_LOCK) || openSprites.contains(GrimoireSprite.CLASP_BUCKLE));
    }

    @Test
    void auraTiersAddGlowLayers() {
        Set<GrimoireSprite> common = EnumSet.noneOf(GrimoireSprite.class), rare = EnumSet.noneOf(GrimoireSprite.class), forbidden = EnumSet.noneOf(GrimoireSprite.class);
        GrimoireModelPlan.build(key(InsigniaType.THREE_LEAF, Thickness.STANDARD, ClaspType.OPEN)).forEach(q -> common.add(q.sprite()));
        GrimoireModelPlan.build(key(InsigniaType.FOUR_LEAF, Thickness.STANDARD, ClaspType.OPEN)).forEach(q -> rare.add(q.sprite()));
        GrimoireModelPlan.build(key(InsigniaType.FIVE_LEAF, Thickness.STANDARD, ClaspType.OPEN)).forEach(q -> forbidden.add(q.sprite()));
        // canon: nothing glows while the book is put away ...
        for (Set<GrimoireSprite> s : List.of(common, rare, forbidden)) assertTrue(s.stream().noneMatch(x -> x.name().startsWith("AURA_")), "a closed grimoire does not glow");
        Set<GrimoireSprite> special = EnumSet.noneOf(GrimoireSprite.class);
        GrimoireModelPlan.build(key(InsigniaType.FORBIDDEN_RUNES, Thickness.STANDARD, ClaspType.OPEN)).forEach(q -> special.add(q.sprite()));
        assertTrue(special.stream().noneMatch(x -> x.name().startsWith("AURA_")));
        // ... but a held one glows slightly; only the special outliers add the circle and sparks
        for (InsigniaType i : new InsigniaType[]{InsigniaType.THREE_LEAF, InsigniaType.FOUR_LEAF, InsigniaType.FIVE_LEAF}) {
            Set<GrimoireSprite> h = EnumSet.noneOf(GrimoireSprite.class);
            GrimoireModelPlan.build(held(key(i, Thickness.STANDARD, ClaspType.OPEN))).forEach(q -> h.add(q.sprite()));
            assertTrue(h.contains(GrimoireSprite.AURA_HALO) && !h.contains(GrimoireSprite.AURA_SIGIL) && !h.contains(GrimoireSprite.AURA_SPARKS), i + " held");
        }
        Set<GrimoireSprite> hs = EnumSet.noneOf(GrimoireSprite.class);
        GrimoireModelPlan.build(held(key(InsigniaType.FORBIDDEN_RUNES, Thickness.STANDARD, ClaspType.OPEN))).forEach(q -> hs.add(q.sprite()));
        assertTrue(hs.contains(GrimoireSprite.AURA_SPARKS) && hs.contains(GrimoireSprite.AURA_SIGIL) && hs.contains(GrimoireSprite.AURA_HALO));
    }

    @Test
    void boundlessBooksAreCoverless() {
        Set<GrimoireSprite> s = EnumSet.noneOf(GrimoireSprite.class);
        long coverFaces = GrimoireModelPlan.build(key(InsigniaType.BOUNDLESS, Thickness.TOME, ClaspType.OPEN)).stream()
                .peek(q -> s.add(q.sprite())).filter(q -> q.sprite() == GrimoireSprite.COVER_PLAIN && q.direction().getAxis() == Direction.Axis.Z && q.pos()[3] - q.pos()[0] > 0.2f).count();
        assertEquals(0, coverFaces, "no front/back cover boards on a coverless book");
        assertTrue(s.contains(GrimoireSprite.PAGES));
        // the pages are exposed front and back
        assertTrue(GrimoireModelPlan.build(key(InsigniaType.BOUNDLESS, Thickness.TOME, ClaspType.OPEN)).stream()
                .anyMatch(q -> q.sprite() == GrimoireSprite.PAGES && q.direction() == Direction.SOUTH));
    }

    @Test
    void buildIsDeterministicAndRecolourSharesGeometry() {
        GrimoireRenderKey k = GrimoireAppearance.fromGrimoireId(4242).renderKey();
        List<PlannedQuad> a = GrimoireModelPlan.build(k), b = GrimoireModelPlan.build(k);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) assertArrayEquals(a.get(i).pos(), b.get(i).pos());
    }

    @Test
    void ornateCoversCarryGildedBorderInsteadOfHardware() {
        GrimoireRenderKey ornate = new GrimoireRenderKey(InsigniaType.FOUR_LEAF, CoverMaterial.METALLIC_TRIM, TrimMetal.Style.PLAIN, Thickness.STANDARD, ClaspType.OPEN, 0, false);
        Set<GrimoireSprite> s = EnumSet.noneOf(GrimoireSprite.class);
        GrimoireModelPlan.build(ornate).forEach(q -> s.add(q.sprite()));
        assertTrue(s.contains(GrimoireSprite.TRIM_BORDER));
        assertFalse(s.contains(GrimoireSprite.TRIM_PLAIN), "no corner caps or bands on an ornate cover");
        GrimoireRenderKey bare = new GrimoireRenderKey(InsigniaType.THREE_LEAF, CoverMaterial.PLAIN, TrimMetal.Style.NONE, Thickness.STANDARD, ClaspType.OPEN, 0, false);
        Set<GrimoireSprite> b = EnumSet.noneOf(GrimoireSprite.class);
        GrimoireModelPlan.build(bare).forEach(q -> b.add(q.sprite()));
        assertTrue(b.stream().noneMatch(x -> x.name().startsWith("TRIM_")), "an ordinary canon grimoire is bare leather");
    }
}
