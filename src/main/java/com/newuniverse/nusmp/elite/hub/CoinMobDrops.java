package com.newuniverse.nusmp.elite.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import com.newuniverse.nusmp.skilltree.loot.LootRules;
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
 * Why: the global loot modifier ({@link CoinLootModifier}) pays every monster whose class rolls a loot table, but a creature whose class overrides {@code dropFromLootTable} to do nothing never
 * reaches it: the 400 Mob Foundry mobs ({@code nusmp:foundry_mob}) are exactly that (their loot is the economy's listeners, not a table), and so is any other mob a mod makes that way. The owner
 * wants coins from "all Abyssal/Holy mobs (and the 400 foundry mobs)", so the same payout is also made from the other door every death goes through, {@code LivingDropsEvent}, for the creatures the
 * modifier did NOT see. The two never double pay: the modifier stamps the creature with {@link #PAID_TAG} the moment it decides (whether it pays or not), and this listener skips a creature that
 * carries it. The rules are the modifier's: Mega-Dungeon only, coins as the currency, a PLAYER's kill, an ordinary hostile monster ({@link LootVictims#kindOf}) or a foundry mob that is not a summoner's
 * minion, a floor that is not the Hub, and the pure {@link CoinLoot} roll with the killer's Looting. LOW priority, only ADDS to the drops, never throws out of the event.
 */
public final class CoinMobDrops {
    private CoinMobDrops() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /** The scoreboard tag the loot modifier puts on a creature it has already decided for. */
    public static final String PAID_TAG = "nusmp_hub_coin_decided";

    /** The death listener (game bus, LOW priority). */
    public static void onDrops(LivingDropsEvent event) {
        try {
            process(event, null, HubConfig.lootParams());
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] the dungeon mob coin drop failed once and is skipped for that death", ex);
        }
    }

    /**
     * Decides and adds the coins of one death.
     *
     * @param dice the random source, or null for the level's own
     * @return the value added, in bronze coins
     */
    public static long process(LivingDropsEvent event, Dice dice, CoinLoot.Params params) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !MegaDungeon.in(level) || !Currency.coins() || dead.getTags().contains(PAID_TAG)) return 0;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return 0;
        boolean foundry = dead instanceof FoundryMobEntity;
        if (foundry ? ((FoundryMobEntity) dead).isMinion() : LootVictims.kindOf(dead) != LootRules.Kind.MONSTER) return 0;
        ServerPlayer killer = LootVictims.killer(event.getSource(), dead);
        if (killer == null) return 0;
        dead.addTag(PAID_TAG);
        int floor = MegaDungeon.floorAt(level, dead.blockPosition());
        if (floor == 0 && foundry) floor = ((FoundryMobEntity) dead).floor();
        Dice d = dice != null ? dice : level.getRandom()::nextDouble;
        long value = CoinLoot.roll(CoinLoot.Source.MOB, floor, CoinLootModifier.looting(level, killer), params, d);
        if (value <= 0) return 0;
        List<ItemStack> stacks = CoinWallet.stacksFor(value);
        for (ItemStack stack : stacks) event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY() + 0.3, dead.getZ(), stack));
        return stacks.isEmpty() ? 0 : value;
    }
}
