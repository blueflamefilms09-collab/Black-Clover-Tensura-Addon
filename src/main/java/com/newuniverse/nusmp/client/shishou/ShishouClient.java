package com.newuniverse.nusmp.client.shishou;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.shishou.ShishouComponents;
import com.newuniverse.nusmp.shishou.ShishouConcealment;
import com.newuniverse.nusmp.shishou.ShishouConfig;
import com.newuniverse.nusmp.shishou.ShishouEntities;
import com.newuniverse.nusmp.shishou.ShishouItems;
import com.newuniverse.nusmp.shishou.ShishouNet;
import com.newuniverse.nusmp.shishou.ShishouSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Why: the client half of Shishou is small and decides nothing. No keys of its own (2.1.0: his abilities are the Tensura Ultimate skill {@code nusmp:shishou}, used with the skill system's keys). While the sword is in a hand, once a second, the client tells the server what is under the crosshair (an entity id, or -1 for the off-hand item) for
 * Appraisal; the answer and the sword's state come back as two small packets and are drawn by {@link ShishouHud} (movable). The tooltip of a stack that is not the viewer's own shows the false "Rusty Sword" lines
 * (Stat Concealment). The entity renderer is {@link ShishouRenderer}. Call {@link #init} from the client setup with the mod bus.
 */
public final class ShishouClient {
    private ShishouClient() {}

    private static int askTimer;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ShishouEntities.SHISHOU.get(), ShishouRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "shishou_hud"), ShishouHud::render));
        ShishouNet.clientSync = ShishouClientState::onSync;
        ShishouNet.clientAppraisal = ShishouClientState::onReply;
        NeoForge.EVENT_BUS.addListener(ShishouClient::onTick);
        NeoForge.EVENT_BUS.addListener(ShishouClient::onTooltip);
    }

    private static boolean holding(Minecraft mc) {
        return mc.player != null && (mc.player.getMainHandItem().getItem() == ShishouItems.SHISHOU.get() || mc.player.getOffhandItem().getItem() == ShishouItems.SHISHOU.get());
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.getConnection() == null || !holding(mc)) return;
        if (++askTimer < 20) return;                                                          // Appraisal: one ask a second, only while he is in a hand
        askTimer = 0;
        Entity looked = mc.crosshairPickEntity;
        if (looked != null) PacketDistributor.sendToServer(new ShishouNet.AppraiseRequest(looked.getId()));
        else if (!mc.player.getOffhandItem().isEmpty() && mc.player.getOffhandItem().getItem() != ShishouItems.SHISHOU.get()) PacketDistributor.sendToServer(new ShishouNet.AppraiseRequest(-1));
    }

    /** Stat Concealment: anyone but the wielder who reads the tooltip of the sword sees a rusty sword. A stack with no wielder written (the creative tab) shows the truth. */
    static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.getItem() != ShishouItems.SHISHOU.get() || !ShishouConfig.skillOn(ShishouSkill.STAT_CONCEALMENT)) return;
        UUID owner = ShishouComponents.owner(s);
        if (owner == null || e.getEntity() == null || owner.equals(e.getEntity().getUUID())) return;
        var lines = e.getToolTip();
        lines.clear();
        lines.add(Component.translatableWithFallback(ShishouConcealment.FALSE_NAME_KEY, "Rusty Sword"));
        lines.add(Component.translatableWithFallback("shishou.false.line", "An old, ordinary sword.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
