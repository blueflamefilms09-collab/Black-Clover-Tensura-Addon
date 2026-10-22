package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;

import java.util.UUID;

/**
 * Why: the game side of feeding Shishou from the inventory (2.1.0). The Shishou item answers the two inventory clicks the Djinn Seal answers (the proven {@code overrideStackedOnOther} /
 * {@code overrideOtherStackedOnMe} pattern): a stack on the cursor clicked on the sword in a slot, or the sword on the cursor clicked on a stack. The decision to take the click over is pure and the same on the
 * client and the server ({@link ShishouFeed#takesOver}: the stack is food and the sword is the viewer's own), so the cursor never desyncs; the server then decides the outcome ({@link ShishouFeed#check}):
 * only the wielder, only his own inventory, a rate limit per player, a server switch ({@code relic.shishouFeed}), a per-click cap, and a sword at the level cap keeps the stack. Eaten items are removed once (never
 * duplicated), become experience through the Predation table, may let Skill Eater steal a skill, and Thought Communication tells the wielder. Cost: O(1) per click (one table lookup, at most four steal rolls,
 * one particle burst through the perf budget).
 */
public final class ShishouFeedService {
    private ShishouFeedService() {}

    /** The pure description of a stack. */
    public static ShishouFeed.Item describe(ItemStack s) {
        var item = s.getItem();
        return new ShishouFeed.Item(BuiltInRegistries.ITEM.getKey(item).toString(), s.getCount(), s.getRarity().ordinal(), s.isEnchanted(),
                s.isDamageableItem() || item instanceof TieredItem || item instanceof ProjectileWeaponItem, item instanceof ArmorItem || item instanceof ShieldItem, item instanceof BlockItem, s.has(net.minecraft.core.component.DataComponents.CONTAINER) || s.has(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS));
    }

    /** Is the sword stack the viewer's own (its written wielder is the viewer)? Known to both sides: the owner is on the stack. */
    public static boolean ownSword(ItemStack sword, Player p) {
        UUID o = ShishouComponents.owner(sword);
        return o != null && p != null && o.equals(p.getUUID());
    }

    /**
     * A click with a stack and the sword. {@code sword} is the Shishou stack, {@code food} the other one (the one that is eaten and shrunk), {@code whole} true for a left click. Returns true when the click is
     * taken over; the outcome (a message or the meal) happens on the server only.
     */
    public static boolean onStacked(ItemStack sword, ItemStack food, Player player, boolean whole, boolean ownInventory) {
        if (food.isEmpty() || sword.isEmpty() || food == sword) return false;
        ShishouFeed.Item it = describe(food);
        if (!ShishouFeed.takesOver(it, ownSword(sword, player))) return false;                // both sides decide this the same way
        if (player.level().isClientSide || !(player instanceof ServerPlayer p)) return true;  // the client predicts nothing; the server answers
        if (!ownInventory) {
            p.displayClientMessage(Component.translatableWithFallback("shishou.feed.own_inventory", "Feed me from your own inventory.").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        feed(p, sword, food, it, whole);
        return true;
    }

    /** The server's outcome of one feeding click. */
    static ShishouFeed.Verdict feed(ServerPlayer p, ItemStack sword, ItemStack food, ShishouFeed.Item it, boolean whole) {
        if (!ShishouService.enabled()) return ShishouFeed.Verdict.DISABLED;
        ShishouData d = ShishouData.get();
        ShishouCap cap = d.cap();
        boolean isOwner = cap.isOwner(p.getUUID()) && cap.validStack(ShishouComponents.token(sword));
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        ShishouRecord rec = d.record();
        boolean rateOk = ShishouLive.gap(ShishouLive.FEED_GAP, p.getUUID(), now, ShishouConfig.feedGap());
        ShishouFeed.Verdict v = ShishouFeed.check(it, isOwner, ShishouConfig.feed(), rateOk, rec.level() >= ShishouLevels.MAX_LEVEL);
        switch (v) {
            case OK -> { }
            case VALUABLE -> {
                say(p, "shishou.feed.valuable", "That is too precious to eat.");
                return v;
            }
            case DISABLED -> {
                say(p, "shishou.feed.off", "Feeding is switched off on this server.");
                return v;
            }
            case FULL_LEVEL -> {
                say(p, "shishou.feed.full", "I can grow no more. Keep it.");
                return v;
            }
            default -> {
                return v;                                                                      // too fast, not the wielder, nothing there: silent
            }
        }
        int n = ShishouFeed.take(food.getCount(), whole, ShishouConfig.feedPerClick());
        long exp = ShishouService.scaleExp(ShishouFeed.value(it, n));
        if (n <= 0 || exp <= 0L) return ShishouFeed.Verdict.NOT_FOOD;
        food.shrink(n);                                                                        // taken once, here: nothing is ever returned or duplicated
        ShishouRecord.Gain g = rec.addExp(exp);
        int level = rec.level();
        ShishouSkill stolen = null;
        boolean isNew = false;
        ShishouStealTable.Row row = ShishouStealTable.lookup(ShishouFeed.sourceId(it.id()), false);
        if (row != null && ShishouConfig.skillOn(ShishouSkill.SKILL_EATER)) {
            for (int i = 0, rolls = ShishouFeed.stealRolls(n); i < rolls && stolen == null; i++) {
                if (p.getRandom().nextDouble() < ShishouStealTable.chance(row, ShishouConfig.stealScale()) * ShishouFeed.ITEM_STEAL_SHARE) {
                    isNew = rec.learn(row.skill());
                    rec.step(isNew, true);
                    stolen = row.skill();
                }
            }
        }
        sword.set(ShishouComponents.LEVEL.get(), level);                                       // the stack's mirror of his level
        d.touch();
        p.displayClientMessage(Component.translatableWithFallback("shishou.feed.ate", "Ate %s: +%s experience.", n, exp).withStyle(ChatFormatting.AQUA), true);
        ShishouSpeech.say(p, sl, ShishouVoiceLines.Group.EAT);
        if (g.leveled()) ShishouService.levelUp(p, sl, g);
        if (isNew) ShishouSpeech.say(p, sl, ShishouService.groupOf(stolen), Component.translatableWithFallback(stolen.langKey(), stolen.english()));
        ShishouSpells.fx(sl, Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), p.position().add(0, 1.2, 0), 5, 0.35);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.4f);
        ShishouService.syncHud(p);
        return ShishouFeed.Verdict.OK;
    }

    private static void say(ServerPlayer p, String key, String fallback) { p.displayClientMessage(Component.translatableWithFallback(key, fallback).withStyle(ChatFormatting.GRAY), true); }

    // ---------------------------------------------------------------- the two inventory hooks (called by ShishouItem)
    /** The sword is on the cursor and clicks the stack in {@code slot}: left click eats the stack, right click one. */
    public static boolean swordOnStack(ItemStack sword, Slot slot, ClickAction action, Player player) {
        boolean took = onStacked(sword, slot.getItem(), player, action == ClickAction.PRIMARY, slot.container instanceof Inventory);
        if (took && !player.level().isClientSide) slot.setChanged();
        return took;
    }

    /** A stack is on the cursor and clicks the sword in {@code slot}. */
    public static boolean stackOnSword(ItemStack sword, ItemStack cursor, Slot slot, ClickAction action, Player player) {
        return onStacked(sword, cursor, player, action == ClickAction.PRIMARY, slot.container instanceof Inventory);
    }
}
