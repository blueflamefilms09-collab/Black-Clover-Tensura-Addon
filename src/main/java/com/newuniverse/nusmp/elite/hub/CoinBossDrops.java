package com.newuniverse.nusmp.elite.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.skilltree.loot.LootVictims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the decennial Guardian is the one monster a global loot modifier can never see (its class rolls no loot table: the Mana Core comes from its altar, so {@link CoinLootModifier} is never asked),
 * and the owner wants coins from the Decennial Bosses. So, exactly as {@code BossBookDrops} does for the skill books, the boss pays through the other door every kill goes through,
 * {@code LivingDropsEvent}: the Guardian carries the sector tag, a PLAYER killed it, it is the Mega-Dungeon, coins are the currency, the {@code doMobLoot} rule is on, and the pure
 * {@link CoinLoot} rolls the BOSS payout at the boss floor of the sector it guards ({@link LootVictims#guardianSite}); a Guardian made by hand for a test belongs to no sector and pays nothing.
 * The coins are item entities in the best coin mix, invulnerable so lava or an explosion cannot eat the prize. It only ADDS to the drops, at LOW priority, and never throws out of the event.
 */
public final class CoinBossDrops {
    private CoinBossDrops() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /** The death listener (game bus, LOW priority). */
    public static void onDrops(LivingDropsEvent event) {
        try {
            process(event, null, HubConfig.lootParams(), null);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] the Guardian's coin drop failed once and is skipped for that death", ex);
        }
    }

    /**
     * Decides and adds the coins of one Guardian death.
     *
     * @param dice the random source, or null for the level's own
     * @param site the sector the Guardian guards, or null to read it from the Guardian (the self-test passes a made-up one)
     * @return the value added, in bronze coins
     */
    public static long process(LivingDropsEvent event, Dice dice, CoinLoot.Params params, LootVictims.Site site) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !MegaDungeon.in(level) || !Currency.coins()) return 0;
        if (!dead.getTags().contains(LabyrinthGuardianEntity.SECTOR_TAG) || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return 0;
        ServerPlayer killer = LootVictims.killer(event.getSource(), dead);
        if (killer == null) return 0;
        LootVictims.Site where = site != null ? site : LootVictims.guardianSite(level, dead);
        if (where == null) return 0;
        Dice d = dice != null ? dice : level.getRandom()::nextDouble;
        long value = CoinLoot.roll(CoinLoot.Source.BOSS, where.floor(), 0, params, d);
        if (value <= 0) return 0;
        List<ItemStack> stacks = CoinWallet.stacksFor(value);
        for (ItemStack stack : stacks) {
            ItemEntity drop = new ItemEntity(level, dead.getX(), dead.getY() + 0.3, dead.getZ(), stack);
            drop.setInvulnerable(true);
            event.getDrops().add(drop);
        }
        return stacks.isEmpty() ? 0 : value;
    }
}
