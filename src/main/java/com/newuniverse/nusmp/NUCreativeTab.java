package com.newuniverse.nusmp;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The "Multiverse of Anime in Tensura" creative tabs (3.53.0: seven tabs instead of one). */
public final class NUCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NUSMP.MODID);

    /** 3.53.0: the creative menu is split into tabs; each item belongs to exactly one. */
    public enum G { GRIM, WEAPONS, ARMOR, RELICS, EGGS, DUNGEON, MISC }

    private static final CreativeModeTab.Output DISCARD = (stack, vis) -> {};

    /** Weapons by their main-hand attack damage, armor by item type, anything else in the last tab. */
    private static CreativeModeTab.Output autoTab(Item item, Function<G, CreativeModeTab.Output> o) {
        if (item instanceof ArmorItem) return o.apply(G.ARMOR);
        for (var e : item.getDefaultInstance().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers())
            if (e.attribute().is(Attributes.ATTACK_DAMAGE)) return o.apply(G.WEAPONS);
        return o.apply(G.MISC);
    }

    private static void fill(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output, G only) {
        Function<G, CreativeModeTab.Output> o = g -> only == null || g == only ? output : DISCARD;   // only == null: every group (the franchise tabs)
                // One of every cover state (Flame shown), then every magic as a three-leaf.
                for (GrimoireCover c : GrimoireCover.values()) {
                    MagicType m = c.isForbidden() ? MagicType.DARK : MagicType.FLAME;
                    o.apply(G.GRIM).accept(GrimoireItem.createUnbound(c, m, c.isForbidden() ? Devil.MEGICULA : null));
                }
                o.apply(G.GRIM).accept(GrimoireItem.createUnbound(GrimoireCover.FIVE_LEAF, MagicType.ANTI_MAGIC, Devil.LIEBE));
                o.apply(G.GRIM).accept(GrimoireItem.createUnbound(GrimoireCover.FIVE_LEAF, MagicType.KOTODAMA, Devil.ZAGRED));   // 0.47: creative only
                for (var it : com.newuniverse.nusmp.item.NUItems.all()) autoTab(it.get(), o).accept(it.get());
                for (var egg : com.newuniverse.nusmp.dungeon.floor.goliath.GoliathEntities.spawnEggs()) o.apply(G.EGGS).accept(egg.get());                                 // 1.23.0
                for (var egg : com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisEntities.spawnEggs()) o.apply(G.EGGS).accept(egg.get());
                for (net.minecraft.world.item.Item ci : com.newuniverse.nusmp.charter.CharterItems.all()) o.apply(G.GRIM).accept(ci);                   // 1.25.0: Charter Grimoires
                for (net.minecraft.world.item.ItemStack egg : com.newuniverse.nusmp.arc.ArcItems.creativeEggs()) o.apply(G.EGGS).accept(egg);          // 1.58.0: spawn eggs of the arc characters
                for (var it : com.newuniverse.nusmp.relics.RelicCatalog.creativeItems()) o.apply(G.RELICS).accept(it.get());                                  // 1.24.0: Relics of Many Worlds
                for (var egg : com.newuniverse.nusmp.relics.order.OrderEntities.spawnEggs()) o.apply(G.EGGS).accept(egg.get());
                o.apply(G.MISC).accept(com.newuniverse.nusmp.relics.order.OrderEntities.NOTICE.get());
                for (var d : com.newuniverse.nusmp.relics.djinn.Djinn.values()) o.apply(G.RELICS).accept(com.newuniverse.nusmp.relics.djinn.DjinnItems.sealStack(d));
                o.apply(G.RELICS).accept(com.newuniverse.nusmp.relics.djinn.DjinnItems.darkShardStack());
                o.apply(G.MISC).accept(com.newuniverse.nusmp.compat.CompatBookItem.stack());
                o.apply(G.MISC).accept(com.newuniverse.nusmp.compat.CompatItems.codexStack());                                  // 1.34.0: grimoire-style codex
                o.apply(G.MISC).accept(com.newuniverse.nusmp.engraving.codex.EngravingCodexItems.codexStack());                 // 1.99.0: the Engravement Codex
                if (params.hasPermissions()) for (net.minecraft.world.item.ItemStack w : com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItems.creativeStacks()) o.apply(G.RELICS).accept(w);   // 1.34.0: Djinn weapons (operators)                                     // 1.33.0: Compatibility Codex
                for (net.minecraft.world.item.Item ti : com.newuniverse.nusmp.relics.tier.TierItems.all()) o.apply(G.RELICS).accept(ti);                  // 1.30.0: catalysts
                for (var sc : com.newuniverse.nusmp.relics.socket.SocketItems.all()) o.apply(G.RELICS).accept(sc.get());                                 // Spirit and Devil Cores                                              // 1.25.0: Dark Rukh Shard
                for (var egg : com.newuniverse.nusmp.entity.avatar.AvatarEntities.spawnEggs()) o.apply(G.EGGS).accept(egg.get());              // 1.22.0
                for (var egg : com.newuniverse.nusmp.entity.zogratis.ZogratisEntities.spawnEggs()) o.apply(G.EGGS).accept(egg.get());      // 1.20.0: the Zogratis bosses
                for (var ai : com.newuniverse.nusmp.arena.ArenaSystem.creativeItems()) o.apply(G.MISC).accept(ai.get());                  // 1.46.0: the Hourglass of the Final Hour and the Return Sigil
                for (var ai : com.newuniverse.nusmp.arc.world.ArcWorldItems.creativeItems()) o.apply(G.MISC).accept(ai.get());            // 1.61.0: the Chronicle Sigil
                for (net.minecraft.world.item.ItemStack st : com.newuniverse.nusmp.skilltree.SkillBookSystem.creativeItems()) o.apply(G.MISC).accept(st);     // 1.96.0: one Skill Book per unique skill
                o.apply(G.WEAPONS).accept(com.newuniverse.nusmp.shishou.ShishouItems.creativeStack());                                      // Shishou: a bare stack; it becomes the sword only when no Shishou exists, else it dissolves (the one-per-world cap)
                o.apply(G.WEAPONS).accept(com.newuniverse.nusmp.greed.GreedItems.creativeStack());                                         // the Black Sword Greed: a bare stack; it becomes THE sword only when none exists (the 1-of-1 rule), else it dissolves
                for (net.minecraft.world.item.Item hw : com.newuniverse.nusmp.heroweapon.HeroWeaponSystem.creativeItems()) o.apply(G.WEAPONS).accept(hw);   // 1.62.0: the 21 hero weapons and the Calling Gems
                o.apply(G.WEAPONS).accept(com.newuniverse.nusmp.fairytail.storm.StormBoot.STAFF.get());                                          // Storm Magic (Fairy Tail): the Stormcaller Staff
                o.apply(G.WEAPONS).accept(com.newuniverse.nusmp.firecast.FireCastBoot.BAT.get());                                                // fire casting: Magna's Soul Bat
                for (var egg : com.newuniverse.nusmp.companion.sample.CompanionEntities.spawnEggs()) o.apply(G.EGGS).accept(egg.get());                       // companions: the sample companion's spawn egg (Charmy)
                for (var sb : com.newuniverse.nusmp.spiritblades.SpiritBladesBoot.creativeItems()) o.apply(G.WEAPONS).accept(sb.get());                  // the Wind Spirit blades: Spirit of Zephyr and Spirit of Boreas (creative only)
                for (var vl : com.newuniverse.nusmp.valkyrielance.ValkyrieLanceBoot.creativeItems()) o.apply(G.WEAPONS).accept(vl.get());                // the Valkyrie Drill Lance (creative only)
                for (var rs : com.newuniverse.nusmp.rebirth.RebirthItems.creativeItems()) o.apply(G.GRIM).accept(rs.get());                                  // the Sigil of Rebirth and the Sigil of the Second Book
                for (var db : com.newuniverse.nusmp.dragonballs.DbItems.creativeItems()) o.apply(G.MISC).accept(db.get());                                  // the Dragon Balls (2 sets x 8 stars) and the Dragon Radar
                for (var sw : com.newuniverse.nusmp.sinweapons.SinWeaponBoot.creativeItems()) o.apply(G.WEAPONS).accept(sw.get());                // the five other Berserk weapons (creative only)
                for (var lf : com.newuniverse.nusmp.lords.LordItems.renderables()) o.apply(G.WEAPONS).accept(lf.get());                // the new forms of the four Lord weapons (creative only; a bare stack becomes THE weapon only if none exists)
                o.apply(G.GRIM).accept(com.newuniverse.nusmp.block.NUBlocks.GRIMOIRE_ALTAR_ITEM.get());
                o.apply(G.RELICS).accept(com.newuniverse.nusmp.block.NUBlocks.VORMIR_ALTAR_ITEM.get());                                  // Infinity Stones Phase B: the altar of Vormir (creative only)
                for (net.minecraft.world.item.ItemStack bw : com.newuniverse.nusmp.beastwill.BeastWillItems.creativeStacks()) o.apply(G.RELICS).accept(bw);             // 3.45.0: one Beast Will core per shipped will
                for (net.minecraft.world.item.ItemStack mc : com.newuniverse.nusmp.manacore.ManaCoreItems.creativeStacks()) o.apply(G.RELICS).accept(mc);              // 3.51.0: the 24 rank Mana Cores
                com.newuniverse.nusmp.labyrinth.LabyrinthCreative.addTo(o.apply(G.DUNGEON));                                       // 1.35.0: the Labyrinth's blocks and items
                com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreItems.addTo(o.apply(G.DUNGEON), o.apply(G.ARMOR));              // the Boss Cores: seven cores (dungeon tab) and twenty-eight armour pieces (armour tab)
                com.newuniverse.nusmp.saiyan.armor.SaiyanArmorItems.addTo(o.apply(G.ARMOR), o.apply(G.MISC));                       // the craftable Saiyan alloy set (armour tab) and its ingot (misc tab); no Dragon Ball tab exists, so the Anime Worlds franchise tab (FranchiseTabs) shows them too
                com.newuniverse.nusmp.bossdrops2.BossSigItems.addTo(o.apply(G.RELICS));                                          // the boss signature drops and the echoes of the one-of-ones
                com.newuniverse.nusmp.bossdrops.BossDropItems.addTo(o.apply(G.RELICS), o.apply(G.ARMOR));                         // the boss trophies (relic tab) and the Trophy Plate / Lucifero armour (armour tab)

                for (var relic : new net.neoforged.neoforge.registries.DeferredItem[]{com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.GLUTTON_CHALICE, com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.MANTLE_OF_THE_SCAVENGER,
                        com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.BAND_OF_THE_SPATIAL_ANCHOR, com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.DEVIL_BOUND_SHEATH, com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.GAMBLERS_GRIMOIRE_PAGE,
                        com.newuniverse.nusmp.dungeon.nexus.relic.RelicItems.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS}) o.apply(G.RELICS).accept((net.minecraft.world.item.Item) relic.get());
                for (var it : com.newuniverse.nusmp.dungeon.sentient.SentientItems.all()) o.apply(G.DUNGEON).accept(it.get());                    // 1.22.0: synthesis materials
                for (var f : new net.neoforged.neoforge.registries.DeferredItem[]{com.newuniverse.nusmp.dungeon.DungeonFood.RAW_SLIME_JELLY, com.newuniverse.nusmp.dungeon.DungeonFood.RAW_ARMOR_FLANK,   // 1.19.0
                        com.newuniverse.nusmp.dungeon.DungeonFood.RAW_BASILISK_FILLET, com.newuniverse.nusmp.dungeon.DungeonFood.RAW_DIREWOLF_HAUNCH, com.newuniverse.nusmp.dungeon.DungeonFood.RAW_MONSTER_MEAT,
                        com.newuniverse.nusmp.dungeon.DungeonFood.RUINED_MEAT, com.newuniverse.nusmp.dungeon.DungeonFood.PURIFIED_MEAT, com.newuniverse.nusmp.dungeon.DungeonFood.CANDIED_SLIME_JELLY,
                        com.newuniverse.nusmp.dungeon.DungeonFood.GRILLED_ARMOR_FLANK, com.newuniverse.nusmp.dungeon.DungeonFood.ROAST_BASILISK_FILLET, com.newuniverse.nusmp.dungeon.DungeonFood.ROAST_DIREWOLF_HAUNCH,
                        com.newuniverse.nusmp.dungeon.DungeonFood.SEARED_MONSTER_MEAT}) o.apply(G.DUNGEON).accept((net.minecraft.world.item.Item) f.get());
                for (MagicType m : MagicType.values()) {
                    if (m == MagicType.ANTI_MAGIC || m == MagicType.KOTODAMA) continue;   // Kotodama: only Zagred's five-leaf below
                    o.apply(G.GRIM).accept(GrimoireItem.createUnbound(GrimoireCover.THREE_LEAF, m, null));
                }
                o.apply(G.MISC).accept(com.newuniverse.nusmp.avatar.item.AvatarItems.coreStack());                                        // 1.51.0: Genesis Core
                o.apply(G.MISC).accept(com.newuniverse.nusmp.avatar.item.SoulForgeBlocks.SOUL_FORGE_ITEM.get());                         // 1.58.0: the Soul Forge altar
                // the named canon grimoires (Fuegoleon, Yuno, Asta, Noelle, ...)
                for (var b : com.newuniverse.nusmp.grimoire.CanonBook.values()) o.apply(G.GRIM).accept(GrimoireItem.createCanon(b));
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String id, G g, java.util.function.Supplier<ItemStack> icon) {
        return TABS.register(id, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.anime_multiverse." + id))
                .icon(icon)
                .displayItems((params, output) -> fill(params, output, g))
                .build());
    }

    /** "main" keeps its id (saved menus, mods that reference it): it is the grimoires tab now. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = tab("main", G.GRIM, () -> GrimoireItem.createUnbound(5, MagicType.ANTI_MAGIC, Devil.LIEBE));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS = tab("weapons", G.WEAPONS, () -> new ItemStack(com.newuniverse.nusmp.item.NUItems.DEMON_SLAYER.get()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARMOR = tab("armor", G.ARMOR, () -> new ItemStack(com.newuniverse.nusmp.item.NUItems.SETS.get(0).chest().get()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RELICS = tab("relics", G.RELICS, () -> new ItemStack(com.newuniverse.nusmp.item.NUItems.SPIRIT_CHARM.get()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EGGS = tab("eggs", G.EGGS, () -> new ItemStack(Items.ZOMBIE_SPAWN_EGG));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DUNGEON = tab("dungeon", G.DUNGEON, () -> new ItemStack(com.newuniverse.nusmp.dungeon.DungeonFood.RAW_MONSTER_MEAT.get()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MISC = tab("misc", G.MISC, () -> new ItemStack(com.newuniverse.nusmp.compat.CompatItems.WIKI.get()));

    /** 3.88.0: the franchise tabs. Each one re-runs the whole fill and keeps the items its franchise owns (FranchiseTabs), so the older tabs stay as they were and an item can be in both. Empty (= hidden) when the client option is off. */
    private static void fillFranchise(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output, String id) {
        if (!com.newuniverse.nusmp.franchise.FranchiseTabsConfig.enabled()) return;
        fill(params, (stack, vis) -> {
            net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null && NUSMP.MODID.equals(key.getNamespace())
                    && id.equals(com.newuniverse.nusmp.franchise.FranchiseTabs.tabOf(key.getPath(), com.newuniverse.nusmp.franchise.FranchiseMap.franchiseOf(key.getPath())))) output.accept(stack, vis);
        }, null);
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> franchiseTab(String id, java.util.function.Supplier<ItemStack> icon) {
        return TABS.register("franchise_" + id, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.anime_multiverse.franchise_" + id))
                .icon(icon)
                .displayItems((params, output) -> fillFranchise(params, output, id))
                .build());
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> F_BLACK_CLOVER = franchiseTab("black_clover", () -> GrimoireItem.createUnbound(5, MagicType.ANTI_MAGIC, Devil.LIEBE));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> F_TENSURA = franchiseTab("tensura", () -> new ItemStack(com.newuniverse.nusmp.beastwill.BeastWillItems.CORE.get()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> F_BERSERK = franchiseTab("berserk", () -> com.newuniverse.nusmp.greed.GreedItems.creativeStack());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> F_FAIRY_TAIL = franchiseTab("fairy_tail", () -> new ItemStack(com.newuniverse.nusmp.fairytail.storm.StormBoot.STAFF.get()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> F_MAGI = franchiseTab("magi", () -> com.newuniverse.nusmp.relics.djinn.DjinnItems.darkShardStack());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> F_HERO_WEAPONS = franchiseTab("hero_weapons", () -> com.newuniverse.nusmp.heroweapon.HeroItems.stack(com.newuniverse.nusmp.heroweapon.HeroKind.SWORD));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> F_ANIME_WORLDS = franchiseTab("anime_worlds", () -> new ItemStack(Items.NETHER_STAR));

    private NUCreativeTab() {}
}
