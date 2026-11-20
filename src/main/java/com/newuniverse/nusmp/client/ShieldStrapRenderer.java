package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.relics.item.HolderTracker;
import com.newuniverse.nusmp.visual.shields.ShieldStraps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 4.72.0 (docs/visual_upgrade/shield_fix.md, "Straps"): draws the arm straps of a shield on its item frame, before WeaponRenderer moves the mesh onto the plate, so the straps and the
 * fit of the mesh never see each other. Client only; the geometry is {@link ShieldStraps}'s. The client option {@code visual.shieldStraps} (FlatPropsConfig) is the switch: off draws nothing.
 * <p>
 * Cost: the straps of a (mesh, hand state) are built once, on first use; a frame then emits at most {@link ShieldStraps#MAX_QUADS} quads (two vertex passes over cached lists, the scratch
 * vectors of {@link WeaponRenderer#emitRange}), nothing allocated; a shield further than 24 blocks draws none.
 */
final class ShieldStrapRenderer {
    private ShieldStrapRenderer() {}

    private static final ResourceLocation TEX = DemonSlayerRenderer.rl("textures/entity/weapon/shield_straps.png");
    private static ShieldStraps.Data data;
    private static boolean tried;

    static void clear() {
        data = null;
        tried = false;
    }

    private static ShieldStraps.Data data() {
        if (!tried) {
            tried = true;
            try {
                Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(DemonSlayerRenderer.rl("shield_straps.json"));
                if (res.isPresent()) {
                    try (java.io.InputStream in = res.get().open()) {
                        data = ShieldStraps.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception ex) {
                LogUtils.getLogger().error("[anime_multiverse] shield_straps.json failed to load; shields are drawn without straps", ex);
                data = null;
            }
        }
        return data;
    }

    /** Builds (once per mesh and hand state) the straps of {@code ctx}; null when the data is missing. */
    @SuppressWarnings("unchecked")
    private static List<float[]>[] straps(WeaponRenderer.Mesh mesh, int ctx, float[] sf) {
        if (mesh.straps == null) mesh.straps = new List[ShieldStraps.CONTEXTS][];
        if (mesh.straps[ctx] == null) {
            ShieldStraps.Data d = data();
            if (d == null) return null;
            List<float[]> body = mesh.layers[WeaponRenderer.BODY];
            float back = ShieldStraps.plateBackZ(body.isEmpty() ? new ArrayList<>(mesh.layers[WeaponRenderer.GLOW]) : body);
            mesh.straps[ctx] = ShieldStraps.build(d, ctx, ShieldStraps.anchorZ(sf, back));
        }
        return mesh.straps[ctx];
    }

    /** {@code p} is the pose of the item frame (nothing of the shield's fit applied); {@code sf} is the fit {scale, tx, ty, tz} the caller is about to apply. */
    static void draw(PoseStack.Pose p, MultiBufferSource buffers, WeaponRenderer.Mesh mesh, ItemDisplayContext ctx, float[] sf, ItemStack stack, int light, int overlay) {
        if (!ShieldStraps.enabled()) return;
        boolean first = ctx.firstPerson();
        boolean left = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (!first && !left && ctx != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return;
        Matrix4f m = p.pose();
        if (!first && m.m30() * m.m30() + m.m31() * m.m31() + m.m32() * m.m32() > ShieldStraps.CULL_DIST_SQ) return;
        int c = ShieldStraps.CTX_FIRST_PERSON;
        if (!first) {
            LivingEntity holder = HolderTracker.holderFor(ctx);
            c = ShieldStraps.ctxOf(left, holder != null && holder.isUsingItem() && holder.getUseItem() == stack);
        }
        List<float[]>[] set = straps(mesh, c, sf);
        if (set == null) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEX));
        WeaponRenderer.emitRange(m, p.normal(), vc, set[ShieldStraps.LEATHER], 0, set[ShieldStraps.LEATHER].size(), 0xFF000000 | ShieldStraps.LEATHER_RGB, light, overlay);
        WeaponRenderer.emitRange(m, p.normal(), vc, set[ShieldStraps.TRIM], 0, set[ShieldStraps.TRIM].size(), 0xFF000000 | ShieldStraps.trimRgb(mesh.id), light, overlay);
    }
}
