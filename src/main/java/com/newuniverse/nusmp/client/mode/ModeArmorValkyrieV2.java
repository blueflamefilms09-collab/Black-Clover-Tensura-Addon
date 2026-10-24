package com.newuniverse.nusmp.client.mode;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.ModeArmorValkyrieBudget;
import com.newuniverse.nusmp.book.ModeArmorValkyrieMesh;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Look;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Piece;
import com.newuniverse.nusmp.book.ModeArmorValkyrieSpin;
import com.newuniverse.nusmp.client.SafeAdditive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * The Valkyrie Dress overlay of Noelle, version 2: a player-fitted 3D armour (marbled veil wings with a stepped notch and two frill sheets, the blade scarf, fin boots, the tiara, petal bodice and skirt,
 * shoulders, cuffs, sleeves, the water-rope legs, the grimoire), its SAINT look (feathered wings, halo, winged helm, gold sash) and its MERMAID look (a scaled tail, shell top, horn), and the spinning drill lance AB
 * (docs/vfx_remake/valkyrie_ingame.md). Drawn while the mode {@link ModeArmor.Mode#VALKYRIE} runs and the client option {@code modeArmor.valkyrieLook} is V2 (the default); CLASSIC keeps the old overlay of
 * {@link ModeArmorLayer} exactly.
 *
 * <p>How it is drawn (the pattern of {@link ModeArmorWindV2}): the meshes are static arrays baked once into the space of the player-model part they are fitted to ({@link ModeArmorValkyrieMesh}); per wearer the
 * layer captures one {@link PoseStack.Pose} per part (body, head, both arms, both legs, the lance), then draws pass by pass (opaque, the tail's scales, translucent, additive), so a wearer costs four buffer
 * switches, five with the Mermaid tail. Only the wing vertices do per-frame maths ({@link ModeArmorValkyrieMesh.Wings}); nothing is allocated per frame by this class.</p>
 *
 * <p>Cost (rule 11): the armour has its OWN pool of {@value ModeArmorValkyrieBudget#POOL_QUADS} mesh quads per frame for all wearers together ({@link ModeArmorValkyrieBudget}), nearest wearer first, each gets the best
 * level of detail that still fits (NEAR to 16 blocks, MID to 32, FAR to 64, nothing beyond); the lance is drawn at NEAR and MID only. The plan is made once a frame, on the first wearer drawn (or the first
 * leg-hiding query). Render thread only; no packet, no server state.</p>
 */
public final class ModeArmorValkyrieV2 {
    private ModeArmorValkyrieV2() {}

    private static volatile Look look = ModeArmorValkyrieRules.DEFAULT_LOOK;
    private static volatile Form form = ModeArmorValkyrieRules.DEFAULT_FORM;
    private static volatile LanceMode lance = ModeArmorValkyrieRules.DEFAULT_LANCE;

    /** The client option {@code modeArmor.valkyrieLook}. */
    public static Look look() { return look; }

    public static void setLook(Look l) { look = l == null ? ModeArmorValkyrieRules.DEFAULT_LOOK : l; }

    /** The client option {@code modeArmor.valkyrieForm}. */
    public static Form form() { return form; }

    public static void setForm(Form f) { form = f == null ? ModeArmorValkyrieRules.DEFAULT_FORM : f; }

    /** The client option {@code modeArmor.valkyrieLance}. */
    public static LanceMode lance() { return lance; }

    public static void setLance(LanceMode l) { lance = l == null ? ModeArmorValkyrieRules.DEFAULT_LANCE : l; }

    /** True when the overlay of {@code mode} is drawn by this class (the Valkyrie Dress with the option on V2). */
    public static boolean active(ModeArmor.Mode mode) { return mode == ModeArmor.Mode.VALKYRIE && look == Look.V2; }

    /**
     * True while the armour v2 is what draws the wings, the helm and the halo of player {@code entityId}: the Water Spirit Dive aura asks this and then leaves out its own wing, helm and halo pieces
     * ({@link ModeArmorValkyrieRules#replacesAuraPiece}); its plates, skirt, gauntlets, greaves, tail and backdrop stay. One map read per diver per frame.
     */
    public static boolean replacesAuraPieces(int entityId) {
        if (look != Look.V2) return false;
        var level = Minecraft.getInstance().level;
        return level != null && ModeArmorClient.mode(entityId, level.getGameTime()) == ModeArmor.Mode.VALKYRIE;
    }

    /** True for a Spirit Dive piece id the armour v2 replaces (the water wings, the helm, the saint wings and halo). */
    public static boolean replacesAuraPiece(String id) { return ModeArmorValkyrieRules.replacesAuraPiece(id); }

    /** True while the Mermaid tail of player {@code entityId} is drawn: the vanilla legs are then hidden (RenderPlayerEvent in {@link ModeArmorValkyrieEvents}). */
    public static boolean hidesLegs(int entityId) {
        if (look != Look.V2 || form != Form.MERMAID) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        long gt = mc.level.getGameTime();
        if (ModeArmorClient.mode(entityId, gt) != ModeArmor.Mode.VALKYRIE) return false;
        ensurePlan(mc, gt);
        int slot = slotOf(entityId);
        return slot >= 0 && LEVEL[slot] >= 0;
    }

    // ================================================================ the textures and render types
    private static final ResourceLocation ATLAS = tex("valk2_atlas"), SCALES = tex("valk2_scales");

    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/mode/" + n + ".png"); }

    private static VertexConsumer buffer(MultiBufferSource b, int pass) {
        return switch (pass) {
            case ModeArmorValkyrieMesh.OPAQUE -> b.getBuffer(RenderType.entityCutoutNoCull(ATLAS));
            case ModeArmorValkyrieMesh.SCALES -> b.getBuffer(RenderType.entityCutoutNoCull(SCALES));
            case ModeArmorValkyrieMesh.TRANSLUCENT -> b.getBuffer(RenderType.entityTranslucent(ATLAS));
            default -> b.getBuffer(SafeAdditive.additive(ATLAS));
        };
    }

    // ================================================================ the frame plan (nearest first, the armour's own pool)
    private static final int MAX = ModeArmorValkyrieBudget.MAX_WEARERS;
    private static final int[] IDS = new int[MAX], FORMS = new int[MAX], LEVEL = new int[MAX];
    private static final double[] DIST = new double[MAX];
    private static final boolean[] HOLDS = new boolean[MAX], WANT = new boolean[MAX], LANCE = new boolean[MAX], DONE = new boolean[MAX];
    private static final int[] ONE_LEVEL = new int[1];
    private static final boolean[] ONE_LANCE = new boolean[1];
    private static int count, used, pool = ModeArmorValkyrieBudget.POOL_QUADS;
    private static float stamp = Float.NaN;
    /** The wearers drawn since the plan was made: one wearer is drawn once a frame, so one drawn twice means a new frame with the same clock (the game paused, a second view): the plan is made again. */
    private static final int[] SEEN = new int[MAX];
    private static int seen;

    /** Plans the frame once: the wearers in view, nearest first, each with its level and lance, under the pool; frees the spin slots of wearers that are gone. */
    private static void ensurePlan(Minecraft mc, long gameTime) {
        float s = gameTime + mc.getTimer().getGameTimeDeltaPartialTick(false);
        if (s == stamp) return;
        stamp = s;
        seen = 0;
        pool = ModeArmorValkyrieWorld.pool();
        count = ModeArmorValkyrieWorld.scan(gameTime, IDS, DIST, HOLDS);
        boolean lanceOn = lance != LanceMode.OFF;
        int f = form.ordinal();
        for (int i = 0; i < count; i++) {
            FORMS[i] = f;
            WANT[i] = lanceOn && !HOLDS[i];
        }
        used = ModeArmorValkyrieBudget.admit(DIST, FORMS, WANT, count, LEVEL, LANCE, DONE, pool);
        for (int i = 0; i < SPIN_N; i++) {
            if (SPIN_ID[i] == FREE) continue;
            boolean live = false;
            for (int k = 0; k < count; k++) if (IDS[k] == SPIN_ID[i]) { live = true; break; }
            if (!live) SPIN_ID[i] = FREE;
        }
    }

    private static int slotOf(int entityId) {
        for (int i = 0; i < count; i++) if (IDS[i] == entityId) return i;
        return -1;
    }

    // ================================================================ the lance spin (per wearer: an angle and a speed; at most MAX slots)
    private static final int SPIN_N = MAX, FREE = Integer.MIN_VALUE;
    private static final int[] SPIN_ID = new int[SPIN_N];
    private static final float[] SPIN_ANGLE = new float[SPIN_N], SPIN_SPEED = new float[SPIN_N], SPIN_AT = new float[SPIN_N];

    static {
        java.util.Arrays.fill(SPIN_ID, FREE);
    }

    /** The lance's turn of wearer {@code id} (degrees): idle 60 a second, up to 720 while the wearer charges or attacks, back to idle in ten ticks. */
    private static float spin(int id, float now, boolean busy) {
        int at = -1;
        for (int i = 0; i < SPIN_N; i++) if (SPIN_ID[i] == id) { at = i; break; }
        if (at < 0) {
            for (int i = 0; i < SPIN_N; i++) if (SPIN_ID[i] == FREE) { at = i; break; }
            if (at < 0) return now * ModeArmorValkyrieSpin.idleSpeedPerTick();
            SPIN_ID[at] = id;
            SPIN_ANGLE[at] = 0f;
            SPIN_SPEED[at] = ModeArmorValkyrieSpin.idleSpeedPerTick();
            SPIN_AT[at] = now;
        }
        float dt = now - SPIN_AT[at];
        if (dt > 0f) {
            dt = Math.min(dt, 4f);
            SPIN_SPEED[at] = ModeArmorValkyrieSpin.step(SPIN_SPEED[at], busy, dt);
            SPIN_ANGLE[at] = (SPIN_ANGLE[at] + SPIN_SPEED[at] * dt) % 360f;
            SPIN_AT[at] = now;
        }
        return SPIN_ANGLE[at];
    }

    // ================================================================ where the lance sits (part space of the body: px, y down, front -z)
    /** Attached at the right hip, pointing forward and a little down (the preview's "attached at the legs", flattened so it does not go into the floor of a standing wearer). */
    private static final float[] ATTACH = {-1f, 12f, -4f}, ATTACH_DIR = {-0.28f, 0.22f, -0.93f};
    /** The Mermaid look: beside the tail, pointing down and back like the lance below the tail of frame 132. */
    private static final float[] TAIL = {-6.5f, 11f, 0.5f}, TAIL_DIR = {-0.30f, 0.30f, 0.90f};
    /** Floating beside the wearer's right shoulder, pointing forward, the grip near the anchor. */
    private static final float[] FLOAT = {-17f, 2f, -3f}, FLOAT_DIR = {0.02f, -0.05f, -1f};
    private static final float FLOAT_BACK = 0.35f * ModeArmorValkyrieRules.LANCE_LENGTH;
    private static final Quaternionf Q_ATTACH = aim(ATTACH_DIR), Q_TAIL = aim(TAIL_DIR), Q_FLOAT = aim(FLOAT_DIR), Q_SPIN = new Quaternionf();

    private static Quaternionf aim(float[] d) {
        float l = (float) Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        return new Quaternionf().rotationTo(0f, 1f, 0f, d[0] / l, d[1] / l, d[2] / l);
    }

    // ================================================================ the draw
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final int P_BODY = 0, P_HEAD = 1, P_ARM_L = 2, P_ARM_R = 3, P_LEG_L = 4, P_LEG_R = 5, P_LANCE = 6;
    /** One pose per part, captured while the wearer's pose stack stands in the part, drawn pass by pass; cleared after the draw. */
    private static final PoseStack.Pose[] POSE = new PoseStack.Pose[7];
    private static final float[] FLUTTER = new float[5], OUT = new float[3];

    private static void enter(PoseStack pose, ModelPart part) {
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
    }

    /**
     * Draws one wearer. {@code k} = grow-in x fade-out (0..1), {@code now} = game time + partial tick, {@code swing} = limb swing amount (the wings beat harder when running), {@code distSq} = squared distance to the camera
     * (used when the wearer is not in the frame plan).
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> m, AbstractClientPlayer player, float now, float k, float swing, double distSq) {
        if (k <= 0.02f) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int id = player.getId();
        for (int i = 0; i < seen; i++) if (SEEN[i] == id) { stamp = Float.NaN; break; }                   // drawn twice under one clock: a new frame
        ensurePlan(mc, mc.level.getGameTime());
        if (seen < MAX) SEEN[seen++] = id;
        int slot = slotOf(id), level;
        boolean withLance;
        Form f = form;
        if (slot >= 0) {
            level = LEVEL[slot];
            withLance = LANCE[slot];
        } else {                                                                     // not in the plan (an inventory screen, a wearer the scan did not reach): admitted on the spot, under the same pool
            boolean want = lance != LanceMode.OFF && !ModeArmorValkyrieWorld.holdsLance(player);
            ONE_LEVEL[0] = ModeArmorValkyrieBudget.NONE;
            ONE_LANCE[0] = false;
            int q = ModeArmorValkyrieBudget.place(distSq, f, want, used, pool, ONE_LEVEL, ONE_LANCE, 0);
            level = ONE_LEVEL[0];
            withLance = ONE_LANCE[0];
            used += q;
        }
        if (level < 0) return;
        Piece[] pieces = ModeArmorValkyrieRules.worn(f);
        int a256 = Math.min(256, (int) (k * 256f));
        capture(pose, m);
        if (withLance) lancePose(pose, m, f, now, ModeArmorValkyrieWorld.busy(player), id);
        ModeArmorValkyrieRules.flutter(now, swing, FLUTTER);
        int lit = lift(light);
        for (int pass = 0; pass < ModeArmorValkyrieMesh.PASSES; pass++) {
            if (pass == ModeArmorValkyrieMesh.SCALES && f != Form.MERMAID) continue;
            VertexConsumer vc = null;
            int pl = pass == ModeArmorValkyrieMesh.OPAQUE || pass == ModeArmorValkyrieMesh.SCALES ? light : pass == ModeArmorValkyrieMesh.TRANSLUCENT ? lit : FULL;
            for (int pi = 0; pi < pieces.length; pi++) {
                Piece p = pieces[pi];
                for (int c = 0; c < p.copies; c++) {
                    ModeArmorValkyrieMesh mesh = ModeArmorValkyrieMesh.armour(p, level, c == 1);
                    int from = mesh.passFrom[pass], to = mesh.passFrom[pass + 1];
                    if (from == to) continue;
                    PoseStack.Pose pp = POSE[poseOf(p, c)];
                    if (pp == null) continue;
                    if (vc == null) vc = buffer(buffers, pass);
                    if (p.bone.wing) {
                        ModeArmorValkyrieMesh.Wings.set(p, c == 1, FLUTTER[0], FLUTTER[1], FLUTTER[2], FLUTTER[3], FLUTTER[4], 0.4f + 0.6f * k);
                        emitWing(vc, pp, mesh, from, to, pl, a256);
                    } else emit(vc, pp, mesh, from, to, pl, a256, pass);
                }
            }
            if (withLance) {
                ModeArmorValkyrieMesh mesh = ModeArmorValkyrieMesh.lance(level);
                int from = mesh.passFrom[pass], to = mesh.passFrom[pass + 1];
                if (from != to && pass != ModeArmorValkyrieMesh.SCALES) {
                    if (vc == null) vc = buffer(buffers, pass);
                    emit(vc, POSE[P_LANCE], mesh, from, to, pl, a256, pass);
                }
            }
        }
        java.util.Arrays.fill(POSE, null);
    }

    /** The part a copy of a piece is fitted to: arms and legs by copy (0 left, 1 right), the head, and the body (which the wings hinge on too). */
    private static int poseOf(Piece p, int copy) {
        return switch (p.bone) {
            case ARM -> copy == 0 ? P_ARM_L : P_ARM_R;
            case LEG -> copy == 0 ? P_LEG_L : P_LEG_R;
            case HEAD -> P_HEAD;
            default -> P_BODY;
        };
    }

    private static void capture(PoseStack pose, PlayerModel<AbstractClientPlayer> m) {
        enter(pose, m.body);
        POSE[P_BODY] = pose.last();
        pose.popPose();
        enter(pose, m.head);
        POSE[P_HEAD] = pose.last();
        pose.popPose();
        enter(pose, m.leftArm);
        POSE[P_ARM_L] = pose.last();
        pose.popPose();
        enter(pose, m.rightArm);
        POSE[P_ARM_R] = pose.last();
        pose.popPose();
        enter(pose, m.leftLeg);
        POSE[P_LEG_L] = pose.last();
        pose.popPose();
        enter(pose, m.rightLeg);
        POSE[P_LEG_R] = pose.last();
        pose.popPose();
    }

    /** The pose of the lance: on the body part, at the spot of the lance mode, aimed along its direction, turned about its own axis. */
    private static void lancePose(PoseStack pose, PlayerModel<AbstractClientPlayer> m, Form f, float now, boolean busy, int id) {
        enter(pose, m.body);
        float[] at;
        Quaternionf q;
        float back = 0f;
        if (lance == LanceMode.FLOATING) {
            at = FLOAT;
            q = Q_FLOAT;
            back = FLOAT_BACK;
        } else if (f == Form.MERMAID) {
            at = TAIL;
            q = Q_TAIL;
        } else {
            at = ATTACH;
            q = Q_ATTACH;
        }
        float bob = lance == LanceMode.FLOATING ? 0.8f * Mth.sin(now * 0.08f) : 0f;
        pose.translate(at[0], at[1] + bob, at[2]);
        pose.mulPose(q);
        pose.mulPose(Q_SPIN.rotationY(spin(id, now, busy) * (float) (Math.PI / 180.0)));
        pose.translate(0f, -back, 0f);
        POSE[P_LANCE] = pose.last();
        pose.popPose();
    }

    /** The light of the translucent pass: the wearer's own, lifted to at least block level 9, so the veils never go black in the dark. */
    private static int lift(int light) {
        int block = Math.max((light >> 4) & 15, 9), sky = (light >> 20) & 15;
        return sky << 20 | block << 4;
    }

    // ================================================================ the vertices
    private static void emit(VertexConsumer vc, PoseStack.Pose p, ModeArmorValkyrieMesh m, int from, int to, int light, int a256, int pass) {
        float[] pos = m.pos, uv = m.uv, nr = m.nrm;
        int[] col = m.argb;
        boolean solid = pass == ModeArmorValkyrieMesh.OPAQUE || pass == ModeArmorValkyrieMesh.SCALES;
        for (int i = from; i < to; i++) {
            int c = col[i];
            int a = solid ? 255 : ((c >>> 24) * a256) >> 8;
            vc.addVertex(p, pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]).setColor((c >> 16) & 255, (c >> 8) & 255, c & 255, a).setUv(uv[i * 2], uv[i * 2 + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nr[i * 3], nr[i * 3 + 1], nr[i * 3 + 2]);
        }
    }

    private static void emitWing(VertexConsumer vc, PoseStack.Pose p, ModeArmorValkyrieMesh m, int from, int to, int light, int a256) {
        float[] uv = m.uv, nr = m.nrm;
        int[] col = m.argb;
        for (int i = from; i < to; i++) {
            ModeArmorValkyrieMesh.Wings.apply(m, i, OUT);
            int c = col[i];
            int a = ((c >>> 24) * a256) >> 8;
            vc.addVertex(p, OUT[0], OUT[1], OUT[2]).setColor((c >> 16) & 255, (c >> 8) & 255, c & 255, a).setUv(uv[i * 2], uv[i * 2 + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nr[i * 3], nr[i * 3 + 1], nr[i * 3 + 2]);
        }
    }

    // ================================================================ the first person arm
    /**
     * Draws the armour of one arm at full detail (sleeve, shoulder cap, cuff and, in the Valkyrie look, the sleeve veil): what a hand-render hook shows in first person. {@code arm} is the player model's arm part
     * (its rotation already zeroed by the caller), {@code right} says which. Costs at most 69 quads, outside the frame pool (only the local player calls it, only in first person, where the body layers are not drawn).
     */
    public static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, ModelPart arm, boolean right, float k) {
        Form f = form;
        Piece[] pieces = ModeArmorValkyrieRules.worn(f);
        int a256 = Math.min(256, (int) (k * 256f)), lit = lift(light);
        enter(pose, arm);
        PoseStack.Pose pp = pose.last();
        pose.popPose();
        for (int pass = 0; pass < ModeArmorValkyrieMesh.PASSES; pass++) {
            if (pass == ModeArmorValkyrieMesh.SCALES) continue;
            VertexConsumer vc = null;
            int pl = pass == ModeArmorValkyrieMesh.OPAQUE ? light : pass == ModeArmorValkyrieMesh.TRANSLUCENT ? lit : FULL;
            for (int pi = 0; pi < pieces.length; pi++) {
                Piece p = pieces[pi];
                if (p.bone != ModeArmorValkyrieRules.Bone.ARM) continue;
                ModeArmorValkyrieMesh mesh = ModeArmorValkyrieMesh.armour(p, 0, right);
                int from = mesh.passFrom[pass], to = mesh.passFrom[pass + 1];
                if (from == to) continue;
                if (vc == null) vc = buffer(buffers, pass);
                emit(vc, pp, mesh, from, to, pl, a256, pass);
            }
        }
    }
}
