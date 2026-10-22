package examples;

import com.newuniverse.nusmp.client.prop.SpiritLightDraw;
import com.newuniverse.nusmp.client.prop.PropPainters;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.spiritlight.LightMoodRules;
import com.newuniverse.nusmp.spiritlight.SpiritLightLod;
import com.newuniverse.nusmp.spiritlight.SpiritLightRules;

/**
 * Spirit of Light preview: draws the REAL helper the game's SpiritLightPropPainter uses (client/prop/SpiritLightDraw: the colour model plus the additive glow-only model, the beam, the pulse ring), at the
 * place the game puts each thing relative to a standing player at the origin (facing +z): the cub beside the right shoulder, the seraph behind, the beam from the cub's mouth or the seraph's core, the
 * pulse ring on the ground. Registered under PropKind.BRONZE_1; the scene's "param" = variant * 1000 + lod * 100 + pose (variant 0 cub, 1 seraph, 2 beam (pose 1 = powered), 3 pulse; pose = the pose id of
 * LightMoodRules). Scenes: tools/render_preview/scenes.d/spirit_light_props.json (written by tools/render_preview/gen_spirit_light_scenes.py; they list "realFiles").
 */
public final class SpiritLightProp {
    private SpiritLightProp() {}

    public static void register() {
        PropPainters.register(PropKind.BRONZE_1, (e, partial, age, pose, buffers, light) -> {
            int p = e.param(), variant = p / 1000, lod = (p / 100) % 10, idx = p % 100;
            pose.pushPose();
            switch (variant) {
                case 0, 1 -> {
                    boolean seraph = variant == 1;
                    double[] a = seraph ? SpiritLightRules.seraph(0, 0, 0, 0) : SpiritLightRules.companion(0, 0, 0, 0);
                    pose.translate(a[0], a[1] + (seraph ? 0.07 * Math.sin(age * 0.07) : 0.045 * Math.sin(age * 0.12)), a[2]);
                    int pid = Math.min(idx, LightMoodRules.poseCount(seraph) - 1);
                    String clip = pid == LightMoodRules.POSE_IDLE ? "idle" : LightMoodRules.poseName(seraph, pid);
                    SpiritLightDraw.form(pose, buffers, seraph, lod, clip, age / 20f, age, 1f, light);
                }
                case 2 -> {
                    boolean powered = idx == 1;
                    double[] m = powered ? SpiritLightRules.seraphCore(0, 0, 0, 0) : SpiritLightRules.companionMouth(0, 0, 0, 0);
                    pose.translate(m[0], m[1], m[2]);
                    SpiritLightDraw.beam(pose, buffers, 14f, 0f, 0f, 1f, age / SpiritLightRules.BEAM_LIFE, powered, true);
                }
                default -> SpiritLightDraw.pulse(pose, buffers, (float) SpiritLightRules.pulseRadius(false), age / SpiritLightRules.PULSE_LIFE, false);
            }
            pose.popPose();
            if (lod < 0) System.out.println(SpiritLightLod.FRAME_QUADS);       // keeps the Lod class on the compile path of the preview
        });
    }
}
