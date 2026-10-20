package com.newuniverse.nusmp.client.relics.worn.lib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.relics.fx.FxQuality;
import com.newuniverse.nusmp.client.relics.fx.WornBudget;
import com.newuniverse.nusmp.client.relics.fx.WornFxPlan;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Draws one player's worn relics from the model library, three passes over the whole list so each render type is fetched once:
 * <ol>
 *   <li><b>Solid</b> (entityCutoutNoCull over the library atlas, lit): every quad of every model, tinted per quad from the relic's palette slot.</li>
 *   <li><b>Emissive</b> (RenderType.eyes over the atlas): the gems, runes and flames of the models (pulsing), and the procedural motes, rune ring and halo of the
 *       grade ({@link WornFxDraw}).</li>
 *   <li><b>Shell</b> (entityTranslucentEmissive over the scrolling shimmer texture): the shell parts pushed a hair out along their normals, faint at the middle and
 *       bright towards the silhouette (the rim glow), with the shimmer sliding over them.</li>
 * </ol>
 * The vertex budget of the quality option decides what is drawn ({@link WornBudget}); a wearer beyond 16 blocks gets the models alone. The loop writes vertices through
 * scratch matrices and vectors: nothing is allocated per frame except what the pose stack itself does (one push per piece).
 * <p>
 * Coordinates: the model's own pixels in layer space; a piece's matrix is the pose of its body part ({@code ModelPart#translateAndRotate}, so walk swing, sneak and the
 * attack swing carry over) times 1/16. A mirrored piece flips x on both the matrix and the normal matrix.
 */
public final class WornLibDraw {
    private WornLibDraw() {}

    public static final ResourceLocation ATLAS = WornModels.rl("textures/relic_worn/atlas.png"), SHIMMER = WornModels.rl("textures/relic_worn/shimmer.png");
    private static final float PX = 1f / 16f, PUSH = 0.18f;
    private static final int FULL = LightTexture.FULL_BRIGHT, MAX_SETS = 24, POOL = 72;

    /** One piece being drawn this frame: reused from a pool. */
    private static final class Slot {
        WornMesh mesh;
        boolean mirror, first;
        int set;
        int[] colors;
        final Matrix4f m = new Matrix4f();
        final Matrix3f n = new Matrix3f();
        WornAnchor anchor;
    }

    private static final Slot[] SLOTS = new Slot[POOL];
    private static final int[] BASE = new int[MAX_SETS], EXTRA = new int[MAX_SETS], INDEX = new int[MAX_SETS], BUDGET_BASE = new int[MAX_SETS];
    private static final boolean[] DRAWN = new boolean[MAX_SETS], EXTRAS = new boolean[MAX_SETS], MOTES = new boolean[MAX_SETS], USE_LOD = new boolean[MAX_SETS], NO_ADD = new boolean[MAX_SETS];
    /** The vertices a relic may cost the budget of the effects at most: the old per-ensemble cap, so an upgraded relic's big model does not starve the effects of its wearer's other relics. */
    private static final int LEGACY_BUDGET = 2600;
    private static final ResourceLocation[] DEFAULT_SKIN = {ATLAS, ATLAS};
    private static final java.util.Map<String, ResourceLocation[]> SKINS = new java.util.HashMap<>();

    /** The albedo texture {@code [0]} and the emissive glow map {@code [1]} a mesh samples: its own skin (1.102.0), else the shared atlas twice. Cached per skin name: no allocation per frame. */
    static ResourceLocation[] skinOf(WornMesh m) {
        if (m.skin == null) return DEFAULT_SKIN;
        ResourceLocation[] t = SKINS.get(m.skin);
        if (t == null) {
            t = new ResourceLocation[]{WornModels.rl("textures/relic_worn/god/" + m.skin + ".png"), WornModels.rl("textures/relic_worn/god/" + m.skin + "_glow.png")};
            SKINS.put(m.skin, t);
        }
        return t;
    }
    private static final WornFxPlan[] PLAN_OF = new WornFxPlan[MAX_SETS];
    private static final WornFxPlan[][][] PLANS = new WornFxPlan[8][FxQuality.values().length][2];
    private static final Matrix4f SM = new Matrix4f();
    private static final Matrix3f SN = new Matrix3f();
    private static final Vector3f V = new Vector3f(), NV = new Vector3f();
    private static final WornAnim.Xf XF = new WornAnim.Xf();

    static {
        for (int i = 0; i < POOL; i++) SLOTS[i] = new Slot();
    }

    private static WornFxPlan plan(int tier, FxQuality q, boolean near) {
        int t = Math.min(7, Math.max(0, tier));
        WornFxPlan p = PLANS[t][q.ordinal()][near ? 1 : 0];
        if (p == null) {
            p = WornFxPlan.of(t, q, near ? 0.0 : WornFxPlan.CULL_RANGE * WornFxPlan.CULL_RANGE);
            PLANS[t][q.ordinal()][near ? 1 : 0] = p;
        }
        return p;
    }

    private static ModelPart part(HumanoidModel<?> m, WornAnchor a) {
        return switch (a) {
            case HEAD -> m.head;
            case BODY -> m.body;
            case RIGHT_ARM -> m.rightArm;
            case LEFT_ARM -> m.leftArm;
            case RIGHT_LEG -> m.rightLeg;
            case LEFT_LEG -> m.leftLeg;
        };
    }

    /**
     * Draws the given relics (best grade first), all in the pose of {@code model}.
     *
     * @param distSq distance from the camera to the wearer, squared
     * @param limb   the wearer's stride, 0 standing .. 1 running
     */
    public static void draw(List<WornLibrary.Ensemble> sets, PoseStack pose, MultiBufferSource buffers, int light, HumanoidModel<?> model,
                            boolean crouching, double distSq, float age, float limb, FxQuality q) {
        draw(sets, pose, buffers, light, model, crouching, distSq, age, limb, q, 0f);
    }

    /**
     * The same for a wearer with slim arms ({@code armInset} 0.5, see {@code WornPlan#SLIM_INSET}): every relic piece anchored on an arm is moved that many model pixels toward the
     * body, because a slim arm is one pixel narrower on its outer side (an armour item's own pieces are made for the arm and are not moved). 0 for wide arms.
     */
    public static void draw(List<WornLibrary.Ensemble> sets, PoseStack pose, MultiBufferSource buffers, int light, HumanoidModel<?> model,
                            boolean crouching, double distSq, float age, float limb, FxQuality q, float armInset) {
        int n = Math.min(sets.size(), MAX_SETS);
        if (n == 0) return;
        boolean near = distSq <= WornFxPlan.EXTRAS_RANGE * WornFxPlan.EXTRAS_RANGE;
        double lodSq = WornLibrary.Ensemble.LOD_DISTANCE * WornLibrary.Ensemble.LOD_DISTANCE, addSq = WornLibrary.Ensemble.ADDITIVE_CUTOFF * WornLibrary.Ensemble.ADDITIVE_CUTOFF;
        for (int i = 0; i < n; i++) {
            WornLibrary.Ensemble e = sets.get(i);
            WornFxPlan p = plan(e.tier, q, near);
            PLAN_OF[i] = p;
            // 1.102.0: an upgraded relic is drawn as its low detail models beyond 12 blocks and at the low quality, and its additive passes stop beyond 24 blocks
            boolean lod = e.upgraded && !e.lodPieces.isEmpty() && (distSq > lodSq || q == FxQuality.LOW);
            USE_LOD[i] = lod;
            NO_ADD[i] = e.upgraded && distSq > addSq;
            BASE[i] = lod ? e.lodVerts : e.baseVerts;
            BUDGET_BASE[i] = e.upgraded ? Math.min(BASE[i], LEGACY_BUDGET) : BASE[i];
            MOTES[i] = false;
            EXTRA[i] = (p.usesShell() ? e.shellVerts : 0);
        }
        int ranked = 0;
        for (int i = 0; i < n && ranked < 2; i++) {                                           // motes, rune ring and halo: the two best relics, and the chest piece of an armour set
            WornLibrary.Ensemble e = sets.get(i);
            if (e.exact && !e.slotKey.equals(ArmorLooks.Slot.CHEST.key()) && !e.slotKey.equals("MODE")) continue;
            MOTES[i] = true;
            EXTRA[i] += PLAN_OF[i].fxVertices();
            ranked++;
        }
        int models = 0;
        for (int i = 0; i < n; i++) models += BUDGET_BASE[i];
        WornBudget.allocate(BUDGET_BASE, EXTRA, n, Math.max(q.vertexCap, models), DRAWN, EXTRAS);      // every relic's model is always drawn (none hides another); only the extras are budgeted
        for (int i = 0; i < n; i++) {                                                          // the place of each among the relics of its slot (by id: stable while grades change)
            int idx = 0;
            for (int j = 0; j < n; j++) if (j != i && sets.get(j).slotKey.equals(sets.get(i).slotKey) && sets.get(j).id.compareTo(sets.get(i).id) < 0) idx++;
            INDEX[i] = idx;
        }

        // ---- pass 1: solid (one buffer per skin: a relic with its own skin samples its own texture)
        VertexConsumer vc;
        int used = 0;
        for (int i = 0; i < n && used < POOL; i++) {
            if (!DRAWN[i]) continue;
            WornLibrary.Ensemble e = sets.get(i);
            List<WornLibrary.Resolved> pieces = USE_LOD[i] ? e.lodPieces : e.pieces;
            for (int k = 0; k < pieces.size() && used < POOL; k++) {
                WornLibrary.Resolved r = pieces.get(k);
                Slot s = SLOTS[used++];
                s.mesh = r.mesh();
                s.mirror = r.mirror();
                s.first = k == 0;
                s.set = i;
                s.colors = e.colors;
                s.anchor = r.anchor();
                pose.pushPose();
                part(model, r.anchor()).translateAndRotate(pose);
                pose.scale(PX, PX, PX);
                s.m.set(pose.last().pose());
                s.n.set(pose.last().normal());
                pose.popPose();
                WornPlacer.Placement pl = e.exact ? WornPlacer.Placement.NONE      // an armour item or a mode: the garment's own size, where the garment stands
                        : WornPlacer.fit(r.anchor(), r.mesh().name).then(WornPlacer.place(e.slotKey, INDEX[i]));      // a relic: the smaller body fit, then the place among its slot-mates
                if (!e.exact && armInset != 0f && (r.anchor() == WornAnchor.RIGHT_ARM || r.anchor() == WornAnchor.LEFT_ARM)) {      // slim arms: toward the body (+x for the right arm, -x for the left)
                    pl = pl.then(new WornPlacer.Placement(r.anchor() == WornAnchor.RIGHT_ARM ? armInset : -armInset, 0f, 0f, 0f, 1f, false));
                }
                if (pl != WornPlacer.Placement.NONE) {
                    s.m.translate(pl.dx(), pl.dy(), pl.dz()).rotateY((float) Math.toRadians(pl.rotY())).scale(pl.scale());
                    s.n.rotateY((float) Math.toRadians(pl.rotY()));
                    s.mirror ^= pl.mirror();
                }
                vc = buffers.getBuffer(RenderType.entityCutoutNoCull(skinOf(s.mesh)[0]));
                solid(vc, s, light, age, limb, crouching);
            }
        }
        if (used == 0) return;

        // ---- pass 2: emissive parts (an upgraded relic samples its glow map) and the procedural effects
        float hot = 0.78f + 0.22f * (float) Math.sin(age * 0.14f);
        boolean shell = false;
        for (int u = 0; u < used; u++) {
            Slot s = SLOTS[u];
            WornFxPlan p = PLAN_OF[s.set];
            if (NO_ADD[s.set]) continue;                                                        // beyond 24 blocks an upgraded relic draws no additive pass at all
            if (s.mesh.emitQuads > 0) {
                vc = buffers.getBuffer(RenderType.eyes(skinOf(s.mesh)[1]));
                glow(vc, s, age, limb, crouching, hot * (1f + p.pulse() * (float) Math.sin(age * 0.09f)));
            }
            if (EXTRAS[s.set]) {
                if (s.first && MOTES[s.set]) {
                    vc = buffers.getBuffer(RenderType.eyes(ATLAS));
                    int rgb = s.colors[WornMesh.GLOW];
                    if (p.motes() > 0) WornFxDraw.motes(vc, s.m, s.anchor, rgb, p.motes(), age, p.pulse());
                    if (p.runeRing()) WornFxDraw.ring(vc, s.m, s.anchor, rgb, age, p.pulse());
                    if (p.halo()) WornFxDraw.halo(vc, s.m, s.anchor, rgb, age, p.pulse());
                }
                if (p.usesShell() && s.mesh.shellQuads > 0) shell = true;
            }
        }

        // ---- pass 3: the rim glow and the shimmer
        if (!shell) return;
        vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(SHIMMER));
        for (int u = 0; u < used; u++) {
            Slot s = SLOTS[u];
            WornFxPlan p = PLAN_OF[s.set];
            if (NO_ADD[s.set]) continue;
            if (EXTRAS[s.set] && p.usesShell() && s.mesh.shellQuads > 0) shellPass(vc, s, age, limb, crouching, p.rim(), p.shimmer());
        }
    }

    /** Where each anchor's pivot stands in the rest pose of the player model (layer space: model pixels, y down), for drawing an ensemble without a player. */
    private static float[] rest(WornAnchor a) {
        return switch (a) {
            case RIGHT_ARM -> new float[]{-5f, 2f, 0f};
            case LEFT_ARM -> new float[]{5f, 2f, 0f};
            case RIGHT_LEG -> new float[]{-1.9f, 12f, 0f};
            case LEFT_LEG -> new float[]{1.9f, 12f, 0f};
            default -> new float[]{0f, 0f, 0f};
        };
    }

    /**
     * Draws an ensemble as an object (an armour item in the hand, on the ground or in a frame): the pieces at the rest pose of the player model, solid and emissive, no
     * shell pass. {@code pose} must already be scaled so that one unit is one model pixel and centred on the ensemble ({@link #itemFit}).
     */
    public static void drawItem(WornLibrary.Ensemble e, PoseStack pose, MultiBufferSource buffers, int light, float age) {
        if (e == null || e.pieces.isEmpty()) return;
        int used = 0;
        VertexConsumer vc;
        for (int k = 0; k < e.pieces.size() && used < POOL; k++) {
            WornLibrary.Resolved r = e.pieces.get(k);
            Slot s = SLOTS[used++];
            s.mesh = r.mesh();
            s.mirror = r.mirror();
            s.first = k == 0;
            s.set = 0;
            s.colors = e.colors;
            s.anchor = r.anchor();
            float[] o = rest(r.anchor());
            pose.pushPose();
            pose.translate(o[0], o[1], o[2]);
            s.m.set(pose.last().pose());
            s.n.set(pose.last().normal());
            pose.popPose();
            vc = buffers.getBuffer(RenderType.entityCutoutNoCull(skinOf(s.mesh)[0]));
            solid(vc, s, light, age, 0f, false);
        }
        float hot = 0.78f + 0.22f * (float) Math.sin(age * 0.14f);
        for (int u = 0; u < used; u++) {
            if (SLOTS[u].mesh.emitQuads <= 0) continue;
            vc = buffers.getBuffer(RenderType.eyes(skinOf(SLOTS[u].mesh)[1]));
            glow(vc, SLOTS[u], age, 0f, false, hot);
        }
    }

    /**
     * The transform that fits an ensemble into the unit cube of an item model (origin at the corner, as the custom item renderer is given it) with its largest side
     * {@code size} blocks: after it, one unit is one model pixel. The piece is turned upright (the library's layer space has y down).
     */
    public static void itemFit(WornLibrary.Ensemble e, PoseStack pose, float size) {
        float[] b = e.bounds();
        float ext = Math.max(0.001f, Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2])));
        float sc = size / ext;
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f));
        pose.scale(sc, sc, sc);
        pose.translate(-(b[0] + b[3]) / 2f, -(b[1] + b[4]) / 2f, -(b[2] + b[5]) / 2f);
    }

    // ================================================================ the passes
    /** The matrices of a slot with the group's motion applied on top (group null: none). */
    private static void setup(Slot s, WornMesh.Group g, float age, float limb, boolean crouching) {
        SM.set(s.m);
        SN.set(s.n);
        if (s.mirror) {
            SM.scale(-1f, 1f, 1f);
            SN.scale(-1f, 1f, 1f);
        }
        if (g == null) return;
        WornAnim.eval(g, age, limb, crouching, XF);
        SM.translate(g.px() + XF.tx, g.py() + XF.ty, g.pz() + XF.tz).rotateX(XF.rx).rotateY(XF.ry).rotateZ(XF.rz).scale(XF.s).translate(-g.px(), -g.py(), -g.pz());
        SN.rotateX(XF.rx).rotateY(XF.ry).rotateZ(XF.rz);
    }

    private static boolean wave(WornMesh.Group g) { return g != null && g.kind() == WornMesh.Kind.WAVE; }

    private static void solid(VertexConsumer vc, Slot s, int light, float age, float limb, boolean crouching) {
        WornMesh m = s.mesh;
        for (int g = 0; g < m.groups.length; g++) {
            int q0 = m.groupStart[g], q1 = m.groupStart[g + 1];
            if (q0 == q1) continue;
            WornMesh.Group gr = m.groups[g];
            setup(s, gr, age, limb, crouching);
            boolean w = wave(gr);
            for (int i = q0; i < q1; i++) {
                if ((m.flags[i] & WornMesh.ADD) != 0) continue;                                // 1.102.0: a halo or soft sprite exists only in the additive pass
                int c = s.colors[m.slot[i]];
                if ((m.flags[i] & WornMesh.EMIT) != 0) c = WornPalette.scale(c, 0.55f);        // a lit stone is dark glass until the glow pass adds its light
                for (int k = 0; k < 4; k++) vertex(vc, m, i, k, c >> 16 & 255, c >> 8 & 255, c & 255, 255, light, w ? gr : null, age);
            }
        }
    }

    private static void glow(VertexConsumer vc, Slot s, float age, float limb, boolean crouching, float hot) {
        WornMesh m = s.mesh;
        for (int g = 0; g < m.groups.length; g++) {
            int q0 = m.groupStart[g], q1 = m.groupStart[g + 1];
            if (q0 == q1) continue;
            WornMesh.Group gr = m.groups[g];
            setup(s, gr, age, limb, crouching);
            boolean w = wave(gr);
            for (int i = q0; i < q1; i++) {
                int f = m.flags[i];
                if ((f & (WornMesh.EMIT | WornMesh.LIT | WornMesh.ADD)) == 0) continue;
                float k = (f & WornMesh.HOT) != 0 ? hot : 0.9f;
                int c = s.colors[(f & (WornMesh.EMIT | WornMesh.ADD)) != 0 ? m.slot[i] : WornMesh.GLOW];       // 1.102.0: a lit-only quad (engraved runes, veins) glows in the glow colour
                int r = Math.min(255, (int) ((c >> 16 & 255) * k)), gg = Math.min(255, (int) ((c >> 8 & 255) * k)), b = Math.min(255, (int) ((c & 255) * k));
                // 1.99.0: RenderType.eyes culls back faces, and a mirrored piece (the left arm's copy of a right arm model) has its winding reversed by the x flip: its glowing parts
                // were culled away entirely. Writing the corners in the other order turns them front again.
                for (int j = 0; j < 4; j++) vertex(vc, m, i, WornMesh.corner(s.mirror, j), r, gg, b, 255, FULL, w ? gr : null, age);
            }
        }
    }

    private static void shellPass(VertexConsumer vc, Slot s, float age, float limb, boolean crouching, float rim, float shimmer) {
        WornMesh m = s.mesh;
        int rgb = s.colors[WornMesh.GLOW], r = rgb >> 16 & 255, gg = rgb >> 8 & 255, b = rgb & 255;
        for (int g = 0; g < m.groups.length; g++) {
            int q0 = m.groupStart[g], q1 = m.groupStart[g + 1];
            if (q0 == q1) continue;
            WornMesh.Group gr = m.groups[g];
            setup(s, gr, age, limb, crouching);
            boolean w = wave(gr);
            for (int i = q0; i < q1; i++) {
                if ((m.flags[i] & WornMesh.SHELL) == 0) continue;
                for (int k = 0; k < 4; k++) {
                    int o = i * 12 + k * 3;
                    float nx = m.nrm[o], ny = m.nrm[o + 1], nz = m.nrm[o + 2];
                    float x = m.pos[o] + nx * PUSH, y = m.pos[o + 1] + ny * PUSH, z = m.pos[o + 2] + nz * PUSH;
                    if (w) z += WornAnim.waveOffset(gr, y, age);
                    SM.transformPosition(x, y, z, V);
                    SN.transform(nx, ny, nz, NV).normalize();
                    float len = (float) Math.sqrt(V.x * V.x + V.y * V.y + V.z * V.z);
                    float facing = len > 1e-4f ? Math.abs(NV.x * V.x + NV.y * V.y + NV.z * V.z) / len : 1f;
                    float edge = 1f - Math.min(1f, facing);
                    int a = Math.min(255, Math.max(0, (int) ((rim * edge * edge * 1.6f + shimmer) * 255f)));
                    vc.addVertex(V.x, V.y, V.z).setColor(r, gg, b, a).setUv(x * 0.05f + z * 0.03f + age * 0.004f, y * 0.05f - age * 0.01f)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(NV.x, NV.y, NV.z);
                }
            }
        }
    }

    /** One vertex of quad {@code i}, corner {@code k}, through the scratch matrices. */
    private static void vertex(VertexConsumer vc, WornMesh m, int i, int k, int r, int g, int b, int a, int light, WornMesh.Group wave, float age) {
        int o = i * 12 + k * 3;
        float x = m.pos[o], y = m.pos[o + 1], z = m.pos[o + 2];
        if (wave != null) z += WornAnim.waveOffset(wave, y, age);
        SM.transformPosition(x, y, z, V);
        SN.transform(m.nrm[o], m.nrm[o + 1], m.nrm[o + 2], NV).normalize();
        vc.addVertex(V.x, V.y, V.z).setColor(r, g, b, a).setUv(m.uv[i * 8 + k * 2], m.uv[i * 8 + k * 2 + 1])
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(NV.x, NV.y, NV.z);
    }
}
