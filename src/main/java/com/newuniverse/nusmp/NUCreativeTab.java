package com.newuniverse.nusmp;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The "Multiverse of Anime in Tensura" creative tab. */
public final class NUCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NUSMP.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.nusmp.main"))
            .icon(() -> GrimoireItem.createUnbound(5, MagicType.ANTI_MAGIC, Devil.LIEBE))
            .displayItems((params, output) -> {
                // One of every cover state (Flame shown), then every magic as a three-leaf.
                for (GrimoireCover c : GrimoireCover.values()) {
                    MagicType m = c.isForbidden() ? MagicType.DARK : MagicType.FLAME;
                    output.accept(GrimoireItem.createUnbound(c, m, c.isForbidden() ? Devil.MEGICULA : null));
                }
                output.accept(GrimoireItem.createUnbound(GrimoireCover.FIVE_LEAF, MagicType.ANTI_MAGIC, Devil.LIEBE));
                output.accept(GrimoireItem.createUnbound(GrimoireCover.FIVE_LEAF, MagicType.KOTODAMA, Devil.ZAGRED));   // 0.47: creative only
                for (var it : com.newuniverse.nusmp.item.NUItems.all()) output.accept(it.get());
                for (var egg : com.newuniverse.nusmp.dungeon.floor.goliath.GoliathEntities.spawnEggs()) output.accept(egg.get());                                 // 1.23.0
                for (var egg : com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisEntities.spawnEggs()) output.accept(egg.get());
                for (net.minecraft.world.item.Item ci : com.newuniverse.nusmp.charter.CharterItems.all()) output.accept(ci);                   // 1.25.0: Charter Grimoires
                for (var it : com.newuniverse.nusmp.relics.RelicCatalog.creativeItems()) output.accept(it.get());                                  // 1.24.0: Relics of Many Worlds
                for (var egg : com.newuniverse.nusmp.relics.order.OrderEntities.spawnEggs()) output.accept(egg.get());
                output.accept(com.newuniverse.nusmp.relics.order.OrderEntities.NOTICE.get());
                for (var d : com.newuniverse.nusmp.relics.djinn.Djinn.values()) output.accept(com.newuniverse.nusmp.relics.djinn.DjinnItems.sealStack(d));
                output.accept(com.newuniverse.nusmp.relics.djinn.DjinnItems.darkShardStack());                                              // 1.25.0: Dark Rukh Shard
                for (var egg : com.newuniverse.nusmp.entity.avatar.AvatarEntities.spawnEggs()) output.accept(egg.get());              // 1.22.0
                for (var egg : com.newuniverse.nusmp.entity.zogratis.ZogratisEntities.spawnEggs()) output.accept(egg.get());      // 1.20.0: the Zogratis bosses
                output.accept(com.newuniverse.nusmp.block.NUBlocks.GRIMOIRE_ALTAR_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.DungeonBlocks.DUNGEON_MAW_ITEM.get());           // 1.17.0
                output.accept(com.newuniverse.nusmp.dungeon.DungeonBlocks.DUNGEON_GATE_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.DungeonBlocks.MAW_ROOT_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.DungeonBlocks.DUNGEON_MYCELIUM_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.city.CityBlocks.AURELIAN_ALTAR_ITEM.get());          // 1.19.0
                output.accept(com.newuniverse.nusmp.dungeon.city.CityBlocks.AURELIAN_CORE.get());
                output.accept(com.newuniverse.nusmp.dungeon.MawReward.MAW_FRUIT.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.vault.NexusBlocks.ILLUSION_STONE_ITEM.get());                      // 1.21.0: the Variable Core
                output.accept(com.newuniverse.nusmp.dungeon.nexus.vault.NexusBlocks.ILLUSION_DEEPSLATE_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.vault.NexusBlocks.DESCENT_GATE_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.vault.NexusBlocks.WARP_TRAP_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.vault.NexusBlocks.NUTRIENT_BLOOM_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.vault.NexusBlocks.NUTRIENT_NECTAR.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.relic.RelicBlocks.ARTIFACT_BLOCK_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.relic.RelicBlocks.MANA_GATE_ITEM.get());
                for (var relic : new net.neoforged.neoforge.registries.DeferredItem[]{com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.GLUTTON_CHALICE, com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.MANTLE_OF_THE_SCAVENGER,
                        com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.BAND_OF_THE_SPATIAL_ANCHOR, com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.DEVIL_BOUND_SHEATH, com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.GAMBLERS_GRIMOIRE_PAGE,
                        com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS}) output.accept((net.minecraft.world.item.Item) relic.get());
                for (var it : com.newuniverse.nusmp.dungeon.sentient.SentientItems.all()) output.accept(it.get());                    // 1.22.0: synthesis materials
                output.accept(com.newuniverse.nusmp.dungeon.nexus.zone.GoldenBlocks.GOLDEN_LEAVES_ITEM.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.nav.NavItems.KI_RESONANCE_LANTERN.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.nav.NavItems.ANTI_BIRD_COMPASS.get());
                output.accept(com.newuniverse.nusmp.dungeon.nexus.nav.NavItems.VENOM_GLAND.get());
                for (var f : new net.neoforged.neoforge.registries.DeferredItem[]{com.newuniverse.nusmp.dungeon.DungeonFood.RAW_SLIME_JELLY, com.newuniverse.nusmp.dungeon.DungeonFood.RAW_ARMOR_FLANK,   // 1.19.0
                        com.newuniverse.nusmp.dungeon.DungeonFood.RAW_BASILISK_FILLET, com.newuniverse.nusmp.dungeon.DungeonFood.RAW_DIREWOLF_HAUNCH, com.newuniverse.nusmp.dungeon.DungeonFood.RAW_MONSTER_MEAT,
                        com.newuniverse.nusmp.dungeon.DungeonFood.RUINED_MEAT, com.newuniverse.nusmp.dungeon.DungeonFood.PURIFIED_MEAT, com.newuniverse.nusmp.dungeon.DungeonFood.CANDIED_SLIME_JELLY,
                        com.newuniverse.nusmp.dungeon.DungeonFood.GRILLED_ARMOR_FLANK, com.newuniverse.nusmp.dungeon.DungeonFood.ROAST_BASILISK_FILLET, com.newuniverse.nusmp.dungeon.DungeonFood.ROAST_DIREWOLF_HAUNCH,
                        com.newuniverse.nusmp.dungeon.DungeonFood.SEARED_MONSTER_MEAT}) output.accept((net.minecraft.world.item.Item) f.get());
                for (MagicType m : MagicType.values()) {
                    if (m == MagicType.ANTI_MAGIC || m == MagicType.KOTODAMA) continue;   // Kotodama: only Zagred's five-leaf below
                    output.accept(GrimoireItem.createUnbound(GrimoireCover.THREE_LEAF, m, null));
                }
                // the named canon grimoires (Fuegoleon, Yuno, Asta, Noelle, ...)
                for (var b : com.newuniverse.nusmp.grimoire.CanonBook.values()) output.accept(GrimoireItem.createCanon(b));
            })
            .build());

    private NUCreativeTab() {}
}
