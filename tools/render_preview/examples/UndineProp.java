package examples;

import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.client.prop.UndineDraw;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.undine.UndinePose;

/**
 * Undine preview: draws the REAL helper the game's UndinePropPainter uses (client/prop/UndineDraw) at the origin, facing +z. Registered under PropKind.BRONZE_1; the scene's "param" =
 * face * 100000 + pose * 10000 + form * 1000 + lod * 100 + seconds*10 (the seconds into the clip in tenths, 0..99). The pose is an UndinePose id (0 = the idle of the form). Scenes: tools/render_preview/scenes.d/undine.json.
 */
public final class UndineProp {
    private UndineProp() {}

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param(), face = p / 100000, id = (p / 10000) % 10, form = (p / 1000) % 10, lod = (p / 100) % 10, tenths = p % 100;
            boolean full = form == 1;
            String clip = UndinePose.of(id).clip(full);
            float secs = tenths > 0 ? tenths / 10f : age / 20f;
            UndineDraw.undine(pose, buffers, full, lod, clip, secs, age, 1f, light, face);
        });
    }
}
