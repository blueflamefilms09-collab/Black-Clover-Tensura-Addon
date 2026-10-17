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
 * {@link AvatarBuilderMenu} (the classic screen) or a {@link SoulForgeMenu} (the Soul Forge ritual), so a client cannot roll or create without the Genesis
 * Core and the forge having opened it. This registers both MenuTypes ({@code nusmp:avatar_builder}, and since 1.58.0 {@code nusmp:soul_forge}); AvatarSystem
 * calls {@link #init} once.
 */
public final class AvatarMenus {
    private AvatarMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<AvatarBuilderMenu>> BUILDER = MENUS.register("avatar_builder",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new AvatarBuilderMenu(id)));
    /** 1.58.0: the full-screen Soul Forge. The client half is built with no ritual (the server owns it). */
    public static final DeferredHolder<MenuType<?>, MenuType<SoulForgeMenu>> SOUL_FORGE = MENUS.register("soul_forge",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new SoulForgeMenu(id)));

    public static void init(IEventBus modBus) { MENUS.register(modBus); }
}
