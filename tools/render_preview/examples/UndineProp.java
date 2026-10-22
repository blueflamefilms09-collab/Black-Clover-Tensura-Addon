package examples;

import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.client.prop.UndineDraw;
import com.newuniverse.nusmp.prop.PropKind;

/**
 * Undine preview: draws the REAL helper the game's UndinePropPainter uses (client/prop/UndineDraw) at the origin, facing +z. Registered under PropKind.BRONZE_1; the scene's "param" =
 * form * 1000 + lod * 100 + clip (form 0 companion, 1 full form; clip 0 idle, 1 full_idle, 2 cast, 3 open). Scenes: tools/render_preview/scenes.d/undine.json.
 */
public final class UndineProp {
    private UndineProp() {}

    private static final String[] CLIPS = {"idle", "full_idle", "cast", "open"};

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param(), form = p / 1000, lod = (p / 100) % 10, clip = p % 10;
            UndineDraw.undine(pose, buffers, form == 1, lod, CLIPS[Math.min(clip, CLIPS.length - 1)], age / 20f, age, 1f, light);
        });
    }
}
