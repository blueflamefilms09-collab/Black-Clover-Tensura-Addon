package examples;

import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.client.prop.SpiritDarkDraw;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.spiritdark.SpiritDarkRules;

/**
 * Spirit of Darkness preview: draws the REAL helper the game's SpiritDarkPropPainter uses (client/prop/SpiritDarkDraw), at the place the game puts each form relative to a standing player at the
 * origin (facing +z): the companion above the left shoulder, the powered form behind and a little to the right. Registered under PropKind.BRONZE_1; the scene's "param" = form * 10000 + lod * 1000
 * + clip * 10 + face (form 0 companion, 1 powered; clip: index in CLIPS; face: DarkMoodRules.Face ordinal) and "ages" are game ticks. Scenes: tools/render_preview/scenes.d/spirit_dark.json
 * (written by tools/spirit_dark_scenes.py).
 */
public final class SpiritDarkProp {
    private SpiritDarkProp() {}

    private static final String[] CLIPS = {"idle", "flap", "bow", "smug", "cast", "feast", "recoil", "cackle", "battle", "vanish", "appear", "farewell"};

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param(), form = p / 10000, lod = (p / 1000) % 10, clip = (p / 10) % 100, face = p % 10;
            boolean powered = form == 1;
            double[] a = powered ? SpiritDarkRules.powered(0, 0, 0, 0) : SpiritDarkRules.companion(0, 0, 0, 0);
            pose.pushPose();
            pose.translate(a[0], a[1], a[2]);
            String name = CLIPS[Math.min(clip, CLIPS.length - 1)];
            boolean loops = name.equals("idle") || name.equals("flap") || name.equals("smug") || name.equals("cackle") || name.equals("battle");
            float secs = loops ? age / 20f : Math.min(age, 60f) / 20f;
            SpiritDarkDraw.spirit(pose, buffers, powered, lod, name, secs, age, 1f, light, face);
            pose.popPose();
        });
    }
}
