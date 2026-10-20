package examples;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.GreatSpiritModels;
import com.newuniverse.nusmp.client.SpiritLooks;
import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.prop.PropKind;

/**
 * 1.99.0 Great Spirits preview: draws the REAL helpers the game's ElementalSpiritRenderer uses (client/GreatSpiritModels), so a scene shows what the game draws, at the size the game
 * draws it. Registered under PropKind.BRONZE_1; the scene's "param" picks the spirit and the clip: param = spirit * 16 + clip, spirit 0 Sylph, 1 Undine, 2 Salamander; clip 0 idle,
 * 1 move, 2 cast, 3 hit, 4 annoyed (Sylph only). Scenes: tools/render_preview/scenes.d/great_spirits.json (they list "realFiles").
 */
public final class GreatSpiritsProp {
    private GreatSpiritsProp() {}

    private static final String[] CLIPS = {"idle", "move", "cast", "hit", "annoyed"};

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int spirit = e.param() / 16, clip = e.param() % 16;
            String anim = CLIPS[Math.min(clip, CLIPS.length - 1)];
            float secs = age / 20f;
            if (clip == 4) secs = Math.min(secs, SpiritLooks.ANNOYED_LENGTH);
            switch (spirit) {
                case 0 -> GreatSpiritModels.sylph(pose, buffers, anim, secs, age, light);
                case 1 -> GreatSpiritModels.undine(pose, buffers, anim, secs, age, light);
                default -> GreatSpiritModels.salamander(pose, buffers, anim, secs, age, light);
            }
        });
    }
}
