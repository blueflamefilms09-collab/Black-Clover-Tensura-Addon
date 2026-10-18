package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Why: the one entry point of the Curse Series system (despair, the offer and the Embrace, corruption, the Overrun, purification, the config
 * {@code nusmp-heroweapon-curse-server.toml}, the self-tests {@code hero.curse}), called once from {@code HeroWeaponSystem.init}, which keeps the start-up of the
 * hero weapons one line long. {@link #init} registers the config, the item, the two packets, the game-bus handlers and the bond listener; nothing else in the
 * mod has to know. It also holds the three calls the other systems make with a player in hand: the Weapon Book's Embrace button calls {@link #embrace}, the
 * dispatcher is told through {@link HeroCurse}, and the creative tab asks {@link #creativeItems}. Safe to call twice.
 */
public final class CurseSystem {
    private CurseSystem() {}

    private static boolean done;

    /** Registers everything of the Curse Series. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        modContainer.registerConfig(ModConfig.Type.SERVER, CurseConfig.SPEC, "nusmp-heroweapon-curse-server.toml");
        CurseItems.REGISTER.register(modBus);
        modBus.addListener(CurseNet::register);
        CurseEvents.register(NeoForge.EVENT_BUS);
        HeroBonds.addListener(CurseEvents.BOND_LISTENER);
        CurseSelfTests.register();
    }

    /** The items this system adds to the creative tab (after the weapons and the gem system's). */
    public static List<Item> creativeItems() { return CurseItems.creativeItems(); }

    /**
     * The wielder accepts the weapon's open offer (the Weapon Book's Embrace button, the held key, the command). Checks everything again: a bond, an open offer, no
     * Overrun. True when the Curse Series was embraced; otherwise the player was told why. Server thread only.
     */
    public static boolean embrace(ServerPlayer player) {
        if (player == null) return false;
        return CurseService.embrace(player, CurseService.now(player), CurseEvents.tuning(player.getServer()));
    }

    /** The wielder turns the open offer away. True when there was one. Server thread only. */
    public static boolean decline(ServerPlayer player) {
        if (player == null) return false;
        return CurseService.decline(player, CurseService.now(player), CurseEvents.tuning(player.getServer()));
    }
}
