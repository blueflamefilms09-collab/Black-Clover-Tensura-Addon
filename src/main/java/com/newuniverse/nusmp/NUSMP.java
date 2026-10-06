package com.newuniverse.nusmp;

import com.newuniverse.nusmp.blackclover.BlackCloverRegistry;
import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireCommand;
import com.newuniverse.nusmp.command.FaithCommand;
import com.newuniverse.nusmp.skill.LiarisFreeseSkill;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.skill.SkillEvolution;
import com.newuniverse.nusmp.vfx.VfxCommand;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(NUSMP.MODID)
public class NUSMP {
    public static final String MODID = "nusmp";

    /** ManasCore cooldowns tick down once per SECOND (every 20 ticks). Convert our tick numbers. */
    public static int ticksToSeconds(int ticks) { return ticks <= 0 ? 0 : Math.max(1, (ticks + 19) / 20); }

    public NUSMP(IEventBus modEventBus, ModContainer modContainer) {
        NUSkills.SKILLS.register(modEventBus);
        BlackCloverRegistry.ITEMS.register(modEventBus);
        com.newuniverse.nusmp.grimoire.GrimoireComponents.COMPONENTS.register(modEventBus);
        com.newuniverse.nusmp.block.NUBlocks.init();
        com.newuniverse.nusmp.block.NUBlocks.BLOCKS.register(modEventBus);
        com.newuniverse.nusmp.world.NUWorldgen.FEATURES.register(modEventBus);
        com.newuniverse.nusmp.item.NUItems.ITEMS.register(modEventBus);
        com.newuniverse.nusmp.item.NUItems.MATERIALS.register(modEventBus);
        com.newuniverse.nusmp.entity.NUEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.entity.NUEntities::attributes);
        NUCreativeTab.TABS.register(modEventBus);
        modEventBus.addListener(VfxSpawn::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.book.GrimoireSummon::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::registerPayloads);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(com.newuniverse.nusmp.client.NUClient::onClientSetup);
            com.newuniverse.nusmp.vfx.client.VfxClientEvents.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireClient.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireShelfClient.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireFloatClient.init();
            com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient.init(modEventBus);
            modContainer.registerConfig(ModConfig.Type.CLIENT, com.newuniverse.nusmp.client.multiverse.MultiverseClientConfig.SPEC, "nusmp-multiverse-client.toml");
            // Spirit Lords are small floating orbs for now (SpiritLordRenderer, the full Tensura body, is kept for later)
            modEventBus.addListener(com.newuniverse.nusmp.client.SpiritOrbRenderer::register);
        }
        modContainer.registerConfig(ModConfig.Type.SERVER, NUConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.multiverse.MultiverseConfig.SPEC, "nusmp-multiverse-server.toml");
        // Multiverse: /multiverse commands, ceremony, mastery pages, ranks & stars, squads, status sync, world sites
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MultiverseCommands::register);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.Ceremony::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MasteryPages::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.Squads::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::onLogin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.WorldSites::onServerTick);
        NeoForge.EVENT_BUS.addListener(FaithCommand::register);
        NeoForge.EVENT_BUS.addListener(LiarisFreeseSkill::onKill);
        NeoForge.EVENT_BUS.addListener(SkillEvolution::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(GrimoireAcceptance::onPlayerTick);
        // Summon Grimoire (base ability): floating book state
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.GrimoireSummon::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.GrimoireSummon::onDeath);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.GrimoireSummon::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.GrimoireSummon::onDimension);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.GrimoireSummon::onStartTracking);
        NeoForge.EVENT_BUS.addListener(GrimoireCommand::register);
        NeoForge.EVENT_BUS.addListener(VfxCommand::register);
        // Grimoire pages & Time magic engine
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.GrimoirePages::onDeath);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.TimeStop::onEntityTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.TimeStop::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.TimeStop::onAttack);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.TimeStop::onUseItem);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.TimeStop::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.TimedModifiers::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpellRuntime::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.ForbiddenMagic::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.ForbiddenMagic::onKill);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.MagicGear::onPlayerTick);
        // Spirit bond: what the spirit notices
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onDeath);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onHurt);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onBreak);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onPlace);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onUseFinish);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onDimension);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onWake);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onFish);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onTrade);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onTame);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onBreed);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SpiritBond::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.MagicGear::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.MagicGear::onDeath);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.MagicGear::onChat);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.MagicGear::onPickup);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.BlockHistory::onBreak);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.BlockHistory::onExplode);
    }
}
