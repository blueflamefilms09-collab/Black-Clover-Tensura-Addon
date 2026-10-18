package com.newuniverse.nusmp.client.heroweapon;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.client.heroweapon.carriage.CarriageClient;
import com.newuniverse.nusmp.client.heroweapon.curse.CurseClient;
import com.newuniverse.nusmp.client.heroweapon.forms.FormsClient;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroTooltip;
import com.newuniverse.nusmp.heroweapon.HeroWeaponSystem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one client entry point of the hero weapons (the keybinds, the HUD, the look of a summoned weapon, the payload receivers of the core), called once from
 * the mod constructor's client block; it also starts the forms and curse client parts so that the mod constructor has one line. The core's client part is: the
 * five keys ({@link HeroKeysClient}), the HUD layer ({@link HeroHud}) and its client config {@code nusmp-heroweapon-client.toml}, the client's copy of the bond
 * ({@link ClientHeroBond}, filled by the payload handlers and forgotten at logout), the real names of the keys for the tooltip, and the two model predicates the
 * art needs: {@code blocking} on the Legendary Shield (set while the guard is up) and {@code pull} / {@code pulling} on the Legendary Bow, exactly as vanilla
 * names them, so a weapon's item model may switch its display transforms with an {@code overrides} entry. {@code FormsClient} and {@code CurseClient} are
 * the forms and curse builders'.
 */
public final class HeroClient {
    private HeroClient() {}

    public static void init(IEventBus modBus) {
        HeroWeaponSystem.modContainer().registerConfig(ModConfig.Type.CLIENT, HeroClientConfig.SPEC, "nusmp-heroweapon-client.toml");
        modBus.addListener(HeroKeysClient::register);
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "hero_hud"), HeroHud::render));
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(HeroClient::registerPredicates));
        NeoForge.EVENT_BUS.addListener(HeroKeysClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientHeroBond.clear());
        HeroTooltip.keyNames = HeroKeysClient::name;
        FormsClient.init(modBus);
        CurseClient.init(modBus);
        CarriageClient.init(modBus);                       // the Vassal Carriage's renderer; idempotent, its logic also calls it
    }

    /** The vanilla predicate names for a raised guard and a drawn string, for the two weapons that use them. */
    private static void registerPredicates() {
        ItemProperties.register(HeroItems.get(HeroKind.SHIELD).get(), ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        ItemProperties.register(HeroItems.get(HeroKind.BOW).get(), ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        ItemProperties.register(HeroItems.get(HeroKind.BOW).get(), ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0.0F : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
    }
}
