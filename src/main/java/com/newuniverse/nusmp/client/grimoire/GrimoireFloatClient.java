package com.newuniverse.nusmp.client.grimoire;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireCarry;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireShelfLayout;
import com.newuniverse.nusmp.grimoire.GrimoireBookPlan;
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
 *   <li><b>Open V</b> (0.23): once it arrives the book opens in three quick steps into the V of the reference renders, spine and
 *       covers towards onlookers, glowing pages towards its owner; on a stow it closes again before floating home.</li>
 *   <li><b>Page flip</b> on a spell switch while summoned: {@link GrimoireCarry#FLIP_PAGES} loose pages turn from the right-hand
 *       page block to the left-hand one.
 *       Parchment for most books; anti-magic pages are torn, soot-dark and tremble with red-black sparks; Flame-soul pages singe
 *       with embers, Water-soul pages shed droplets, Wind-soul pages throw puffs of air, Earth-soul pages shed dust.</li>
 * </ul>
 * Everything is computed every frame from game time + partial tick; no entities, nothing ticked over the network.
 */
public final class GrimoireFloatClient {
    private static final ResourceLocation LEAF = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/item/grimoire_book/page_leaf.png");
    private static final ResourceLocation LEAF_TATTERED = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/item/grimoire_book/page_leaf_tattered.png");
    private static ResourceLocation runeTexture(ItemStack stack) {
        MagicType magic = MagicType.byName(GrimoireItem.data(stack).getString("Magic"));
        String id = magic.name().toLowerCase(java.util.Locale.ROOT);
        ResourceLocation ring = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/magic_runes/" + id + "_rune_ring.png");
        if (Minecraft.getInstance().getResourceManager().getResource(ring).isPresent()) return ring;
        return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/magic_runes/" + id + ".png");
    }

    private static final class Entry {
        ItemStack stack;
        long start;
        long vanishAt = -1;
        /** Render copies of the stack for opening steps 1..3 (see GrimoireItem#withOpenView), made once per stack. */
        final ItemStack[] open = new ItemStack[GrimoireCarry.OPEN_STEPS + 1];
        Entry(ItemStack stack, long start) { this.stack = stack; this.start = start; }

        ItemStack view(int step) {
            if (step <= 0) return stack;
            ItemStack v = open[step];
            if (v == null) open[step] = v = GrimoireItem.withOpenView(stack, step);
            return v;
        }
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
        if (e != null && e.vanishAt < 0) { e.stack = stack; java.util.Arrays.fill(e.open, null); }   // swapped books: keep floating, change the look
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
            if (ent == null || !ent.isAlive() || (e.vanishAt >= 0 && now - e.vanishAt > GrimoireCarry.CLOSE_TICKS + GrimoireCarry.TRAVEL_TICKS + 1)) it.remove();
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
            float arrived;                                   // 1 = fully summoned (glows, bobs, opens, can flip)
            float age = 0;
            int step = 0;                                    // opening step: 0 closed .. 3 the full V
            float back = 0;
            if (e != null && e.vanishAt < 0) {
                age = now - e.start;
                arrived = GrimoireCarry.travel(age);
                p = GrimoireCarry.trip(true, age, sneak);
                step = GrimoireCarry.openStep(age);
                stack = e.view(step);
            } else if (e != null) {
                back = now - e.vanishAt;
                step = GrimoireCarry.closeStep(back);
                if (step > 0) {                              // closing up in front of the hand first
                    arrived = 1;
                    p = sneak ? GrimoireCarry.HAND_SNEAK : GrimoireCarry.HAND;
                    age = now - e.start;
                } else {
                    arrived = 0;
                    p = GrimoireCarry.trip(false, back - GrimoireCarry.CLOSE_TICKS, sneak);
                }
                stack = e.view(step);
            } else {
                if (hipStack.isEmpty() || self) continue;
                arrived = 0;
                p = sneak ? GrimoireCarry.HIP_SNEAK : GrimoireCarry.HIP;
                stack = hipStack;
            }
            // with nothing in the slot (book lost while out) the stowed book fades instead of landing at the hip
            float fade = e != null && e.vanishAt >= 0 && hipStack.isEmpty() ? 1f - GrimoireCarry.travel(back - GrimoireCarry.CLOSE_TICKS) : 1f;
            float bob = GrimoireShelfLayout.bob(0, age, arrived);
            float sway = Mth.sin(age * 0.05f) * 4f * arrived;

            pose.pushPose();
            if (self) {
                // first person: from below your view (t = 0) up to your lower right (t = 1)
                float t = e.vanishAt < 0 || step > 0 ? arrived : 1f - GrimoireCarry.travel(back - GrimoireCarry.CLOSE_TICKS);
                if (t <= 0.01f) { pose.popPose(); continue; }
                Vector3f look = camera.getLookVector(), up = camera.getUpVector(), left = camera.getLeftVector();
                double drop = (1 - t) * 0.9;
                // 0.29: the book is 2x bigger, so it sits a little further out, right and down to keep the crosshair clear
                Vec3 at = cam.add(look.x() * 1.35, look.y() * 1.35, look.z() * 1.35)
                        .add(-left.x() * 0.75, -left.y() * 0.75, -left.z() * 0.75)
                        .add(up.x() * (bob - 0.42 - drop), up.y() * (bob - 0.42 - drop), up.z() * (bob - 0.42 - drop));
                pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot() - 18f + sway));   // covers away from you: you read the pages
                pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot() * 0.6f + 22f - 40f * (1 - t)));   // 0.36: tilted, pages up at you
                float s = Mth.lerp(t, 0.84f, 1.6f);
                pose.scale(s, s, s);
            } else {
                double yaw = Math.toRadians(Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot));
                Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
                Vec3 at = julius(stack)
                        ? owner.getPosition(partial).add(fwd.scale(-0.42)).add(0, owner.getBbHeight() + 0.28 + bob, 0)
                        : owner.getPosition(partial).add(right.scale(p.right())).add(fwd.scale(p.forward())).add(0, p.up() + bob, 0);
                pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                pose.mulPose(Axis.YP.rotationDegrees(-(float) Math.toDegrees(yaw) + (julius(stack) ? 0f : p.yaw()) + sway));
                pose.mulPose(Axis.XP.rotationDegrees(julius(stack) ? 0f : p.pitch()));
                pose.mulPose(Axis.ZP.rotationDegrees(julius(stack) ? 0f : p.roll()));
                float s = p.scale() * fade;
                if (s <= 0.01f) { pose.popPose(); continue; }
                pose.scale(s, s, s);
            }
            int light = arrived > 0.5f ? LightTexture.FULL_BRIGHT : lightAt(owner, partial);
            long[] flip = FLIPS.get(id);
            Vector3f strapTop = null;
            if (julius(stack)) {
                // the Time grimoire has no covers and no spine: a page drum standing upright (0.37: undo the book's tilt)
                if (!self) {
                    pose.mulPose(Axis.ZP.rotationDegrees(-p.roll()));
                    pose.mulPose(Axis.XP.rotationDegrees(-p.pitch()));
                }
                juliusCylinder(pose, buffers, now, arrived, step, light);   // 0.38: no page flip, his pages never need turning
                pose.popPose();
                if (!self && !hipStack.isEmpty()) belt(buffers, owner, partial, cam, sneak, null, lightAt(owner, partial));
                continue;
            }
            if (arrived >= 1f) {
                // summoned: the in-hand context makes the model glow; step > 0 draws the open V
                mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, id);
            } else {
                // dormant or travelling: same placement as the in-hand transform, without the glow
                pose.pushPose();
                handTransform(pose);
                pose.translate(0.5f, 0.5f, 0.5f);
                mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, id);
                pose.popPose();
                if (e == null) strapTop = bookStrap(pose, buffers, light);       // 0.35: the holster strap round the dormant book
            }
            if (flip != null && step == GrimoireCarry.OPEN_STEPS) pages(pose, buffers, cam, stack, now - flip[0], flip[1] == 1, light);
            pose.popPose();
            if (!self && !hipStack.isEmpty()) belt(buffers, owner, partial, cam, sneak, strapTop, lightAt(owner, partial));
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

    // ---------------------------------------------------------------- 0.35: Julius's coverless grimoire, the holster harness
    private static final ResourceLocation HARNESS_LEATHER = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/grimoire_harness_leather.png");
    private static final ResourceLocation HARNESS_BRASS = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/grimoire_harness_brass.png");

    /**
     * Julius Novachrono's canon Time grimoire is drawn as the coverless page drum of the anime (the only grimoire in the Clover
     * Kingdom with no front or back cover). 0.37 drew every Time grimoire this way; since 0.38 it is Julius's book only.
     */
    static boolean julius(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var d = GrimoireItem.data(stack);
        return "julius".equalsIgnoreCase(d.getString("Canon"));         // 0.38: Julius's book only again; other Time grimoires are books
    }

    private static final ResourceLocation DRUM_EDGE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/item/grimoire_book/drum_edge.png");

    /**
     * The Time grimoire's page drum (0.37, after the owner's screenshot): a solid cylinder of cream pages packed edge to edge from
     * the centre out, with a ribbed band of page edges round the outside - no covers, no spine. Twice the book's size, standing
     * clear of the leg at the hip. Dormant it turns slowly; summoned it glows, turns faster and its pages flutter; opening fans it
     * a little wider; no page flip (0.38: he has no need to turn pages). Model units, around x = z = 8.
     */
    private static void juliusCylinder(PoseStack pose, MultiBufferSource buffers, float now, float arrived, int step, int light) {
        pose.pushPose();
        handTransform(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float push = 3.5f * (1 - arrived);                                   // at the hip: stand off the leg
        pose.translate(8f, 8f, 8f + push);
        float k0 = 1 - 0.5f * arrived;                                      // the summoned pose is 2x the hip size: keep the drum ~0.8 blocks across
        pose.scale(1.7f * k0, 1.45f * k0, 1.7f * k0);
        pose.translate(-8f, -8f, -8f);
        PoseStack.Pose last = pose.last();
        float open = step / (float) GrimoireCarry.OPEN_STEPS;
        float spin = now * (0.004f + 0.02f * arrived);
        float ro = 6.4f + 0.6f * open, y0 = 2.2f, y1 = 13.8f;
        // the pages: radial sheets packed edge to edge
        VertexConsumer pages = buffers.getBuffer(RenderType.entityCutoutNoCull(LEAF));
        int n = 96;
        for (int k = 0; k < n; k++) {
            float a = Mth.TWO_PI * k / n + spin + 0.025f * arrived * Mth.sin(now * 0.25f + k * 0.9f);
            float lift = 0.15f * arrived * Mth.sin(now * 0.3f + k * 1.3f);
            float ca = Mth.cos(a), sa = Mth.sin(a);
            int[] tint = k % 2 == 0 ? new int[]{244, 236, 214} : new int[]{226, 216, 190};
            strip(pages, last, 8 + ca * 0.35f, 8 + sa * 0.35f, 8 + ca * ro, 8 + sa * ro, y0 + lift, y1 + lift, 0f, 1f, tint, 255, light);
        }
        // the ribbed band of page edges round the outside
        VertexConsumer edge = buffers.getBuffer(RenderType.entityCutoutNoCull(DRUM_EDGE));
        int seg = 32;
        for (int k = 0; k < seg; k++) {
            float a0 = Mth.TWO_PI * k / seg + spin, a1 = Mth.TWO_PI * (k + 1) / seg + spin;
            float x0 = 8 + Mth.cos(a0) * (ro + 0.02f), z0 = 8 + Mth.sin(a0) * (ro + 0.02f), x1 = 8 + Mth.cos(a1) * (ro + 0.02f), z1 = 8 + Mth.sin(a1) * (ro + 0.02f);
            float nx = Mth.cos((a0 + a1) / 2), nz = Mth.sin((a0 + a1) / 2);
            face(edge, last, x0, y1, z0, x1, y1, z1, x1, y0, z1, x0, y0, z0, 1f, light, nx, 0, nz);
        }
        pose.popPose();
    }

    /**
     * The holster strap round the dormant book (after the owner's belt-holster references): down over the front cover to a
     * brass tip, over the top, and down the back. Model units, in the book's own pose. Returns where the strap leaves the top
     * of the book (camera-relative), for the belt to hang it from.
     */
    private static Vector3f bookStrap(PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        handTransform(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        PoseStack.Pose last = pose.last();
        VertexConsumer leather = buffers.getBuffer(RenderType.entityCutoutNoCull(HARNESS_LEATHER));
        float x0 = 7f, x1 = 9f, top = 15.1f, zf = 9.55f, zb = 6.45f;    // 0.36: on the slim boards (front 9.4, back 6.6)
        face(leather, last, x0, top, zf, x0, 3.2f, zf, x1, 3.2f, zf, x1, top, zf, 3f, light, 0, 0, 1);       // front
        face(leather, last, x0, top, zb, x0, top, zf, x1, top, zf, x1, top, zb, 1.3f, light, 0, 1, 0);       // over the top
        face(leather, last, x1, top, zb, x1, 8.5f, zb, x0, 8.5f, zb, x0, top, zb, 1.6f, light, 0, 0, -1);    // down the back
        face(leather, last, 6.4f, 7.2f, zf + 0.05f, 6.4f, 5.6f, zf + 0.05f, 9.6f, 5.6f, zf + 0.05f, 9.6f, 7.2f, zf + 0.05f, 0.5f, light, 0, 0, 1);   // keeper loop
        VertexConsumer brass = buffers.getBuffer(RenderType.entityCutoutNoCull(HARNESS_BRASS));
        face(brass, last, x0 - 0.15f, 3.4f, zf + 0.1f, x0 + 1f, 1.4f, zf + 0.1f, x1 - 1f, 1.4f, zf + 0.1f, x1 + 0.15f, 3.4f, zf + 0.1f, 1f, light, 0, 0, 1);
        Vector3f anchor = last.pose().transformPosition(new Vector3f(8f, top, 8f));
        pose.popPose();
        return anchor;
    }

    /**
     * The belt: a leather band round the owner's waist with a brass buckle in front, and the hanging strap from the right hip to
     * the book. Drawn on top of the player like a cosmetic layer, so it never takes an armour slot. World space, camera-relative.
     */
    private static void belt(MultiBufferSource buffers, Player owner, float partial, Vec3 cam, boolean sneak, Vector3f strapTop, int light) {
        PoseStack.Pose last = new PoseStack().last();
        double yaw = Math.toRadians(Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot));
        Vector3f fwd = new Vector3f((float) -Math.sin(yaw), 0, (float) Math.cos(yaw)), right = new Vector3f((float) -Math.cos(yaw), 0, (float) -Math.sin(yaw));
        Vec3 pos = owner.getPosition(partial).subtract(cam);
        Vector3f c = new Vector3f((float) pos.x, (float) pos.y + (sneak ? 0.62f : 0.78f), (float) pos.z);
        float hw = 0.27f, hd = 0.155f, hh = 0.05f;
        VertexConsumer leather = buffers.getBuffer(RenderType.entityCutoutNoCull(HARNESS_LEATHER));
        float[][] corners = {{-1, 1}, {1, 1}, {1, -1}, {-1, -1}};          // (side, front) round the waist: front, right, back, left
        for (int k = 0; k < 4; k++) {
            float[] a = corners[k], b = corners[(k + 1) % 4];
            Vector3f pa = new Vector3f(c).add(new Vector3f(right).mul(a[0] * hw)).add(new Vector3f(fwd).mul(a[1] * hd));
            Vector3f pb = new Vector3f(c).add(new Vector3f(right).mul(b[0] * hw)).add(new Vector3f(fwd).mul(b[1] * hd));
            Vector3f n = k == 0 ? fwd : k == 1 ? right : k == 2 ? new Vector3f(fwd).negate() : new Vector3f(right).negate();
            face(leather, last, pa.x, pa.y + hh, pa.z, pb.x, pb.y + hh, pb.z, pb.x, pb.y - hh, pb.z, pa.x, pa.y - hh, pa.z,
                    k % 2 == 0 ? 2f : 1f, light, n.x, n.y, n.z);
        }
        // the hanging strap from the right hip down to the book
        Vector3f hip = new Vector3f(c).add(new Vector3f(right).mul(hw + 0.01f)).add(0, -0.02f, 0);
        Vector3f end = strapTop != null ? strapTop : new Vector3f(hip).add(new Vector3f(right).mul(0.05f)).add(0, -0.25f, 0);
        Vector3f across = new Vector3f(end).sub(hip).cross(right);
        if (across.lengthSquared() > 1e-6f) {
            across.normalize().mul(0.035f);
            face(leather, last, hip.x - across.x, hip.y - across.y, hip.z - across.z, end.x - across.x, end.y - across.y, end.z - across.z,
                    end.x + across.x, end.y + across.y, end.z + across.z, hip.x + across.x, hip.y + across.y, hip.z + across.z, 1f, light, right.x, right.y, right.z);
        }
        // the brass buckle at the front
        VertexConsumer brass = buffers.getBuffer(RenderType.entityCutoutNoCull(HARNESS_BRASS));
        Vector3f b = new Vector3f(c).add(new Vector3f(fwd).mul(hd + 0.006f));
        Vector3f r = new Vector3f(right).mul(0.05f);
        face(brass, last, b.x - r.x, b.y + 0.06f, b.z - r.z, b.x - r.x, b.y - 0.06f, b.z - r.z, b.x + r.x, b.y - 0.06f, b.z + r.z, b.x + r.x, b.y + 0.06f, b.z + r.z,
                1f, light, fwd.x, fwd.y, fwd.z);
    }

    /** One textured quad a-b-c-d: U runs a -> b (0 .. uLen, tiling), V runs a -> d (0 .. 1). */
    private static void face(VertexConsumer vc, PoseStack.Pose last, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float uLen, int light, float nx, float ny, float nz) {
        put(vc, last, ax, ay, az, 0, 0, light, nx, ny, nz);
        put(vc, last, bx, by, bz, uLen, 0, light, nx, ny, nz);
        put(vc, last, cx, cy, cz, uLen, 1, light, nx, ny, nz);
        put(vc, last, dx, dy, dz, 0, 1, light, nx, ny, nz);
    }

    private static void put(VertexConsumer vc, PoseStack.Pose last, float x, float y, float z, float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(last, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
    }

    // ---------------------------------------------------------------- page flip
    private enum Style { PARCHMENT, ANTI, EMBER, DROPLET, GUST, DUST }

    private static Style style(ItemStack stack) {
        MagicType m = MagicType.byName(GrimoireItem.data(stack).getString("Magic"));
        if (m == MagicType.ANTI_MAGIC || m == MagicType.KOTODAMA || GrimoireItem.cover(stack).isForbidden()) return Style.ANTI;   // 0.47: Zagred's corrupted book
        return switch (m.soulType) {
            case "FLAME" -> Style.EMBER;
            case "WATER" -> Style.DROPLET;
            case "WIND" -> Style.GUST;
            case "EARTH" -> Style.DUST;
            default -> Style.PARCHMENT;
        };
    }

    /**
     * Loose pages turning inside the open book, in the book model's own units (0..16): each starts on the right-hand page block,
     * swings through the reader's side and lands on the left-hand block ({@link GrimoireCarry#pageHeading}); two strips per page,
     * the outer half lagging behind ({@link GrimoireCarry#pageBend}).
     */
    private static void pages(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, float age, boolean reverse, int light) {
        Style style = style(stack);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(style == Style.ANTI ? LEAF_TATTERED : LEAF));
        VertexConsumer runes = buffers.getBuffer(RenderType.entityTranslucent(runeTexture(stack)));
        int[] tint = switch (style) {
            case ANTI -> new int[]{120, 104, 108};
            case EMBER -> new int[]{255, 226, 196};
            case DROPLET -> new int[]{222, 238, 255};
            default -> new int[]{255, 250, 236};
        };
        pose.pushPose();
        handTransform(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float openDeg = GrimoireBookPlan.OPEN_DEGREES[GrimoireBookPlan.OPEN_STEPS];
        final float hx = GrimoireBookPlan.OPEN_CX, hz = GrimoireBookPlan.OPEN_HINGE_Z, r0 = 0.9f, half = 3.0f, y0 = 1.8f, y1 = 14.2f;
        for (int k = 0; k < GrimoireCarry.FLIP_PAGES; k++) {
            float alpha = GrimoireCarry.pageAlpha(k, age);
            if (alpha <= 0f) continue;
            float a1 = (float) Math.toRadians(GrimoireCarry.pageHeading(k, age, openDeg, reverse));
            float a2 = a1 + (float) Math.toRadians((reverse ? -1 : 1) * GrimoireCarry.pageBend(k, age));
            float jx = 0, jz = 0;
            if (style == Style.ANTI) { jx = (RNG.nextFloat() - 0.5f) * 0.3f; jz = (RNG.nextFloat() - 0.5f) * 0.3f; }   // crackling tremble
            float x0 = hx + Mth.cos(a1) * r0, z0 = hz + Mth.sin(a1) * r0;
            float x1 = x0 + Mth.cos(a1) * half + jx, z1 = z0 + Mth.sin(a1) * half + jz;
            float x2 = x1 + Mth.cos(a2) * half, z2 = z1 + Mth.sin(a2) * half;
            int a = (int) (alpha * (style == Style.ANTI ? 230 : 245));
            strip(vc, pose.last(), x0, z0, x1, z1, y0, y1, 0f, 0.5f, tint, a, light);
            strip(vc, pose.last(), x1, z1, x2, z2, y0, y1, 0.5f, 1f, tint, a, light);
            strip(runes, pose.last(), x0, z0, x1, z1, y0, y1, 0f, 0.5f, tint, a, light);
            strip(runes, pose.last(), x1, z1, x2, z2, y0, y1, 0.5f, 1f, tint, a, light);
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
