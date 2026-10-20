package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.worn.WornExec;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import com.newuniverse.nusmp.client.relics.worn.WornTrace;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the relic layer of the game ({@code RelicWornLayer}) is added to the two vanilla player renderers only, so a humanoid drawn by the player-model kit (the arcs' NPCs, the
 * named humanoids with a relic pack, the player's Avatars) wore its relics invisibly. This layer draws what such an entity wears (a Curios slot, head, chest and feet armour relics, the
 * pack of a named mob) with the same model library and the same placement, effects and vertex budget as a player. What is drawn is decided by {@link WornPlan} (the same decision as the
 * player layer and {@code /multiverse relic debug}); a relic the library cannot dress gets the generic floating charm ({@link WornLibrary#fallbackCharm}), so none is ever invisible.
 * A mob's pack lives in persistent data that never reaches a client, so what the client reads here includes the list the server sends ({@code Relics#locatedList}, {@code WornSync}).
 * Nothing is read when the entity wears no relic (one cached look every 10 ticks, see {@link Relics}).
 */
public class KitRelicLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
    private static final double[] DIST = new double[1];
    private static final List<WornLibrary.Ensemble> SETS = new ArrayList<>(8);
    private final PlayerModelRenderer.Body<T> owner;

    public KitRelicLayer(PlayerModelRenderer.Body<T> owner) {
        super(owner);
        this.owner = owner;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        boolean slim = owner.look() != null && owner.look().slim();
        long now = e.level().getGameTime();
        if (e.isInvisible() || e.isSpectator()) {
            WornTrace.reached(e.getUUID(), now, "kit", slim, WornPlan.View.OTHER, 0, null, 0);
            return;
        }
        List<Relics.Located> found = Relics.locatedList(e);
        if (found.isEmpty()) {
            WornTrace.reached(e.getUUID(), now, "kit", slim, WornPlan.View.OTHER, 0, null, 0);
            return;
        }
        boolean far = WornExec.tooFar(e, DIST);
        WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(slim, WornPlan.View.OTHER, false, false, far, RelicFxOptions.models(), RelicFxOptions.hideArmourRelics(),
                WornPlan.Fallback.CHARM), WornExec.entries(found, null, false));                  // a kit entity has no own-renderer relics and no Curios eye icons
        SETS.clear();
        if (!plan.isEmpty()) {
            WornExec.collect(plan, found, SETS);
            WornExec.draw(SETS, pose, buffers, light, getParentModel(), e.isCrouching(), DIST[0], age, limbSwingAmount, plan.armInset());
        }
        WornTrace.reached(e.getUUID(), now, "kit", slim, WornPlan.View.OTHER, found.size(), plan, SETS.size());
        SETS.clear();
    }
}
