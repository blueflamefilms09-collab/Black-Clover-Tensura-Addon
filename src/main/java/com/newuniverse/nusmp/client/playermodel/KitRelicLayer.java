package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.fx.WornFxPlan;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibDraw;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the relic layer of the game ({@code RelicWornLayer}) is added to the two vanilla player renderers only, so a humanoid drawn by the player-model kit (the arcs' NPCs, the
 * named humanoids with a relic pack) wore its relics invisibly. This layer draws what such an entity wears (a Curios slot, head, chest and feet armour relics, the pack of a named
 * mob) with the same model library and the same placement, effects and vertex budget as a player, through {@link WornLibDraw}. A relic the library cannot dress gets the generic
 * floating charm ({@link WornLibrary#fallbackCharm}), so none is ever invisible. Nothing is read when the entity wears no relic (one cached look every 10 ticks, see {@link Relics}).
 */
public class KitRelicLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
    public KitRelicLayer(PlayerModelRenderer.Body<T> owner) { super(owner); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        if (e.isInvisible() || e.isSpectator()) return;
        List<Relics.Worn> worn = Relics.wornList(e);
        if (worn.isEmpty()) return;
        double distSq = e.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        if (distSq > WornFxPlan.CULL_RANGE * WornFxPlan.CULL_RANGE) return;
        boolean hideArmour = RelicFxOptions.hideArmourRelics();
        List<WornLibrary.Ensemble> sets = new ArrayList<>(worn.size());
        for (Relics.Worn w : worn) {
            RelicDef d = w.def();
            if (d.slot().isHand() || (d.slot().isArmor() && hideArmour)) continue;
            WornLibrary.Ensemble en = WornLibrary.of(d);
            if (en == null) en = WornLibrary.fallbackCharm(d);
            if (en == null) continue;
            int at = sets.size();
            while (at > 0 && (en.tier > sets.get(at - 1).tier || (en.tier == sets.get(at - 1).tier && en.id.compareTo(sets.get(at - 1).id) < 0))) at--;
            sets.add(at, en);
        }
        if (sets.isEmpty()) return;
        WornLibDraw.draw(sets, pose, buffers, light, getParentModel(), e.isCrouching(), distSq, age, limbSwingAmount, RelicFxOptions.quality());
    }
}
