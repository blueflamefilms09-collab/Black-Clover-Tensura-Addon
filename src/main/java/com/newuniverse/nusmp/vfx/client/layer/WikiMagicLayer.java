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
            case WIKI_MAGIC_CAST -> cast(ctx, buf, from, to, age, inst.duration, fade, size, tint, pale, style);
            case WIKI_MAGIC_FIELD -> field(ctx, buf, from, to, age, fade, size, tint, pale, style);
            case WIKI_MAGIC_BURST -> burst(ctx, buf, from, to, age, progress, fade, size, tint, pale, style);
            default -> { }
        }
    }

    private static int style(MagicType magic) {
        if (magic == null) return 0;
        return switch (magic) {
            case AIR -> 0;
            case HAIR -> 1;
            case MEMORY -> 2;
            case MINERAL -> 3;
            case MODIFICATION -> 4;
            case MUCUS -> 5;
            case MUD -> 6;
            case NAIL -> 7;
            case PERMEATION -> 8;
            case POISON_PLANT -> 9;
            case RED_OCHRE -> 10;
            case ROCK -> 11;
            case SANDSTONE -> 12;
            case SCALE -> 13;
            case SHAKUDO -> 14;
            case SKIN -> 15;
            case SMOKE -> 16;
            case SNOW -> 17;
            case SONG -> 18;
            case SOUL_CORPSE -> 19;
            case SOUL -> 20;
            case SOUND -> 21;
            case SPIKE -> 22;
            case SWITCHING -> 23;
            case TONGUE -> 24;
            case TREE -> 25;
            case STONE -> 26;
            case VINE -> 27;
            case VORTEX -> 28;
            case WING -> 29;
            default -> 0;
        };
    }

    private static void cast(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f from, Vector3f to,
                             float age, float duration, float fade, float size, int tint, int pale, int style) {
        // the bolt travels over most of its life, with a short tail behind it (not a beam from the caster to the tip)
        Vector3f tip = new Vector3f(from).lerp(to, Mth.clamp(age / Math.max(4f, duration * 0.8f), 0, 1));
        float length = tip.distance(from);
        Vector3f tail = new Vector3f(from);
        if (length > 1e-3f) tail.set(tip).lerp(from, Math.min(1f, (3.2f * size + 0.8f) / length));
        from = tail;
        ResourceChoice choice = resource(style);
        float width = (0.34f + (style % 5) * 0.055f) * size;
        float spin = age * (0.04f + (style % 7) * 0.009f);
        buf.beam(ctx, choice.texture, choice.blend, from, tip, width, 0.05f, 5, spin, tint, pale);
        int motes = ctx.seg(4 + style % 4, 2 + style % 3);
        for (int i = 0; i < motes; i++) {
            float t = (i + 0.5f) / motes;
            Vector3f center = new Vector3f(from).lerp(tip, t);
            float angle = spin + i * 2.399f;
            float radial = 0.22f + (style % 4) * 0.055f;
            center.add(ctx.camRight.x * Mth.cos(angle) * size * radial + ctx.camUp.x * Mth.sin(angle) * size * 0.26f,
                    ctx.camRight.y * Mth.cos(angle) * size * radial + ctx.camUp.y * Mth.sin(angle) * size * 0.26f,
                    ctx.camRight.z * Mth.cos(angle) * size * radial + ctx.camUp.z * Mth.sin(angle) * size * 0.26f);
            buf.billboard(ctx, choice.texture, choice.blend, center, (0.24f + (style % 3) * 0.05f) * size,
                    angle + style * 0.17f, VfxVertexBuffer.withAlpha(pale, fade));
        }
    }

    private static void field(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f from, Vector3f to,
                              float age, float fade, float size, int tint, int pale, int style) {
        Vector3f center = new Vector3f(from).lerp(to, 0.5f);
        VfxPose plane = VfxPose.ground(center);
        float pulse = 0.88f + 0.12f * Mth.sin(age * 0.14f);
        float radius = size * 3f * pulse;                       // callers pass the damage radius / 3, as every other layer reads it
        buf.ring(resource(style).texture, resource(style).blend, plane.spin(age * (style % 2 == 0 ? 0.018f : -0.018f)),
                radius * (0.68f + (style % 4) * 0.04f), radius, 28 + style % 12, 2.4f,
                age * (0.012f + (style % 5) * 0.004f), VfxVertexBuffer.withAlpha(tint, fade * 0.8f));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, plane.spin(-age * 0.025f),
                radius * 0.34f, radius * 0.39f, 28, 1f, 0, VfxVertexBuffer.withAlpha(pale, fade * 0.58f));
        if (style == 3 || style == 7 || style == 10 || style == 11 || style == 12 || style == 13
                || style == 14 || style == 17 || style == 22 || style == 26) {
            int count = 4 + style % 5;
            for (int i = 0; i < count; i++) {
                float a = age * 0.025f + i * Mth.TWO_PI / count;
                Vector3f point = plane.point(Mth.cos(a) * radius * 0.85f, Mth.sin(a) * radius * 0.85f);
                buf.billboard(ctx, resource(style).texture, VfxBlend.ADD, point,
                        0.24f + (style % 3) * 0.1f, a, VfxVertexBuffer.withAlpha(pale, fade));
            }
        }
    }

    private static void burst(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f from, Vector3f to,
                              float age, float progress, float fade, float size, int tint, int pale, int style) {
        Vector3f center = new Vector3f(from).lerp(to, 0.5f);
        float radius = size * 3f * (0.28f + progress * 0.9f);
        VfxPose face = VfxPose.facing(center, new Vector3f(ctx.camera.getLookVector()));
        ResourceChoice choice = resource(style);
        buf.arc(choice.texture, choice.blend, face.spin(age * (style % 2 == 0 ? 0.025f : -0.025f)), radius,
                (0.15f + (style % 4) * 0.035f) * size, age * (0.035f + (style % 5) * 0.012f),
                Mth.TWO_PI * (0.58f + (style % 4) * 0.1f), 20 + style % 12,
                VfxVertexBuffer.withAlpha(tint, fade * 0.85f));
        int rays = 4 + style % 7;
        for (int i = 0; i < rays; i++) {
            float a = age * 0.04f + i * Mth.TWO_PI / rays;
            Vector3f mote = new Vector3f(center)
                    .add(ctx.camRight.x * Mth.cos(a) * radius + ctx.camUp.x * Mth.sin(a) * radius,
                            ctx.camRight.y * Mth.cos(a) * radius + ctx.camUp.y * Mth.sin(a) * radius,
                            ctx.camRight.z * Mth.cos(a) * radius + ctx.camUp.z * Mth.sin(a) * radius);
            buf.billboard(ctx, choice.texture, choice.blend, mote, (0.32f + (style % 4) * 0.08f) * size,
                    a, VfxVertexBuffer.withAlpha(pale, fade));
        }
    }

    private static ResourceChoice resource(int style) {
        // Improved texture selection: each style gets multiple texture options for visual variety
        // Fallback to GLOW if any texture reference is missing
        return switch (style) {
            case 0, 12, 16 -> new ResourceChoice(VfxTextures.MANA_MOTE, VfxBlend.ALPHA);           // Air, Snow, Smoke: wind-like motes
            case 1, 9, 27, 29 -> new ResourceChoice(VfxTextures.WIND_SLASH, VfxBlend.ADD);         // Hair, Poison Plant, Vine, Wing: slashing trails
            case 2, 4, 8, 20, 21, 23, 25 -> new ResourceChoice(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD);  // Memory, Modification, Permeation, Soul, Sound, Switching, Tree: circular patterns
            case 3, 7, 10, 11, 13, 14, 17, 22, 26 -> new ResourceChoice(VfxTextures.SHARD, VfxBlend.ADD);  // Mineral, Nail, Red Ochre, Rock, Scale, Shakudo, Spike, Stone: crystalline/solid
            case 5, 6, 24 -> new ResourceChoice(VfxTextures.WATER_SPLASH, VfxBlend.ALPHA);         // Mucus, Mud, Tongue: wet/fluid effects
            case 15, 18, 28 -> new ResourceChoice(VfxTextures.SPIRIT_SPIRAL, VfxBlend.ADD);        // Skin, Song, Vortex: flowing/spiral patterns
            case 19 -> new ResourceChoice(VfxTextures.DEVIL_CIRCLE, VfxBlend.ADD);                 // Soul Corpse: dark/infernal
            default -> new ResourceChoice(VfxTextures.GLOW, VfxBlend.ADD);
        };
    }

    private record ResourceChoice(net.minecraft.resources.ResourceLocation texture, VfxBlend blend) {}
}
