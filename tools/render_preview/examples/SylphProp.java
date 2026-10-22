package examples;

import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.client.prop.SylphDraw;
import com.newuniverse.nusmp.prop.PropKind;

/**
 * Sylph preview: draws the REAL helper the game's SylphPropPainter and ElementalSpiritRenderer use (client/prop/SylphDraw), at the place the game puts it. Registered under PropKind.BRONZE_1; the
 * scene's "param" = placed * 10000 + form * 1000 + lod * 100 + clip (form 0 small, 1 powered; clip = the index into CLIPS, "rest" is no clip: the rest pose). The anchor is placed where the game puts
 * the small form (beside the caster's head) or the powered form (above and behind), when "placed" is 1. Scenes: tools/render_preview/scenes.d/sylph.json (they list "realFiles"), written by
 * tools/sylph_scenes.py, which keeps the same list of clip names.
 */
public final class SylphProp {
    private SylphProp() {}

    private static final String[] CLIPS = {"idle", "laugh", "cast", "hover", "powered_cast", "shout", "rest", "point", "fly", "swing", "wave", "crossed", "pout", "victory", "sleepy", "startled", "hurt", "worry",
            "cry", "cheer", "battle", "fury", "triumph"};

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param() % 10000, placed = e.param() / 10000;
            int form = p / 1000, lod = (p / 100) % 10, clip = p % 100;
            pose.pushPose();
            if (placed == 1) {
                if (form == 0) pose.translate(-0.55, 1.95, 0.1);
                else pose.translate(0, 3.0, -1.6);
            }
            SylphDraw.sylph(pose, buffers, form == 1, lod, CLIPS[Math.min(clip, CLIPS.length - 1)], age / 20f, age, 1f, light);
            pose.popPose();
        });
    }
}
