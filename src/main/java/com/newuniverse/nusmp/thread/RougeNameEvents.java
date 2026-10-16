package com.newuniverse.nusmp.thread;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Why: while Rouge rests on the head the caster cannot easily aim at her, so a name tag could never reach her. This listener lets the caster
 * look straight up at their own head and use a named name tag in the air to name her; it does nothing in any other situation (a name tag in
 * the air does nothing in vanilla either), so it takes nothing away from normal name tags. Wire it with
 * {@code NeoForge.EVENT_BUS.addListener(RougeNameEvents::onUseNameTag)}.
 */
public final class RougeNameEvents {
    private RougeNameEvents() {}

    /** How far up the caster must be looking (pitch, degrees; negative is up) to mean "my head". */
    public static final float LOOK_UP_PITCH = -60f;

    public static void onUseNameTag(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.getItemStack().is(Items.NAME_TAG)) return;
        if (p.getXRot() > LOOK_UP_PITCH || !RougeManager.anyActive()) return;
        RougeEntity cat = RougeManager.of(p.getUUID());
        if (cat == null || cat.state() != RougeEntity.HEAD && cat.state() != RougeEntity.SIT) return;
        Component tag = e.getItemStack().get(DataComponents.CUSTOM_NAME);
        String name = tag == null ? null : RougePerch.cleanName(tag.getString());
        if (name == null) return;
        if (!p.getAbilities().instabuild) e.getItemStack().shrink(1);
        cat.rename(p, name);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }
}
