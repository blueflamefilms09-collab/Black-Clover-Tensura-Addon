package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnComponents;
import com.newuniverse.nusmp.relics.djinn.VesselAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The client half of the Djinn system, registered in one call from the client section of the mod constructor ({@code DjinnClient.init(modEventBus)}):
 * the keys, the armour layer on both player skins, the HUD layer, the field-of-view and shake events, the first-person arms, the vessel tooltip and the
 * assembling bursts.
 */
public final class DjinnClient {
    private DjinnClient() {}

    public static void init(IEventBus modBus) {
        DjinnWeaponClient.init(modBus);                                                  // 1.34.0: the 24 temporary Djinn weapons drawn in 3D
        modBus.addListener(DjinnKeys::register);
        modBus.addListener(DjinnClient::addLayers);
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "djinn_hud"), DjinnScreenFx::render));
        modBus.addListener((ModelEvent.BakingCompleted e) -> DjinnMeshes.clear());
        NeoForge.EVENT_BUS.addListener(DjinnKeys::onClientTick);
        NeoForge.EVENT_BUS.addListener(DjinnScreenFx::onFov);
        NeoForge.EVENT_BUS.addListener(DjinnScreenFx::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(DjinnFirstPerson::onRenderArm);
        NeoForge.EVENT_BUS.addListener(DjinnFirstPerson::onRenderHand);
        NeoForge.EVENT_BUS.addListener(DjinnClient::onTooltip);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { DjinnClientState.clear(); DjinnAmbient.clear(); });
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer r) r.addLayer(new DjinnArmorLayer(r));
        }
    }

    /** A vessel says what sleeps in it; a relic that could be one says so. At most two short lines (the core's tooltip handles the rest). */
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.isEmpty() || Relics.defOf(s) == null) return;
        String id = s.get(DjinnComponents.BOUND.get());
        Djinn d = id == null ? null : Djinn.byName(id);
        if (d != null) {
            e.getToolTip().add(Component.literal("Metal Vessel: " + d.fullName()).withColor(d.color()));
            e.getToolTip().add(Component.literal(d.affinity().label + " Djinn. Wear or hold it, then press the Djinn key.").withStyle(ChatFormatting.GRAY));
        } else if (VesselAccess.isVessel(s)) {
            e.getToolTip().add(Component.literal("Can hold a Djinn (Metal Vessel)").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** The assembling and shattering burst: a ring of the Djinn's light round the player (client particles, a few dozen once). */
    static void burst(int entityId, Djinn d, boolean up) { burst(entityId, d, up, false); }

    /** The same ring for a Dark Djinn is black rukh: black-violet dust and ink instead of the Djinn's light. */
    static void burst(int entityId, Djinn d, boolean up, boolean dark) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity en = mc.level.getEntity(entityId);
        if (en == null) return;
        int rgb = dark ? DjinnLook.darkVignette(d.glow()) : d.glow();
        var dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.4f);
        var accent = dark ? ParticleTypes.SQUID_INK : ParticleTypes.END_ROD;
        var rnd = mc.level.getRandom();
        for (int i = 0; i < 36; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = up ? 2.2 : 0.4, y = rnd.nextDouble() * en.getBbHeight();
            double vx = Math.cos(a) * (up ? -0.12 : 0.18), vz = Math.sin(a) * (up ? -0.12 : 0.18);
            mc.level.addParticle(i % 3 == 0 ? accent : dust, en.getX() + Math.cos(a) * r, en.getY() + y, en.getZ() + Math.sin(a) * r, vx, 0.02 + rnd.nextDouble() * 0.05, vz);
        }
    }
}
