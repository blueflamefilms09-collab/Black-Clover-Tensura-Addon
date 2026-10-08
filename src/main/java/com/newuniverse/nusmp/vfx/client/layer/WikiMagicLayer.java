package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.WikiBooks;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/** Palette-driven layered effects shared by the additional Black Clover wiki attributes. */
public final class WikiMagicLayer extends AbstractVfxLayer {
    private static final Set<VfxShape> SHAPES = EnumSet.of(
            VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_FIELD, VfxShape.WIKI_MAGIC_BURST);

    @Override public Set<VfxShape> shapes() { return SHAPES; }
    @Override public int defaultDuration(VfxShape shape) { return shape == VfxShape.WIKI_MAGIC_FIELD ? 42 : 22; }
    @Override public int defaultColor(VfxShape shape) { return 0xFF8AD8E8; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float progress = Mth.clamp(age / inst.duration, 0, 1);
        float fade = 1f - progress;
        float size = Mth.clamp(Math.abs(inst.power), 0.45f, 5f);
        Vector3f from = ctx.rel(inst.from(ctx));
        Vector3f to = ctx.rel(inst.to(ctx));
        int tint = VfxVertexBuffer.withAlpha(inst.color, fade);
        int pale = VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(inst.color, 0.68f), fade);
        MagicType magic = WikiBooks.magicForColor(inst.color);
        int style = style(magic);

        switch (inst.shape) {
            case WIKI_MAGIC_CAST -> cast(ctx, buf, from, to, age, fade, size, tint, pale, style);
            case WIKI_MAGIC_FIELD -> field(ctx, buf, from, to, age, fade, size, tint, pale, style);
            case WIKI_MAGIC_BURST -> burst(ctx, buf, from, to, age, progress, fade, size, tint, pale, style);
            default -> { }
        }
    }

    private static int style(MagicType magic) {
        if (magic == null) return 0;
        return switch (magic) {
            case AIR, SMOKE, VORTEX, WING -> 0;
            case HAIR, POISON_PLANT, TONGUE, TREE, VINE -> 1;
            case MINERAL, NAIL, RED_OCHRE, ROCK, SANDSTONE, SCALE, SHAKUDO, SKIN, SPIKE, STONE -> 2;
            case MUCUS, MUD -> 3;
            case MEMORY, PERMEATION, SOUL_CORPSE, SOUL, SWITCHING -> 4;
            case SNOW -> 5;
            case SONG, SOUND -> 6;
            case MODIFICATION -> 7;
            default -> 0;
        };
    }

    private static void cast(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f from, Vector3f to,
                             float age, float fade, float size, int tint, int pale, int style) {
        Vector3f tip = new Vector3f(from).lerp(to, Mth.clamp(age / 22f, 0, 1));
        ResourceChoice choice = resource(style);
        buf.beam(ctx, choice.texture, choice.blend, from, tip, 0.48f * size, 0.05f, 5, age * 0.07f, tint, pale);
        int motes = ctx.seg(7, 3);
        for (int i = 0; i < motes; i++) {
            float t = (i + 0.5f) / motes;
            Vector3f center = new Vector3f(from).lerp(tip, t);
            float angle = age * 0.14f + i * 2.399f;
            center.add(ctx.camRight.x * Mth.cos(angle) * size * 0.34f + ctx.camUp.x * Mth.sin(angle) * size * 0.26f,
                    ctx.camRight.y * Mth.cos(angle) * size * 0.34f + ctx.camUp.y * Mth.sin(angle) * size * 0.26f,
                    ctx.camRight.z * Mth.cos(angle) * size * 0.34f + ctx.camUp.z * Mth.sin(angle) * size * 0.26f);
            buf.billboard(ctx, style == 2 || style == 5 ? VfxTextures.SHARD : VfxTextures.GLOW,
                    VfxBlend.ADD, center, 0.34f * size, angle, VfxVertexBuffer.withAlpha(pale, fade));
        }
    }

    private static void field(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f from, Vector3f to,
                              float age, float fade, float size, int tint, int pale, int style) {
        Vector3f center = new Vector3f(from).lerp(to, 0.5f);
        VfxPose plane = VfxPose.ground(center);
        float pulse = 0.88f + 0.12f * Mth.sin(age * 0.14f);
        float radius = size * pulse;
        buf.ring(VfxTextures.RUNE_RING, VfxBlend.ALPHA, plane.spin(age * (style % 2 == 0 ? 0.018f : -0.018f)),
                radius * 0.78f, radius, 36, 2.4f, age * 0.025f, VfxVertexBuffer.withAlpha(tint, fade * 0.8f));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, plane.spin(-age * 0.025f),
                radius * 0.34f, radius * 0.39f, 28, 1f, 0, VfxVertexBuffer.withAlpha(pale, fade * 0.58f));
        if (style == 2 || style == 5) {
            for (int i = 0; i < 6; i++) {
                float a = age * 0.025f + i * Mth.TWO_PI / 6f;
                Vector3f point = plane.point(Mth.cos(a) * radius * 0.85f, Mth.sin(a) * radius * 0.85f);
                buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, point, 0.35f, a, VfxVertexBuffer.withAlpha(pale, fade));
            }
        }
    }

    private static void burst(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f from, Vector3f to,
                              float age, float progress, float fade, float size, int tint, int pale, int style) {
        Vector3f center = new Vector3f(from).lerp(to, 0.5f);
        float radius = size * (0.28f + progress * 0.9f);
        VfxPose face = VfxPose.facing(center, new Vector3f(ctx.camera.getLookVector()));
        buf.arc(VfxTextures.GLOW, VfxBlend.ADD, face.spin(age * 0.025f), radius, 0.22f * size,
                age * 0.06f, Mth.TWO_PI * 0.78f, 24, VfxVertexBuffer.withAlpha(tint, fade * 0.85f));
        int rays = 5 + style % 4;
        for (int i = 0; i < rays; i++) {
            float a = age * 0.04f + i * Mth.TWO_PI / rays;
            Vector3f mote = new Vector3f(center)
                    .add(ctx.camRight.x * Mth.cos(a) * radius + ctx.camUp.x * Mth.sin(a) * radius,
                            ctx.camRight.y * Mth.cos(a) * radius + ctx.camUp.y * Mth.sin(a) * radius,
                            ctx.camRight.z * Mth.cos(a) * radius + ctx.camUp.z * Mth.sin(a) * radius);
            buf.billboard(ctx, style == 2 || style == 5 ? VfxTextures.SHARD : VfxTextures.SPARK,
                    VfxBlend.ADD, mote, 0.5f * size, a, VfxVertexBuffer.withAlpha(pale, fade));
        }
    }

    private static ResourceChoice resource(int style) {
        return switch (style) {
            case 1 -> new ResourceChoice(VfxTextures.WIND_SLASH, VfxBlend.ADD);
            case 2, 5 -> new ResourceChoice(VfxTextures.SHARD, VfxBlend.ADD);
            case 3 -> new ResourceChoice(VfxTextures.WATER_SPLASH, VfxBlend.ALPHA);
            case 4, 7 -> new ResourceChoice(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD);
            case 6 -> new ResourceChoice(VfxTextures.SPIRIT_SPIRAL, VfxBlend.ADD);
            default -> new ResourceChoice(VfxTextures.GLOW, VfxBlend.ADD);
        };
    }

    private record ResourceChoice(net.minecraft.resources.ResourceLocation texture, VfxBlend blend) {}
}
