package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.quests.NusmpEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the owner's fourth brief says the Normal, Special and Rare gear must drop "from mobs AND standard chests" and asks for Global Loot Modifier JSONs for the
 * dungeon. Mobs are {@link GearDrops}; this is the chest side, and a NeoForge global loot modifier is the right tool because it works on the loot that has
 * ALREADY been rolled, whoever rolled it: a plain chest, a Lootr container (which rolls the table once per opening player), a chest minecart, a mod's own container,
 * a table that is added later by another builder or a data pack. No template, no chest block and no loot-table JSON has to be edited, and nothing in the original
 * tables is replaced (the new stack is added beside what the table gave: add the new, keep the old). It extends {@link LootModifier}, NeoForge's base class,
 * so the JSON carries the standard {@code conditions} list: the shipped {@code nusmp:gear_chests} only applies where the vanilla {@code location_check} says the
 * dimension is {@code nusmp:mega_dungeon}, and the code asks {@code MegaDungeon.in} as well (belt and braces: a pack that edits the conditions still cannot make gear
 * appear in the Overworld, the Spire, or the Sanctuary Hub, which is a different dimension and is not a Labyrinth). The decision itself is the pure
 * {@link ChestGear} over the pure {@link ChestRules}: the table must be a chest table, never one of the two relic tables, and the roll adds AT MOST ONE standard-gear
 * stack. It never rolls a table, never touches the relic ledger and never adds a relic (the stack carries only the {@code nusmp:gear_grade} component), so it cannot loop
 * and cannot leak a Unique-or-higher relic: the Crucible stays their only source. The floor comes from the chest's own position ({@code ORIGIN}), the Luck from the
 * context, and when a player opened the chest the quest key {@code mega.gear} fires for the grade (first of each grade only; a fake player is ignored). Server side;
 * the codec is registered on the mod bus by {@link GearRegistry#init} and switched on by the entry in {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}.
 * An exception is logged once and the loot goes through unchanged: a loot roll must never fail because of a gear piece.
 */
public final class ChestLootModifier extends LootModifier {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static final MapCodec<ChestLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, ChestLootModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);

    /** {@code nusmp:gear_chests}, the type id the JSON names. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<ChestLootModifier>> TYPE = MODIFIERS.register("gear_chests", () -> CODEC);

    /** Registers the modifier type (mod bus; called from {@link GearRegistry#init}). */
    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    public ChestLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            add(generatedLoot, context);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] Mega-Dungeon chest gear failed once and is skipped for that roll", ex);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }

    /**
     * The whole decision with the live inputs: in the Mega-Dungeon only, {@code gear.enabled} on, the queried table a chest table that is no relic table, then the
     * pure roll at the floor of the chest with the opener's Luck. Adds the stack to the list.
     *
     * @return the number of pieces added: 0 or 1, never more
     */
    public static int add(ObjectArrayList<ItemStack> loot, LootContext context) {
        ServerLevel level = context.getLevel();
        if (level == null || !MegaDungeon.in(level) || !GearConfig.get(GearConfig.ENABLED)) return 0;
        ResourceLocation table = context.getQueriedLootTableId();
        if (table == null) return 0;
        GearTables tables = GearTablesReload.tables();
        ChestRules.Kind kind = tables.chests().classify(table.getNamespace(), table.getPath());
        if (kind == ChestRules.Kind.NONE) return 0;
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        int floor = origin == null ? 0 : MegaDungeon.floorAt(level, BlockPos.containing(origin));
        Dice dice = context.getRandom()::nextDouble;
        Optional<GearSpec> spec = ChestGear.decide(dice, kind, GearConfig.chestRates(), GearConfig.get(GearConfig.CHEST_TREASURE_FACTOR), tables, floor,
                ChestGear.luckLevels(context.getLuck()));
        if (spec.isEmpty()) return 0;
        ItemStack stack = GearFactory.create(spec.get(), level.registryAccess());
        if (stack.isEmpty()) return 0;
        loot.add(stack);
        if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer opener) NusmpEvents.fireOnce(opener, GearEvents.QUEST_KEY, spec.get().grade().id());
        return 1;
    }
}
