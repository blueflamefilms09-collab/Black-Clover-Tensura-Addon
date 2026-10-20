package com.newuniverse.nusmp.client.playermodel;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the one thing a character has to tell the kit's renderer ({@link PlayerModelRenderer}). Everything else (the vanilla player model, its walk, swing, bow, crossbow, sneak, riding and
 * death animations, the armour, held items, head items and elytra, the hair, the extras, the glow pass) the kit does by itself. A body says WHICH {@link PlayerLook} the entity wears now (its
 * state may change the answer: a second form, a devil's colour, a decay stage), how it is doing this frame ({@link Dynamic}: fade, tint, glow level, size, a hover, what it is casting) and what
 * its arms and head do beyond the vanilla pose ({@link PoseState}, written with {@link PosePatterns}). All methods but {@link #look} have a neutral default.
 *
 * <p>Keep the methods cheap: they are called every frame for every entity. {@link #look} should be a table lookup ({@link PlayerAssets#look}), not a parse.
 */
public interface PlayerBody<T extends LivingEntity> {
    /** The look this entity wears now. */
    PlayerLook look(T e);

    /**
     * A texture composed for this entity (an Avatar's skin from its creator's choices), or null to use the skin the look names. The returned location must be a loaded texture.
     */
    default ResourceLocation skin(T e) { return null; }

    /** Fills what changes frame to frame; the caller has reset {@code d} to the neutral values. */
    default void dynamic(T e, float partial, Dynamic d) { }

    /** Writes the repo's own pose over the vanilla animation; the caller has cleared {@code p}. */
    default void pose(T e, float partial, PoseState p) { }

    /** False: nothing but the name tag is drawn (a character resting on someone's head, an invisible phase). */
    default boolean visible(T e) { return true; }

    /** How far past the hitbox the look reaches (wings, a halo, a long weapon), in blocks, for the culling box. */
    default float cullGrow(T e, PlayerLook look) { return look.extras().isEmpty() ? 0.4f : 1.2f * Math.max(1f, look.scale()); }

    /** Whether the name tag shows; {@code vanilla} is what vanilla would do, {@code distSqr} the squared distance to the camera. */
    default boolean showName(T e, boolean vanilla, double distSqr) { return vanilla; }

    /** The text of the name tag ({@code vanilla} is what vanilla would draw). */
    default Component name(T e, Component vanilla) { return vanilla; }

    /** Called once when the renderer is built: add layers of your own (an Avatar's horns, eyes) to {@code body}. */
    default void layers(PlayerModelRenderer.Body<T> body, EntityRendererProvider.Context ctx) { }

    /** Called inside the body's pose after everything else is drawn (an aura, a ring on the ground); {@code d} is this frame's state. */
    default void decorate(T e, float partial, Decoration deco) { }

    /** What a body may draw on top of the character. {@code pose} is at the entity's feet (blocks, y up) turned to its body, not scaled. */
    interface Decoration {
        com.mojang.blaze3d.vertex.PoseStack pose();

        net.minecraft.client.renderer.MultiBufferSource buffers();

        int light();
    }

    /**
     * Per-frame state a body may set. Everything starts neutral: opaque, white, full glow, scale 1, on the ground, casting nothing.
     */
    final class Dynamic {
        /** 0..1 opacity of the whole character (a dying body fades, a clone fades out). */
        public float alpha = 1f;
        /** 0xRRGGBB multiplied into the body, hair and extras (a paint colour, a gold flash); 0xFFFFFF = none. */
        public int tint = 0xFFFFFF;
        /** 0..1 how bright the glow pass is (the look's glow map and the extras' glow maps). */
        public float glowLevel = 1f;
        /** 0xRRGGBB the glow pass is tinted with, or -1 to use the look's colour. */
        public int glowRgb = -1;
        /** A factor on the look's scale (a rise, a hop). */
        public float size = 1f;
        /** An extra non-uniform scale (x, y, z), or null. */
        public float[] squash;
        /** Blocks the character floats above the ground. */
        public float lift;
        /** 0 nothing, 1 a cast, 2 a big cast (picks the extras' cast clips). */
        public int cast;
        /** 0..1 progress of the cast, of the transformation, or of a one-shot clip. */
        public float progress;
        public boolean transforming;
        /** True when the entity's own death animation must not be used (it lies down and fades with {@link #alpha} instead of falling over). */
        public boolean keepUpright;
        /** The shadow's radius in blocks; 0 = the kit's default (scaled with the character). */
        public float shadow;
        /** No shadow at all (a body still sunk in the floor). */
        public boolean noShadow;
        /** 0..1 strength of a flash of {@link #flashRgb} over the whole body (the power-up's burst), drawn additive. */
        public float flash;
        public int flashRgb = 0xFFFFFF;

        public void reset() {
            alpha = 1f;
            tint = 0xFFFFFF;
            glowLevel = 1f;
            glowRgb = -1;
            size = 1f;
            squash = null;
            lift = 0f;
            cast = 0;
            progress = 0f;
            transforming = false;
            keepUpright = false;
            shadow = 0f;
            noShadow = false;
            flash = 0f;
            flashRgb = 0xFFFFFF;
        }
    }
}
