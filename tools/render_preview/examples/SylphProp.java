package examples;

import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.client.prop.SylphDraw;
import com.newuniverse.nusmp.prop.PropKind;

/**
 * Sylph preview: draws the REAL helper the game's SylphPropPainter and ElementalSpiritRenderer use (client/prop/SylphDraw), at the place the game puts it. Registered under PropKind.BRONZE_1; the
 * scene's "param" = form * 100 + lod * 10 + clip (form 0 small, 1 powered; clip index into CLIPS). The anchor is placed where the game puts the small form (beside the caster's head) or the
 * powered form (above and behind), when "place" is on (param + 1000). Scenes: tools/render_preview/scenes.d/sylph.json (they list "realFiles").
 */
public final class SylphProp {
    private SylphProp() {}

    private static final String[] CLIPS = {"idle", "laugh", "cast", "hover", "powered_cast", "shout", "rest", "point", "fly"};

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param() % 1000, placed = e.param() / 1000;
            int form = p / 100, lod = (p / 10) % 10, clip = p % 10;
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
