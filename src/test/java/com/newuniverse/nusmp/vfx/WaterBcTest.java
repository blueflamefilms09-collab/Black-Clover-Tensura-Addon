package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the pure rules of the Water hybrid (docs/vfx_remake/water_ingame.md) are what the painter, the frame plan and the CI self-test all read, so they are pinned here against the REAL scene builder: which
 * shapes the look owns (never the six old WaterSpellLayer shapes, the Valkyrie Dress among them: the wings are deferred), how a payload is told into one of the seven looks, the worst case of every look
 * at every tier against the caps the frame ceilings are built on, the admission of a crowd of casters nearest first under the per-frame ceilings (a randomised crowd can never pass them), the quality
 * scaling, the toon shading of the cores (tones, two sided lighting, glass versus solid, the tint), the buffer's hard ceiling, determinism, finite numbers, and that a tinted effect only uses textures that
 * have a grey twin.
 */
class WaterBcTest {
    @Test
    void theLookOwnsFiveShapesAndNeverTheOldWaterSpellShapesOrTheValkyrie() {
        assertEquals(Set.of(VfxShape.WATERFX_CAST, VfxShape.WATERFX_ZONE, VfxShape.WATERFX_BURST, VfxShape.WATER_SPLASH, VfxShape.WATER_RING), new HashSet<>(WaterBc.SHAPES));
        for (VfxShape s : WaterBc.UNTOUCHED) assertFalse(WaterBc.owns(s), s + " must keep its old layer in every setting");
        assertEquals(Set.of(VfxShape.WATER_DRAGON, VfxShape.WATER_CRADLE, VfxShape.WATER_BURST, VfxShape.WATER_JAVELIN, VfxShape.WATER_NEST, VfxShape.WATER_DRESS), new HashSet<>(WaterBc.UNTOUCHED));
        for (VfxShape s : WaterBc.UNTOUCHED) assertEquals(-1, WaterBc.kindOf(s, 20, 20, true));
    }

    @Test
    void aPayloadIsToldIntoTheSevenLooks() {
        assertEquals(WaterBc.ROAR, WaterBc.kindOf(VfxShape.WATERFX_CAST, 12, 24, false), "the dragon flies about 0.5 to 0.9 blocks a tick");
        assertEquals(WaterBc.ROAR, WaterBc.kindOf(VfxShape.WATERFX_CAST, 22, 24, false), "the fan of three dragons");
        assertEquals(WaterBc.ROAR, WaterBc.kindOf(VfxShape.WATERFX_CAST, 9, 16, false), "the torrent");
        assertEquals(WaterBc.LANCE, WaterBc.kindOf(VfxShape.WATERFX_CAST, 26, 12, false), "Aqua Javelin");
        assertEquals(WaterBc.LANCE, WaterBc.kindOf(VfxShape.WATERFX_CAST, 33, 16, false), "Valkyrie Tide Lance");
        assertEquals(WaterBc.ROAR, WaterBc.kindOf(VfxShape.WATERFX_CAST, 0, 0, false), "a zero length or duration is the dragon, never a division by zero");
        assertEquals(WaterBc.CRADLE, WaterBc.kindOf(VfxShape.WATERFX_ZONE, 4, 160, true));
        assertEquals(WaterBc.NEST, WaterBc.kindOf(VfxShape.WATERFX_ZONE, 1, 200, false));
        assertEquals(WaterBc.NEST, WaterBc.kindOf(VfxShape.WATERFX_ZONE, 1, 60, false), "the Scale Mail zone");
        assertEquals(WaterBc.BURST, WaterBc.kindOf(VfxShape.WATERFX_BURST, 0, 26, false));
        assertEquals(WaterBc.SPLASH, WaterBc.kindOf(VfxShape.WATER_SPLASH, 0, 20, false));
        assertEquals(WaterBc.RING, WaterBc.kindOf(VfxShape.WATER_RING, 0, 30, false));
        assertEquals(-1, WaterBc.kindOf(VfxShape.WIND_SLASH, 5, 5, false));
        assertEquals(-1, WaterBc.kindOf(null, 5, 5, false));
        assertEquals(WaterBc.KINDS, WaterBc.KIND_NAMES.length);
    }

