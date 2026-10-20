package com.newuniverse.nusmp.client.relics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.worn.WornExec;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import com.newuniverse.nusmp.client.relics.worn.WornTrace;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.relics.RelicBearer;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.WornSync;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a head, chest or boots relic with a worn model has a deliberately blank painted armour texture (the model stands where the box would, see {@code RelicTextures}), so on any wearer
 * that has no relic layer the helmet or the boots are simply gone: an armour stand, a zombie or a skeleton a player gave a relic helmet to, a piglin, any humanoid mob of another mod.
 * This layer is added (by {@code RelicClient.addLayers}) to every living-entity renderer whose model is a humanoid model and that is not a player renderer (those have {@code RelicWornLayer},
 * the player-model characters {@code KitRelicLayer}). It draws through the same plan and the same library as they do ({@link WornPlan}, {@link WornExec}). It costs one inexpensive look
 * per frame per entity: nothing more happens unless the entity wears a relic in an armour slot or the server has sent a list for it ({@link WornSync}).
 */
public class HumanoidRelicLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET};
    private static final double[] DIST = new double[1];
    private static final List<WornLibrary.Ensemble> SETS = new ArrayList<>(8);

    public HumanoidRelicLayer(RenderLayerParent<T, M> parent) { super(parent); }

    /** Does this entity wear a relic in an armour slot, or has the server sent a list for it? Allocation free. */
    static boolean candidate(LivingEntity e) {
        for (EquipmentSlot s : ARMOUR) if (e.getItemBySlot(s).getItem() instanceof RelicBearer) return true;
        return !WornSync.client(e.getUUID()).isEmpty();
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        if (e.isInvisible() || e.isSpectator() || !candidate(e)) return;
        List<Relics.Located> found = Relics.locatedList(e);
        if (found.isEmpty()) return;
        boolean far = WornExec.tooFar(e, DIST);
        WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(false, WornPlan.View.OTHER, false, false, far, RelicFxOptions.models(), RelicFxOptions.hideArmourRelics(),
                WornPlan.Fallback.CHARM), WornExec.entries(found, null, false));
        SETS.clear();
        if (!plan.isEmpty()) {
            WornExec.collect(plan, found, SETS);
            WornExec.draw(SETS, pose, buffers, light, getParentModel(), e.isCrouching(), DIST[0], age, limbSwingAmount, 0f);
        }
        WornTrace.reached(e.getUUID(), e.level().getGameTime(), "humanoid", false, WornPlan.View.OTHER, found.size(), plan, SETS.size());
        SETS.clear();
    }
}
