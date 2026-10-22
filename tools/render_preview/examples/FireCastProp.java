package examples;

import com.newuniverse.nusmp.client.prop.FireCastDraw;
import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.firecast.FireCastLod;
import com.newuniverse.nusmp.firecast.FireCastRules;
import com.newuniverse.nusmp.firecast.SalamanderMoodRules;
import com.newuniverse.nusmp.prop.PropKind;

/**
 * Fire casting preview: draws the REAL helper the game's FireCastPropPainter uses (client/prop/FireCastDraw), at the place the game puts each thing relative to a standing player at the origin
 * (facing +z): the small Salamander on the right shoulder, the big dragon above and behind, the ball in front of the eye, the spiral in front of the eyes. Registered under PropKind.BRONZE_1; the
 * scene's "param" = variant * 1000 + lod * 100 + level * 10 + clip (variant 0 ball, 1 spiral, 2 small Salamander, 3 dragon; clip 0 perch/fly, 1 idle, 2 breathe); a param of 100000 and up is a mood scene: 100000 + face * 10000 + variant * 1000 + lod * 100 + pose id (the pose id of SalamanderMoodRules, so its clip name). Scenes:
 * tools/render_preview/scenes.d/firecast_props.json (they list "realFiles").
 */
public final class FireCastProp {
    private FireCastProp() {}

    private static final String[] SMALL = {"perch", "idle", "breathe"}, BIG = {"fly", "idle", "breathe"};

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param(), variant = p / 1000, lod = (p / 100) % 10, level = (p / 10) % 10, clip = p % 10, face = 0, moodPose = -1;
            if (p >= 100000) {                                                                      // the mood scenes: 1 F V L PP (face, variant, lod, pose id of SalamanderMoodRules)
                int q = p - 100000;
                face = q / 10000;
                variant = (q / 1000) % 10;
                lod = (q / 100) % 10;
                moodPose = q % 100;
            }
            pose.pushPose();
            switch (variant) {
                case 0 -> {
                    double[] b = FireCastRules.ballPosition(0, 1.62, 0, 0, 0, 1);
                    pose.translate(b[0], b[1], b[2]);
                    FireCastDraw.ball(pose, buffers, age, 1f);
                }
                case 1 -> {
                    pose.translate(0, 1.62 - 0.1, 1.5);
                    FireCastDraw.spiral(pose, buffers, level, age, 1f, FireCastLod.spiralPoints(level, 0), 0f, 0f, 1f);
                }
                case 2 -> {
                    double[] a = FireCastRules.shoulder(0, 0, 0, 0);
                    pose.translate(a[0], a[1], a[2]);
                    float secs = clip == 2 ? Math.min(age, 32f) / 20f : age / 20f;
                    if (moodPose >= 0) {
                        String name = SalamanderMoodRules.clipName(false, moodPose);
                        FireCastDraw.salamander(pose, buffers, false, lod, name, age / 20f, age, 1f, light, face);
                    } else {
                        FireCastDraw.salamander(pose, buffers, false, lod, SMALL[Math.min(clip, 2)], secs, age, 1f, light);
                    }
                }
                default -> {
                    double[] a = FireCastRules.dragonPosition(0, 0, 0, 0);
                    pose.translate(a[0], a[1], a[2]);
                    float secs = clip == 2 ? Math.min(age, 40f) / 20f : age / 20f;
                    if (moodPose >= 0) {
                        String name = SalamanderMoodRules.clipName(true, moodPose);
                        FireCastDraw.salamander(pose, buffers, true, lod, name, age / 20f, age, 1f, light, face);
                    } else {
                        FireCastDraw.salamander(pose, buffers, true, lod, BIG[Math.min(clip, 2)], secs, age, 1f, light);
                    }
                }
            }
            pose.popPose();
        });
    }
}