    @Test
    void theRealSceneStaysInsideTheCapsOfEveryTier() {
        for (int k = 0; k < WaterBc.KINDS; k++) {
            for (int tier = WaterBc.LOD0; tier <= WaterBc.SPRITE; tier++) {
                WaterBcBudget.Peak pk = WaterBcBudget.peak(k, tier, false);
                String what = WaterBc.KIND_NAMES[k] + " tier " + tier + ": " + pk.mesh + " mesh + " + pk.sprites + " sprite quads";
                assertTrue(pk.mesh <= WaterBc.meshCap(k, tier), what + " over the mesh cap " + WaterBc.meshCap(k, tier));
                assertTrue(pk.sprites <= WaterBc.spriteCap(k, tier), what + " over the sprite cap " + WaterBc.spriteCap(k, tier));
                assertTrue(pk.sprites > 0, what + ": nothing drawn");
                if (tier == WaterBc.SPRITE) assertEquals(0, pk.mesh, what);
                else assertTrue(pk.mesh * 100 >= WaterBc.meshCap(k, tier) * 90, what + ": the cap is more than a tenth above the real peak, it is not a pinned number any more");
            }
            assertTrue(WaterBc.meshCap(k, WaterBc.LOD1) < WaterBc.meshCap(k, WaterBc.LOD0));
            assertEquals(0, WaterBc.meshCap(k, WaterBc.SPRITE));
            assertEquals(0, WaterBc.spriteCap(k, WaterBc.SKIP));
            assertTrue(WaterBc.spriteCap(k, WaterBc.SPRITE) < WaterBc.spriteCap(k, WaterBc.LOD0));
        }
        // the peaks of the preview (water_bc.md section 4) hold in the game's scene: at most these full-detail totals, and the burst and the roar need the most
        assertEquals(335, WaterBcBudget.peak(WaterBc.ROAR, WaterBc.LOD0, false).mesh);
        assertEquals(54, WaterBcBudget.peak(WaterBc.ROAR, WaterBc.LOD0, false).sprites);
        assertEquals(110, WaterBcBudget.peak(WaterBc.LANCE, WaterBc.LOD0, false).mesh);
        assertEquals(176, WaterBcBudget.peak(WaterBc.CRADLE, WaterBc.LOD0, false).mesh);
        assertEquals(120, WaterBcBudget.peak(WaterBc.NEST, WaterBc.LOD0, false).mesh);
        assertEquals(208, WaterBcBudget.peak(WaterBc.BURST, WaterBc.LOD0, false).mesh);
        assertEquals(144, WaterBcBudget.peak(WaterBc.SPLASH, WaterBc.LOD0, false).mesh);
        assertEquals(132, WaterBcBudget.peak(WaterBc.RING, WaterBc.LOD0, false).mesh);
        for (int s = 0; s < 3; s++) for (VfxShape sh : WaterBc.SHAPES) assertTrue(WaterBc.vertexBudget(sh) > 0 && WaterBc.vertexBudget(sh) <= 400, sh + " vertex budget");
    }

    @Test
    void theTintedSpritesAreAllOnesWithAGreyTwin() {
        for (int k = 0; k < WaterBc.KINDS; k++) {
            if (!WaterBc.tintable(k)) continue;
            for (int tier = WaterBc.LOD0; tier <= WaterBc.SPRITE; tier++) {
                WaterBcBudget.Peak pk = WaterBcBudget.peak(k, tier, true);
                for (int t = 0; t < pk.textures.length; t++) if (pk.textures[t]) assertTrue(WaterBc.hasTwin(t), WaterBc.KIND_NAMES[k] + " tier " + tier + " draws " + WaterBc.SPRITES[t] + " tinted, which has no grey twin");
            }
        }
        assertTrue(WaterBc.tintable(WaterBc.SPLASH) && WaterBc.tintable(WaterBc.RING));
        for (int k : new int[]{WaterBc.ROAR, WaterBc.LANCE, WaterBc.CRADLE, WaterBc.NEST, WaterBc.BURST}) assertFalse(WaterBc.tintable(k));
        assertEquals(8, WaterBc.TINT_TWINS.length);
    }

    private static int[] admitCrowd(double[] dist, int[] kind, float scale) {
        int n = dist.length;
        int[] tier = new int[n];
        WaterBc.admit(dist, kind, n, tier, new boolean[n], scale);
        return tier;
    }

