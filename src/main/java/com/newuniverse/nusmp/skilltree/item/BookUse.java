package com.newuniverse.nusmp.skilltree.item;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Why: reading a book is the moment a player's valuable item is used up and their permanent tree changes, so it is done in ONE server-side place that both items and
 * the self-tests call, never in the item classes' client-facing methods. The server is the only one allowed to change the tree (the skill-tree design: the server
 * decides, the client shows), and a book must never be consumed unless the tree really changed: so the order is always ask the rules ({@link BookRules}, pure), ask
 * the one door {@code SkillTreeApi} whether the engine is on (its writes do nothing while it is off) and then to do it, look at what it answered (it alone knows if the Skill
 * Points are at their cap), and only then shrink the stack and tell the player. Every attempt that reaches the server starts a short use cooldown (a second) so a held right-click cannot spam messages or
 * race the unlock, and a creative player keeps the book. Nothing here throws: the callers are item and event code where an exception would be a crash. The route
 * unlock effects (circle, pillar, sounds, banner) are NOT played here; the API announces the unlock to its listeners and {@link UnlockFx} plays them, so the same show
 * runs whether the unlock came from a book, a command, the shop or a quest.
 */
public final class BookUse {
    private BookUse() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** Ticks a reader waits before the next book of the same item (one second). */
    public static final int USE_COOLDOWN_TICKS = 20;

    /** What a reading did. */
    public enum Result {
        /** A new route was unlocked and the book used up. */
        UNLOCKED,
        /** The route was already known; the copy became Skill Points and was used up. */
        DUPLICATE_SP,
        /** The route was already known and a copy is not accepted (server setting, or worth nothing); nothing was consumed. */
        DUPLICATE_REFUSED,
        /** The book names no route that exists, or has no content; nothing was consumed. */
        BLANK,
        /** The Skill Tree is switched off on this server ({@code SkillTreeApi.isEnabled()}); nothing was consumed. */
        DISABLED,
        /** The reader is still on cooldown (or the stack is empty); nothing happened. */
        COOLDOWN,
        /** An SP Book gave Skill Points and was used up. */
        SP_GRANTED,
        /** The Skill Points could not be added because the player is at the cap, for an SP Book or for a copy of a route already known; nothing was consumed. */
        SP_FULL
    }

