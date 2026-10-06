package com.newuniverse.nusmp.client.grimoire;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireCarry;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireShelfLayout;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Draws carried grimoires (0.22):
 * <ul>
 *   <li><b>Dormant</b>: while the owner's Grimoire Slot is filled, the book hangs at their right hip, cover out (not drawn for
 *       yourself in first person).</li>
 *   <li><b>Summoned</b> (book.GrimoireSummon): it floats up from the hip to the front of the right hand along
 *       {@link GrimoireCarry#trip}, then bobs ({@link GrimoireShelfLayout#bob}, faded in by the trip) and sways; stowing plays the
 *       trip backwards to the hip. In first person it rises from below your view to your lower right.</li>
 *   <li><b>Page flip</b> on a spell switch while summoned: {@link GrimoireCarry#FLIP_PAGES} loose pages swing over the spine.
 *       Parchment for most books; anti-magic pages are torn, soot-dark and tremble with red-black sparks; Flame-soul pages singe
 *       with embers, Water-soul pages shed droplets, Wind-soul pages throw puffs of air, Earth-soul pages shed dust.</li>
 * </ul>
 * Everything is computed every frame from game time + partial tick; no entities, nothing ticked over the network.
 */
public final class GrimoireFloatClient {
    private static final ResourceLocation LEAF = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/item/grimoire_book/page_leaf.png");
    private static final ResourceLocation LEAF_TATTERED = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/item/grimoire_book/page_leaf_tattered.png");

    private static final class Entry {
        ItemStack stack;
        long start;
        long vanishAt = -1;
        Entry(ItemStack stack, long start) { this.stack = stack; this.start = start; }
    }

    /** Summoned (or just stowed) books by entity id. */
    private static final Map<Integer, Entry> FLOATING = new HashMap<>();
    /** Grimoire Slot contents by entity id (the hip book). */
    private static final Map<Integer, ItemStack> HIP = new HashMap<>();
    /** Page flips by entity id: {start game time, reverse ? 1 : 0}. */
    private static final Map<Integer, long[]> FLIPS = new HashMap<>();
    private static final RandomSource RNG = RandomSource.create();

    private GrimoireFloatClient() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(GrimoireFloatClient::onRender);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { FLOATING.clear(); HIP.clear(); FLIPS.clear(); });
    }

    /** The local view of a player's Grimoire Slot (EMPTY if unknown). */
    public static ItemStack hip(int entityId) { return HIP.getOrDefault(entityId, ItemStack.EMPTY); }

    // ---------------------------------------------------------------- network handlers (client thread)
    public static void receive(int entityId, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        Entry e = FLOATING.get(entityId);
        if (stack.isEmpty()) {
            if (e != null && e.vanishAt < 0) e.vanishAt = now;
            return;
        }
        if (e != null && e.vanishAt < 0) e.stack = stack;          // swapped books: keep floating, change the look
        else FLOATING.put(entityId, new Entry(stack, now));
    }

    public static void receiveHip(int entityId, ItemStack stack) {
        if (stack.isEmpty()) HIP.remove(entityId); else HIP.put(entityId, stack);
    }

    public static void flip(int entityId, boolean reverse) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !FLOATING.containsKey(entityId)) return;
        long now = mc.level.getGameTime();
        long[] f = FLIPS.get(entityId);
        if (f != null && now - f[0] < 2) return;
        FLIPS.put(entityId, new long[]{now, reverse ? 1 : 0});
        if (mc.level.getEntity(entityId) instanceof Player p)
            mc.level.playLocalSound(p.getX(), p.getY() + 1, p.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.5f, 1.6f, false);
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { FLOATING.clear(); HIP.clear(); FLIPS.clear(); return; }
        long now = mc.level.getGameTime();
        Iterator<Map.Entry<Integer, Entry>> it = FLOATING.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Entry e = en.getValue();
            var ent = mc.level.getEntity(en.getKey());
            if (ent == null || !ent.isAlive() || (e.vanishAt >= 0 && now - e.vanishAt > GrimoireCarry.TRAVEL_TICKS + 1)) it.remove();
        }
        FLIPS.entrySet().removeIf(f -> now - f.getValue()[0] > GrimoireCarry.flipDuration() + 2);
        if (now % 40 == 0) HIP.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }

    // ---------------------------------------------------------------- rendering
    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (FLOATING.isEmpty() && HIP.isEmpty())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = mc.level.getGameTime() + partial;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = new PoseStack();

        Set<Integer> ids = new HashSet<>(HIP.keySet());
        ids.addAll(FLOATING.keySet());
        for (int id : ids) {
            if (!(mc.level.getEntity(id) instanceof Player owner) || owner.isInvisible() || owner.isSleeping()) continue;
            Entry e = FLOATING.get(id);
            ItemStack hipStack = HIP.getOrDefault(id, ItemStack.EMPTY);
            boolean sneak = owner.isCrouching();
            boolean self = firstPerson && owner == mc.getCameraEntity();

            GrimoireCarry.Pose p;
            ItemStack stack;
            float arrived;                                   // 1 = fully summoned (glows, bobs, can flip)
            float age = 0;
            if (e != null && e.vanishAt < 0) {
                age = now - e.start;
                arrived = GrimoireCarry.travel(age);
                p = GrimoireCarry.trip(true, age, sneak);
                stack = e.stack;
            } else if (e != null) {
                float back = now - e.vanishAt;
                arrived = 0;
                p = GrimoireCarry.trip(false, back, sneak);
                stack = e.stack;
            } else {
                if (hipStack.isEmpty() || self) continue;
                arrived = 0;
                p = sneak ? GrimoireCarry.HIP_SNEAK : GrimoireCarry.HIP;
                stack = hipStack;
            }
            // with nothing in the slot (book lost while out) the stowed book fades instead of landing at the hip
            float fade = e != null && e.vanishAt >= 0 && hipStack.isEmpty() ? 1f - GrimoireCarry.travel(now - e.vanishAt) : 1f;
            float bob = GrimoireShelfLayout.bob(0, age, arrived);
            float sway = Mth.sin(age * 0.05f) * 4f * arrived;

            pose.pushPose();
            if (self) {
                // first person: from below your view (t = 0) up to your lower right (t = 1)
                float t = e.vanishAt < 0 ? arrived : 1f - GrimoireCarry.travel(now - e.vanishAt);
                if (t <= 0.01f) { pose.popPose(); continue; }
                Vector3f look = camera.getLookVector(), up = camera.getUpVector(), left = camera.getLeftVector();
                double drop = (1 - t) * 0.9;
                Vec3 at = cam.add(look.x() * 0.85, look.y() * 0.85, look.z() * 0.85)
                        .add(-left.x() * 0.42, -left.y() * 0.42, -left.z() * 0.42)
                        .add(up.x() * (bob - 0.2 - drop), up.y() * (bob - 0.2 - drop), up.z() * (bob - 0.2 - drop));
                pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                pose.mulPose(Axis.YP.rotationDegrees(180f - camera.getYRot() - 18f + sway));
                pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot() * 0.6f - 12f - 40f * (1 - t)));
                float s = Mth.lerp(t, 0.38f, 0.55f);
                pose.scale(s, s, s);
            } else {
                double yaw = Math.toRadians(Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot));
                Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
                Vec3 at = owner.getPosition(partial).add(right.scale(p.right())).add(fwd.scale(p.forward())).add(0, p.up() + bob, 0);
                pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                pose.mulPose(Axis.YP.rotationDegrees(-(float) Math.toDegrees(yaw) + p.yaw() + sway));
                pose.mulPose(Axis.XP.rotationDegrees(p.pitch()));
                pose.mulPose(Axis.ZP.rotationDegrees(p.roll()));
                float s = p.scale() * fade;
                if (s <= 0.01f) { pose.popPose(); continue; }
                pose.scale(s, s, s);
            }
            int light = arrived > 0.5f ? LightTexture.FULL_BRIGHT : lightAt(owner, partial);
            if (arrived >= 1f) {
                // summoned: the in-hand context makes the model glow
                mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, id);
            } else {
                // dormant or travelling: same placement as the in-hand transform, without the glow
                pose.pushPose();
                handTransform(pose);
                pose.translate(0.5f, 0.5f, 0.5f);
                mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, id);
                pose.popPose();
            }
            long[] f = FLIPS.get(id);
            if (f != null && arrived >= 1f) pages(pose, buffers, cam, stack, now - f[0], f[1] == 1, light);
            pose.popPose();
        }
        buffers.endBatch();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /** The grimoire model's third-person right-hand display transform (models/item/grimoire.json), then the item renderer's centring. */
    private static void handTransform(PoseStack pose) {
        pose.translate(0f, 3f / 16f, 1.5f / 16f);
        pose.scale(0.7f, 0.7f, 0.7f);
        pose.translate(-0.5f, -0.5f, -0.5f);
    }

    private static int lightAt(Player owner, float partial) {
        Minecraft mc = Minecraft.getInstance();
        return mc.getEntityRenderDispatcher().getPackedLightCoords(owner, partial);
    }

    // ---------------------------------------------------------------- page flip
    private enum Style { PARCHMENT, ANTI, EMBER, DROPLET, GUST, DUST }

    private static Style style(ItemStack stack) {
        MagicType m = MagicType.byName(GrimoireItem.data(stack).getString("Magic"));
        if (m == MagicType.ANTI_MAGIC || GrimoireItem.cover(stack).isForbidden()) return Style.ANTI;
        return switch (m.soulType) {
            case "FLAME" -> Style.EMBER;
            case "WATER" -> Style.DROPLET;
            case "WIND" -> Style.GUST;
            case "EARTH" -> Style.DUST;
            default -> Style.PARCHMENT;
        };
    }

    /**
     * Loose pages turning over the spine, in the book model's own units (0..16, front cover at z 10.5, spine at x 2.7). Each page is
     * two strips: the inner half at the hinge angle, the outer half lagging behind ({@link GrimoireCarry#pageBend}).
     */
    private static void pages(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, float age, boolean reverse, int light) {
        Style style = style(stack);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(style == Style.ANTI ? LEAF_TATTERED : LEAF));
        int[] tint = switch (style) {
            case ANTI -> new int[]{120, 104, 108};
            case EMBER -> new int[]{255, 226, 196};
            case DROPLET -> new int[]{222, 238, 255};
            default -> new int[]{255, 250, 236};
        };
        pose.pushPose();
        handTransform(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        final float hx = 2.7f, hz = 10.95f, y0 = 1.8f, y1 = 14.2f, half = 5.15f;
        for (int k = 0; k < GrimoireCarry.FLIP_PAGES; k++) {
            float alpha = GrimoireCarry.pageAlpha(k, age);
            if (alpha <= 0f) continue;
            float deg = GrimoireCarry.pageAngle(k, age);
            if (reverse) deg = GrimoireCarry.FLIP_MAX_DEG - deg;
            float a1 = (float) Math.toRadians(deg), a2 = (float) Math.toRadians(deg + (reverse ? -1 : 1) * GrimoireCarry.pageBend(k, age));
            float jx = 0, jz = 0;
            if (style == Style.ANTI) { jx = (RNG.nextFloat() - 0.5f) * 0.3f; jz = (RNG.nextFloat() - 0.5f) * 0.3f; }   // crackling tremble
            float x1 = hx + Mth.cos(a1) * half + jx, z1 = hz + Mth.sin(a1) * half + jz;
            float x2 = x1 + Mth.cos(a2) * half, z2 = z1 + Mth.sin(a2) * half;
            int a = (int) (alpha * (style == Style.ANTI ? 230 : 245));
            strip(vc, pose.last(), hx, hz, x1, z1, y0, y1, 0f, 0.5f, tint, a, light);
            strip(vc, pose.last(), x1, z1, x2, z2, y0, y1, 0.5f, 1f, tint, a, light);
            if (RNG.nextFloat() < 0.4f) particle(pose.last().pose(), cam, style, x2, (y0 + y1) * 0.5f + (RNG.nextFloat() - 0.5f) * 10f, z2);
        }
        pose.popPose();
    }

    /** One quad from edge (xa,za) to edge (xb,zb), y0..y1, both sides visible (entityTranslucent does not cull). */
    private static void strip(VertexConsumer vc, PoseStack.Pose last, float xa, float za, float xb, float zb, float y0, float y1,
                              float u0, float u1, int[] c, int a, int light) {
        float nx = -(zb - za), nz = xb - xa;
        float len = Mth.sqrt(nx * nx + nz * nz);
        if (len < 1e-4f) return;
        nx /= len; nz /= len;
        vertex(vc, last, xa, y1, za, u0, 0f, c, a, light, nx, nz);
        vertex(vc, last, xa, y0, za, u0, 1f, c, a, light, nx, nz);
        vertex(vc, last, xb, y0, zb, u1, 1f, c, a, light, nx, nz);
        vertex(vc, last, xb, y1, zb, u1, 0f, c, a, light, nx, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose last, float x, float y, float z, float u, float v, int[] c, int a, int light, float nx, float nz) {
        vc.addVertex(last, x, y, z).setColor(c[0], c[1], c[2], a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0f, nz);
    }

    private static final DustParticleOptions RED_SPARK = new DustParticleOptions(new Vector3f(0.75f, 0.06f, 0.1f), 0.7f);
    private static final DustParticleOptions EARTH_DUST = new DustParticleOptions(new Vector3f(0.55f, 0.42f, 0.26f), 0.8f);

    /** A particle at a point of the page's free edge (model units, transformed to the world). */
    private static void particle(Matrix4f m, Vec3 cam, Style style, float x, float y, float z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vector4f v = m.transform(new Vector4f(x, y, z, 1f));
        double wx = cam.x + v.x(), wy = cam.y + v.y(), wz = cam.z + v.z();
        ParticleOptions type;
        double vy = 0;
        switch (style) {
            case ANTI -> { type = RNG.nextBoolean() ? ParticleTypes.SMOKE : RED_SPARK; vy = 0.01; }
            case EMBER -> { type = RNG.nextInt(4) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME; vy = 0.02; }
            case DROPLET -> type = RNG.nextInt(3) == 0 ? ParticleTypes.SPLASH : ParticleTypes.FALLING_WATER;
            case GUST -> { type = ParticleTypes.CLOUD; vy = 0.01; }
            case DUST -> type = EARTH_DUST;
            default -> { return; }                                                 // plain parchment: just the rustle
        }
        mc.level.addParticle(type, wx, wy, wz, (RNG.nextFloat() - 0.5f) * 0.02, vy, (RNG.nextFloat() - 0.5f) * 0.02);
    }
}
