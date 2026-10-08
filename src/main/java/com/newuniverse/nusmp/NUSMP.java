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
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(NUGameRules::init));   // 0.48: config switches as gamerules
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
        com.newuniverse.nusmp.sound.NUSounds.SOUNDS.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.entity.NUEntities::attributes);
        NUCreativeTab.TABS.register(modEventBus);
        modEventBus.addListener(VfxSpawn::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.book.GrimoireSummon::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.blackclover.ModeArmor::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.book.RougeCat::register);                             // 0.49: Rouge on her summoner's head
        modEventBus.addListener(com.newuniverse.nusmp.aura.PlayerAuraPayload::register);                     // 0.53: player render layers
        com.newuniverse.nusmp.prop.MagicProps.init();                                                      // 0.53: props of the attribute expansion
        modEventBus.addListener(com.newuniverse.nusmp.anim.SwordDrawPayload::register);
        modEventBus.addListener(com.newuniverse.nusmp.anim.CastAnimPayload::register);                      // 0.54: casting body animations                      // 0.52: the grimoire sword draw
        modEventBus.addListener(com.newuniverse.nusmp.entity.ZagredStatePayload::register);                 // 0.48: Zagred's state, word and reticle
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.newuniverse.nusmp.vfx.client.VfxClientEvents.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireClient.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireShelfClient.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireFloatClient.init();
            com.newuniverse.nusmp.client.mode.ModeArmorClient.init(modEventBus);
            com.newuniverse.nusmp.client.DreamSkyClient.init(modEventBus);
            com.newuniverse.nusmp.client.grimoire.GrimoireSlotClient.init(modEventBus);
            com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient.init(modEventBus);
            com.newuniverse.nusmp.client.multiverse.FourKingdomsSlot.init();                                   // 0.46: the slot in Tensura's magic screen
            modContainer.registerConfig(ModConfig.Type.CLIENT, com.newuniverse.nusmp.client.multiverse.MultiverseClientConfig.SPEC, "nusmp-multiverse-client.toml");
            // Spirit Lords are small floating orbs for now (SpiritLordRenderer, the full Tensura body, is kept for later)
            modEventBus.addListener(com.newuniverse.nusmp.client.SpiritOrbRenderer::register);
            modEventBus.addListener(com.newuniverse.nusmp.client.MirrorDoubleRenderer::register);
            modEventBus.addListener(com.newuniverse.nusmp.client.PaintedConstructRenderer::register);      // 0.44
            modEventBus.addListener(com.newuniverse.nusmp.client.PaintedConstructRenderer::layers);
            modEventBus.addListener(com.newuniverse.nusmp.client.ZagredRenderer::register);              // 0.47
            modEventBus.addListener(com.newuniverse.nusmp.client.ZagredRenderer::layers);
            modEventBus.addListener(com.newuniverse.nusmp.client.GrimoireDaemonRenderer::register);      // 0.52: Zagred's daemons
            modEventBus.addListener(com.newuniverse.nusmp.client.GrimoireDaemonRenderer::layers);
            modEventBus.addListener(com.newuniverse.nusmp.client.CottonSheepRenderer::register);        // 0.52: Cotton Magic's sheep and cloud
            modEventBus.addListener(com.newuniverse.nusmp.client.CottonSheepRenderer::layers);
            modEventBus.addListener(com.newuniverse.nusmp.client.WindSpiritLordRenderer::register);          // 0.53: Sylph, the Wind Spirit Lord boss
            com.newuniverse.nusmp.client.DemonSlayerRenderer.init(modEventBus);
            com.newuniverse.nusmp.client.aura.PlayerAuraClient.init(modEventBus);                             // 0.53: player render layers
            modEventBus.addListener(com.newuniverse.nusmp.client.prop.MagicPropRenderer::register);           // 0.53: props
            com.newuniverse.nusmp.client.geo.GeoModels.init(modEventBus);                                      // 0.53: geo.json models re-read on a resource reload
            com.newuniverse.nusmp.client.SwordDrawClient.init();                                              // 0.52: the draw animation
            com.newuniverse.nusmp.client.WeaponRenderer.init(modEventBus);                                  // 0.50: every weapon in 3D
            modEventBus.addListener(com.newuniverse.nusmp.client.RougeCatLayer::layers);                    // 0.49: Rouge's cat model
            modEventBus.addListener(com.newuniverse.nusmp.client.RougeCatLayer::addLayers);                            // 0.48: the Genesis Demon-Slayer's 3D model and shaders
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
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.DreamWorld::onServerStopping);
        // 0.42: Mirror Magic (array interception, lethal-hit shatter, Full Reflection)
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.MirrorWorks::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.MirrorWorks::onIncomingDamage);
        // 0.44: Painting Magic remake - palette & brush, mood, element counter
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.PaintStudio::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.PaintStudio::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.PaintStudio::onDeath);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.PaintStudio::onUseItemFinish);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.SlashBook::onIncomingDamage);              // 0.45: forearm blades
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.KotodamaWords::onChat);                    // 0.47: Kotodama command words
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.KotodamaWords::onIncomingDamage);          // 0.48: "Reverse"
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.antimagic.AntiMagic::migrate);                  // 0.48: old Spirit Lords fold into the grimoire
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.GrimoireBook::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.TimeBook::passiveTick);                    // 0.49: Time Magic's passive every tick               // 0.48: book upkeep without relying on skill ticks
        // 0.53: the hook point of the 37 attributes (passives, buffs that last, damage rewrites; see book.ext.AttributeEvents)
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.ext.AttributeEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.ext.AttributeEvents::onDamaged);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.ext.AttributeEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.ext.AttributeEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.ext.AttributeEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.ext.AttributeEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.WeaponEngravings::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.BossRelics::onIncomingDamage);              // 0.52: Shroud of Margins
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.aura.PlayerAuras::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.aura.PlayerAuras::onStartTracking);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.entity.ZagredAttacks::onHeal);                 // 0.52: Overwrite's "No healing"              // 0.50: Tensura engravings on the weapons
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.TreeRestore::onServerTick);                // 0.48: World Tree trees taken back (saved)
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.antimagic.NihilityZone::onServerTick);          // 0.48: Black Meteorite's Nihility zones
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> com.newuniverse.nusmp.antimagic.NihilityZone.clear());
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.UnderworldMatter::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.book.UnderworldMatter::onServerStopping);
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
