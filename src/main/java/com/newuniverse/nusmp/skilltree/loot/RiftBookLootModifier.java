package com.newuniverse.nusmp.skilltree.loot;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.skilltree.rarity.RarityConfig;
import com.newuniverse.nusmp.skilltree.rarity.RarityRules;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the third door of the skill-book loot (the first two are {@link SkillBookLootModifier} for the Abyssal monsters and {@link BossBookDrops} for the decennial Guardians): the chests of the
 * dimensional rifts' pocket realms. A pocket is a hidden, timed, instanced vault behind a secret boss or a puzzle, so what it holds is meant to be rare, and a global loot modifier is the same
 * tool as for the monsters: it works on the loot that has ALREADY been rolled, so the three chests (which use the dungeon's relic-vault and trial tables, untouched) keep everything they
 * gave and a book is ADDED beside it. Its numbers are DATA: {@code data/nusmp/loot_modifiers/rift_books.json} lists which loot tables count and the chance of a book for each ({@code tables}),
 * so a datapack can add a table or retune a number with no code, and the server's config scales all of them at once ({@code riftBookScale}, 0 switches the rift books off). The chests only count
 * when they stand INSIDE a pocket's cell ({@link RiftBookRules#inPocket}), because the same two tables also fill ordinary chests of the main dungeon and those must not pay. Which line the book
 * opens is {@link LootDrops#routeBook}: weighted by {@code loot_weight}, from the cached table, with the pity rule for the player who opened the chest. Server side; an exception is logged
 * once and the loot goes through unchanged: a loot roll must never fail because of a book.
 */
public final class RiftBookLootModifier extends LootModifier {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static final MapCodec<RiftBookLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .and(Codec.unboundedMap(ResourceLocation.CODEC, Codec.doubleRange(0.0, 1.0)).fieldOf("tables").forGetter(m -> m.rawTables))
            .apply(inst, RiftBookLootModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);

    /** {@code nusmp:rift_books}, the type id the JSON names. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<RiftBookLootModifier>> TYPE = MODIFIERS.register("rift_books", () -> CODEC);

    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    private final Map<ResourceLocation, Double> rawTables;
    private final Map<String, Double> tables;

    public RiftBookLootModifier(LootItemCondition[] conditions, Map<ResourceLocation, Double> tables) {
        super(conditions);
        this.rawTables = Map.copyOf(tables);
        Map<String, Double> byString = new HashMap<>();
        for (Map.Entry<ResourceLocation, Double> e : tables.entrySet()) byString.put(e.getKey().toString(), e.getValue());
        this.tables = Map.copyOf(byString);
    }

    /** The tables this modifier pays for and their chances, by id string. */
    public Map<String, Double> tables() { return tables; }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            add(generatedLoot, context, tables, LootConfig.get(LootConfig.RIFT_BOOK_SCALE), null);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] the rift Skill Book loot modifier failed once and is skipped for that roll", ex);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }

    /**
     * The whole decision with the live inputs: the Mega-Dungeon only, {@code riftBooks} and {@code enabled} on, the queried table listed, the chest's origin inside a pocket cell, then the roll,
     * then one route book added.
     *
     * @param dice the random source, or null for the loot context's own
     * @return the number of stacks added (0 or 1)
     */
    public static int add(ObjectArrayList<ItemStack> loot, LootContext context, Map<String, Double> tables, double scale, Dice dice) {
        return add(loot, context, tables, scale, LootConfig.rarity(), dice);
    }

    /**
     * The same with the rarity settings given (3.26.0). {@code rarityTiers} off: the 3.25 rule untouched (the chance as listed, a weighted pick of any line). On: the chance is multiplied by
     * {@code min(3, 1 + 0.1 * Luck)} of the opening player and the book is a MYTHIC line the opener does not own yet, an epic one when none is left ({@link LootDrops#riftBook}). The Luck is read only after the
     * table, the pocket and the config have said yes, so an ordinary chest costs nothing.
     */
    public static int add(ObjectArrayList<ItemStack> loot, LootContext context, Map<String, Double> tables, double scale, RarityConfig rarity, Dice dice) {
        ServerLevel level = context.getLevel();
        if (level == null || !MegaDungeon.in(level) || !LootConfig.get(LootConfig.ENABLED) || !LootConfig.get(LootConfig.RIFT_BOOKS)) return 0;
        ResourceLocation table = context.getQueriedLootTableId();
        if (table == null) return 0;
        double base = RiftBookRules.chance(tables, table.toString(), scale);
        if (base <= 0.0) return 0;
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null || !RiftBookRules.inPocket(origin.x, origin.z)) return 0;
        Dice d = dice != null ? dice : context.getRandom()::nextDouble;
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        ServerPlayer opener = entity instanceof ServerPlayer p ? p : null;
        double chance = rarity.tiers() ? RarityRules.riftChance(base, LootStats.luckOf(opener)) : base;
        if (!RiftBookRules.decide(true, true, chance, d)) return 0;
        ItemStack book = rarity.tiers() ? LootDrops.riftBook(d, opener) : LootDrops.routeBook(d, opener);
        if (book.isEmpty()) return 0;
        loot.add(book);
        return 1;
    }
}
