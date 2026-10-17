package com.newuniverse.nusmp.avatar.inventory;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.avatar.inventory.client.AvatarInventoryClient;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Why: a window needs a menu type that both sides know ({@code nusmp:avatar_inventory}) and, on the client, a screen bound to it. The Avatar creation system's own
 * entry point is not mine to change, so this registers itself: {@link #init()} is called from the Avatar AI's event wiring while the mod is being constructed, takes
 * the mod's event bus from the active mod container, registers the menu type there and (on the client only) the screen. Called twice it does nothing the second time,
 * and the lead may also call {@link #init(IEventBus)} with the bus from the mod constructor, which is the same thing done explicitly. If the bus cannot be found the
 * window simply cannot open: the Avatar's action menu still works and the log says why.
 */
public final class AvatarInventoryMenus {
    private static final Logger LOG = LogUtils.getLogger();

    private AvatarInventoryMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<AvatarInventoryMenu>> INVENTORY = MENUS.register("avatar_inventory",
            () -> IMenuTypeExtension.create(AvatarInventoryMenu::new));

    private static boolean registered;

    /** Registers on the mod bus of the mod that is being constructed right now. */
    public static void init() {
        try {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            init(container == null ? null : container.getEventBus());
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] The Avatar inventory window could not find the mod event bus ({}); it will not open", e.toString());
        }
    }

    public static synchronized void init(IEventBus modBus) {
        if (registered || modBus == null) return;
        registered = true;
        MENUS.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) AvatarInventoryClient.register(modBus);
    }
}
