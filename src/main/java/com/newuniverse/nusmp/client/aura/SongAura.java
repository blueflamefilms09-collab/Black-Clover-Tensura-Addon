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
 * 1.93.0 Song Magic: the Concert Regalia, the temporary armour after a cast (Aura.SONG), a real set of brass pieces on the posed player model:
 * helm (circlet, treble-clef crest, two trumpet-bell ear horns, plume), chest (breastplate with a lyre of glowing strings), the staff cape (five glowing staff
 * lines), conductor's tails, a lyre halo behind the back, three eighth notes orbiting the torso, trumpet-bell pauldrons, piano-key vambraces, brass gauntlets and
 * greaves with a keyed ankle cuff. Every piece is a bone of song_armour.geo.json and is anchored on the model part it was built for; SongArmourKit draws them
 * with EquipAnimation (the Djinn Full Equip forge-in and shatter-out). Passes: 1. solid pieces, cutout (translucent only while forging or shattering);
 * 2. the glow map of every piece, additive (RenderType.eyes), full bright; 3. extras, additive: rose motes rising off the shoulders and a staff ring on the
 * ground, camera-facing sprites. Style is unused.
 */
public final class SongAura {
    private SongAura() {}

    private static final int ROSE = 0xFFB4E8, PALE = 0xFFE8F8;
    private static final GeoSpec SPEC = GeoSpec.of("song", "armour");
    private static final Piece[] PIECES = {
            SongArmourKit.head("helm", 0, 0f, -16f, 0f),
            SongArmourKit.piece("chest", Anchor.BODY, 2, 0f, 0f, -14f),
            SongArmourKit.piece("back", Anchor.BODY, 4, 0f, 0f, 14f),
            SongArmourKit.piece("tails", Anchor.BODY, 8, 0f, 6f, 12f),
            SongArmourKit.piece("lyre", Anchor.BODY, 6, 0f, -6f, 14f),
            SongArmourKit.piece("note_a", Anchor.BODY, 10, 0f, -14f, 0f),
            SongArmourKit.piece("note_b", Anchor.BODY, 11, 0f, -14f, 0f),
            SongArmourKit.piece("note_c", Anchor.BODY, 12, 0f, -14f, 0f),
            SongArmourKit.piece("pauldron_r", Anchor.ARM_R, 3, -14f, -4f, 0f),
            SongArmourKit.piece("vambrace_r", Anchor.ARM_R, 5, -12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_r", Anchor.ARM_R, 7, -10f, 8f, -6f),
            SongArmourKit.piece("pauldron_l", Anchor.ARM_L, 3, 14f, -4f, 0f),
            SongArmourKit.piece("vambrace_l", Anchor.ARM_L, 5, 12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_l", Anchor.ARM_L, 7, 10f, 8f, -6f),
            SongArmourKit.piece("greave_r", Anchor.LEG_R, 9, -3f, 14f, -6f),
            SongArmourKit.piece("greave_l", Anchor.LEG_L, 10, 3f, 14f, -6f)};
    private static final SongArmourKit.Set SET = new SongArmourKit.Set(SPEC, ROSE, PIECES);
    private static ResourceLocation glow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        glow = AuraRender.tex("particle/glow");
        PlayerAuraClient.register(Aura.SONG, SongAura::paint);
    }

    private static void paint(AuraContext c) {
        SongArmourKit.paint(c, SET);
        float f = c.fade();
        if (f <= 0.002f) return;
        // 3. extras: rose motes rising off the shoulders (camera-facing sprites), additive
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(glow));
        float age = c.age();
        for (int i = 0; i < 9; i++) {
            float ph = (age * 0.016f + i * 0.137f) % 1f;
            float a = i * 2.399f + age * 0.02f, r = 0.5f + 0.12f * Mth.sin(i * 1.7f);
            float fade = Mth.sin(ph * Mth.PI);
            SealDraw.glow(pose, vc, Mth.cos(a) * r, 1.3f - ph * 1.8f, Mth.sin(a) * r * 0.8f, 0.045f + 0.04f * fade, age * 0.07f + i,
                    SealDraw.argb(0.8f * f * fade, i % 3 == 0 ? PALE : ROSE), PropDraw.FULL_BRIGHT);
        }
    }
}
