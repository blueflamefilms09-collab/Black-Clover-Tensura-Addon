package com.newuniverse.nusmp.skilltree.loot;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.skilltree.rarity.RarityConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the owner asked for the Skill Books to drop from the Mega-Dungeon's Abyssal monsters as a Global Loot Modifier, and a NeoForge global loot modifier is the right
 * tool because it works on the loot that has ALREADY been rolled, whoever rolled it: every kind of monster, from this mod or another, with its own table or none of the
 * mod's doing, gets the same behaviour without a single mob class, loot-table JSON or event hook being edited, and nothing the table gave is replaced (the new stacks are
 * ADDED beside it: add the new, keep the old). It extends {@link LootModifier}, NeoForge's base class, so the shipped JSON ({@code data/nusmp/loot_modifiers/skill_books.json})
 * carries the standard {@code conditions} list, which is empty because the decision is in the code (one place to read it: {@link LootRules}, with the dimension, the road,
 * the kill and the kind of monster all checked there, and the self-test proves the Overworld and the Holy road get nothing). The codec is registered on the mod bus under
 * {@code nusmp:skill_books} by {@link #register} and switched on by the entry in {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}.
 *
 * <p>It serves the ORDINARY monsters. The sector Guardian never reaches it, and that is a fact of the game and not a choice: the Guardian overrides {@code dropFromLootTable}
 * to do nothing (the Mana Core comes from its altar), so no table is ever rolled for it and a loot modifier is never asked. Its book drop is {@link BossBookDrops}, a death
 * listener that asks the very same rule and builds the very same stacks. To keep that from ever doubling, this class refuses a victim that carries the Guardian tag even
 * if a pack gave it a table. The entry point {@link #add} takes the odds and the dice as arguments (the live ones when called from the pipeline), which is also what the
 * self-test calls with a made-up kill. An exception is logged once and the loot goes through unchanged: a loot roll must never fail because of a book. Server side.
 */
public final class SkillBookLootModifier extends LootModifier {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static final MapCodec<SkillBookLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, SkillBookLootModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);

    /** {@code nusmp:skill_books}, the type id the JSON names. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<SkillBookLootModifier>> TYPE = MODIFIERS.register("skill_books", () -> CODEC);

    /** Registers the modifier type (mod bus; called from {@code ItemsSystem.init}). */
    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    public SkillBookLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            add(generatedLoot, context, LootConfig.chances(), null);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] the Skill Book loot modifier failed once and is skipped for that kill", ex);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }

    /**
     * The whole decision with the live inputs: the Mega-Dungeon only, {@code enabled} on, the victim a living ordinary monster (never the Guardian), then the pure rule at the
     * road of the sector it stood in, then the stacks added to the list.
     *
     * @param dice the random source, or null for the loot context's own
     * @return the number of stacks added
     */
    public static int add(ObjectArrayList<ItemStack> loot, LootContext context, LootRules.Chances chances, Dice dice) {
        return add(loot, context, chances, LootConfig.rarity(), dice);
    }

    /**
     * The same with the rarity settings given (3.26.0): {@code rarityTiers} off is the 3.25 rule untouched; on, the chance is the base chance times the Luck and Looting multiplier (read once, here) and the book's rarity is
     * drawn from the tier-1 weights. The Guardian is still refused (it is {@link BossBookDrops}'), so one death never pays twice.
     */
    public static int add(ObjectArrayList<ItemStack> loot, LootContext context, LootRules.Chances chances, RarityConfig rarity, Dice dice) {
        ServerLevel level = context.getLevel();
        if (level == null || !MegaDungeon.in(level) || !LootConfig.get(LootConfig.ENABLED)) return 0;
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof LivingEntity dead)) return 0;
        LootRules.Kind kind = LootVictims.kindOf(dead);
        if (kind != LootRules.Kind.MONSTER) return 0;                       // the Guardian is BossBookDrops' (it rolls no table); anything else never pays
        ServerPlayer killer = LootVictims.killer(context);
        Dice d = dice != null ? dice : context.getRandom()::nextDouble;
        LootRules.Victim victim = new LootRules.Victim(true, killer != null, kind, LootVictims.polarityAt(level, dead), LootVictims.floorAt(level, dead));
        double luck = rarity.tiers() ? LootStats.luckOf(killer) : 0.0;                           // read once per drop, and only when the tiers use it
        int looting = rarity.tiers() ? LootStats.lootingOf(level, killer, context.getParamOrNull(LootContextParams.DAMAGE_SOURCE)) : 0;
        List<ItemStack> stacks = LootDrops.materialize(LootRules.plan(victim, chances, rarity, luck, looting, d), d, killer, null);
        for (ItemStack s : stacks) loot.add(s);
        return stacks.size();
    }
}
