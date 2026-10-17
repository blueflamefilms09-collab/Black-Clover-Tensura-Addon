package com.newuniverse.nusmp.arc;

/**
 * Why: how a character is drawn, as plain numbers, so the boss class (server) can state it next to its stats and the one generic renderer (client) can
 * read it without a renderer class per character. The model, animations and textures are found by {@link #model()}: the art kit writes
 * {@code assets/nusmp/geo/entity/arc_<model>.geo.json}, {@code animations/entity/arc_<model>.animation.json} and {@code textures/entity/arc_<model>*.png}.
 *
 * @param model     the model key ("heath_grice"; files are prefixed arc_)
 * @param height    the model's height in blocks (its true size; the renderer scales the hitbox to it)
 * @param walkCycle blocks of ground one walk cycle covers (so the feet do not slide)
 * @param runCycle  blocks of ground one run cycle covers
 * @param shadow    shadow radius
 * @param growXz    extra horizontal culling box (wings, capes, weapons reaching past the hitbox)
 * @param growUp    extra vertical culling box
 * @param glow      whether the model has a glow texture layer ({@code arc_<model>_glow.png})
 */
public record ArcLook(String model, float height, float walkCycle, float runCycle, float shadow, float growXz, float growUp, boolean glow) {
    /** A human-sized character: 1.9 blocks tall, normal stride. */
    public static ArcLook humanoid(String model) { return new ArcLook(model, 1.9f, 1.6f, 2.6f, 0.5f, 1.2f, 0.6f, true); }

    /** The same look at another height (the stride grows with it). */
    public ArcLook sized(float blocks) {
        float k = blocks / height;
        return new ArcLook(model, blocks, walkCycle * k, runCycle * k, shadow * k, growXz * k, growUp * k, glow);
    }
}
