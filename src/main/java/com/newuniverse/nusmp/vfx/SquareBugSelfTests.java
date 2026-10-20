package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

/**
 * Why (docs/vfx_square_bug.md): the CI server boots the real mod, so {@code vfx.square_bug} checks, from the textures that are in the jar, the facts the fix stands on: the ground-ring and glow sprites of the
 * auras have transparent corners but carry a colour under them (so the alpha-weighted additive blend is chosen for them), a texture that is black under its transparent texels is left on vanilla's eyes
 * type, the forced safe path chooses the alpha-weighted blend for everything, the soft circle that stands in for a missing sprite is transparent at the corners, and the Time Magic textures are never
 * swapped. Touches no client class, so it runs on the dedicated server.
 */
public final class SquareBugSelfTests {
    private SquareBugSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("vfx.square_bug", SquareBugSelfTests::run);
    }

    private static SelfTests.Result run(net.minecraft.server.MinecraftServer server) {
        try {
            for (String name : SquareBugAudit.SPRITES) {
                SquareBugAudit.Pixels px = SquareBugAudit.particle(name);
                if (px == null) return SelfTests.Result.fail("texture particle/" + name + ".png is not in the jar");
                if (!TextureAlpha.cornersTransparent(px.w(), px.h(), px.argb(), 16)) return SelfTests.Result.fail(name + " has an opaque corner: it would draw as a square");
                TextureAlpha.Kind k = TextureAlpha.classify(px.w(), px.h(), px.argb());
                if (k != TextureAlpha.Kind.COLOUR_UNDER_ALPHA && k != TextureAlpha.Kind.BLACK_UNDER_ALPHA) return SelfTests.Result.fail(name + " is " + k + ", a sprite must have transparent texels");
                AdditivePolicy.Blend b = AdditivePolicy.choose(k, false);
                if ((k == TextureAlpha.Kind.COLOUR_UNDER_ALPHA) != (b == AdditivePolicy.Blend.ALPHA_ADD)) return SelfTests.Result.fail(name + ": the blend choice does not follow the audit");
                if (AdditivePolicy.choose(k, true) != AdditivePolicy.Blend.ALPHA_ADD) return SelfTests.Result.fail("the forced safe path must choose the alpha-weighted blend");
            }
            SquareBugAudit.Pixels air = SquareBugAudit.particle("air_ring");
            if (TextureAlpha.classify(air.w(), air.h(), air.argb()) != TextureAlpha.Kind.COLOUR_UNDER_ALPHA) return SelfTests.Result.fail("air_ring should carry a colour under its alpha (the square-prone kind)");
            SquareBugAudit.Pixels seal = SquareBugAudit.particle("spike_sigil");
            if (seal != null && TextureAlpha.classify(seal.w(), seal.h(), seal.argb()) != TextureAlpha.Kind.BLACK_UNDER_ALPHA) return SelfTests.Result.fail("spike_sigil should be black under its alpha (left on vanilla's eyes type)");

            if (AdditivePolicy.choose(null, false) != AdditivePolicy.Blend.ALPHA_ADD) return SelfTests.Result.fail("an unreadable texture must take the alpha-weighted blend");
            if (!AdditivePolicy.replaceWithSoftCircle(null, false, "textures/particle/gone.png", false)) return SelfTests.Result.fail("a missing sprite must become the soft circle");
            if (AdditivePolicy.replaceWithSoftCircle(TextureAlpha.Kind.OPAQUE_BRIGHT_EDGE, true, "textures/particle/time_numeral_band.png", true)) return SelfTests.Result.fail("a Time Magic texture must never be swapped");
            if (AdditivePolicy.replaceWithSoftCircle(TextureAlpha.Kind.BLACK_UNDER_ALPHA, true, "textures/particle/x_ring.png", true)) return SelfTests.Result.fail("a sprite with alpha must never be swapped");

            int[] soft = SoftCircle.argb(SoftCircle.SIZE);
            if (!TextureAlpha.cornersTransparent(SoftCircle.SIZE, SoftCircle.SIZE, soft, 0) || (soft[SoftCircle.SIZE / 2 * SoftCircle.SIZE + SoftCircle.SIZE / 2] >>> 24) < 240)
                return SelfTests.Result.fail("the soft circle is not a disc with transparent corners");
            return SelfTests.Result.ok(SquareBugAudit.SPRITES.length + " ring and glow sprites: transparent corners, colour under alpha gets the alpha-weighted additive blend, safe path, soft circle, Time Magic exempt");
        } catch (Exception e) {
            return SelfTests.Result.fail("square-bug check failed: " + e);
        }
    }
}
