package com.newuniverse.nusmp.avatar.menu;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: a menu is the server's proof that the creation screen is really open: the net handlers only act for a player whose open container is an
 * {@link AvatarBuilderMenu}, so a client cannot roll or create without the Genesis Core having opened it. This registers its MenuType
 * ({@code nusmp:avatar_builder}); AvatarSystem calls {@link #init} once.
 */
public final class AvatarMenus {
    private AvatarMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<AvatarBuilderMenu>> BUILDER = MENUS.register("avatar_builder",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new AvatarBuilderMenu(id)));

    public static void init(IEventBus modBus) { MENUS.register(modBus); }
}
