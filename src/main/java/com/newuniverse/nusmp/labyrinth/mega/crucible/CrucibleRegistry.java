package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlock;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the Crucible's classes are deliberately package-private except for the few the rest of the game needs (a Cursed Spirit's rules, the vault's
 * ticker pieces), and {@code ThemeRegistry}, the one registration entry the Mega-Dungeon's system class calls for the themes and the Crucible together,
 * lives in the {@code theme} package. This class is the door between them: the two additive hooks the pedestal block entity and the pedestal block ask
 * before they act ({@link #installHooks}), and every game-bus handler of the Crucible ({@link #register}). Nothing here has logic of its own.
 */
public final class CrucibleRegistry {
    private CrucibleRegistry() {}

    /**
     * Puts the Crucible in front of the Relic Pedestal's pick and in front of its drop-on-removal. Both hooks answer "not mine" (null / true) for any
     * pedestal that is not in a sector's saved pedestal list, so the Spire's pedestals and an admin's keep their old behaviour.
     */
    public static void installHooks() {
        RelicPedestalBlockEntity.setTakeHook(CrucibleHook::onTake);
        RelicPedestalBlock.setDropGuard(CrucibleHook::mayDrop);
    }

    /** Game-bus handlers: the Cursed Spirit roster (a data reload), the vault ticker and catastrophes, the spirit rules and the clean-up at server stop. */
    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new CursedSpirits()));
        gameBus.addListener(CrucibleVault::onServerTick);
        gameBus.addListener(CrucibleVault::onServerStopping);
        gameBus.addListener(SpiritRules::onHeal);
        gameBus.addListener(SpiritRules::onDrops);
        gameBus.addListener(SpiritRules::onExperience);
        gameBus.addListener(SpiritRules::onJoin);
    }
}
