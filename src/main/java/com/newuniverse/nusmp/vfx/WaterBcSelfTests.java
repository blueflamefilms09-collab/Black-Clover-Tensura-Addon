package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Water hybrid (Roar, Javelin / Lance, Cradle, Nest, Burst, Splash, Ring) is drawn by the client and cannot be seen in a game by its author, so the real game proves what can be proved without eyes
 * (the CI server smoke test runs it; key prefix {@code magic.water_bc}): (1) the five shapes it owns are exactly the WATERFX trio, the splash and the ring, and none of the six old WaterSpellLayer shapes (the
 * Valkyrie Dress among them: the wings are deferred) is among them; (2) the real scene builder, run over the whole life of every effect at every tier, stays inside the quad caps the frame ceilings are
 * built on, and a crowd of casters is admitted under the per-frame ceilings of mesh and sprite quads; (3) all textures and all 22 mesh files are in the jar and well formed; (4) the option defaults to BC and
 * parses V1; (5) the tint choice (water blues keep the native textures, every other colour of the tintable splash and ring is tinted) and the choice of the look from a payload. Pure checks: nothing
 * touches a client class, so it runs on the dedicated server; the effect sends no packet and does no server work.
 */
public final class WaterBcSelfTests {
    private WaterBcSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.water_bc", WaterBcSelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<VfxShape> owned = WaterBc.SHAPES;
        if (owned.size() != 5 || !owned.containsAll(List.of(VfxShape.WATERFX_CAST, VfxShape.WATERFX_ZONE, VfxShape.WATERFX_BURST, VfxShape.WATER_SPLASH, VfxShape.WATER_RING))) bad.add("the owned shapes are wrong: " + owned);
        for (VfxShape untouched : WaterBc.UNTOUCHED) if (WaterBc.owns(untouched)) bad.add(untouched + " must stay with the old layer");
        // ---- the looks from a payload
        if (WaterBc.kindOf(VfxShape.WATERFX_CAST, 12, 24, false) != WaterBc.ROAR || WaterBc.kindOf(VfxShape.WATERFX_CAST, 26, 12, false) != WaterBc.LANCE || WaterBc.kindOf(VfxShape.WATERFX_CAST, 33, 16, false) != WaterBc.LANCE
                || WaterBc.kindOf(VfxShape.WATERFX_ZONE, 1, 160, true) != WaterBc.CRADLE || WaterBc.kindOf(VfxShape.WATERFX_ZONE, 1, 200, false) != WaterBc.NEST || WaterBc.kindOf(VfxShape.WATERFX_BURST, 0, 26, false) != WaterBc.BURST
                || WaterBc.kindOf(VfxShape.WATER_SPLASH, 0, 20, false) != WaterBc.SPLASH || WaterBc.kindOf(VfxShape.WATER_RING, 0, 30, false) != WaterBc.RING || WaterBc.kindOf(VfxShape.WATER_DRESS, 0, 600, true) != -1)
            bad.add("the choice of the look from a payload is wrong");
        // ---- the real scene against the caps (mesh quads per tier, sprite quads per tier)
        int worstMesh = 0, worstSprites = 0;
        for (int k = 0; k < WaterBc.KINDS; k++) {
            for (int tier = WaterBc.LOD0; tier <= WaterBc.SPRITE; tier++) {
                WaterBcBudget.Peak pk = WaterBcBudget.peak(k, tier, false);
                worstMesh = Math.max(worstMesh, pk.mesh);
                worstSprites = Math.max(worstSprites, pk.sprites);
                if (pk.mesh > WaterBc.meshCap(k, tier) || pk.sprites > WaterBc.spriteCap(k, tier) || pk.sprites <= 0)
                    bad.add(WaterBc.KIND_NAMES[k] + " tier " + tier + " draws " + pk.mesh + " mesh + " + pk.sprites + " sprite quads, caps " + WaterBc.meshCap(k, tier) + " + " + WaterBc.spriteCap(k, tier));
                if (tier == WaterBc.SPRITE && pk.mesh != 0) bad.add(WaterBc.KIND_NAMES[k] + " sprite tier draws a mesh");
            }
        }
        if (WaterBc.vertexBudget(VfxShape.WATERFX_CAST) > 400 || WaterBc.vertexBudget(VfxShape.WATERFX_CAST) <= 0) bad.add("vertex budget of the cast is " + WaterBc.vertexBudget(VfxShape.WATERFX_CAST));
        // ---- a crowd of casters under the ceilings
        int n = 40;
        double[] dist = new double[n];
        int[] kind = new int[n], tier = new int[n];
        boolean[] done = new boolean[n];
        for (int i = 0; i < n; i++) { dist[i] = i * 2.5; kind[i] = i % WaterBc.KINDS; }
        WaterBc.admit(dist, kind, n, tier, done, 1f);
        int mesh = 0, sprites = 0, lod0 = 0;
        for (int i = 0; i < n; i++) { mesh += WaterBc.meshCap(kind[i], tier[i]); sprites += WaterBc.spriteCap(kind[i], tier[i]); if (tier[i] == WaterBc.LOD0) lod0++; }
        if (mesh > WaterBc.FRAME_MESH_CEILING || sprites > WaterBc.FRAME_SPRITE_CEILING || lod0 > WaterBc.LOD0_MAX_IN_VIEW) bad.add("a crowd of " + n + " casters is admitted over the ceilings: " + mesh + " / " + sprites + " / " + lod0);
        // ---- assets
        int checked = 0;
        for (String path : WaterBc.allTextures()) {
            String file = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
            boolean twin = file.endsWith("_t");
            try {
                SquareBugAudit.Pixels px = SquareBugAudit.particle(file);
                if (px == null) { bad.add("missing texture " + file); continue; }
                if (!file.equals(WaterBc.fileName(WaterBc.SOLID, false))) {
                    String problem = WaterBc.checkTexture(file, twin, px.w(), px.h(), px.argb());
                    if (problem != null) bad.add(file + ": " + problem);
                }
                checked++;
            } catch (java.io.IOException | RuntimeException e) {
                bad.add(file + " cannot be read: " + e);
            }
        }
        int meshes = 0;
        for (int p = 0; p < WaterBc.PIECES.length; p++) {
            for (int lo = 0; lo < 2; lo++) {
                if (!WaterBcMeshes.exists(WaterBc.meshFile(p, lo == 1)) || WaterBcMeshes.get(p, lo == 1).quads == 0) bad.add("missing or empty mesh " + WaterBc.meshFile(p, lo == 1));
                else meshes++;
            }
        }
        // ---- option and tint
        if (WaterBc.DEFAULT_STYLE != WaterBc.Style.BC || WaterBc.parse("garbage") != WaterBc.Style.BC || WaterBc.parse(null) != WaterBc.Style.BC || WaterBc.parse("v1") != WaterBc.Style.V1 || WaterBc.parse(" BC ") != WaterBc.Style.BC)
            bad.add("the vfx.element.water option does not default to BC / parse V1");
        if (!WaterBc.nativePalette(0xFF4FA8FF) || !WaterBc.nativePalette(0xFF3A7BD5) || !WaterBc.nativePalette(0xFF6EC8FF) || WaterBc.nativePalette(0xFFB0121B) || WaterBc.nativePalette(0xFFFF8C1A) || WaterBc.nativePalette(0xFF2EC27E)
                || WaterBc.nativePalette(0xFF1A1A1A) || WaterBc.nativePalette(0xFFFFFFFF))
            bad.add("the tint choice is wrong (water blues keep the native textures, every other colour is tinted)");
        if (!WaterBc.tintable(WaterBc.SPLASH) || !WaterBc.tintable(WaterBc.RING) || WaterBc.tintable(WaterBc.ROAR)) bad.add("only the splash and the ring are tintable");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(checked + " textures, " + meshes + " meshes; worst case one effect " + worstMesh + " mesh + " + worstSprites + " sprite quads; a crowd of " + n + " casters is held to " + mesh + " mesh + " + sprites
                + " quads (ceilings " + WaterBc.FRAME_MESH_CEILING + " + " + WaterBc.FRAME_SPRITE_CEILING + "); the old Valkyrie / WaterSpellLayer shapes untouched; default BC, V1 restores the old layers");
    }
}
