package com.newuniverse.nusmp.client.merge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.AuraContext;
import com.newuniverse.nusmp.client.aura.AuraPainter;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.aura.PlayerAuraClient;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.core.magic.merge.MergedBlend;
import com.newuniverse.nusmp.core.magic.merge.MergedDef;
import com.newuniverse.nusmp.core.magic.merge.MergedRegistry;
import com.newuniverse.nusmp.core.magic.profile.MagicProfile;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * 1.27.0: the temporary armour layer of a merged magic (Aura.MERGED, style = the merged magic's slot; started for 5 s by every merged
 * cast, core.magic.merge.MergeContext.stance). It is the parents' armours laid over each other and a blend on top:
 * <ol>
 *   <li>the two heaviest parents that have an armour painter of their own draw it, one after the other, each a few percent bigger than
 *       the one under it (so their solid skins never fight), the second softer;</li>
 *   <li>every other parent, and any parent without an armour of its own, is a glowing shell of the player's model in that parent's
 *       colour;</li>
 *   <li>one more shell in the blended colour, breathing;</li>
 *   <li>the floating emblem: the baked 3D model of the merged magic ({@link MergedMesh}) orbiting behind the caster.</li>
 * </ol>
 * Any painter that throws is logged once and skipped; the rest still draws.
 */
public final class MergedArmorPainter {
    private MergedArmorPainter() {}

    private static final ResourceLocation SHELL = AuraRender.tex("particle/glow");
    private static final Quaternionf SCRATCH = new Quaternionf();
    private static final float DEG = (float) (Math.PI / 180.0);

    /** Called once by AuraRegistry on the client. */
    public static void register() { PlayerAuraClient.register(Aura.MERGED, MergedArmorPainter::paint); }

    private static AuraPainter painterOf(String parent) {
        MagicProfile p = MagicProfiles.of(parent);
        Aura a = p.auraStyle();
        return a == null || a == Aura.MERGED ? null : PlayerAuraClient.painter(a);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        MergedDef def = MergedRegistry.bySlot(c.style());
        if (def == null) return;
        MergedBlend b = MergedRegistry.blend(def);
        float pulse = 0.65f + 0.35f * Mth.sin(c.age() * 0.1f);
        int n = b.size();
        int drawn = 0;
        for (int i = 0; i < n; i++) {
            AuraPainter painter = drawn < 2 ? painterOf(def.parents().get(i)) : null;
            if (painter != null) {
                drawn++;
                overlay(c, painter, def.parents().get(i), drawn - 1, f * (drawn == 1 ? 1f : 0.7f));
            } else {
                shell(c, 1.05f + 0.018f * i, b.tinted(i), 0.30f * pulse * f);
            }
        }
        shell(c, 1.11f, b.primary, (0.16f + 0.10f * pulse) * f);
        try {
            emblem(c, def, b, f * (0.8f + 0.2f * pulse));
        } catch (RuntimeException e) {
            SafeCast.clientFault("merge-emblem", e);
        }
    }

    /** One parent's own armour painter, run on a pose scaled a little about the body so that stacked armours do not share a surface. */
    private static void overlay(AuraContext c, AuraPainter painter, String parent, int layer, float fade) {
        PoseStack pose = c.pose();
        float k = 1f + 0.03f * layer;
        pose.pushPose();
        pose.translate(0f, 0.75f, 0f);
        pose.scale(k, k, k);
        pose.translate(0f, -0.75f, 0f);
        try {
            painter.paint(new AuraContext(c.player(), c.model(), pose, c.buffers(), c.light(), c.partial(), c.age(), c.t(), c.left(), c.total(), 0,
                    c.limbSwing(), c.limbSwingAmount(), c.netHeadYaw(), c.headPitch(), fade));
        } catch (RuntimeException | LinkageError e) {
            SafeCast.clientFault("merge-armor:" + parent, e);
        } finally {
            pose.popPose();
        }
    }

    /** The player's posed model again, additive, in one colour. */
    private static void shell(AuraContext c, float scale, int argb, float alpha) {
        if (alpha <= 0.004f) return;
        AuraRender.modelBright(c, AuraRender.additive(SHELL), scale, scale * 0.985f, scale, AuraRender.alpha(argb | 0xFF000000, Mth.clamp(alpha, 0f, 1f)));
    }

    // ---------------------------------------------------------------- the emblem
    private static void emblem(AuraContext c, MergedDef def, MergedBlend b, float fade) {
        MergedMesh mesh = MergedMeshCache.get(def);
        if (mesh.parts.length == 0) return;
        PoseStack pose = c.pose();
        float age = c.age();
        for (int i = 0; i < mesh.parts.length; i++) {
            MergedMesh.Part part = mesh.parts[i];
            float ang = (part.angle + age * 1.1f * (i % 2 == 0 ? 1f : -0.8f)) * DEG;
            float bob = 0.05f * Mth.sin(age * 0.07f + i);
            pose.pushPose();
            // layer space: y points down from the neck line, the feet are 1.5 blocks below it; the plan's height is above the feet
            pose.translate(part.radius * Mth.sin(ang), 1.5f - part.height + bob, part.radius * Mth.cos(ang));
            pose.mulPose(SCRATCH.rotationYXZ(age / 20f * part.spin * DEG, 0f, part.lean * DEG * Mth.sin(age * 0.05f + i)));
            pose.scale(part.scale, part.scale, part.scale);
            if (part.cutout) {
                VertexConsumer vc = c.buffers().getBuffer(AuraRender.cutout(part.texture));
                MergedMesh.replay(pose.last(), vc, part, AuraRender.alpha(part.tint, fade), c.light());
                if (part.glow != null) {
                    VertexConsumer gv = c.buffers().getBuffer(AuraRender.additive(part.glow));
                    MergedMesh.replay(pose.last(), gv, part, AuraRender.alpha(part.tint, 0.55f * fade), PropDraw.FULL_BRIGHT);
                }
            } else {
                VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(part.texture));
                MergedMesh.replay(pose.last(), vc, part, AuraRender.alpha(part.tint, 0.9f * fade), PropDraw.FULL_BRIGHT);
            }
            pose.popPose();
        }
    }
}