    @Test
    void aCrowdIsAdmittedNearestFirstUnderTheCeilings() {
        // ten roars in view (water_bc.md): the near ones get the mesh, the far ones the light mesh, then the flat sprites, and the sum never passes the ceilings
        double[] dist = new double[10];
        int[] kind = new int[10];
        for (int i = 0; i < 10; i++) { dist[i] = 4 + 2 * i; kind[i] = WaterBc.ROAR; }
        int[] tier = admitCrowd(dist, kind, 1f);
        int mesh = 0, sprite = 0;
        for (int i = 0; i < 10; i++) { mesh += WaterBc.meshCap(kind[i], tier[i]); sprite += WaterBc.spriteCap(kind[i], tier[i]); }
        assertTrue(mesh <= WaterBc.FRAME_MESH_CEILING && sprite <= WaterBc.FRAME_SPRITE_CEILING, mesh + " / " + sprite);
        assertEquals(WaterBc.LOD0, tier[0]);
        assertEquals(WaterBc.LOD0, tier[1]);
        for (int i = 1; i < 10; i++) assertTrue(tier[i] >= tier[i - 1], "a nearer effect never gets a worse tier than a farther one: " + java.util.Arrays.toString(tier));
        assertTrue(tier[9] == WaterBc.SPRITE, "the farthest falls back to the flat sprites once the mesh ceiling is spent: " + java.util.Arrays.toString(tier));
        int lod0 = 0;
        for (int t : tier) if (t == WaterBc.LOD0) lod0++;
        assertTrue(lod0 <= WaterBc.LOD0_MAX_IN_VIEW);
        // one effect alone, near: the full mesh; far: the light mesh
        assertEquals(WaterBc.LOD0, admitCrowd(new double[]{3}, new int[]{WaterBc.BURST}, 1f)[0]);
        assertEquals(WaterBc.LOD1, admitCrowd(new double[]{20}, new int[]{WaterBc.BURST}, 1f)[0]);
        assertEquals(WaterBc.LOD0, admitCrowd(new double[]{WaterBc.LOD0_RANGE}, new int[]{WaterBc.BURST}, 1f)[0]);
        // the fourth nearest is LOD1 whatever its distance (more than three in view)
        int[] four = admitCrowd(new double[]{1, 2, 3, 4}, new int[]{WaterBc.RING, WaterBc.RING, WaterBc.RING, WaterBc.RING}, 1f);
        assertEquals(WaterBc.LOD0, four[2]);
        assertEquals(WaterBc.LOD1, four[3]);
    }

    @Test
    void noCrowdCanPassTheCeilingsAtAnyQuality() {
        Random r = new Random(7);
        for (int round = 0; round < 400; round++) {
            int n = 1 + r.nextInt(64);
            double[] dist = new double[n];
            int[] kind = new int[n];
            for (int i = 0; i < n; i++) { dist[i] = r.nextDouble() * 60; kind[i] = r.nextInt(WaterBc.KINDS); }
            float scale = new float[]{1f, 0.35f, 0.15f}[r.nextInt(3)];
            int[] tier = admitCrowd(dist, kind, scale);
            int mesh = 0, sprite = 0;
            for (int i = 0; i < n; i++) { mesh += WaterBc.meshCap(kind[i], tier[i]); sprite += WaterBc.spriteCap(kind[i], tier[i]); assertTrue(tier[i] >= WaterBc.LOD0 && tier[i] <= WaterBc.SKIP); }
            assertTrue(mesh <= WaterBc.meshCeiling(scale), "mesh " + mesh + " over " + WaterBc.meshCeiling(scale) + " at scale " + scale);
            assertTrue(sprite <= WaterBc.spriteCeiling(scale), "sprites " + sprite + " over " + WaterBc.spriteCeiling(scale));
        }
        assertEquals(1f, WaterBc.qualityScale(40000));
        assertTrue(WaterBc.qualityScale(14000) < 1f && WaterBc.qualityScale(6000) < WaterBc.qualityScale(14000));
        assertTrue(WaterBc.qualityScale(0) >= 0.1f);
        assertEquals(1200, WaterBc.meshCeiling(1f));
        assertEquals(500, WaterBc.spriteCeiling(1f));
        assertEquals(1200, WaterBc.meshCeiling(9f), "never above the full ceiling");
    }

    @Test
    void aForcedTierIsOnlyAHookForTestsAndPreviews() {
        try {
            WaterBc.forceTier(WaterBc.SPRITE);
            assertEquals(WaterBc.SPRITE, admitCrowd(new double[]{1}, new int[]{WaterBc.ROAR}, 1f)[0]);
            WaterBc.forceTier(99);
            assertEquals(-1, WaterBc.forcedTier());
        } finally {
            WaterBc.forceTier(-1);
        }
        assertEquals(-1, WaterBc.forcedTier());
        assertEquals(WaterBc.LOD0, admitCrowd(new double[]{1}, new int[]{WaterBc.ROAR}, 1f)[0]);
    }

