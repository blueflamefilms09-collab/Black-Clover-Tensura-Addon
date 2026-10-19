package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.SongArmourKit.Anchor;
import com.newuniverse.nusmp.client.aura.SongArmourKit.Piece;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Memory Magic: the Reel Vestments, the temporary armour after a cast (Aura.MEMORY): lavender white over film black, paper, gold trim and mirror glass. A film-strip
 * crown with a camera lens for a third eye and two photo-frame fins; three glass shards turning over the head; a cuirass of pages bound in gold with a lens-locket; a film reel
 * standing behind the back and turning, with a strip of film hanging from it; three hand-mirror shards orbiting the waist; tilted photo-frame pauldrons with glass spikes,
 * film-reel vambraces, frame gauntlets with glass fingertips and greaves of stacked pages with a film strip at the ankle. Every piece is a bone of memory_armour.geo.json
 * anchored on the model part it was built for; SongArmourKit draws them with EquipAnimation (the Djinn Full Equip forge-in and shatter-out). Passes: 1. solid pieces (film,
 * paper, gold), cutout (translucent only while forging or shattering); 2. ghost pieces (the glass shards and the mirrors), always translucent; 3. every glow map, additive
 * (RenderType.eyes), full bright; 4. extras, additive: a ring of film frames turning on the ground (particle/memory_ripple) and sparks of remembered light rising
 * (particle/memory_spark), camera-facing. Style is unused.
 */
public final class MemoryAura {
    private MemoryAura() {}

    private static final int LAV = 0xBCA8FF, PALE = 0xF0EAFF;
    private static final GeoSpec SPEC = GeoSpec.of("memory", "armour");
    private static final Piece[] PIECES = {
            SongArmourKit.head("crown", 0, 0f, -16f, 0f),
            SongArmourKit.ghost("shards", Anchor.HEAD, 12, 0f, -14f, 0f),
            SongArmourKit.piece("book", Anchor.BODY, 2, 0f, 0f, -14f),
            SongArmourKit.piece("reel", Anchor.BODY, 5, 0f, -6f, 14f),
            SongArmourKit.piece("film", Anchor.BODY, 8, 0f, 6f, 12f),
            SongArmourKit.ghost("mirrors", Anchor.BODY, 11, 0f, -10f, 0f),
            SongArmourKit.piece("pauldron_r", Anchor.ARM_R, 3, -14f, -4f, 0f),
            SongArmourKit.piece("vambrace_r", Anchor.ARM_R, 5, -12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_r", Anchor.ARM_R, 7, -10f, 8f, -6f),
            SongArmourKit.piece("pauldron_l", Anchor.ARM_L, 3, 14f, -4f, 0f),
            SongArmourKit.piece("vambrace_l", Anchor.ARM_L, 5, 12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_l", Anchor.ARM_L, 7, 10f, 8f, -6f),
            SongArmourKit.piece("greave_r", Anchor.LEG_R, 9, -3f, 14f, -6f),
            SongArmourKit.piece("greave_l", Anchor.LEG_L, 10, 3f, 14f, -6f)};
    private static final SongArmourKit.Set SET = new SongArmourKit.Set(SPEC, LAV, PIECES);
    private static ResourceLocation ripple, spark, glow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        ripple = AuraRender.tex("particle/memory_ripple");
        spark = AuraRender.tex("particle/memory_spark");
        glow = AuraRender.tex("particle/glow");
        PlayerAuraClient.register(Aura.MEMORY, MemoryAura::paint);
    }

    private static void paint(AuraContext c) {
        SongArmourKit.paint(c, SET);
        float f = c.fade();
        if (f <= 0.002f) return;
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float age = c.age();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(ripple));
        pose.pushPose();                                                                      // the ring of film frames on the ground
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, vc, 0.95f * (0.6f + 0.4f * f), age * 0.9f, SealDraw.argb(0.8f * f, LAV), PropDraw.FULL_BRIGHT);
        SealDraw.flat(pose, vc, 0.6f * (0.6f + 0.4f * f), -age * 1.4f, SealDraw.argb(0.5f * f, PALE), PropDraw.FULL_BRIGHT);
        pose.popPose();
        VertexConsumer sp = c.buffers().getBuffer(AuraRender.additive(spark));
        for (int i = 0; i < 9; i++) {                                                         // remembered light rising and fading
            float ph = (age * 0.014f + i * 0.123f) % 1f, a = i * 2.399f + age * 0.02f, r = 0.5f + 0.14f * Mth.sin(i * 1.9f);
            float fade = Mth.sin(ph * Mth.PI);
            SealDraw.glow(pose, sp, Mth.cos(a) * r, 1.3f - ph * 1.9f, Mth.sin(a) * r * 0.8f, 0.05f + 0.045f * fade, age * 0.06f + i,
                    SealDraw.argb(0.85f * f * fade, i % 3 == 0 ? PALE : LAV), PropDraw.FULL_BRIGHT);
        }
        VertexConsumer g = c.buffers().getBuffer(AuraRender.additive(glow));
        SealDraw.glow(pose, g, 0f, 0.28f, -0.13f, 0.16f + 0.03f * Mth.sin(age * 0.22f), 0f, SealDraw.argb(0.5f * f, LAV), PropDraw.FULL_BRIGHT);       // the lens-locket
    }
}
