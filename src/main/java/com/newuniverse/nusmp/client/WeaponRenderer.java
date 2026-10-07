package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 0.50: every Black Clover weapon (and the relics) in 3D, to the Genesis Demon-Slayer's standard ({@link DemonSlayerRenderer}). The inventory
 * keeps each weapon's old icon (nusmp:item/&lt;id&gt;_icon, the old item model unchanged); in hand, on the ground, in frames and
 * on mobs the weapon is a pixel-built model:
 * <ul>
 *   <li><b>Mesh:</b> assets/nusmp/weapon_meshes/&lt;id&gt;.txt from tools/gen_weapon_models.py (beveled blades with a ridge,
 *       extruded pixel guards, boxed grips and pommels), placed on the old sprite's diagonal so the old display transforms fit.</li>
 *   <li><b>Skin:</b> textures/entity/weapon/&lt;id&gt;.png (cutout, with the enchantment glint).</li>
 *   <li><b>Glow:</b> textures/entity/weapon/&lt;id&gt;_glow.png drawn additive and full bright, its tint pulsing (runes, edges,
 *       gems).</li>
 *   <li><b>Void:</b> Asta's demon swords show the crimson dimensional void through their notches and broken patches
 *       ({@link NURenderTypes#demonVoid()}).</li>
 * </ul>
 * Preview without the game: python tools/item_preview/preview_weapons.py.
 */
public class WeaponRenderer extends BlockEntityWithoutLevelRenderer {
    /** The weapons and relics drawn this way (each needs a mesh, a skin, and an &lt;id&gt;_icon model). */
    static final List<Supplier<? extends Item>> WEAPONS = List.of(
            NUItems.DEMON_SLASHER_KATANA, NUItems.MIASMA_KATANA, NUItems.SPELL_FORGED_RAPIER, NUItems.SEVERING_GREATSWORD,
            NUItems.DEMON_DWELLER, NUItems.DEMON_DESTROYER, NUItems.LICHT_DWELLER, NUItems.LICHT_DESTROYER,
            NUItems.RIMEHEART_RUNEBLADE, NUItems.OTHERWORLD_TRIDENT, NUItems.MAGIC_TOOL_SWORD, NUItems.MAGIC_TOOL_SPEAR,
            NUItems.COMMUNICATION_DEVICE, NUItems.RUNE_STONE, NUItems.SPIRIT_CHARM, NUItems.BOND_THREAD, NUItems.FORTUNE_DIE,
            NUItems.GRIMOIRE_CHAIN, NUItems.ANTI_BIRD_CHARM, NUItems.RECOVERY_SALVE, NUItems.WRITTEN_CONSENT, NUItems.DEVIL_CONTRACT,
            NUItems.GAUCHE_MIRROR);

    static final int BODY = 0, VOID = 1, GLOW = 2;

    private static WeaponRenderer instance;
    private static final Map<String, Mesh> MESHES = new HashMap<>();

    public WeaponRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(WeaponRenderer::registerExtensions);
        modBus.addListener(WeaponRenderer::registerModels);
        modBus.addListener((ModelEvent.BakingCompleted e) -> MESHES.clear());     // a resource reload re-reads the meshes
    }

    static void registerExtensions(RegisterClientExtensionsEvent e) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (instance == null) instance = new WeaponRenderer();
                return instance;
            }
        };
        for (Supplier<? extends Item> item : WEAPONS) e.registerItem(ext, item.get());
    }

    static void registerModels(ModelEvent.RegisterAdditional e) {
        for (Supplier<? extends Item> item : WEAPONS) e.register(icon(BuiltInRegistries.ITEM.getKey(item.get()).getPath()));
    }

    static ModelResourceLocation icon(String id) { return ModelResourceLocation.standalone(DemonSlayerRenderer.rl("item/" + id + "_icon")); }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        Mesh mesh = ctx == ItemDisplayContext.GUI ? null : mesh(id);
        if (mesh == null) {                                                        // the inventory (or a missing mesh): the old icon
            BakedModel icon = mc.getModelManager().getModel(icon(id));
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);                                      // render() translates by -0.5 again
            mc.getItemRenderer().render(stack, ctx, false, pose, buffers, light, overlay, icon);
            pose.popPose();
            return;
        }
        float time = mc.level == null ? 0 : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        pose.translate(mesh.ox, mesh.oy, 0.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(mesh.angle - 90f));
        pose.scale(mesh.scale, mesh.scale, mesh.scale);
        pose.translate(0f, -mesh.ymin, 0f);
        PoseStack.Pose p = pose.last();
        emit(p, ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutout(mesh.tex), false, stack.hasFoil()), mesh.layers[BODY], 0xFFFFFFFF, light, overlay);
        if (!mesh.layers[VOID].isEmpty()) {
            VertexConsumer vc = buffers.getBuffer(NURenderTypes.demonVoid());
            Matrix4f m = p.pose();
            for (float[] v : mesh.layers[VOID]) {
                Vector3f q = m.transformPosition(v[0], v[1], v[2], new Vector3f());
                vc.addVertex(q.x, q.y, q.z);
            }
        }
        if (mesh.glowTex != null && !mesh.layers[GLOW].isEmpty()) {
            float k = mesh.glowMin + (mesh.glowMax - mesh.glowMin) * (0.5f + 0.5f * (float) Math.sin(time * mesh.glowSpeed));
            int r = (int) ((mesh.glowRgb >> 16 & 255) * k), g = (int) ((mesh.glowRgb >> 8 & 255) * k), b = (int) ((mesh.glowRgb & 255) * k);
            emit(p, buffers.getBuffer(RenderType.eyes(mesh.glowTex)), mesh.layers[GLOW], 0xFF000000 | r << 16 | g << 8 | b, 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }

    private static void emit(PoseStack.Pose p, VertexConsumer vc, List<float[]> verts, int argb, int light, int overlay) {
        Matrix4f m = p.pose();
        Matrix3f nm = p.normal();
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        for (float[] v : verts) {
            Vector3f q = m.transformPosition(v[0], v[1], v[2], new Vector3f());
            Vector3f n = nm.transform(v[5], v[6], v[7], new Vector3f()).normalize();
            vc.addVertex(q.x, q.y, q.z).setColor(r, g, b, a).setUv(v[3] / 128f, v[4] / 128f).setOverlay(overlay).setLight(light).setNormal(n.x, n.y, n.z);
        }
    }

    // ================================================================ mesh files
    /** One weapon's mesh: its placement on the old sprite's diagonal, its glow pulse, and its vertices per layer. */
    static final class Mesh {
        float ox, oy, angle, scale, ymin, glowSpeed, glowMin, glowMax;
        int glowRgb;
        ResourceLocation tex, glowTex;
        @SuppressWarnings("unchecked")
        final List<float[]>[] layers = new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    }

    /** The parsed mesh for an item id, read once per resource reload; null if it is missing or broken (the icon is used). */
    static Mesh mesh(String id) {
        if (MESHES.containsKey(id)) return MESHES.get(id);
        Mesh mesh = null;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(DemonSlayerRenderer.rl("weapon_meshes/" + id + ".txt"));
            if (res.isPresent()) {
                mesh = new Mesh();
                mesh.tex = DemonSlayerRenderer.rl("textures/entity/weapon/" + id + ".png");
                try (BufferedReader in = res.get().openAsReader()) {
                    String line;
                    while ((line = in.readLine()) != null) parse(mesh, id, line.trim().split("\\s+"));
                }
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] weapon mesh {} failed to load; its icon is used instead", id, ex);
            mesh = null;
        }
        MESHES.put(id, mesh);
        return mesh;
    }

    private static void parse(Mesh mesh, String id, String[] v) {
        switch (v[0]) {
            case "place" -> {
                mesh.ox = Float.parseFloat(v[1]);
                mesh.oy = Float.parseFloat(v[2]);
                mesh.angle = Float.parseFloat(v[3]);
                mesh.scale = Float.parseFloat(v[4]);
                mesh.ymin = Float.parseFloat(v[5]);
            }
            case "glow" -> {
                mesh.glowRgb = Integer.parseInt(v[1], 16);
                mesh.glowSpeed = Float.parseFloat(v[2]);
                mesh.glowMin = Float.parseFloat(v[3]);
                mesh.glowMax = Float.parseFloat(v[4]);
                mesh.glowTex = DemonSlayerRenderer.rl("textures/entity/weapon/" + id + "_glow.png");
            }
            case "q" -> {
                int layer = Integer.parseInt(v[1]);
                float nx = Float.parseFloat(v[22]), ny = Float.parseFloat(v[23]), nz = Float.parseFloat(v[24]);
                for (int k = 0; k < 4; k++) {
                    int o = 2 + k * 5;
                    mesh.layers[layer].add(new float[]{Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2]),
                            Float.parseFloat(v[o + 3]), Float.parseFloat(v[o + 4]), nx, ny, nz});
                }
            }
            default -> { }
        }
    }
}
