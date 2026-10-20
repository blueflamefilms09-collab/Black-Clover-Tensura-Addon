package com.newuniverse.nusmp.client.relics.worn;

import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.fx.WornFxPlan;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibDraw;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the two worn layers (the vanilla player renderers' {@code RelicWornLayer} and the player-model kit's {@code KitRelicLayer}) must make the same decisions, so the steps both need live
 * here once: turning what a wearer has into the {@link WornPlan} inputs, and handing the plan's library models and floating charms to the model library. The decision itself is
 * {@link WornPlan}; this only reads the game and carries it out. Client render thread only.
 */
public final class WornExec {
    private WornExec() {}

    /** The wearer's relics as plan entries, parallel to {@code found}. {@code flags} is the Curios eye list (null: not read yet, read lazily by the caller). */
    public static List<WornPlan.Entry> entries(List<Relics.Located> found, List<CuriosLink.RenderFlag> flags, boolean customAllowed) {
        List<WornPlan.Entry> out = new ArrayList<>(found.size());
        for (Relics.Located l : found) {
            RelicDef d = l.def();
            Boolean eye = null;
            if (l.where() == Relics.Where.CURIOS && flags != null) eye = eyeOf(flags, l.stack());
            boolean hand = d.slot().isHand();
            out.add(new WornPlan.Entry(d.id(), d.slot().name(), d.look().name(), WornPlan.Source.valueOf(l.where().name()), eye,
                    customAllowed && WornRenderers.get(d.id()) != null, !hand && WornLibrary.of(d) != null, d.grade().tier()));
        }
        return out;
    }

    /** Does any of these relics sit in a Curios slot (so the eye flags are worth reading)? */
    public static boolean needsFlags(List<Relics.Located> found) {
        for (Relics.Located l : found) if (l.where() == Relics.Where.CURIOS) return true;
        return false;
    }

    /** The Curios eye icon of the slot this stack sits in (identity first, then an equal stack); null when the list does not know the stack (it is then drawn). */
    public static Boolean eyeOf(List<CuriosLink.RenderFlag> flags, ItemStack stack) {
        for (CuriosLink.RenderFlag f : flags) if (f.stack() == stack) return f.render();
        for (CuriosLink.RenderFlag f : flags) if (ItemStack.matches(f.stack(), stack)) return f.render();
        return null;
    }

    /** Best grade first, ties by id (the order of {@link WornPlan}); the same rule the layers always used to merge into a list that already holds the mod's armour pieces. */
    public static boolean before(WornLibrary.Ensemble a, WornLibrary.Ensemble b) {
        return a.tier > b.tier || (a.tier == b.tier && a.id.compareTo(b.id) < 0);
    }

    /** Inserts an ensemble into a list kept in {@link #before} order. */
    public static void insert(List<WornLibrary.Ensemble> into, WornLibrary.Ensemble e) {
        int at = into.size();
        while (at > 0 && before(e, into.get(at - 1))) at--;
        into.add(at, e);
    }

    /** Adds the plan's library models (and its floating charms) to {@code into}, keeping the order. */
    public static void collect(WornPlan.Plan plan, List<Relics.Located> found, List<WornLibrary.Ensemble> into) {
        for (WornPlan.Item it : plan.library()) {
            WornLibrary.Ensemble e = WornLibrary.of(found.get(it.index()).def());
            if (e != null) insert(into, e);
        }
        for (WornPlan.Item it : plan.charms()) {
            WornLibrary.Ensemble e = WornLibrary.fallbackCharm(found.get(it.index()).def());
            if (e != null) insert(into, e);
        }
    }

    /** The view of a wearer in the frame being drawn. */
    public static WornPlan.View viewOf(LivingEntity wearer) {
        Minecraft mc = Minecraft.getInstance();
        return WornPlan.viewOf(wearer == mc.player, mc.options.getCameraType().isFirstPerson(), mc.screen != null);
    }

    /** True when the wearer is beyond the draw range of the worn layers; also gives the squared distance through {@code out[0]}. */
    public static boolean tooFar(LivingEntity wearer, double[] out) {
        double distSq = wearer.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        out[0] = distSq;
        return distSq > WornFxPlan.CULL_RANGE * WornFxPlan.CULL_RANGE;
    }

    /** Draws the collected ensembles in the pose of {@code model}. */
    public static void draw(List<WornLibrary.Ensemble> sets, PoseStack pose, MultiBufferSource buffers, int light, HumanoidModel<?> model, boolean crouching, double distSq,
                            float age, float limb, float armInset) {
        if (sets.isEmpty()) return;
        WornLibDraw.draw(sets, pose, buffers, light, model, crouching, distSq, age, limb, RelicFxOptions.quality(), armInset);
    }
}