    @Test
    void theOptionDefaultsToBcAndParsesV1() {
        assertEquals(WaterBc.Style.BC, WaterBc.DEFAULT_STYLE);
        assertEquals(WaterBc.Style.BC, WaterBc.parse(null));
        assertEquals(WaterBc.Style.BC, WaterBc.parse("garbage"));
        assertEquals(WaterBc.Style.BC, WaterBc.parse(" bc "));
        assertEquals(WaterBc.Style.V1, WaterBc.parse("v1"));
        assertEquals(WaterBc.Style.V1, WaterBc.parse("V1"));
        assertEquals("vfx.element.water", WaterBc.CONFIG_PATH);
        assertEquals("nusmp-water-bc-client.toml", WaterBc.CONFIG_FILE);
        WaterBc.Style before = WaterBc.style();
        try {
            WaterBc.setStyle(WaterBc.Style.V1);
            assertFalse(WaterBc.bcLook());
            WaterBc.setStyle(null);
            assertTrue(WaterBc.bcLook());
        } finally {
            WaterBc.setStyle(before);
        }
    }

    @Test
    void theTintChoiceKeepsWaterBluesAndTintsEveryOtherColour() {
        for (int c : new int[]{0xFF4FA8FF, 0xFF3A7BD5, 0xFF6EC8FF, 0xFF2F6FE0, 0xFF58B0F0}) assertTrue(WaterBc.nativePalette(c), Integer.toHexString(c));
        for (int c : new int[]{0xFFB0121B, 0xFFFF8C1A, 0xFF2EC27E, 0xFFFFE066, 0xFFB050E0, 0xFF1A1A1A, 0xFFFFFFFF, 0xFF808080, 0xFF101830})
            assertFalse(WaterBc.nativePalette(c), Integer.toHexString(c) + " is not a water blue");
    }

    // ------------------------------------------------------------------------------------------------ the scene, the toon shading, the buffer
    private static final class Count implements WaterBcScene.Out {
        int sprites, meshes;
        boolean finite = true;
        final long[] hash = new long[1];

        @Override public void sprite(int tex, boolean add, float u0, float u1, int argb, float[] q) {
            sprites++;
            for (int i = 0; i < 12; i++) if (!Float.isFinite(q[i])) finite = false;
            if (!Float.isFinite(u0) || !Float.isFinite(u1) || tex < 0 || tex >= WaterBc.SPRITES.length || (argb >>> 24) == 0) finite = false;
            for (int i = 0; i < 12; i++) hash[0] = hash[0] * 31 + Float.floatToIntBits(q[i]);
            hash[0] = hash[0] * 31 + argb + tex;
        }

        @Override public void mesh(WaterBcToon.Inst m) {
            meshes++;
            for (float v : new float[]{m.px, m.py, m.pz, m.yx, m.yy, m.yz, m.hx, m.hy, m.hz, m.sx, m.sy, m.sz, m.alpha, m.foamAlpha}) if (!Float.isFinite(v)) finite = false;
            hash[0] = hash[0] * 31 + Float.floatToIntBits(m.px) + 7L * m.piece;
        }
    }

    @Test
    void theSceneIsFiniteAndDeterministicAtAnyCameraAndSize() {
        for (int k = 0; k < WaterBc.KINDS; k++) {
            for (int tier = WaterBc.LOD0; tier <= WaterBc.SPRITE; tier++) {
                long first = 0;
                for (int run = 0; run < 2; run++) {
                    Count c = new Count();
                    WaterBcScene s = new WaterBcScene();
                    s.view.set(0.6f, 0f, -0.8f, 0f, 1f, 0f);
                    for (int age = 0; age <= 30; age++) {
                        s.begin(c, 3f, 1f, -9f, 3f, 1f, -9.0001f, 0.0f + 1.7f, age + 0.25f, 30, 987654321L, false, 0).build(k, tier);       // a zero length effect (a dash on the spot), a big one
                        s.begin(c, -2f, 5f, 4f, -2f, 5f + 30f, 4f, 3.5f, age, 30, 1L, true, 0xB0121B).build(k, tier);                          // straight up, tinted
                    }
                    assertTrue(c.finite, WaterBc.KIND_NAMES[k] + " tier " + tier + " made a number that is not finite");
                    if (run == 0) first = c.hash[0];
                    else assertEquals(first, c.hash[0], WaterBc.KIND_NAMES[k] + " is not deterministic");
                }
            }
        }
        Count none = new Count();
        new WaterBcScene().begin(none, 0, 0, 5, 0, 1, 5, 1, 3, 20, 1, false, 0).build(WaterBc.SPLASH, WaterBc.SKIP);
        assertEquals(0, none.sprites + none.meshes, "the skip tier draws nothing");
        new WaterBcScene().begin(none, 0, 0, 5, 0, 1, 5, 1, 3, 20, 1, false, 0).build(-1, WaterBc.LOD0);
        assertEquals(0, none.sprites + none.meshes);
    }

