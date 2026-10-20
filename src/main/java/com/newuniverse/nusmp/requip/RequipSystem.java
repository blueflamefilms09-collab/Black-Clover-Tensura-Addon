package com.newuniverse.nusmp.requip;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.newuniverse.nusmp.skill.ErzaRequipSkill;
import com.newuniverse.nusmp.skill.ErzaSelfTests;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the one entry point of Requip: The Knight that the mod's constructor calls ({@code NUSMP}): it attaches the item register, the admin command, the dungeon loot and the in-game self-test. The
 * skill itself is {@code skill.ErzaRequipSkill} (registered in {@code NUSkills}); the armour table is {@link Armour}, the rules {@link RequipRules}. This class also holds the two ways of learning
 * the skill: the book ({@link TitaniasRequipItem}, gated) and the admin command (not gated).
 */
public final class RequipSystem {
    private RequipSystem() {}

    private static boolean started;

    /** Loot tables that may hold a Titania's Requip, with the chance (0..1) each opening has of adding one. Vanilla vaults of old magic plus this mod's own dungeon vaults. */
    static final Map<String, Float> LOOT = Map.ofEntries(
            Map.entry("minecraft:chests/ancient_city", 0.06f), Map.entry("minecraft:chests/end_city_treasure", 0.05f),
            Map.entry("minecraft:chests/stronghold_library", 0.05f), Map.entry("minecraft:chests/bastion_treasure", 0.05f),
            Map.entry("minecraft:chests/woodland_mansion", 0.04f), Map.entry("minecraft:chests/simple_dungeon", 0.03f),
            Map.entry("minecraft:chests/trial_chambers/reward_unique", 0.08f),
            Map.entry("nusmp:chests/dungeon_relic_vault", 0.10f), Map.entry("nusmp:chests/library_ruins", 0.06f),
            Map.entry("nusmp:chests/grimoire_tower", 0.08f), Map.entry("nusmp:chests/hidden_cache", 0.04f),
            Map.entry("nusmp:chests/labyrinth_pedestal", 0.06f));

    /** Called once from the mod constructor, before the item register is frozen. */
    public static void init(IEventBus modBus) {
        if (started) return;
        started = true;
        RequipItems.REGISTER.register(modBus);
        NeoForge.EVENT_BUS.addListener(RequipSystem::onCommands);
        NeoForge.EVENT_BUS.addListener(RequipSystem::onLoot);
        ErzaSelfTests.register();                                          // the CI server smoke test runs skill.erza_requip
        RequipMagic.init(modBus);                                          // the Equip Magic: the pocket Armoury, Loadouts, Blade / Barrage / Strike, Partial Armour, the finishers (self-test skill.requip_magic)
    }

    // ---------------------------------------------------------------- learning
    /** The reader's count of Unique skills at full mastery (a mastered Unique counts toward the book's gate). */
    public static int masteredUniques(ServerPlayer p) {
        int n = 0;
        try {
            for (ManasSkillInstance i : new ArrayList<>(SkillAPI.getSkillsFrom(p).getLearnedSkills())) {
                if (i.getSkill() instanceof Skill s && s.getType() == Skill.SkillType.UNIQUE && i.isMastered(p)) n++;
            }
        } catch (Throwable noSkills) { /* a player without skill data has none */ }
        return n;
    }

    /** Teaches the skill. {@code force} skips the EP gate (the admin command). Returns a reason on failure, or null on success. */
    public static String teach(ServerPlayer p, boolean force) {
        var skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(NUSkills.ERZA_REQUIP.getId()).isPresent()) return "You already know Requip: The Knight.";
        if (!force) {
            double ep = com.newuniverse.nusmp.antimagic.Nullification.maxEP(p);
            if (!RequipRules.eligible(ep, masteredUniques(p)))
                return "The book stays closed: it wants " + (long) RequipRules.REQUIRED_EP + " max EP, or " + (long) RequipRules.REQUIRED_EP_WITH_UNIQUE + " with a mastered Unique skill.";
        }
        ManasSkillInstance inst = NUSkills.ERZA_REQUIP.get().createDefaultInstance();
        if (!skills.learnSkill(inst, Component.literal("Titania's Requip opens: the Knight's armours are yours.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)))
            return "The skill could not be learned (another mod refused it).";
        return null;
    }

    // ---------------------------------------------------------------- loot
    private static void onLoot(LootTableLoadEvent e) {
        Float chance = LOOT.get(e.getName().toString());
        if (chance == null) return;
        e.getTable().addPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1f)).when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(RequipItems.TITANIAS_REQUIP.get())).build());
    }

    // ---------------------------------------------------------------- admin command
    private static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("requip").requires(s -> s.hasPermission(2))
                .then(Commands.literal("book").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    p.getInventory().placeItemBackInInventory(new ItemStack(RequipItems.TITANIAS_REQUIP.get()));
                    ctx.getSource().sendSuccess(() -> Component.literal("Gave Titania's Requip to " + p.getName().getString()), true);
                    return 1;
                })))
                .then(Commands.literal("teach").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    String why = teach(p, true);
                    if (why != null) { ctx.getSource().sendFailure(Component.literal(why)); return 0; }
                    ctx.getSource().sendSuccess(() -> Component.literal("Taught Requip: The Knight to " + p.getName().getString()), true);
                    return 1;
                })))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("armour", StringArgumentType.word())
                                .suggests((c, b) -> { for (Armour a : Armour.values()) b.suggest(a.id()); return b.buildFuture(); })
                                .executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    Armour a = Armour.byId(StringArgumentType.getString(ctx, "armour").toLowerCase(Locale.ROOT));
                                    if (a == null) { ctx.getSource().sendFailure(Component.literal("No such armour.")); return 0; }
                                    String why = ErzaRequipSkill.adminEquip(p, a);
                                    if (why != null) { ctx.getSource().sendFailure(Component.literal(why)); return 0; }
                                    ctx.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + " now wears " + a.displayName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("dismiss").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    ErzaRequipSkill.adminDismiss(p);
                    CommandSourceStack src = ctx.getSource();
                    src.sendSuccess(() -> Component.literal("Dismissed the armour of " + p.getName().getString()), true);
                    return 1;
                }))));
    }

    /** The id of a registered resource under this mod's namespace (helper for tests). */
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }
}
