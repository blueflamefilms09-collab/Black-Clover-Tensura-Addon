package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.48: the Genesis Demon-Slayer in hand, on the ground and in frames; the inventory keeps the old 32x32 icon (the model
 * nusmp:item/demon_slayer_sword_icon, the old item model unchanged).
 * <ul>
 *   <li><b>Body:</b> {@link DemonSlayerMesh}, a hand-built pixel model (textures/entity/demon_slayer_sword.png from
 *       tools/gen_demon_slayer_textures.py): a broad, chipped, slightly tapering blade with a slanted tip, a riveted crossguard,
 *       a cloth-wrapped grip and a pommel with a crimson stone, on the same diagonal as the old sprite (so the old display
 *       transforms still fit).</li>
 *   <li><b>Dimensional parallax:</b> the central split (a slot through the blade) and the dark spots on both faces are drawn with
 *       {@link NURenderTypes#demonVoid()}: a crimson end-portal parallax (vanilla's end portal if the shader didn't load).</li>
 *   <li><b>Five-leaf bloom:</b> a clover on both faces of the pommel that grows and pulses crimson and black with the stack's
 *       "Resonance" (0..4, set by the server near high-EP beings).</li>
 *   <li><b>Fracture:</b> while "MeteorUntil" is ahead (Black Meteorite), the blade splits into four shards that drift apart,
 *       shiver and come back together, with crimson light between them.</li>
 * </ul>
 * Preview without the game: python tools/item_preview/preview_demon_slayer.py.
 */
public class DemonSlayerRenderer extends BlockEntityWithoutLevelRenderer {
    static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    public static final ResourceLocation TEX = rl("textures/entity/demon_slayer_sword.png"), CLOVER = rl("textures/entity/demon_slayer_bloom.png"),
            BLOOM_GLOW = rl("textures/entity/demon_slayer_bloom_glow.png"), GLOW = rl("textures/particle/glow.png");
    public static final ModelResourceLocation ICON = ModelResourceLocation.standalone(rl("item/demon_slayer_sword_icon"));

    private static DemonSlayerRenderer instance;

    public DemonSlayerRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(DemonSlayerRenderer::registerExtensions);
        modBus.addListener(DemonSlayerRenderer::registerModels);
        modBus.addListener(NUShaders::register);
    }

    static void registerExtensions(RegisterClientExtensionsEvent e) {
        e.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (instance == null) instance = new DemonSlayerRenderer();
                return instance;
            }
        }, NUItems.DEMON_SLAYER.get());
    }

    static void registerModels(ModelEvent.RegisterAdditional e) { e.register(ICON); }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (ctx == ItemDisplayContext.GUI) {                                    // the inventory keeps the old icon
            BakedModel icon = mc.getModelManager().getModel(ICON);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);                                    // render() translates by -0.5 again
            mc.getItemRenderer().render(stack, ctx, false, pose, buffers, light, overlay, icon);
            pose.popPose();
            return;
        }
        float time = mc.level == null ? 0 : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag data = cd == null ? new CompoundTag() : cd.copyTag();
        long until = data.getLong("MeteorUntil");
        float fracture = until > 0 && mc.level != null ? DemonSlayerMesh.fracture(until - time) : 0;
        GameSink sink = new GameSink(pose);
        long growthUntil = data.getLong("BladeGrowthUntil");
        if (mc.level != null && growthUntil > time) {
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.scale(1.6f, 1.6f, 1.6f);
            pose.translate(-0.5f, -0.5f, -0.5f);
        }
        DemonSlayerMesh.draw(sink, data.getInt("Resonance"), fracture, time);
        if (mc.level != null && growthUntil > time) pose.popPose();
        sink.flush(buffers, stack.hasFoil(), light, overlay);
    }

    /** Collects the mesh's quads already transformed by the pose stack, then draws them one render type at a time. */
    static final class GameSink implements DemonSlayerMesh.Sink {
        private final PoseStack pose;
        @SuppressWarnings("unchecked")
        private final List<float[]>[] layers = new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};

        GameSink(PoseStack pose) { this.pose = pose; }

        @Override public void push() { pose.pushPose(); }
        @Override public void pop() { pose.popPose(); }
        @Override public void translate(float x, float y, float z) { pose.translate(x, y, z); }
        @Override public void rotateZ(float degrees) { pose.mulPose(Axis.ZP.rotationDegrees(degrees)); }
        @Override public void rotateX(float degrees) { pose.mulPose(Axis.XP.rotationDegrees(degrees)); }
        @Override public void scale(float s) { pose.scale(s, s, s); }

        @Override
        public void quad(int layer, float[] a, float[] b, float[] c, float[] d, float nx, float ny, float nz, int argb) {
            PoseStack.Pose p = pose.last();
            Vector3f n = p.transformNormal(nx, ny, nz, new Vector3f());
            for (float[] k : new float[][]{a, b, c, d}) {
                Vector3f v = p.pose().transformPosition(k[0], k[1], k[2], new Vector3f());
                layers[layer].add(new float[]{v.x, v.y, v.z, k[3], k[4], n.x, n.y, n.z, argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24});
            }
        }

        void flush(MultiBufferSource buffers, boolean foil, int light, int overlay) {
            emit(ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutout(TEX), false, foil), layers[DemonSlayerMesh.BODY], 1 / 64f, light, overlay);
            if (!layers[DemonSlayerMesh.VOID].isEmpty()) {
                VertexConsumer vc = buffers.getBuffer(NURenderTypes.demonVoid());
                for (float[] v : layers[DemonSlayerMesh.VOID]) vc.addVertex(v[0], v[1], v[2]);
            }
            emit(buffers.getBuffer(RenderType.entityTranslucent(CLOVER)), layers[DemonSlayerMesh.CLOVER], 1, light, overlay);
            emit(buffers.getBuffer(RenderType.eyes(BLOOM_GLOW)), layers[DemonSlayerMesh.BLOOM], 1, 0xF000F0, OverlayTexture.NO_OVERLAY);
            if (!layers[DemonSlayerMesh.CRACK].isEmpty())
                emit(buffers.getBuffer(RenderType.eyes(GLOW)), layers[DemonSlayerMesh.CRACK], 1, 0xF000F0, OverlayTexture.NO_OVERLAY);
        }

        private static void emit(VertexConsumer vc, List<float[]> verts, float uvScale, int light, int overlay) {
            for (float[] v : verts)
                vc.addVertex(v[0], v[1], v[2]).setColor((int) v[8], (int) v[9], (int) v[10], (int) v[11])
                        .setUv(v[3] * uvScale, v[4] * uvScale).setOverlay(overlay).setLight(light).setNormal(v[5], v[6], v[7]);
        }
    }
}