    private static WaterBcToon.View view() {
        WaterBcToon.View v = new WaterBcToon.View();
        v.set(-1f, 0f, 0f, 0f, 1f, 0f);
        return v;
    }

    @Test
    void theCameraFrameAndTheLightAreRightHanded() {
        WaterBcToon.View v = view();
        assertEquals(0f, v.fx, 1e-6f);
        assertEquals(0f, v.fy, 1e-6f);
        assertEquals(1f, v.fz, 1e-6f, "right = -x, up = +y: the camera looks along +z (as in the game)");
        assertEquals(1f, v.sx * v.sx + v.sy * v.sy + v.sz * v.sz, 1e-5f);
        assertTrue(v.sx * v.rx + v.sy * v.ry + v.sz * v.rz < 0f && v.sy > 0f, "the light comes from the upper left");
        assertTrue(v.sx * v.fx + v.sy * v.fy + v.sz * v.fz < 0f, "and from the viewer's side");
        WaterBcToon.View turned = new WaterBcToon.View();
        turned.set(0f, 0f, 1f, 0f, 1f, 0f);                          // looking along +x: right = +z
        assertEquals(1f, turned.fx, 1e-6f);
        assertEquals(0f, turned.fz, 1e-6f);
    }

    @Test
    void aPieceIsPlacedShadedAndSortedIntoItsPass() {
        WaterBcMeshBuffer buf = new WaterBcMeshBuffer(WaterBc.FRAME_MESH_CEILING);
        WaterBcToon.Inst solid = new WaterBcToon.Inst().set(WaterBc.P_HEAD_UPPER, false, 0f, 1f, 6f, 0f, 0f, 1f, 0f, 1f, 0f, false, 1f, 1f, 1f, 1f, 1f, WaterBcToon.PAL_CORE, false);
        int n = WaterBcToon.emit(solid, view(), buf, false, 0);
        WaterBcMeshes.Piece head = WaterBcMeshes.get(WaterBc.P_HEAD_UPPER, false);
        assertEquals(head.quads, n, "every quad of a piece in front of the camera is shaded");
        assertEquals(head.perLayer[0] + head.perLayer[2], buf.count(WaterBcMeshBuffer.SOLID), "opaque body and foam go to the solid pass");
        assertEquals(head.perLayer[1], buf.count(WaterBcMeshBuffer.GLOW), "the emissive layer is additive");
        assertEquals(0, buf.count(WaterBcMeshBuffer.TRANSLUCENT));
        for (int q = 0; q < buf.count(WaterBcMeshBuffer.SOLID); q++) assertEquals(255, buf.colours(WaterBcMeshBuffer.SOLID)[q] >>> 24);
        // a glass hull is translucent and its alpha follows the fresnel rule (never opaque in the middle)
        buf.reset();
        WaterBcToon.Inst glass = new WaterBcToon.Inst().set(WaterBc.P_HEAD_HULL, false, 0f, 1f, 6f, 0f, 0f, 1f, 0f, 1f, 0f, false, 1f, 1f, 1f, 0.32f, 1f, WaterBcToon.PAL_GLASS, true);
        WaterBcToon.emit(glass, view(), buf, false, 0);
        assertTrue(buf.count(WaterBcMeshBuffer.TRANSLUCENT) > 0 && buf.count(WaterBcMeshBuffer.SOLID) == 0);
        for (int q = 0; q < buf.count(WaterBcMeshBuffer.TRANSLUCENT); q++) {
            int a = buf.colours(WaterBcMeshBuffer.TRANSLUCENT)[q] >>> 24;
            assertTrue(a > 0 && a < 255, "a glass quad is blended, alpha " + a);
        }
        // behind the camera: nothing; a zero scale: nothing
        buf.reset();
        assertEquals(0, WaterBcToon.emit(new WaterBcToon.Inst().set(WaterBc.P_SPHERE, false, 0f, 0f, -20f, 0f, 1f, 0f, 0f, 0f, 1f, false, 16f, 16f, 16f, 1f, 1f, 0, false), view(), buf, false, 0));
        assertEquals(0, WaterBcToon.emit(new WaterBcToon.Inst().set(WaterBc.P_SPHERE, false, 0f, 0f, 6f, 0f, 1f, 0f, 0f, 0f, 1f, false, 0f, 0f, 0f, 1f, 1f, 0, false), view(), buf, false, 0));
        assertEquals(0, buf.total());
    }

