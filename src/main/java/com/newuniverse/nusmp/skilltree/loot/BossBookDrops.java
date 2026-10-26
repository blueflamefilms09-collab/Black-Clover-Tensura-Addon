package com.newuniverse.nusmp.skilltree.loot;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.skilltree.rarity.RarityConfig;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
 * Why: the decennial Guardian is the one monster a global loot modifier can never see. Its class overrides {@code dropFromLootTable} to do nothing (the Mana Core that
 * opens the way on comes from its altar, and "it drops NOTHING" is a rule of the guard module), so no loot table is rolled when it dies and {@link SkillBookLootModifier}
 * is never called. The boss branch of the owner's drop (a high chance of a route book and always one to three SP Books) therefore needs the other door that every
 * kill goes through: {@code LivingDropsEvent}, the same event the dungeon's equipment drop and the Metal Vessel reward already use for exactly this Guardian. This
 * listener asks the SAME rule ({@link LootRules#decide}) and builds the SAME stacks ({@link LootDrops}) as the modifier, so the odds and the contents are defined once and
 * the two doors only differ in how a stack reaches the ground (added to the rolled list there, spawned as an item entity here). It acts only on an entity that carries the
 * Guardian's scoreboard tag, in the Mega-Dungeon, killed by a player, with {@code enabled} on and the {@code doMobLoot} game rule on; the sector it guards (its road and its
 * boss floor) is read from the sector id the guard module stores on it, and a Guardian made by hand for a test belongs to no sector and pays nothing. A route book that
 * falls here is a prize worth keeping safe in a dark hall: it glows, cannot burn or be blown up, and does not despawn. It runs at LOW priority, after the listeners that
 * decided what the Guardian drops before it, and only ADDS to the list. Never throws out of the event: a failure is logged once and that death drops no book.
 */
public final class BossBookDrops {
    private BossBookDrops() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /** The death listener (game bus, LOW priority). */
    public static void onDrops(LivingDropsEvent event) {
        try {
            process(event, null, LootConfig.chances(), null);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] the Guardian's Skill Book drop failed once and is skipped for that death", ex);
        }
    }

    /**
     * Decides and adds the books of one Guardian death.
     *
     * @param dice   the random source, or null for the level's own
     * @param site   the sector the Guardian guards, or null to read it from the Guardian (the self-test passes a made-up one)
     * @return the number of stacks added
     */
    public static int process(LivingDropsEvent event, Dice dice, LootRules.Chances chances, LootVictims.Site site) {
        return process(event, dice, chances, LootConfig.rarity(), site);
    }

    /**
     * The same with the rarity settings given (3.26.0). {@code rarityTiers} off: the 3.25 rule untouched. On: the Guardian drops ONE themed book at {@code guardianBookChance} (1.0 = guaranteed), of the
     * rarity its boss floor asks for, picked deterministically from the sector's Region Theme and the floor ({@link LootDrops#guardianBook}); this door is the only one that serves a Guardian (the loot
     * modifier refuses it), so a death never pays twice.
     */
    public static int process(LivingDropsEvent event, Dice dice, LootRules.Chances chances, RarityConfig rarity, LootVictims.Site site) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return 0;
        if (!dead.getTags().contains(LabyrinthGuardianEntity.SECTOR_TAG)) return 0;
        if (!LootConfig.get(LootConfig.ENABLED) || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return 0;
        ServerPlayer killer = LootVictims.killer(event.getSource(), dead);
        if (killer == null) return 0;
        LootVictims.Site where = site != null ? site : LootVictims.guardianSite(level, dead);
        if (where == null) return 0;
        Dice d = dice != null ? dice : level.getRandom()::nextDouble;
        LootRules.Victim victim = new LootRules.Victim(true, true, LootRules.Kind.GUARDIAN, where.polarity(), where.floor());
        List<ItemStack> stacks = LootDrops.materialize(LootRules.plan(victim, chances, rarity, 0.0, 0, d), d, killer, where);       // the Guardian's book does not depend on Luck or Looting
        ItemStack routeBook = ItemStack.EMPTY;
        for (ItemStack stack : stacks) {
            ItemEntity drop = new ItemEntity(level, dead.getX(), dead.getY() + 0.3, dead.getZ(), stack);
            if (stack.getItem() instanceof SkillBookItem) {
                routeBook = stack;
                drop.setUnlimitedLifetime();
                drop.setInvulnerable(true);                                                       // it cannot burn, drown in lava or be blown up
                drop.setGlowingTag(true);                                                         // and it can be found in the dark of the arena
            }
            event.getDrops().add(drop);
        }
        if (!routeBook.isEmpty()) {
            MutableComponent line = Component.translatableWithFallback("message.nusmp.skillbook.guardian_book", "The Guardian lets go of a Skill Book: %s!", routeBook.getHoverName());
            try {
                killer.sendSystemMessage(line.withStyle(ChatFormatting.GOLD));
            } catch (RuntimeException ignored) {
                // the line is only news: a stand-in player has no connection, and the books are already in the drops
            }
        }
        return stacks.size();
    }
}
