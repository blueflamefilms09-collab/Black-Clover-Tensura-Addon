package com.newuniverse.nusmp.client.relics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.fx.WornFxPlan;
import com.newuniverse.nusmp.client.relics.worn.ArmorSlotPlan;
import com.newuniverse.nusmp.client.relics.worn.ArmorTrace;
import com.newuniverse.nusmp.client.relics.worn.ArmorVisible;
import com.newuniverse.nusmp.client.relics.worn.WornCtx;
import com.newuniverse.nusmp.client.relics.worn.WornExec;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import com.newuniverse.nusmp.client.relics.worn.WornTrace;
import com.newuniverse.nusmp.client.relics.worn.WornRenderer;
import com.newuniverse.nusmp.client.relics.worn.WornRenderers;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorGlow;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorItems;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibDraw;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.client.relics.worn.lib.WornPlacer;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws the relics a player wears. Three tiers, best first:
 * <ol>
 *   <li>A relic with its own renderer ({@link WornRenderers}: the three boss relics) is drawn by it, after everything else.</li>
 *   <li>A relic the model library knows ({@link WornLibrary}: a signature entry by id, else the models of its slot and look) is drawn as lofted, smooth-shaded, textured
 *       3D models tinted with its palette, with the effects of its grade ({@link WornLibDraw}: rim glow, shimmer, orbiting motes, rune ring, halo) inside the vertex budget
 *       of the client option {@code relicfx.quality}. This covers the Curios slots and the head, chest and feet armour relics; the models follow the body parts of
 *       the player model, so they follow every animation.</li>
 *   <li>Anything else keeps the old plain boxes (a pendant and chain at the neck, a cloak on the back, a belt, forearm cuffs, a charm at the hip) that the mod had
 *       before the library, so no relic is ever drawn as nothing.</li>
 * </ol>
 * An armour relic that keeps its painted armour box also gets the emissive pass of its glow channel ({@link ArmorGlow}). Wearers beyond 32 blocks are not drawn. Reads
 * the player's relics through the 10-tick cache of {@link Relics}. Without Curios nothing is worn in the Curios slots and nothing is drawn for them.
 */
