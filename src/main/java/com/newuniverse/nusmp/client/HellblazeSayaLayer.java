package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.relics.item.VariantRules;
import com.newuniverse.nusmp.client.upgrade.HellblazeHits;
import com.newuniverse.nusmp.client.upgrade.UpgradeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Why: Hellblaze, The Demon Sin is a katana (docs/hellblaze_katana.md), and a katana has a sheath. While a player holds Hellblaze in the main hand and the OTHER hand is empty, this draws the saya in that hand: in
 * third person a render layer on every player (other players are seen with it too: the main-hand stack is synced by the game), in first person the off hand's own hand draw ({@link RenderHandEvent} fires for an empty off
 * hand, as {@code GreedHandsClient} uses). It never replaces a real off-hand item (an occupied off hand draws nothing here) and is purely visual: no item, no packet, no server state, nothing stored per player.
 * <p>
 * Cost: one mesh of at most {@link #QUADS} quad lines (body plus glow copies), read once per resource reload; per frame one draw per player that holds Hellblaze with an empty other hand and is within {@link #RANGE}
 * blocks (far players are skipped); no allocation per frame (two scratch quaternions fixed at class load). Off with the client switch {@code visual.hellblazeKatana} (then the classic broadsword is drawn and no sheath).
 * Poses are guesses made from the arm model (the arm hangs along +y, the front is -z): nothing here has been seen in a running game.
 */
public final class HellblazeSayaLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    static final String SAYA_ID = "hellblaze_the_demon_sin__saya";
    static final String FILE = "weapon_meshes/up/hellblaze_the_demon_sin__saya.txt";
    static final String SKIN = "textures/entity/weapon/up/hellblaze_the_demon_sin__saya.png", GLOW = "textures/entity/weapon/up/hellblaze_the_demon_sin__saya_glow.png";
    /** Blocks per mesh unit (the mesh is in cm): the katana's in-hand scale (0.01678 x the 0.85 hand scale) in third person, the first-person item scale in first person. */
    static final float TP_SCALE = 0.01426f, FP_SCALE = 0.0092f;
    /** How far below the mouth of the sheath the hand holds it (mesh units): at the kurikata knob. */
    static final float HAND = 8f;
    /** Beyond this distance (blocks) another player's sheath is not drawn. */
    static final double RANGE = 32.0;
    /** The most quad lines the mesh may have (the asset test checks it). */
    public static final int QUADS = 600;

    // ---- the poses: the mesh's +y (mouth to tip) lies along D, its +x (the edge) turns toward E; third person in the arm's model space (y down the arm, -z in front), first person in the camera's (x right, y up, -z away)
    private static final Quaternionf TP_L = pose(0.12f, 0.45f, 0.88f, 0f, 1f, 0f), TP_R = pose(-0.12f, 0.45f, 0.88f, 0f, 1f, 0f);
    private static final Quaternionf FP_L = pose(-0.22f, 0.30f, -0.93f, 0f, -1f, 0f), FP_R = pose(0.22f, 0.30f, -0.93f, 0f, -1f, 0f);

    private static WeaponRenderer.Mesh mesh;
    private static boolean tried;

    public HellblazeSayaLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    public static void init(IEventBus modBus) {
        modBus.addListener(HellblazeSayaLayer::addLayers);
        modBus.addListener((ModelEvent.BakingCompleted e) -> { mesh = null; tried = false; });          // a resource reload reads the mesh again
        NeoForge.EVENT_BUS.addListener(HellblazeSayaLayer::onRenderHand);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (PlayerSkin.Model skin : e.getSkins())
            if (e.getSkin(skin) instanceof PlayerRenderer r) r.addLayer(new HellblazeSayaLayer(r));
    }

    /** The rotation that puts the mesh's +y along (dx, dy, dz) and its +x as near as it can to (ex, ey, ez); built once. */
    static Quaternionf pose(float dx, float dy, float dz, float ex, float ey, float ez) {
        Vector3f y = new Vector3f(dx, dy, dz).normalize();
        Vector3f x = new Vector3f(ex, ey, ez);
        x.sub(new Vector3f(y).mul(x.dot(y))).normalize();
        Vector3f z = new Vector3f(x).cross(y);
        return new Quaternionf().setFromNormalized(new Matrix3f(x.x, x.y, x.z, y.x, y.y, y.z, z.x, z.y, z.z));
    }

    private static WeaponRenderer.Mesh mesh() {
        if (!tried) {
            tried = true;
            mesh = WeaponRenderer.read(SAYA_ID, FILE, DemonSlayerRenderer.rl(SKIN), DemonSlayerRenderer.rl(GLOW));
        }
        return mesh;
    }

    /** Is a sheath drawn for this player now: Hellblaze in the main hand as a katana, the other hand empty, the player visible. */
    static boolean draws(AbstractClientPlayer p) {
        return UpgradeConfig.hellblazeKatana() && !p.isInvisible() && !p.isSpectator() && p.getOffhandItem().isEmpty() && HellblazeHits.holds(p);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (!draws(p) || p.distanceToSqr(mc.player) > RANGE * RANGE) return;
        HumanoidArm arm = p.getMainArm().getOpposite();
        pose.pushPose();
        getParentModel().translateToHand(arm, pose);
        pose.translate(0f, 0.62f, 0f);                                                                    // the hand: about ten pixels down the arm
        draw(pose, buffers, light, arm == HumanoidArm.LEFT ? TP_L : TP_R, TP_SCALE, age);
        pose.popPose();
    }

    /** First person: the off hand's own draw, at vanilla's item-arm place (the same transforms {@code GreedHandsClient} uses), only while the hand is empty. */
    private static void onRenderHand(RenderHandEvent e) {
        if (e.getHand() != InteractionHand.OFF_HAND || !e.getItemStack().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer p = mc.player;
        if (p == null || mc.level == null || !draws(p) || p.isScoping() || p.isUsingItem()) return;
        HumanoidArm arm = p.getMainArm().getOpposite();
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swing = e.getSwingProgress(), s = Mth.sqrt(swing);
        PoseStack pose = e.getPoseStack();
        pose.pushPose();
        pose.translate(i * -0.4f * Mth.sin(s * (float) Math.PI), 0.2f * Mth.sin(s * (float) Math.PI * 2f), -0.2f * Mth.sin(swing * (float) Math.PI));
        pose.translate(i * 0.56f, -0.52f + e.getEquipProgress() * -0.6f, -0.72f);
        draw(pose, e.getMultiBufferSource(), e.getPackedLight(), i > 0 ? FP_R : FP_L, FP_SCALE, p.tickCount + e.getPartialTick());
        pose.popPose();
    }

    /** One draw of the sheath: the body skin, then the glow in the dark red of the katana's glow line. */
    private static void draw(PoseStack pose, MultiBufferSource buffers, int light, Quaternionf q, float scale, float time) {
        WeaponRenderer.Mesh m = mesh();
        if (m == null) return;
        pose.pushPose();
        pose.mulPose(q);
        pose.scale(scale, scale, scale);
        pose.translate(0f, -HAND, 0f);
        PoseStack.Pose last = pose.last();
        WeaponRenderer.emit(last, buffers.getBuffer(RenderType.entityCutout(m.tex)), m.layers[WeaponRenderer.BODY], 0xFFFFFFFF, light, OverlayTexture.NO_OVERLAY);
        if (m.glowTex != null && !m.layers[WeaponRenderer.GLOW].isEmpty()) {
            float k = VariantRules.pulse(time, m.glowSpeed, m.glowMin, m.glowMax, 1f);
            int r = VariantRules.channel(m.glowRgb >> 16 & 255, k), g = VariantRules.channel(m.glowRgb >> 8 & 255, k), b = VariantRules.channel(m.glowRgb & 255, k);
            WeaponRenderer.emit(last, buffers.getBuffer(SafeAdditive.additive(m.glowTex)), m.layers[WeaponRenderer.GLOW], 0xFF000000 | r << 16 | g << 8 | b, 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }
}