    // ------------------------------------------------------------------ the Skill Book
    /** Reads the Skill Book {@code stack} (the stack in the reader's hand; it is shrunk in place when the book is used up). Never throws. */
    public static Result readSkillBook(ServerPlayer player, ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) return Result.COOLDOWN;
            Item item = stack.getItem();                                       // taken now: a stack shrunk to nothing reports the Air item
            if (player.getCooldowns().isOnCooldown(item)) return Result.COOLDOWN;
            Result result = skillBook(player, stack);
            startCooldown(player, item);
            return result;
        } catch (RuntimeException | LinkageError e) {
            say(player, Component.translatableWithFallback("message.nusmp.tree.items.failed", "The book would not open. (A server error was logged.)").withStyle(ChatFormatting.RED));
            LOG.warn("[nusmp] reading a Skill Book failed", e);
            return Result.BLANK;
        }
    }

    private static Result skillBook(ServerPlayer player, ItemStack stack) {
        if (!SkillTreeApi.isEnabled()) return disabled(player);                    // the engine is off: nothing can be unlocked or added, so nothing is consumed
        String bookId = SkillItems.bookIdOf(stack);
        SkillRoute route = bookId.isEmpty() ? null : RouteRegistry.byBook(bookId);
        BookRules.Outcome plan = decide(player, route);
        if (plan == BookRules.Outcome.UNLOCK) {
            SkillTreeApi.UnlockResult unlocked = SkillTreeApi.unlockRoute(player, route.id(), true);
            switch (unlocked) {
                case OK:
                    consume(player, stack);
                    tell(player, Component.translatableWithFallback("message.nusmp.tree.items.unlocked", "You have learned the route: %s.",
                            Component.literal(route.name()).withStyle(Style.EMPTY.withColor(route.color() & 0xFFFFFF))).withStyle(ChatFormatting.GOLD));
                    return Result.UNLOCKED;
                case ALREADY:
                    plan = decide(player, route);                              // somebody unlocked it between the look and the call: treat the book as a copy
                    break;
                case DISABLED:
                    return disabled(player);
                default:
                    say(player, blank());
                    return Result.BLANK;
            }
        }
        switch (plan) {
            case BLANK:
                say(player, blank());
                return Result.BLANK;
            case DUPLICATE_SP:
                return duplicate(player, stack, route);
            default:
                say(player, Component.translatableWithFallback("message.nusmp.tree.items.duplicate_refused", "You already know the route %s. The book stays closed.",
                        route == null ? "" : route.name()).withStyle(ChatFormatting.GRAY));
                return Result.DUPLICATE_REFUSED;
        }
    }

    private static Result disabled(ServerPlayer player) {
        say(player, Component.translatableWithFallback("message.nusmp.tree.items.disabled", "The Skill Tree is switched off on this server.").withStyle(ChatFormatting.RED));
        return Result.DISABLED;
    }

    private static BookRules.Outcome decide(ServerPlayer player, SkillRoute route) {
        boolean known = route != null;
        boolean unlocked = known && SkillTreeApi.hasRoute(player, route.id());
        return BookRules.skillBook(known, unlocked, SkillTreeApi.duplicateBookGivesSp(), known ? route.duplicateSp() : 0);
    }

    /** A copy of a known route: its Skill Points, but only if the tree really took them (at the cap it takes none and the book is kept). */
    private static Result duplicate(ServerPlayer player, ItemStack stack, SkillRoute route) {
        int before = SkillTreeApi.sp(player);
        SkillTreeApi.addSp(player, route.duplicateSp(), "book_duplicate:" + route.id());
        int gained = SkillTreeApi.sp(player) - before;
        if (gained <= 0) {
            say(player, Component.translatableWithFallback("message.nusmp.tree.items.sp_full", "Your Skill Points are full. The book stays closed.").withStyle(ChatFormatting.GRAY));
            return Result.SP_FULL;
        }
        consume(player, stack);
        tell(player, Component.translatableWithFallback("message.nusmp.tree.items.duplicate_sp", "You already know %s. The book turns into %s Skill Points.",
                route.name(), gained).withStyle(ChatFormatting.GOLD));
        sound(player, 0.6f, 1.2f);
        return Result.DUPLICATE_SP;
    }

    // ------------------------------------------------------------------ the SP Book
    /** Reads the SP Book {@code stack}. {@code dice} rolls the gamble kind. Never throws. */
    public static Result readSpBook(ServerPlayer player, ItemStack stack, Dice dice) {
        try {
            if (stack == null || stack.isEmpty()) return Result.COOLDOWN;
            Item item = stack.getItem();
            if (player.getCooldowns().isOnCooldown(item)) return Result.COOLDOWN;
            Result result = spBook(player, stack, dice);
            startCooldown(player, item);
            return result;
        } catch (RuntimeException | LinkageError e) {
            say(player, Component.translatableWithFallback("message.nusmp.tree.items.failed", "The book would not open. (A server error was logged.)").withStyle(ChatFormatting.RED));
            LOG.warn("[nusmp] reading an SP Book failed", e);
            return Result.BLANK;
        }
    }

    private static Result spBook(ServerPlayer player, ItemStack stack, Dice dice) {
        if (!SkillTreeApi.isEnabled()) return disabled(player);                    // addSp does nothing while the engine is off: the book must not be eaten for nothing
        SpKind kind = SpKind.byId(SkillItems.spKindOf(stack));
        int amount = BookRules.spBookAmount(kind, SkillItems.spAmountOf(stack), dice);
        if (amount <= 0) {
            say(player, blank());
            return Result.BLANK;
        }
        int before = SkillTreeApi.sp(player);
        SkillTreeApi.addSp(player, amount, "sp_book:" + (kind == null ? "custom" : kind.id()));
        int gained = SkillTreeApi.sp(player) - before;
        if (gained <= 0) {
            say(player, Component.translatableWithFallback("message.nusmp.tree.items.sp_full", "Your Skill Points are full. The book stays closed.").withStyle(ChatFormatting.GRAY));
            return Result.SP_FULL;
        }
        consume(player, stack);
        boolean jackpot = kind != null && kind.isGamble() && JackInTheBox.isJackpot(amount);
        if (jackpot) {
            tell(player, Component.translatableWithFallback("message.nusmp.tree.items.jackpot", "JACKPOT! The box bursts open: +%s Skill Points!", gained).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            fanfare(player);
            flare(player, 0xFFFFD24A, 1.6f);
        } else {
            tell(player, Component.translatableWithFallback("message.nusmp.tree.items.sp_gained", "+%s Skill Points", gained).withStyle(ChatFormatting.GOLD));
            if (gained >= 9) flare(player, 0xFF7FE8FF, 1.1f);
        }
        sound(player, 0.6f, jackpot ? 1.5f : 0.9f + Math.min(0.5f, gained * 0.03f));
        return Result.SP_GRANTED;
    }

    // ------------------------------------------------------------------ small helpers
    /** Uses the book up unless the reader is in creative mode. */
    private static void consume(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    private static Component blank() { return Component.translatableWithFallback("message.nusmp.tree.items.blank", "The pages are blank.").withStyle(ChatFormatting.GRAY); }

    /**
     * The helpers below only TELL the player or make a show, after the tree has already changed, so a failure in them (a stand-in player with no connection, a client that is
     * gone) must never turn a reading that happened into an error that says it did not: they swallow their own exceptions.
     */
    private static void say(ServerPlayer player, Component text) {
        try {
            player.displayClientMessage(text, true);
        } catch (RuntimeException ignored) {
            // nobody to tell
        }
    }

    private static void tell(ServerPlayer player, Component text) {
        try {
            player.sendSystemMessage(text);
        } catch (RuntimeException ignored) {
            // nobody to tell
        }
    }

    private static void startCooldown(ServerPlayer player, Item item) {
        try {
            player.getCooldowns().addCooldown(item, USE_COOLDOWN_TICKS);
        } catch (RuntimeException ignored) {
            // a stand-in player has no connection to tell about it: the reading already happened
        }
    }

    private static void sound(ServerPlayer player, float volume, float pitch) {
        try {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, volume, pitch);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, volume, pitch);
        } catch (RuntimeException ignored) {
            // a missing sound is not an error
        }
    }

    private static void fanfare(ServerPlayer player) {
        try {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);
        } catch (RuntimeException ignored) {
            // a missing sound is not an error
        }
    }

    private static void flare(ServerPlayer player, int argb, float size) {
        try {
            ServerLevel level = player.serverLevel();
            Vec3 chest = player.position().add(0, 1.0, 0);
            VfxSpawn.send(level, VfxShape.LIGHT_FLARE, chest, chest, argb, 14, size);
        } catch (RuntimeException ignored) {
            // a missing effect is not an error
        }
    }
}
