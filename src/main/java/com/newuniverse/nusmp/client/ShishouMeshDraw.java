package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Why: Shishou's body in the world is drawn with the very same 3D mesh as the sword in the hand ({@code assets/nusmp/weapon_meshes/shishou.txt}, built by tools/gen_shishou_assets.py), so the flying sword,
 * the dormant sword in the ground and the item in the hand cannot look different. The mesh store and the quad emitter of {@link WeaponRenderer} are package private, so this thin bridge lives next to it;
 * the entity renderer ({@code client.shishou.ShishouRenderer}) only asks it to draw. The mesh is drawn upright (blade along +y, centred on its middle), and the caller turns it. Worst case per body per
 * frame: the quads of the mesh twice (body, glow), the glow pass dropped beyond {@link #GLOW_RANGE} blocks; EX-Singularity adds the halo (the glow quads once more) and 32 shard quads within 24 blocks ({@link ExSingularityFx}).
 */
public final class ShishouMeshDraw {
    private ShishouMeshDraw() {}

    public static final String MESH = "shishou";
    public static final double GLOW_RANGE = 48.0, CULL_RANGE = 160.0;

    /** Does the mesh exist (loaded once per resource reload)? */
    public static boolean available() { return WeaponRenderer.mesh(MESH) != null; }

    /** The length of the sword in blocks as drawn, or 0 when the mesh is missing. */
    public static float length() {
        WeaponRenderer.Mesh m = WeaponRenderer.mesh(MESH);
        return m == null ? 0f : m.length;
    }

    /**
     * Draws the sword centred on the pose's origin, blade along +y, front +z. {@code alpha} below 1 draws it translucent (Stealth); {@code glowStrength} 0..1 scales the additive glow (0 = no glow pass); {@code time} is
     * the game time in ticks for the glow's slow pulse.
     */
    public static boolean draw(PoseStack pose, MultiBufferSource buffers, int light, float alpha, float glowStrength, float time) {
        WeaponRenderer.Mesh mesh = WeaponRenderer.mesh(MESH);
        if (mesh == null) return false;
        pose.pushPose();
        pose.scale(mesh.scale, mesh.scale, mesh.scale);
        pose.translate(0f, -(mesh.ymin + (mesh.ymax - mesh.ymin) * 0.5f), 0f);
        PoseStack.Pose p = pose.last();
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        RenderType rt = alpha < 0.98f ? RenderType.entityTranslucent(mesh.tex) : RenderType.entityCutout(mesh.tex);
        WeaponRenderer.emit(p, buffers.getBuffer(rt), mesh.layers[WeaponRenderer.BODY], a << 24 | 0xFFFFFF, light, OverlayTexture.NO_OVERLAY);
        if (glowStrength > 0.01f && mesh.glowTex != null && !mesh.layers[WeaponRenderer.GLOW].isEmpty()) {
            float k = com.newuniverse.nusmp.client.relics.item.VariantRules.pulse(time, mesh.glowSpeed, mesh.glowMin, mesh.glowMax, 1f) * glowStrength * alpha;
            int rgb = ExSingularityFx.glowRgb(time);                       // EX-Singularity: the glow walks through every hue (client/ExSingularityFx), not the mesh's one blue
            int r = com.newuniverse.nusmp.client.relics.item.VariantRules.channel(rgb >> 16 & 255, k), g = com.newuniverse.nusmp.client.relics.item.VariantRules.channel(rgb >> 8 & 255, k),
                    b = com.newuniverse.nusmp.client.relics.item.VariantRules.channel(rgb & 255, k);
            WeaponRenderer.emit(p, buffers.getBuffer(SafeAdditive.additive(mesh.glowTex)), mesh.layers[WeaponRenderer.GLOW], 0xFF000000 | r << 16 | g << 8 | b, 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
        if (glowStrength > 0.9f && alpha > 0.9f && mesh.glowTex != null) ExSingularityFx.extras(pose, buffers, mesh, time);          // EX-Singularity: the opposite-hue halo and the orbiting shards (not on a dormant or veiled sword)
        pose.popPose();
        return true;
    }
}
