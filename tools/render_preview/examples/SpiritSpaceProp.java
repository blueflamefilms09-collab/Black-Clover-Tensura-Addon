package examples;

import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.client.prop.SpiritSpaceDraw;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules;
import com.newuniverse.nusmp.spiritspace.SpiritSpaceRules;

/**
 * Spirit of Space preview: draws the REAL helper the game's SpiritSpacePropPainter uses (client/prop/SpiritSpaceDraw), at the place the game puts each thing relative to a standing player at the
 * origin (facing +z): the orb circling at head height, the wisp above and behind, a portal ring in front. Registered under PropKind.BRONZE_1; the scene's "param" = form * 100000 + lod * 10000 +
 * mood * 100 + expression * 10 + place: form 0 orb, 1 wisp, 2 portal; lod 0..2; mood = the ordinal of SpaceMoodRules.Mood (the scene's age is the number of ticks since the pose began); expression
 * 0..3 (calm, excited, angry, sleepy); place 0 = where the game puts it next to a player, 1 = centred on the origin (a close-up).
 * Scenes: tools/render_preview/scenes.d/spirit_space.json (they list "realFiles").
 */
public final class SpiritSpaceProp {
    private SpiritSpaceProp() {}

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param(), form = p / 100000, lod = (p / 10000) % 10, mood = (p / 100) % 100, expr = (p / 10) % 10, place = p % 10;
            SpaceMoodRules.Mood m = SpaceMoodRules.Mood.of(mood);
            SpaceMoodRules.Expression x = SpaceMoodRules.Expression.of(expr);
            pose.pushPose();
            double[] a = new double[3];
            switch (form) {
                case 0 -> {
                    if (place == 0) { SpiritSpaceRules.orbitTo(a, 0, 0, 0, 0, age); pose.translate(a[0], a[1], a[2]); }
                    SpiritSpaceDraw.orb(pose, buffers, lod, age, 1f, m, x, age, 0.06f, 0.04f);
                }
                case 1 -> {
                    if (place == 0) { SpiritSpaceRules.wispTo(a, 0, 0, 0, 0); pose.translate(a[0], a[1], a[2]); }
                    SpiritSpaceDraw.wisp(pose, buffers, lod, age, 1f, m, x, age);
                }
                default -> {
                    pose.translate(0, 0, place == 0 ? 2.2 : 0);
                    SpiritSpaceDraw.portal(pose, buffers, age, 1f, 1f);
                }
            }
            pose.popPose();
        });
    }
}
