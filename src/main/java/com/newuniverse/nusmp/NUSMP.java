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
        com.newuniverse.nusmp.dungeon.DungeonBlocks.init();                                                // 1.17.0: the Living Dungeon
        com.newuniverse.nusmp.dungeon.DungeonFood.init();                                                  // 1.19.0: its food
        com.newuniverse.nusmp.dungeon.city.CityBlocks.init();                                              // 1.19.0: the Golden City's altar and core
        com.newuniverse.nusmp.dungeon.MawReward.init();                                                    // 1.19.0: the Maw Fruit
        com.newuniverse.nusmp.dungeon.DungeonBlocks.BLOCKS.register(modEventBus);
        com.newuniverse.nusmp.dungeon.city.CityBlocks.BLOCKS.register(modEventBus);
        com.newuniverse.nusmp.dungeon.DungeonEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.dungeon.DungeonEntities::attributes);
        com.newuniverse.nusmp.dungeon.DungeonEffects.EFFECTS.register(modEventBus);
        com.newuniverse.nusmp.dungeon.LivingDungeon.ATTACHMENTS.register(modEventBus);
        com.newuniverse.nusmp.dungeon.wild.Dweller.ATTACHMENTS.register(modEventBus);                      // 1.19.0: the real mobs of the Dungeon (hunger, home, sleep)
        com.newuniverse.nusmp.entity.zogratis.ZogratisEntities.init();                                    // 1.20.0: the Zogratis bosses
        com.newuniverse.nusmp.entity.zogratis.ZogratisEffects.init();
        com.newuniverse.nusmp.entity.zogratis.ZogratisEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.entity.zogratis.ZogratisEntities::attributes);
        com.newuniverse.nusmp.entity.zogratis.ZogratisEffects.EFFECTS.register(modEventBus);
        com.newuniverse.nusmp.entity.zogratis.VanicaCurse.EFFECTS.register(modEventBus);
        com.newuniverse.nusmp.dungeon.nexus.vault.NexusBlocks.init();                                                    // 1.21.0: the Variable Core (vault blocks)
        com.newuniverse.nusmp.dungeon.nexus.vault.NexusBlocks.BLOCKS.register(modEventBus);
        com.newuniverse.nusmp.relics.order.OrderEntities.init();                                                                  // 1.24.0: Relics of Many Worlds, the God of Order, the Djinn
        com.newuniverse.nusmp.relics.order.OrderEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.relics.order.OrderEntities::attributes);
        com.newuniverse.nusmp.relics.djinn.DjinnItems.init();
        com.newuniverse.nusmp.relics.djinn.DjinnComponents.COMPONENTS.register(modEventBus);
        com.newuniverse.nusmp.relics.djinn.DjinnAttachments.ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.relics.djinn.DjinnNet::registerPayloads);
        com.newuniverse.nusmp.core.discovery.Discovery.init(modEventBus, modContainer);                                    // 1.27.0: what a player has not found stays hidden
        modEventBus.addListener(com.newuniverse.nusmp.core.magic.merge.MergedSync::register);                              // 1.27.0: merged magics
        com.newuniverse.nusmp.relics.tier.TierSystem.registerContent(modEventBus);                                         // 1.30.0: tier component, catalysts (before NUItems.ITEMS.register)
        com.newuniverse.nusmp.relics.socket.SocketItems.init();                                                            // 1.30.0: Spirit and Devil Cores
        com.newuniverse.nusmp.relics.socket.SocketComponents.COMPONENTS.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.compat.CompatPacks::onPacks);                       // 1.33.0: the Tensura No Index bridge pack, only when that mod is loaded
        com.newuniverse.nusmp.compat.CompatItems.init();                                                                   // 1.34.0: the Compatibility Codex item (before NUItems.ITEMS.register)
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.ArmorEngravings::onPlayerTick);   // 1.34.0: armour engravings
        com.newuniverse.nusmp.charter.CharterItems.init();                                                                 // 1.25.0: Charter Grimoires (before NUItems.ITEMS.register)
        modEventBus.addListener(com.newuniverse.nusmp.charter.CharterNet::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.boss.bar.BossBarNet::registerPayloads);
        modEventBus.addListener(com.newuniverse.nusmp.relics.wave.WaveBossNet::register);
        modEventBus.addListener(com.newuniverse.nusmp.relics.RelicHolderSync::registerPayload);
        modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.charter.CharterConfig.SPEC, "nusmp-charter-server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.boss.budget.BossBudgetConfig.SPEC, "nusmp-boss-budget-server.toml");      // 1.30.0: boss damage per second cap
        com.newuniverse.nusmp.relics.RelicSystem.registerContent(modEventBus);                                                     // runs RelicCatalog.init: every relic into NUItems.ITEMS
        com.newuniverse.nusmp.dungeon.sentient.SentientItems.init();                                                     // 1.22.0: the twelve synthesis materials
        com.newuniverse.nusmp.dungeon.floor.goliath.GoliathEntities.init();                                                                  // 1.23.0: the floor arenas and their bosses
        com.newuniverse.nusmp.dungeon.floor.goliath.GoliathEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.dungeon.floor.goliath.GoliathEntities::attributes);
        com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisEntities.init();
        com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisEntities::attributes);
        com.newuniverse.nusmp.entity.avatar.AvatarEntities.init();                                                       // 1.22.0: the Devil Avatar
        com.newuniverse.nusmp.entity.avatar.AvatarEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.newuniverse.nusmp.entity.avatar.AvatarEntities::attributes);
        com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.init();                                                     // the Treasury's five Relics
        com.newuniverse.nusmp.dungeon.nexus.relic.RelicBlocks.init();
        com.newuniverse.nusmp.dungeon.nexus.relic.RelicBlocks.BLOCKS.register(modEventBus);
        com.newuniverse.nusmp.dungeon.nexus.relic.RelicComponents.COMPONENTS.register(modEventBus);
        com.newuniverse.nusmp.dungeon.nexus.nav.NavComponents.COMPONENTS.register(modEventBus);                          // lantern and compass
        com.newuniverse.nusmp.dungeon.nexus.nav.NavItems.init();
        com.newuniverse.nusmp.dungeon.nexus.zone.GoldenBlocks.init();                                                    // the Golden City's leaves
        com.newuniverse.nusmp.dungeon.nexus.zone.GoldenBlocks.BLOCKS.register(modEventBus);
        com.newuniverse.nusmp.world.NUWorldgen.FEATURES.register(modEventBus);
        com.newuniverse.nusmp.labyrinth.LabyrinthSystem.init(modEventBus);                                                  // 1.35.0: the Labyrinth (blocks, gate, trials, seals, vault, processors; block items go on NUItems.ITEMS so this runs before its register)
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
        modEventBus.addListener(com.newuniverse.nusmp.dungeon.nexus.relic.RelicNet::registerPayloads);                  // 1.22.0: the Use Relic key
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
            com.newuniverse.nusmp.client.compat.CompatCodexClient.init(modEventBus);                                           // 1.34.0
            com.newuniverse.nusmp.client.labyrinth.LabyrinthClient.init(modEventBus);                                        // 1.35.0: Labyrinth renderers, sky
            com.newuniverse.nusmp.client.discovery.DiscoveryClient.init();                                                    // 1.27.0
            com.newuniverse.nusmp.client.merge.MergedClient.init(modEventBus);
            com.newuniverse.nusmp.client.ui.theme.UiThemeClient.init(modEventBus);                                           // 1.27.0: the anime GUI theme
            modContainer.registerConfig(ModConfig.Type.CLIENT, com.newuniverse.nusmp.client.ui.theme.UiThemeOptions.SPEC, "nusmp-ui-client.toml");
            modContainer.registerConfig(ModConfig.Type.CLIENT, com.newuniverse.nusmp.client.relics.fx.RelicFxOptions.SPEC, "nusmp-relicfx-client.toml");     // 1.30.0: relic effects quality
            com.newuniverse.nusmp.client.ElsdociaGlitterClient.init();
            com.newuniverse.nusmp.client.gui.bossbar.ModBossBarOverlay.init();                                                      // 1.25.0: animated boss bar
            modContainer.registerConfig(ModConfig.Type.CLIENT, com.newuniverse.nusmp.client.gui.bossbar.BossBarOptions.SPEC, "nusmp-bossbar-client.toml");
            modContainer.registerConfig(ModConfig.Type.CLIENT, com.newuniverse.nusmp.client.multiverse.MultiverseClientConfig.SPEC, "nusmp-multiverse-client.toml");
            // 0.102: the Tensura-style Spirit Lords are small floating orbs (SpiritLordEntity); the exclusive Black Clover ones have bodies (below)
            modEventBus.addListener(com.newuniverse.nusmp.client.SpiritOrbRenderer::register);
            modEventBus.addListener(com.newuniverse.nusmp.client.ElementalSpiritRenderer::register);        // 0.102: the Black Clover Spirit Lords' true bodies
            modEventBus.addListener(com.newuniverse.nusmp.client.MirrorDoubleRenderer::register);
            modEventBus.addListener(com.newuniverse.nusmp.client.PaintedConstructRenderer::register);      // 0.44
            modEventBus.addListener(com.newuniverse.nusmp.client.PaintedConstructRenderer::layers);
            modEventBus.addListener(com.newuniverse.nusmp.client.ZagredRenderer::register);              // 0.47
            modEventBus.addListener(com.newuniverse.nusmp.client.ZagredRenderer::layers);
            modEventBus.addListener(com.newuniverse.nusmp.client.GrimoireDaemonRenderer::register);      // 0.52: Zagred's daemons
            modEventBus.addListener(com.newuniverse.nusmp.client.GrimoireDaemonRenderer::layers);
            modEventBus.addListener(com.newuniverse.nusmp.client.wizard.TowerWizardClient::register);        // 1.6.0
            modEventBus.addListener(com.newuniverse.nusmp.client.dungeon.DungeonMobsClient::register);          // 1.18.0: the Living Dungeon's creatures
            modEventBus.addListener(com.newuniverse.nusmp.client.dungeon.DungeonSky::registerEffects);         // 1.19.0: the Dungeon's own sky and fog
            modEventBus.addListener(com.newuniverse.nusmp.client.zogratis.ZogratisClient::register);            // 1.20.0: the Zogratis renderers
            com.newuniverse.nusmp.client.relics.RelicClient.init(modEventBus);                                                   // 1.24.0
            modEventBus.addListener(com.newuniverse.nusmp.client.relics.order.OrderClient::register);
            com.newuniverse.nusmp.client.relics.djinn.DjinnClient.init(modEventBus);
            modEventBus.addListener(com.newuniverse.nusmp.client.floor.goliath.GoliathClient::register);                            // 1.23.0
            modEventBus.addListener(com.newuniverse.nusmp.client.floor.charybdis.CharybdisClient::register);
            modEventBus.addListener(com.newuniverse.nusmp.client.avatar.AvatarClient::register);                                    // 1.22.0: the Devil Avatar
            modEventBus.addListener(com.newuniverse.nusmp.client.nexus.RelicKeys::register);                                         // 1.22.0: Use Relic (R)
            NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.client.nexus.RelicKeys::onClientTick);
            modEventBus.addListener(com.newuniverse.nusmp.client.nexus.NavClient::registerProperties);                              // 1.21.0: lantern flame, compass needle
            NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.client.dungeon.DungeonSky::onFogColor);
            NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.client.dungeon.DungeonSky::onRenderFog);
            NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.client.dungeon.DungeonSky::onClientTick);
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
        modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.dungeon.DungeonConfig.SPEC, "nusmp-dungeon-server.toml");        modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.labyrinth.LabyrinthConfig.SPEC, "nusmp-labyrinth-server.toml");   // 1.35.0

        // Multiverse: /multiverse commands, ceremony, mastery pages, ranks & stars, squads, status sync, world sites
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MultiverseCommands::register);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.Ceremony::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MasteryPages::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.Squads::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.wizard.WizardLibrary::onServerTick);             // 1.11.0: the flying books
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.wizard.WizardLibrary::onBreak);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.wizard.WizardSystem::onPlayerTick);              // 1.6.0: the summons
        io.github.manasmods.tensura.event.TensuraEntityEvents.AWAKENING_EVENT.register((entity, flag) ->      // 1.8.0: the Wizard King cannot awaken as a hero or a demon lord
                entity instanceof net.minecraft.world.entity.player.Player pl && com.newuniverse.nusmp.multiverse.WizardKing.isKing(pl)
                        ? dev.architectury.event.EventResult.interruptFalse() : dev.architectury.event.EventResult.pass());
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.WizardKing::onPlayerTick);            // 1.6.0: one Wizard King
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.DungeonEcology::onFinalizeSpawn);          // 1.17.0: the Living Dungeon
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.DungeonEcology::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.DungeonEcology::onBreak);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.DungeonEcology::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.DungeonEcology::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.DungeonEcology::onDrops);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.NexusBuilder::onServerTick);                      // 1.21.0: replaces CityBuilder::onServerTick: a core cell builds as one of four zones (the Golden City is one)
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.NexusBuilder::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.vault.VaultDirector::onServerTick);              // hidden vaults, descent gates
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.vault.Distortion::onPlayerTick);                 // spatial distortion
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.vault.Distortion::onPlace);
        com.newuniverse.nusmp.dungeon.nexus.relic.RelicEvents.register(NeoForge.EVENT_BUS);                                  // the five Relics
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, com.newuniverse.nusmp.dungeon.nexus.nav.AvianDrops::onDrops);   // the Anti-Bird Compass
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.nav.NavTicker::onPlayerTick);                    // the Ki-Resonance Lantern
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.MonarchDriver::onJoin);                     // Monarch's Vault
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.MonarchDriver::onServerTick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, com.newuniverse.nusmp.dungeon.nexus.zone.MonarchDriver::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.MonarchDriver::onDeath);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, com.newuniverse.nusmp.dungeon.nexus.zone.MonarchDriver::onDrops);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.MonarchDriver::onCollapse);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.MonarchDriver::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.GravityWarp::onServerTick);                 // Underworld Gate
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.GravityWarp::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.GravityWarp::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.UnderworldGateSite::onCollapse);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.HiveDriver::onServerTick);                  // Tempest Mirage
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.HiveDriver::onJoin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.HiveDriver::onLeave);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.HiveDriver::onDeath);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, com.newuniverse.nusmp.dungeon.nexus.zone.HiveDriver::onDrops);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.HiveDriver::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.HiveDriver::onCollapse);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.zone.HiveDriver::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.collapse.CollapseManager::onCollapse);                      // the Treasury Collapse
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.collapse.CollapseManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.collapse.CollapseManager::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, true, com.newuniverse.nusmp.dungeon.nexus.collapse.CollapseManager::onFallingBlockJoin);   // takes back the guard's cancel for the collapse's own falling blocks
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.nexus.collapse.CollapseManager::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.DungeonAmbience::onServerTick);           // 1.19.0: the Dungeon's ambient sounds
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.MealSkills::onPlayerTick);                // 1.19.0: skills lent by cooked meals
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.MealSkills::onLogin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.MealSkills::onClone);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.MealAttunement::onDamage);                // 1.19.0: a second magic's rider from a meal
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.MawReward::onBreak);                      // 1.19.0: who cut a Maw's roots
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.wild.WildDriver::onJoin);                 // 1.19.0: roster mobs get their record
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.wild.WildDriver::onServerTick);           // the food web sweep
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.wild.WildDriver::onDeath);                // kills feed the killer
        // 1.24.0: Relics of Many Worlds (engine + every wave's hooks), the Order, the Djinn
        com.newuniverse.nusmp.relics.RelicSystem.registerGame(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.relics.order.OrderEvents.register(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.relics.djinn.DjinnEvents.register(NeoForge.EVENT_BUS);
        // 1.25.0: Mod Boss Global Buff Engine, Charter Grimoires + first-kill lock, boss bar sync, holder flags
        com.newuniverse.nusmp.core.magic.merge.MergeEvents.register(NeoForge.EVENT_BUS);                                    // 1.27.0 (also runs the magic audit at start)
        com.newuniverse.nusmp.relics.tier.TierSystem.registerGame(NeoForge.EVENT_BUS);                                      // 1.30.0
        com.newuniverse.nusmp.relics.scaling.ScalingSystem.registerGame(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.relics.socket.SocketEngine.register(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.relics.socket.SocketLoot.register(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.boss.BossHooks.register();
        com.newuniverse.nusmp.charter.CharterEvents.register(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.boss.bar.BossBarSync.init();
        com.newuniverse.nusmp.relics.RelicHolderSync.register(NeoForge.EVENT_BUS);
        // 1.23.0: the floor arenas (Goliath's Pit, the Charybdis Basin)
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.floor.goliath.GoliathPitSite::onCollapse);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.floor.goliath.GoliathPitSite::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisBasinSite::onCollapse);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisBasinSite::onServerStopping);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisMinions::onDrops);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisMinions::onIncomingDamage);
        // 1.22.0: the Sentient Dungeon (room births, born mobs, the immune system)
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.DungeonRoomManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.DungeonRoomManager::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.DungeonRoomManager::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.WallBirth::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.WallBirth::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.BornDriver::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.BornDriver::onJoin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.BornDriver::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.ImmuneSystem::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.ImmuneSystem::onDeath);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.ImmuneSystem::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.sentient.ImmuneSystem::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.dungeon.wild.WildDriver::onServerStarted);        // logs the roster at start-up
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.entity.zogratis.VanicaCurse::onHeal);              // 1.20.0: Megicula's Decay cuts healing
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.entity.zogratis.SpellSeal::onIncomingDamage);       // Spatial Mana Domination
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, com.newuniverse.nusmp.entity.zogratis.SoulPaladins::onDeath);   // Lucius's immortal Paladins
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, com.newuniverse.nusmp.entity.zogratis.SoulPaladins::onIncomingDamage);
        io.github.manasmods.manascore.skill.api.SkillEvents.ACTIVATE_SKILL.register((sealSkill, sealUser, sealKey, sealMode) -> com.newuniverse.nusmp.entity.zogratis.SpellSeal.vetoesSkill(sealSkill.get(), sealUser) ? dev.architectury.event.EventResult.interruptFalse() : dev.architectury.event.EventResult.pass());
        io.github.manasmods.manascore.skill.api.SkillEvents.RELEASE_SKILL.register((sealSkill, sealUser, sealKey, sealMode, sealHeld) -> com.newuniverse.nusmp.entity.zogratis.SpellSeal.vetoesSkill(sealSkill.get(), sealUser) ? dev.architectury.event.EventResult.interruptFalse() : dev.architectury.event.EventResult.pass());
        io.github.manasmods.manascore.skill.api.SkillEvents.TOGGLE_SKILL.register((sealSkill, sealUser) -> com.newuniverse.nusmp.entity.zogratis.SpellSeal.vetoesSkill(sealSkill.get(), sealUser) ? dev.architectury.event.EventResult.interruptFalse() : dev.architectury.event.EventResult.pass());
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onBreak);                // 1.19.0: the unbreakable Dungeon
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onLeftClick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onExplosion);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onLivingDestroy);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onMobGriefing);     // LOWEST on purpose: setCanGrief is not a cancel, the last listener wins
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onPiston);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onToolUse);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onFarmlandTrample);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.newuniverse.nusmp.dungeon.DungeonGuard::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.CuriosLink::onPlayerTick);                  // 1.15.0: weapons worn in Curios slots
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.mana.Heritage::onPlayerTick);                   // 1.6.0: kingdom, blood, stage buffs
        modEventBus.addListener(com.newuniverse.nusmp.wizard.WizardNet::registerPayloads);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.mana.ManaLaw::onPlayerTick);                    // 1.5.0: mana zone, corruption
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::onLogin);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.MultiverseSync::onLogout);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.multiverse.WorldSites::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.blackclover.GrimoireGuard::onPlayerTick);
        com.newuniverse.nusmp.blackclover.GrimoireGuard.registerTensuraHooks();
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
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.MagebaneShieldArts::onIncomingDamage);     // the Magebane Shield: magic damage cut, Spellbreak Parry
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.item.BroochPinArts::onIncomingDamage);          // the Brooch Pin: Ruby Ward absorbs damage
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
