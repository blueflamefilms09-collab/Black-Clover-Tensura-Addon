package examples;

import com.newuniverse.nusmp.client.prop.GnomeDraw;
import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.gnome.GnomeLod;
import com.newuniverse.nusmp.gnome.GnomeRules;
import com.newuniverse.nusmp.prop.PropKind;

/**
 * Gnome preview: draws the REAL helper the game's GnomePropPainter uses (client/prop/GnomeDraw), at the place the game puts each form relative to a standing player at the origin (facing +z): the
 * companion beside the player (right, a little behind), the stone giant four and a half blocks ahead of them. Registered under PropKind.BRONZE_1; the scene's "param" = form * 1000 + lod * 100 + clip * 10
 * (form 0 companion, 1 full, 2 both; lod 0..2; clip an index of GnomeDraw.CLIPS: 0 idle, 1 walk, 2 rise, 3 cast, 4 sink). The clip's time is the scene's age in ticks. Scenes: tools/render_preview/scenes.d/gnome.json
 * (python3 -B tools/gnome_scenes.py writes them; they list "realFiles").
 */
public final class GnomeProp {
    private GnomeProp() {}

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param(), form = p / 1000, lod = (p / 100) % 10, clip = (p / 10) % 10;
            for (int f = 0; f < 2; f++) {
                if (form != 2 && form != f) continue;
                pose.pushPose();
                double[] a = f == GnomeLod.FULL ? GnomeRules.full(0, 0, 0, 0) : GnomeRules.companion(0, 0, 0, 0);
                pose.translate(a[0], a[1], a[2]);
                int c = form == 2 ? 0 : clip;                                                   // both together: both idle
                GnomeDraw.gnome(pose, buffers, f, lod, GnomeDraw.CLIPS[Math.min(c, GnomeDraw.CLIPS.length - 1)], age / 20f, age, 1f, light);
                pose.popPose();
            }
        });
    }
}
