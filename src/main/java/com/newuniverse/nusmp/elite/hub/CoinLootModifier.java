package com.newuniverse.nusmp.elite.hub;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.loot.LootConfig;
import com.newuniverse.nusmp.labyrinth.loot.LuckGauss;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.skilltree.loot.LootVictims;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the owner asked for Tensura coins to be injected into the drop tables of "all Abyssal/Holy mobs, Secret Rift chests and Decennial bosses" with Global Loot Modifiers, and a global loot modifier
 * is the right door for the first two: it works on loot that has ALREADY been rolled, whoever rolled it, so a monster from any mod (and the 400 {@code nusmp:foundry_mob}, which is one entity type
 * with no table of its own) and every chest the dungeon places pay the same way without one mob class or loot-table file being edited, and nothing the table gave is replaced (add the new, keep
 * the old). Same style as {@code SkillBookLootModifier}: a {@link LootModifier} with a codec registered on the mod bus under {@code nusmp:hub_coins}, switched on by one entry in
 * {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}, the shipped JSON ({@code data/nusmp/loot_modifiers/hub_coins.json}) carrying the standard empty {@code conditions} list because the
 * decision is in code. Two cases. (1) A DEATH: the victim is an ordinary hostile monster of the dungeon ({@link LootVictims#kindOf}: a vanilla {@code Enemy} or one of the Dungeon Master's creatures,
 * never a boss or a servant), a PLAYER did the killing (so a monster that died to another in a turf war is no farm), it stood on a Holy or Abyssal floor (never the Hub), and the pure
 * {@link CoinLoot} rolls the payout at that floor with the killer's Looting. (2) A CHEST: any loot table under {@code chests/} rolled in the Mega-Dungeon (the dungeon's own chests) adds coins at
 * the chest's floor; inside a Secret Rift pocket the amount is the rift multiplier and the floor is the pocket's own. When coins are the currency the chest's Magic Stones and crystals are
 * taken out of the roll (the economy is REPLACED), unless {@code economy.keepStoneDrops}. A creature whose class rolls no loot table at all (the 400 Mob Foundry mobs, which are one entity type with
 * {@code dropFromLootTable} switched off) never reaches a modifier, so {@link CoinMobDrops} pays those from {@code LivingDropsEvent}, and the decennial Guardian, which also rolls no table, is paid
 * by {@link CoinBossDrops}; a creature this modifier has decided for carries {@link CoinMobDrops#PAID_TAG} so the two never pay twice. Does nothing unless
 * {@link Currency#coins()}. An exception is logged once and the loot goes through unchanged: a loot roll must never fail because of a coin. Server side.
 */
public final class CoinLootModifier extends LootModifier {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static final MapCodec<CoinLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, CoinLootModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);

    /** {@code nusmp:hub_coins}, the type id the JSON names. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<CoinLootModifier>> TYPE = MODIFIERS.register("hub_coins", () -> CODEC);

    private static final ResourceKey<Enchantment> LOOTING = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("looting"));

    /** The item paths of the Magic Stone economy that a chest loses in coin mode (the ids {@code EconomyDrops} drops). */
    static final Set<String> STONE_PATHS = Set.of("magic_stone", "low_quality_magic_crystal", "medium_quality_magic_crystal", "high_quality_magic_crystal");

    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    public CoinLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            add(generatedLoot, context, HubConfig.lootParams(), null);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] the coin loot modifier failed once and is skipped for that roll", ex);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }

    /** Is this loot table one of a chest? The vanilla convention: {@code chests/...} in any namespace. */
    public static boolean isChestTable(ResourceLocation id) { return id != null && id.getPath().startsWith("chests/"); }

    /**
     * The whole decision with the live inputs.
     *
     * @param dice the random source, or null for the loot context's own
     * @return the value added, in bronze coins (0 when nothing was added)
     */
    public static long add(ObjectArrayList<ItemStack> loot, LootContext context, CoinLoot.Params params, Dice dice) {
        ServerLevel level = context.getLevel();
        if (level == null || !MegaDungeon.in(level) || !Currency.coins()) return 0;
        Dice d = dice != null ? dice : context.getRandom()::nextDouble;
        Entity subject = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        long value;
        if (subject instanceof LivingEntity dead && !(dead instanceof net.minecraft.world.entity.player.Player) && context.getParamOrNull(LootContextParams.DAMAGE_SOURCE) != null) {
            value = deathValue(level, dead, context, params, d);
        } else if (isChestTable(context.getQueriedLootTableId())) {
            value = chestValue(level, context, params, d);
            if (!HubConfig.get(HubConfig.KEEP_STONE_DROPS)) stripStones(loot);
        } else {
            return 0;
        }
        if (value <= 0) return 0;
        loot.addAll(CoinWallet.stacksFor(value));
        return value;
    }

    private static long deathValue(ServerLevel level, LivingEntity dead, LootContext context, CoinLoot.Params params, Dice d) {
        if (LootVictims.kindOf(dead) != com.newuniverse.nusmp.skilltree.loot.LootRules.Kind.MONSTER) return 0;
        ServerPlayer killer = LootVictims.killer(context);
        if (killer == null) return 0;
        dead.addTag(CoinMobDrops.PAID_TAG);                      // decided: the death listener for table-less mobs must not roll a second time
        int floor = MegaDungeon.floorAt(level, dead.blockPosition());
        return CoinLoot.roll(CoinLoot.Source.MOB, floor, looting(level, killer), luck(killer.getLuck()), luckParams(), params, d);
    }

    /** 1.107.0: the Luck that scales a payout: the payer's Luck attribute, or 0 when {@code luck.scaleCoins} is off. */
    static double luck(double raw) { return LootConfig.get(LootConfig.LUCK_COINS) ? LuckGauss.luck(raw) : 0.0; }

    /** 1.107.0: the coin Luck curve from the config. */
    static LuckGauss.Params luckParams() { return LootConfig.luckCoins(); }

    /** The Looting level of the killer's main-hand weapon (0 if it has none or the enchantment is missing); the same read as the dungeon economy's. */
    static int looting(ServerLevel level, ServerPlayer killer) {
        Optional<? extends Holder<Enchantment>> found = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(LOOTING);
        return found.isEmpty() ? 0 : Math.max(0, killer.getMainHandItem().getEnchantments().getLevel(found.get()));
    }

    private static long chestValue(ServerLevel level, LootContext context, CoinLoot.Params params, Dice d) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) return 0;
        int slot = RiftRules.slotAt(origin.x, origin.z);
        int looting = 0;
        if (slot >= 0) {
            PocketRealm.Pocket pocket = PocketRealm.get(level.getServer()).at(slot);
            if (pocket == null || pocket.floor == 0) return 0;
            return CoinLoot.roll(CoinLoot.Source.RIFT_CHEST, pocket.floor, looting, luck(context.getLuck()), luckParams(), params, d);
        }
        int floor = MegaDungeon.floorAt(level, BlockPos.containing(origin));
        return CoinLoot.roll(CoinLoot.Source.CHEST, floor, looting, luck(context.getLuck()), luckParams(), params, d);
    }

    /** Takes the Magic Stone economy's items out of a chest roll (coins replace them). */
    static int stripStones(ObjectArrayList<ItemStack> loot) {
        int removed = 0;
        for (int i = loot.size() - 1; i >= 0; i--) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(loot.get(i).getItem());
            if (id != null && "tensura".equals(id.getNamespace()) && STONE_PATHS.contains(id.getPath())) {
                loot.remove(i);
                removed++;
            }
        }
        return removed;
    }
}
