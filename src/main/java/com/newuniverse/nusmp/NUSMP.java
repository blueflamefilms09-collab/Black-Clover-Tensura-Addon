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
        com.newuniverse.nusmp.blackclover.GrimoireSlot.ATTACHMENTS.register(modEventBus);
        com.newuniverse.nusmp.blackclover.GrimoireSlot.MENUS.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.blackclover.GrimoireSlot::registerPayloads);
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
        modEventBus.addListener(com.newuniverse.nusmp.blackclover.ModeArmor::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::registerPayloads);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.newuniverse.nusmp.vfx.client.VfxClientEvents.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireClient.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireShelfClient.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireFloatClient.init();
            com.newuniverse.nusmp.client.mode.ModeArmorClient.init(modEventBus);
            com.newuniverse.nusmp.client.DreamSkyClient.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireSlotClient.init(modEventBus);
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
        // 0.39: The Convergence (origin roll, stages, awakening), secret quests, grimoire guard
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.Convergence::onLogin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.Convergence::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.Convergence::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.Convergence::onServerStarted);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.SecretQuests::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.SecretQuests::onKill);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.GrimoireGuard::onPlayerTick);
        com.newuniverse.nusmp.blackclover.GrimoireGuard.registerTensuraHooks();
        com.newuniverse.nusmp.multiverse.FtbQuestsChapter.install();
        // 0.41: Dream Magic's pocket dimension, Painting Magic's camouflage
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.DreamWorld::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.DreamWorld::onDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.DreamWorld::onTeleport);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.DreamWorld::onDeath);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.DreamWorld::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.DreamWorld::onLogin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.DreamWorld::onChat);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) -> {
            if (e.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) com.newuniverse.nusmp.book.PaintingBook.breakCamouflage(sp);
        });
        // races: Tensura's race + this mod's buff (Devil is command only)
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.RaceBuffs::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) -> com.newuniverse.nusmp.multiverse.RaceBuffs.reset(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> com.newuniverse.nusmp.multiverse.RaceBuffs.reset(e.getEntity()));
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
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.ModeArmor::onStartTracking);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.ModeArmor::onLogin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.ModeArmor::onDeath);
        // Grimoire Slot (0.22): the bound book lives here, dormant at the hip
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.GrimoireSlot::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.GrimoireSlot::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.GrimoireSlot::onRespawn);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.GrimoireSlot::onDimension);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.GrimoireSlot::onStartTracking);
        NeoForge.EVENT_BUS.addListener(GrimoireCommand::register);
        NeoForge.EVENT_BUS.addListener(VfxCommand::register);
        // Grimoire pages & Time magic engine
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