    @Test
    void aTintedPieceTakesTheTintColourAndAWaterPieceTheBlues() {
        WaterBcMeshBuffer a = new WaterBcMeshBuffer(WaterBc.FRAME_MESH_CEILING), b = new WaterBcMeshBuffer(WaterBc.FRAME_MESH_CEILING);
        WaterBcToon.Inst m = new WaterBcToon.Inst().set(WaterBc.P_RIPPLE, false, 0f, 0f, 6f, 0f, 1f, 0f, 0f, 0f, 1f, false, 2f, 1f, 2f, 1f, 1f, WaterBcToon.PAL_WATER, false);
        WaterBcToon.emit(m, view(), a, false, 0);
        WaterBcToon.emit(m, view(), b, true, 0xC02020);
        long blue = 0, red = 0, blueTint = 0, redTint = 0;
        for (int q = 0; q < a.count(WaterBcMeshBuffer.SOLID); q++) { int c = a.colours(WaterBcMeshBuffer.SOLID)[q]; blue += c & 255; red += (c >> 16) & 255; }
        for (int q = 0; q < b.count(WaterBcMeshBuffer.SOLID); q++) { int c = b.colours(WaterBcMeshBuffer.SOLID)[q]; blueTint += c & 255; redTint += (c >> 16) & 255; }
        assertTrue(a.count(WaterBcMeshBuffer.SOLID) > 0 && a.count(WaterBcMeshBuffer.SOLID) == b.count(WaterBcMeshBuffer.SOLID));
        assertTrue(blue > red, "the water palette is blue");
        assertTrue(redTint > blueTint, "the tinted palette follows the tint colour");
    }

    @Test
    void theBufferIsHardCappedAtItsCeilingAndReusedWithoutGrowing() {
        WaterBcMeshBuffer buf = new WaterBcMeshBuffer(WaterBc.FRAME_MESH_CEILING);
        buf.reset(10);
        for (int i = 0; i < 30; i++) buf.add(i % 3, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0xFF112233);
        assertEquals(10, buf.total());
        assertEquals(20, buf.dropped());
        float[] before = buf.data(0);
        buf.reset(WaterBc.FRAME_MESH_CEILING);
        assertEquals(0, buf.total());
        assertTrue(before == buf.data(0), "the arrays are kept between frames");
        buf.reset(99999);
        assertEquals(WaterBc.FRAME_MESH_CEILING, buf.limit());
        buf.reset(-5);
        assertEquals(0, buf.limit());
    }

    @Test
    void theTextureAndMeshNamesAreConsistent() {
        assertEquals(25, WaterBc.SPRITES.length);
        assertEquals(new HashSet<>(List.of(WaterBc.SPRITES)).size(), WaterBc.SPRITES.length, "no duplicate sprite");
        assertEquals(WaterBc.SPRITES.length + WaterBc.TINT_TWINS.length + 1, WaterBc.allTextures().size());
        assertEquals("wbc_b_body", WaterBc.fileName("b_body", false));
        assertEquals("wbc_mist_t", WaterBc.fileName("mist", true));
        assertEquals(WaterBc.T_A_FLASH, WaterBc.SPRITES.length - 1);
        assertEquals("a_flash", WaterBc.SPRITES[WaterBc.T_A_FLASH]);
        assertEquals("mist", WaterBc.SPRITES[WaterBc.T_MIST]);
        assertEquals("glow", WaterBc.SPRITES[WaterBc.T_GLOW]);
        assertEquals(22, WaterBc.allMeshes().size());
        assertEquals("bc_head_upper_lo", WaterBc.meshFile(WaterBc.P_HEAD_UPPER, true));
        assertEquals("bc_dome", WaterBc.meshFile(WaterBc.P_DOME, false));
        for (String p : WaterBc.PIECES) assertFalse(p.contains("wing"), "no Valkyrie wing piece is part of the set");
        assertEquals(11, WaterBc.PIECES.length);
    }
}
