package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Self-test {@code visual.hero_solid} of the CI server smoke test (docs/heroweapon/flat_parts.md): inside the booted mod, the pure half of the solid arrow / spear tip is right (the aim maths round trips, the
 * per-frame budget stops at its cap and starts again on a new frame, the flight lands at 70 %), the new shapes are appended after the old ones, and the rebuilt meshes and their untouched originals are both on the
 * classpath. Plain data only: runs on a dedicated server, touches no client class and no player state.
 */
public final class HeroSolidSelfTests {
    private HeroSolidSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.hero_solid", server -> run());
    }

    private static SelfTests.Result run() throws Exception {
        double[][] dirs = {{1, 0, 0}, {0, 0, 1}, {-1, 2, 3}, {0.3, -1, 0.2}, {0, 1, 0}};
        for (double[] d : dirs) {
            double len = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            float[] a = HeroSolid.aim(d[0], d[1], d[2]);
            double[] back = HeroSolid.axis(a[0], a[1]);
            for (int i = 0; i < 3; i++) if (Math.abs(back[i] - d[i] / len) > 1.0E-4) return SelfTests.Result.fail("aim does not round trip for " + d[0] + "," + d[1] + "," + d[2]);
        }
        Object frame = new Object();
        int got = 0;
        for (int i = 0; i < HeroSolid.MAX_PER_FRAME + 10; i++) if (HeroSolid.takeSlot(frame)) got++;
        if (got != HeroSolid.MAX_PER_FRAME) return SelfTests.Result.fail("the frame budget gave " + got + " slots");
        if (!HeroSolid.takeSlot(new Object())) return SelfTests.Result.fail("a new frame did not start the budget again");
        if (HeroSolid.flight(0, 8) != 0f || HeroSolid.flight(100, 8) != 1f || Math.abs(HeroSolid.flight(5.6f, 8) - 1f) > 1.0E-4f) return SelfTests.Result.fail("the flight curve is wrong");
        if (VfxShape.HERO_ARROW.ordinal() != VfxShape.GIANT_AVATAR.ordinal() + 1 || VfxShape.HERO_SPEAR.ordinal() != VfxShape.HERO_ARROW.ordinal() + 1) return SelfTests.Result.fail("the hero shapes are not appended");
        for (String m : new String[]{"legendary_spear", "legendary_bow_pull3", "vassal_harpoon"}) {
            String solid = text("/assets/anime_multiverse/weapon_meshes/" + m + ".txt");
            String old = text("/assets/anime_multiverse/weapon_meshes/flat_parts_old/" + m + ".txt");
            if (solid == null || old == null) return SelfTests.Result.fail(m + " or its old flat mesh is missing");
            if (solid.equals(old)) return SelfTests.Result.fail(m + " is still the flat mesh");
        }
        return SelfTests.Result.ok("aim, budget (" + HeroSolid.MAX_PER_FRAME + "/frame), flight, shapes and meshes ok");
    }

    private static String text(String path) throws Exception {
        try (InputStream in = HeroSolidSelfTests.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