public class RelicWornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    /** The relics of the player being drawn that the library knows, best grade first (the render thread draws one player at a time). */
    private static final List<WornLibrary.Ensemble> ENSEMBLES = new ArrayList<>(12);
    private static final double[] DIST = new double[1];

    public RelicWornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_C_RELIC_WORN_LAYER = com.newuniverse.nusmp.perf.PerfProbe.section("c.relic.worn_layer");   // 1.99.0: performance probe (off by default: one flag read)

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            render0(pose, buffers, light, p, limbSwing, limbSwingAmount, partial, age, netHeadYaw, headPitch);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_C_RELIC_WORN_LAYER, t0);
        }
    }

    private void render0(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        RelicTextures.refreshIfOptionChanged();                                        // 1.110.0: visual.upgrade flipped: the armour layers are repainted once
        boolean slim = p.getSkin().model() == PlayerSkin.Model.SLIM;
        WornPlan.View view = WornExec.viewOf(p);
        long now = p.level().getGameTime();
        if (p.isSpectator() || p.isInvisible()) {                                      // nothing worn is drawn on an invisible body
            WornTrace.reached(p.getUUID(), now, "player", slim, view, 0, null, 0);
            return;
        }
        boolean far = WornExec.tooFar(p, DIST);
        double distSq = DIST[0];
        ENSEMBLES.clear();
        if (!far) {
            ArmorItems.collect(p, ENSEMBLES);                                          // the mod's own armour (the robe sets ...) as 3D pieces: drawn with the relics, never hidden by the relic switches
            sortEnsembles();
        }
        List<Relics.Located> found = Relics.locatedList(p);                            // hands, armour, Curios and (client) what the server said: see Relics#scan, WornSync
        if (found.isEmpty() && ENSEMBLES.isEmpty()) {
            WornTrace.reached(p.getUUID(), now, "player", slim, view, 0, null, 0);
            return;
        }
        boolean models = RelicFxOptions.models();
        boolean hideArmour = RelicFxOptions.hideArmourRelics();
        List<CuriosLink.RenderFlag> flags = WornExec.needsFlags(found) ? CuriosLink.flags(p) : null;      // the eye icons of the Curios screen, read once per wearer
        List<WornPlan.Entry> entries = WornExec.entries(found, flags, true);
        WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(slim, view, false, false, far, models, hideArmour, WornPlan.Fallback.BOXES), entries);
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        WornLibDraw.resetDrawRecord();                                                 // 1.111.0: from here on the library records what it really draws, for the armour failsafe below
        CUSTOM_RAN.clear();
        if (!plan.isEmpty()) {
            VertexConsumer vc = null;
            for (WornPlan.Item it : plan.plain()) {                                    // the old plain boxes: a Curios relic the library cannot dress, or every relic with relicfx.models off
                RelicDef d = found.get(it.index()).def();
                RelicSlot slot = d.slot();
                if (vc == null) vc = buffers.getBuffer(RenderType.entityCutoutNoCull(RelicTextures.WHITE));
                int main = d.paletteMain(), trim = d.paletteTrim(), glow = d.paletteGlow();
                boolean shine = d.grade().atLeast(Grade.GOD);
                WornPlacer.Placement pl = WornPlacer.place(slot.name(), it.slotIndex());
                if (slot == RelicSlot.NECK) neck(pose, vc, light, model.body, d.look(), main, trim, glow, pl);
                else if (slot == RelicSlot.BACK) back(pose, vc, light, model.body, d.look(), main, trim, glow, p.isCrouching(), pl);
                else if (slot == RelicSlot.BELT) belt(pose, vc, light, model.body, main, trim, glow, pl);
                else if (slot == RelicSlot.HANDS) { hands(pose, vc, light, model.rightArm, true, main, trim, glow, pl); hands(pose, vc, light, model.leftArm, false, main, trim, glow, pl); }
                else if (slot == RelicSlot.CHARM) charm(pose, vc, light, model.body, main, trim, glow, shine, pl);
            }
            WornExec.collect(plan, found, ENSEMBLES);                                  // the library models and the floating charms, best grade first
            drawEnsembles(pose, buffers, light, model, p.isCrouching(), distSq, age, limbSwingAmount, plan.armInset());
            for (WornPlan.Item it : plan.all()) {                                      // the emissive pass of the painted armour boxes
                Relics.Located l = found.get(it.index());
                RelicDef d = l.def();
                if (l.where() == Relics.Where.ARMOUR && d.slot().isArmor() && ArmorSlotPlan.of(it).glow() && RelicTextures.hasArmorGlow(d.id()))      // 1.110.0: only over a painted layer (a model that replaces it carries its own glow)
                    ArmorGlow.draw(pose, buffers, model, d.slot(), RelicTextures.armorGlow(d.id()), 0.75f + 0.25f * (float) Math.sin(age * 0.09f));
            }
            boolean first = view == WornPlan.View.SELF_FIRST;
            for (WornPlan.Item it : plan.custom()) drawCustom(found.get(it.index()), pose, buffers, light, model, p, partial, age, netHeadYaw, headPitch, first);
        } else if (!ENSEMBLES.isEmpty()) {                                             // only the mod's own armour pieces
            drawEnsembles(pose, buffers, light, model, p.isCrouching(), distSq, age, limbSwingAmount, plan.armInset());
        }
        int rescued = armourFailsafe(pose, buffers, light, model, p, now, found, plan);   // 1.111.0: an armour relic is never invisible: if nothing of it was drawn by any path, its plain painted boxes are
        WornTrace.reached(p.getUUID(), now, "player", slim, view, found.size(), plan, ENSEMBLES.size(), WornLibDraw.setsDrawn() + CUSTOM_RAN.size() + plan.plain().size() + rescued);
        ENSEMBLES.clear();
    }

    /** The relics whose own renderer ran without an error in the frame being drawn (the render thread draws one wearer at a time), and those whose renderer failed once (switched off for the session). */
    private static final Set<String> CUSTOM_RAN = new HashSet<>(), CUSTOM_FAILED = new HashSet<>();
    private static final Set<String> DRAW_FAILED = new HashSet<>();
    /** Vanilla's plain white, for the failsafe boxes: it exists in every game, even when this mod's own white pixel was never registered. */
    private static final net.minecraft.resources.ResourceLocation PLAIN = net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /** The model library's draw, with the pose stack put back and the error logged (once) if it throws: what it did not draw is then caught by {@link #armourFailsafe}. */
    private static void drawEnsembles(PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> model, boolean crouching, double distSq, float age, float limb, float armInset) {
        PoseStack.Pose before = pose.last();
        try {
            WornExec.draw(ENSEMBLES, pose, buffers, light, model, crouching, distSq, age, limb, armInset);
        } catch (RuntimeException | LinkageError failed) {
            for (int i = 0; i < 32 && pose.last() != before; i++) pose.popPose();
            if (DRAW_FAILED.add(failed.getClass().getName())) LogUtils.getLogger().error("[nusmp] the worn model library threw while drawing a player; the armour relics fall back to their painted boxes", failed);
        }
    }

    /**
     * The last resort. After the plan has been carried out, every armour relic (head, chest, feet) of this wearer is checked against what was REALLY drawn this frame: model pieces by the
     * library, an own renderer that ran, or the painted layer of the vanilla armour layer (asked for it by the item's armour-model hook, and its texture is registered and not empty).
     * If none of these happened and the player did not switch armour visuals off, the plain painted boxes of the relic's palette are drawn here, and one warning per relic is logged.
     */
    private static int armourFailsafe(PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer p, long now,
                                      List<Relics.Located> found, WornPlan.Plan plan) {
        VertexConsumer vc = null;
        int rescued = 0;
        for (WornPlan.Item it : plan.all()) {
            Relics.Located l = found.get(it.index());
            RelicDef d = l.def();
            if (l.where() != Relics.Where.ARMOUR || !d.slot().isArmor()) continue;
            String slot = d.slot().name();
            ArmorSlotPlan.Verdict v = ArmorSlotPlan.of(it);
            boolean asked = ArmorTrace.takePainted(p.getUUID(), slot);
            boolean tex = RelicTextures.paintedOk(d.id());
            ArmorVisible.Facts facts = new ArmorVisible.Facts(WornLibDraw.piecesDrawn(d.id()), CUSTOM_RAN.contains(d.id()), asked, tex, d.slot() == RelicSlot.CHEST);
            ArmorVisible.Outcome o = ArmorVisible.resolve(v, facts);
            ArmorTrace.Rec r = ArmorTrace.rec(p.getUUID(), slot);
            r.id = d.id();
            r.slot = slot;
            r.kind = it.kind();
            r.why = it.why();
            r.layer = v.layer();
            r.modelPieces = facts.modelPieces();
            r.customRan = facts.customRan();
            r.paintedAsked = asked;
            r.textureOk = tex;
            r.outcome = o;
            r.tick = now;
            r.failsafeDrawn = false;
            if (o != ArmorVisible.Outcome.FAILSAFE) continue;
            if (vc == null) vc = buffers.getBuffer(RenderType.entityCutoutNoCull(PLAIN));
            try {
                failsafeBoxes(pose, vc, light, model, d);
                r.failsafeDrawn = true;
                rescued++;
            } catch (RuntimeException | LinkageError failed) {
                LogUtils.getLogger().error("[nusmp] even the plain boxes of armour relic {} could not be drawn", d.id(), failed);
            }
            if (ArmorTrace.firstWarning(d.id()))
                LogUtils.getLogger().warn("[nusmp] armour relic {} ({}) was worn but nothing of it was drawn (model pieces {}, own renderer {}, vanilla layer asked {}, painted texture {}; plan {} / {}): its plain painted boxes are drawn instead",
                        d.id(), slot, facts.modelPieces(), facts.customRan(), asked, tex, it.kind(), it.why());
        }
        return rescued;
    }

    /** The plain painted look of an armour relic: a head box, a body with sleeves, or boots, in the palette of the relic, a little inside the vanilla armour layer so it never fights a layer that is there. */
    private static void failsafeBoxes(PoseStack pose, VertexConsumer vc, int light, PlayerModel<AbstractClientPlayer> model, RelicDef d) {
        int main = d.paletteMain(), trim = d.paletteTrim(), glow = d.paletteGlow();
        RelicSlot slot = d.slot();
        if (slot == RelicSlot.HEAD) {
            pose.pushPose();
            model.head.translateAndRotate(pose);
            PoseStack.Pose q = pose.last();
            Boxes.box(q, vc, light, -4.6f, -8.6f, -4.6f, 4.6f, 0.6f, 4.6f, main);              // the helm
            Boxes.box(q, vc, light, -4.7f, -5.2f, -4.7f, 4.7f, -3.9f, 4.7f, trim);              // the band
            Boxes.box(q, vc, FULL, -0.7f, -5.0f, -4.85f, 0.7f, -4.1f, -4.6f, glow);            // the stone at the brow
            pose.popPose();
        } else if (slot == RelicSlot.FEET) {
            for (ModelPart leg : new ModelPart[]{model.rightLeg, model.leftLeg}) {
                pose.pushPose();
                leg.translateAndRotate(pose);
                PoseStack.Pose q = pose.last();
                Boxes.box(q, vc, light, -2.6f, 6.9f, -2.6f, 2.6f, 12.6f, 2.6f, main);          // the boot
                Boxes.box(q, vc, light, -2.7f, 6.7f, -2.7f, 2.7f, 7.7f, 2.7f, trim);           // the cuff
                pose.popPose();
            }
        } else {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            PoseStack.Pose q = pose.last();
            Boxes.box(q, vc, light, -4.6f, -0.6f, -2.6f, 4.6f, 12.6f, 2.6f, main);             // the chest piece
            Boxes.box(q, vc, light, -4.7f, 10.2f, -2.7f, 4.7f, 11.4f, 2.7f, trim);             // the hem band
            Boxes.box(q, vc, FULL, -0.8f, 2.6f, -2.8f, 0.8f, 4.2f, -2.6f, glow);              // the stone at the breast
            pose.popPose();
            for (ModelPart arm : new ModelPart[]{model.rightArm, model.leftArm}) {
                pose.pushPose();
                arm.translateAndRotate(pose);
                PoseStack.Pose a = pose.last();
                Boxes.box(a, vc, light, -3.6f, -2.6f, -2.6f, 3.6f, 5.6f, 2.6f, main);          // the sleeve (a slim arm is a little narrower: the box covers both)
                pose.popPose();
            }
        }
    }

    /** Best grade first, ties by id, so the order is the same every frame. */
    private static boolean before(WornLibrary.Ensemble a, WornLibrary.Ensemble b) {
        return a.tier > b.tier || (a.tier == b.tier && a.id.compareTo(b.id) < 0);
    }

    /** Insertion sort of the (at most a handful of) ensembles collected so far into {@link #before} order; allocation free. */
    private static void sortEnsembles() {
        for (int i = 1; i < ENSEMBLES.size(); i++) {
            WornLibrary.Ensemble e = ENSEMBLES.get(i);
            int j = i;
            while (j > 0 && before(e, ENSEMBLES.get(j - 1))) {
                ENSEMBLES.set(j, ENSEMBLES.get(j - 1));
                j--;
            }
            ENSEMBLES.set(j, e);
        }
    }

    /** The place of a plain-box relic among its slot-mates, applied on top of the body part's pose (model pixels). */
    private static void place(PoseStack pose, WornPlacer.Placement pl) {
        if (pl == WornPlacer.Placement.NONE) return;
        pose.translate(pl.dx() / 16f, pl.dy() / 16f, pl.dz() / 16f);
        if (pl.rotY() != 0f) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(pl.rotY()));
        pose.scale(pl.scale(), pl.scale(), pl.scale());
    }

    /** A relic with its own renderer ({@link WornRenderers}): handed the posed model; a renderer that throws is switched off, and the pose stack is put back. */
    private static void drawCustom(Relics.Located w, PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer p,
                                   float partial, float age, float netHeadYaw, float headPitch, boolean first) {
        String id = w.def().id();
        WornRenderer r = WornRenderers.get(id);
        if (r == null) return;
        PoseStack.Pose before = pose.last();
        try {
            r.render(new WornCtx(w.stack(), pose, buffers, light, model, p, partial, age, netHeadYaw, headPitch, first));
            if (!CUSTOM_FAILED.contains(id)) CUSTOM_RAN.add(id);                       // 1.111.0: it ran without an error: a fact for the armour failsafe
        } catch (RuntimeException | LinkageError failed) {
            CUSTOM_FAILED.add(id);
            for (int i = 0; i < 16 && pose.last() != before; i++) pose.popPose();
            WornRenderers.register(id, c -> { });                                      // draw nothing for this relic from now on
            LogUtils.getLogger().error("[nusmp] worn renderer of {} failed and is switched off", id, failed);
        }
    }

    // ---------------------------------------------------------------- the worn pieces (model units: 16 to a block, y down, front is -z)
    private static void neck(PoseStack pose, VertexConsumer vc, int light, ModelPart body, Look look, int main, int trim, int glow, WornPlacer.Placement pl) {
        pose.pushPose();
        body.translateAndRotate(pose);
        place(pose, pl);
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < 4; i++) {                                                  // the chain: two short runs from the collar to the pendant
            float y = -0.2f + i * 0.7f, x = 2.3f - i * 0.5f;
            Boxes.box(p, vc, light, -x - 0.3f, y, -2.35f, -x + 0.3f, y + 0.6f, -2.0f, trim);
            Boxes.box(p, vc, light, x - 0.3f, y, -2.35f, x + 0.3f, y + 0.6f, -2.0f, trim);
        }
        float size = look == Look.LOCKET ? 3.0f : look == Look.PENDANT ? 2.2f : 2.8f;
        float h = size / 2f;
        Boxes.box(p, vc, light, -h, 2.4f, -2.75f, h, 2.4f + size, -2.2f, trim);        // the setting
        Boxes.box(p, vc, light, -h + 0.5f, 2.9f, -2.95f, h - 0.5f, 2.4f + size - 0.5f, -2.7f, main);
        Boxes.box(p, vc, FULL, -0.5f, 2.4f + size / 2f - 0.5f, -3.05f, 0.5f, 2.4f + size / 2f + 0.5f, -2.9f, glow);   // the stone, lit from inside
        pose.popPose();
    }

    private static void back(PoseStack pose, VertexConsumer vc, int light, ModelPart body, Look look, int main, int trim, int glow, boolean crouching, WornPlacer.Placement pl) {
        pose.pushPose();
        body.translateAndRotate(pose);
        place(pose, pl);
        PoseStack.Pose p = pose.last();
        boolean cape = look == Look.CAPE, mantle = look == Look.MANTLE;
        float len = cape ? 13f : mantle ? 5f : 18f;                                    // the hem, in pixels down from the shoulders
        float lift = crouching ? 0.6f : 0f;
        Boxes.box(p, vc, light, -4.3f, -0.4f, 2.05f, 4.3f, len, 2.7f + lift, main);     // the drop
        Boxes.box(p, vc, light, -4.3f, len - 1.0f, 2.05f, 4.3f, len, 2.8f + lift, trim);   // the hem
        Boxes.box(p, vc, light, -4.6f, -0.8f, -2.4f, 4.6f, 1.0f, 2.75f, main);          // the shoulders and the collar
        Boxes.box(p, vc, light, -4.65f, 0.6f, -2.45f, 4.65f, 1.1f, 2.8f, trim);
        Boxes.box(p, vc, FULL, -0.6f, 0.0f, -2.6f, 0.6f, 1.2f, -2.4f, glow);            // the clasp
        if (mantle) {                                                                  // a mantle hangs over the arms a little
            Boxes.box(p, vc, light, -5.2f, -0.6f, -2.3f, -3.9f, 3.6f, 2.5f, main);
            Boxes.box(p, vc, light, 3.9f, -0.6f, -2.3f, 5.2f, 3.6f, 2.5f, main);
            Boxes.box(p, vc, light, -5.25f, 2.9f, -2.35f, -3.85f, 3.6f, 2.55f, trim);
            Boxes.box(p, vc, light, 3.85f, 2.9f, -2.35f, 5.25f, 3.6f, 2.55f, trim);
        }
        pose.popPose();
    }

    private static void belt(PoseStack pose, VertexConsumer vc, int light, ModelPart body, int main, int trim, int glow, WornPlacer.Placement pl) {
        pose.pushPose();
        body.translateAndRotate(pose);
        place(pose, pl);
        PoseStack.Pose p = pose.last();
        Boxes.box(p, vc, light, -4.3f, 9.2f, -2.3f, 4.3f, 11.2f, 2.3f, main);
        Boxes.box(p, vc, light, -4.35f, 9.1f, -2.35f, 4.35f, 9.5f, 2.35f, trim);
        Boxes.box(p, vc, light, -1.4f, 8.9f, -2.6f, 1.4f, 11.5f, -2.3f, trim);          // the buckle
        Boxes.box(p, vc, FULL, -0.6f, 9.7f, -2.75f, 0.6f, 10.7f, -2.55f, glow);
        for (int i = -1; i <= 1; i += 2) Boxes.box(p, vc, light, i * 3.0f - 0.7f, 11.2f, -2.45f, i * 3.0f + 0.7f, 13.0f, -2.1f, main);   // two straps hanging at the hips
        pose.popPose();
    }

    private static void hands(PoseStack pose, VertexConsumer vc, int light, ModelPart arm, boolean right, int main, int trim, int glow, WornPlacer.Placement pl) {
        pose.pushPose();
        arm.translateAndRotate(pose);
        place(pose, pl);
        PoseStack.Pose p = pose.last();
        float x0 = right ? -3.45f : -1.45f, x1 = right ? 1.45f : 3.45f;
        Boxes.box(p, vc, light, x0, 6.6f, -2.45f, x1, 10.4f, 2.45f, main);             // the gauntlet's cuff
        Boxes.box(p, vc, light, x0 - 0.05f, 6.5f, -2.5f, x1 + 0.05f, 7.1f, 2.5f, trim);
        Boxes.box(p, vc, light, x0 - 0.05f, 10.0f, -2.5f, x1 + 0.05f, 10.5f, 2.5f, trim);
        float mid = (x0 + x1) / 2f;
        Boxes.box(p, vc, FULL, mid - 0.5f, 8.0f, -2.6f, mid + 0.5f, 9.0f, -2.45f, glow);
        pose.popPose();
    }

    private static void charm(PoseStack pose, VertexConsumer vc, int light, ModelPart body, int main, int trim, int glow, boolean shine, WornPlacer.Placement pl) {
        pose.pushPose();
        body.translateAndRotate(pose);
        place(pose, pl);
        PoseStack.Pose p = pose.last();
        Boxes.box(p, vc, light, 4.0f, 8.8f, -0.4f, 4.4f, 10.6f, 0.4f, trim);            // the cord at the left hip
        Boxes.box(p, vc, light, 4.0f, 10.4f, -1.2f, 4.9f, 12.6f, 1.2f, main);           // the charm itself
        Boxes.box(p, vc, shine ? FULL : light, 4.8f, 10.9f, -0.6f, 5.0f, 12.0f, 0.6f, glow);
        pose.popPose();
    }

    /** Axis-aligned boxes written straight into a vertex consumer, tinted by a colour; the white texture is sampled at its middle. */
    static final class Boxes {
        private Boxes() {}

        static void box(PoseStack.Pose pose, VertexConsumer vc, int light, float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
            float s = 1f / 16f;
            x0 *= s; y0 *= s; z0 *= s; x1 *= s; y1 *= s; z1 *= s;
            quad(pose, vc, light, rgb, 1.0f, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);   // top (y is down in model space, so the smaller y is up)
            quad(pose, vc, light, rgb, 0.62f, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);   // bottom
            quad(pose, vc, light, rgb, 0.86f, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);   // front
            quad(pose, vc, light, rgb, 0.80f, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);    // back
            quad(pose, vc, light, rgb, 0.74f, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);   // one side
            quad(pose, vc, light, rgb, 0.74f, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);    // the other side
        }

        private static void quad(PoseStack.Pose pose, VertexConsumer vc, int light, int rgb, float shade, float nx, float ny, float nz,
                                 float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
            int r = (int) ((rgb >> 16 & 255) * shade), g = (int) ((rgb >> 8 & 255) * shade), b = (int) ((rgb & 255) * shade);
            vertex(pose, vc, light, r, g, b, ax, ay, az, 0.1f, 0.1f, nx, ny, nz);          // 1.99.0: unrolled (no float[12] and float[8] per quad, six quads per box, boxes per relic per frame)
            vertex(pose, vc, light, r, g, b, bx, by, bz, 0.9f, 0.1f, nx, ny, nz);
            vertex(pose, vc, light, r, g, b, cx, cy, cz, 0.9f, 0.9f, nx, ny, nz);
            vertex(pose, vc, light, r, g, b, dx, dy, dz, 0.1f, 0.9f, nx, ny, nz);
        }

        private static void vertex(PoseStack.Pose pose, VertexConsumer vc, int light, int r, int g, int b, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            vc.addVertex(pose, x, y, z).setColor(r, g, b, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }
}
