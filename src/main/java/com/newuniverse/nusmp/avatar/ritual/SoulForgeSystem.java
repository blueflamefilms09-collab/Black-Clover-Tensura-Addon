package com.newuniverse.nusmp.avatar.ritual;

import com.newuniverse.nusmp.avatar.aptitude.AptitudeLoader;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkillLoader;
import com.newuniverse.nusmp.avatar.item.AvatarItems;
import com.newuniverse.nusmp.avatar.item.SoulForgeBlocks;
import com.newuniverse.nusmp.avatar.net.AvatarNetServer;
import com.newuniverse.nusmp.avatar.net.SoulForgeNetServer;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the Soul Forge (1.58.0) is one feature made of a block, a ritual, two data loaders, six packets and a screen, and the lead should wire it with ONE
 * line: {@code SoulForgeSystem.init(modEventBus)} inside {@code AvatarSystem.init}, after {@code AvatarMenus.init} and before {@code NUItems.ITEMS.register}
 * (the block item is put on that register when {@link SoulForgeBlocks} loads). This registers the block, the two reload listeners (aptitudes and skills,
 * server side), and the game-bus rules that keep the ritual honest: it ticks every server tick, it ends when the player is hurt, dies, changes dimension or
 * logs out (their movement is given back at once), a login takes off any stray lock, and the world forgets every ritual when the server stops. It also registers
 * the in-game self-tests (key prefix {@code avatar}) that the CI server smoke test runs. Crafting the
 * Soul Forge fires the quest key {@code avatar.forge_crafted}, crafting a Genesis Core {@code avatar.core_crafted}. The payloads are registered by {@code AvatarNet.register}, the menu type by
 * {@code AvatarMenus}, and the client screen by {@code PlayerAvatarClient}; none of them need a line from the lead.
 */
public final class SoulForgeSystem {
    private SoulForgeSystem() {}

    private static boolean started;

    public static void init(IEventBus modBus) {
        if (started) return;
        started = true;
        SoulForgeBlocks.init(modBus);
        SoulForgeSelfTests.register();
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener((AddReloadListenerEvent e) -> { e.addListener(new AptitudeLoader()); e.addListener(new ForgeSkillLoader()); });
        game.addListener((ServerTickEvent.Post e) -> SoulForgeRitual.tick(e.getServer()));
        game.addListener((LivingIncomingDamageEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getAmount() > 0.0f && SoulForgeRitual.active(p)) SoulForgeRitual.cancel(p, "You were hurt: the ritual breaks. Nothing was taken.");
        });
        game.addListener((LivingDeathEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) SoulForgeRitual.cancel(p, null); });
        game.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) SoulForgeRitual.cancel(p, "You left the circle's world: the ritual breaks. Nothing was taken.");
        });
        game.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                SoulForgeRitual.cancel(p, null);
                RitualLock.release(p);
                AvatarNetServer.onLogout(p);
                SoulForgeNetServer.onLogout(p);
            }
        });
        game.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) RitualLock.clean(p); });
        game.addListener((PlayerEvent.ItemCraftedEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            if (e.getCrafting().is(SoulForgeBlocks.SOUL_FORGE_ITEM.get())) NusmpEvents.fire(p, "avatar.forge_crafted");
            else if (e.getCrafting().is(AvatarItems.GENESIS_CORE.get())) NusmpEvents.fire(p, "avatar.core_crafted");
        });
        game.addListener((ServerStoppedEvent e) -> { SoulForgeRitual.clearAll(); ForgeSkillResolver.forgetLogged(); });
    }
}
