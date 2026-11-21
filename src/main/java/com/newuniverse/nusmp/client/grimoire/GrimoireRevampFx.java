package com.newuniverse.nusmp.client.grimoire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.grimoire.BookLook;
import com.newuniverse.nusmp.grimoire.GrimoireBookPlan;
import com.newuniverse.nusmp.grimoire.GrimoireRevampConfig;
import com.newuniverse.nusmp.grimoire.GrimoireRevampMath;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The generic summon / cast look of a revamped grimoire (docs/grimoire_revamp.md), drawn only for a book that is open in front of its owner and only within
 * {@link GrimoireRevampMath#LOD_DIST} blocks of the camera (the caller checks the tier). Three layers, all in the book's own model units behind its pages (towards the reader):
 * a soft glow halo in the magic's glow colour, a turning rune ring (the magic's own rune texture, the same one the page flip uses) with a counter-turning inner ring, and the
 * particles: a burst when the book finishes opening, a burst on every cast (started by the caster's chant / release body animation, so no new packet is needed), and one page
 * flutter mote every {@link GrimoireRevampMath#FLUTTER_TICKS} ticks in the magic's accent. The per-magic bespoke effects of the books (Time and the rebuilt ones) are not touched.
 * {@code visual.classicGrimoires} draws none of it. Worst case per book: 6 quads a frame, {@link GrimoireRevampMath#SUMMON_BURST} particles once, {@link GrimoireRevampMath#maxCast()} per cast.
 */
final class GrimoireRevampFx {
    private static final ResourceLocation HALO = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/entity/grimoire_revamp_halo.png");
    private static final PerfProbe.Section SEC = PerfProbe.section("c.grimoire.revamp");
    private static final RandomSource RNG = RandomSource.create();
    /** Model-unit planes (the open book's pages face -Z; the reader is behind them): the halo sits just in front of the pages, the ring in front of the halo. */
    private static final float HALO_Z = 7.3f, RING_Z = 6.9f;

    private static final class State {
        float flareStart = -1000f, strength, lastFlutter = -1000f, lastSeen;
        int pending;
        boolean arrivedBurst;
        ItemStack src;
        int rgb;
        FxSlot slot = FxSlot.MOTE;
    }

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static float lastPurge;

    private GrimoireRevampFx() {}

    static void clear() { STATES.clear(); }

    /** A body animation clip started on an entity (CastAnimClient): flares the book of that owner if it is out. Nothing happens for a clip with no flare or an owner with no open book. */
    static void onClip(int entityId, String clip) {
        float s = GrimoireRevampMath.flareStrength(clip);
        if (s <= 0f || GrimoireRevampConfig.classic()) return;
        Minecraft mc = Minecraft.getInstance();
        State st = STATES.get(entityId);
        if (mc.level == null || st == null) return;
        float now = mc.level.getGameTime();
        if (now - st.flareStart < GrimoireRevampMath.FLARE_COOLDOWN) return;
        st.flareStart = now;
        st.strength = s;
        st.pending = GrimoireRevampMath.burstFor(s);
    }

    /**
     * Draws the layer for one open book. {@code pose} is the book's pose (the caller's), {@code step} its opening step 0..3, {@code now} game time + partial tick.
     * Nothing is allocated per frame except the pose push; the tinted colour and accent are worked out once per stack.
     */
    static void draw(Minecraft mc, PoseStack pose, MultiBufferSource buffers, Vec3 cam, int id, ItemStack stack, int step, float now) {
        if (GrimoireRevampConfig.classic()) return;
        if (step <= 0) {
            State closed = STATES.get(id);
            if (closed != null) closed.arrivedBurst = false;
            return;
        }
        long t0 = PerfProbe.begin();
        try {
            State st = STATES.computeIfAbsent(id, k -> new State());
            st.lastSeen = now;
            if (st.src != stack) {
                st.src = stack;
                BookLook look = GrimoireItem.look(stack);
                st.rgb = look.glowColor();
                MagicType m = MagicType.byName(GrimoireItem.data(stack).getString("Magic"));
                boolean anti = m == MagicType.ANTI_MAGIC || m == MagicType.KOTODAMA || GrimoireItem.cover(stack).isForbidden();
                st.slot = FxSlot.values()[GrimoireRevampMath.accentSlot(m.soulType, anti)];
            }
            float open = step / (float) GrimoireBookPlan.OPEN_STEPS;
            float fl = GrimoireRevampMath.flare(now - st.flareStart), str = st.strength;
            int r = st.rgb >> 16 & 255, g = st.rgb >> 8 & 255, b = st.rgb & 255;
            pose.pushPose();
            GrimoireFloatClient.handTransform(pose);
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            PoseStack.Pose last = pose.last();
            int full = LightTexture.FULL_BRIGHT;
            float halo = GrimoireRevampMath.haloAlpha(open, fl, str, now);
            if (halo > 0.01f) disc(buffers.getBuffer(RenderType.entityTranslucent(HALO)), last, HALO_Z, 9.5f + 2.5f * fl * str, 0f, r, g, b, (int) (halo * 255), full);
            float ring = GrimoireRevampMath.ringAlpha(open, fl, str);
            if (ring > 0.01f) {
                float rad = GrimoireRevampMath.ringRadius(fl, str, now), spin = GrimoireRevampMath.ringSpin(now, fl);
                RenderType type = RenderType.entityTranslucent(GrimoireFloatClient.runeTexture(stack));
                disc(buffers.getBuffer(type), last, RING_Z, rad, spin, r, g, b, (int) (ring * 255), full);
                disc(buffers.getBuffer(type), last, RING_Z - 0.05f, rad * 0.62f, -spin * 1.4f, r, g, b, (int) (ring * 0.8f * 255), full);
            }
            particles(mc, last.pose(), cam, st, step, now);
            pose.popPose();
            if (now - lastPurge > 100f) {
                lastPurge = now;
                STATES.values().removeIf(s -> now - s.lastSeen > 60f);
            }
        } finally {
            PerfProbe.end(SEC, t0);
        }
    }

    private static void particles(Minecraft mc, Matrix4f m, Vec3 cam, State st, int step, float now) {
        if (mc.level == null) return;
        if (!st.arrivedBurst && step >= GrimoireBookPlan.OPEN_STEPS) {
            st.arrivedBurst = true;
            burst(mc, m, cam, st, GrimoireRevampMath.SUMMON_BURST, 0.5f);
        }
        if (st.pending > 0) {
            burst(mc, m, cam, st, st.pending, 0.65f);
            st.pending = 0;
        }
        if (GrimoireRevampMath.flutterDue(now, st.lastFlutter, step)) {                     // one page-flutter mote, rising from a random spot of the pages
            st.lastFlutter = now;
            float x = 8f + (RNG.nextFloat() - 0.5f) * 9f, y = 3f + RNG.nextFloat() * 10f;
            Vector4f v = m.transform(new Vector4f(x, y, 8.2f, 1f));
            Fx.sprayTinted(mc.level, FxThemes.SKILL_GRIMOIRE, st.slot, st.rgb, 0.45f, cam.x + v.x(), cam.y + v.y(), cam.z + v.z(), 1, 0.02, 0.03, 0.02, 0.01);
        }
    }

    private static void burst(Minecraft mc, Matrix4f m, Vec3 cam, State st, int count, float size) {
        Vector4f v = m.transform(new Vector4f(8f, 8f, 7.6f, 1f));                         // the middle of the pages, a little in front
        Fx.sprayTinted(mc.level, FxThemes.SKILL_GRIMOIRE, st.slot, st.rgb, size, cam.x + v.x(), cam.y + v.y(), cam.z + v.z(), count, 0.32, 0.28, 0.32, 0.05);
    }

    /** A square sprite centred at (8, 8, z) of half-size {@code half}, turned {@code spin} degrees about the book's Z axis, both sides (the translucent type does not cull). */
    private static void disc(VertexConsumer vc, PoseStack.Pose last, float z, float half, float spin, int r, int g, int b, int a, int light) {
        float c = Mth.cos(spin * Mth.DEG_TO_RAD) * half, s = Mth.sin(spin * Mth.DEG_TO_RAD) * half;
        for (int side = 0; side < 2; side++) {
            float nz = side == 0 ? -1f : 1f;
            // corners (-1,-1) (1,-1) (1,1) (-1,1) turned; the second side runs the other way round so it faces the other way
            for (int k = 0; k < 4; k++) {
                int i = side == 0 ? k : 3 - k;
                float sx = i == 0 || i == 3 ? -1f : 1f, sy = i < 2 ? -1f : 1f;
                float x = 8f + sx * c - sy * s, y = 8f + sx * s + sy * c;
                vc.addVertex(last, x, y, z).setColor(r, g, b, a).setUv(sx > 0 ? 1f : 0f, sy > 0 ? 0f : 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0f, 0f, nz);
            }
        }
    }
}
