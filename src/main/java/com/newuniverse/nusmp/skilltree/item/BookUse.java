package com.newuniverse.nusmp.skilltree.item;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.unique.LegacyIds;
import com.newuniverse.nusmp.skilltree.unique.LineIndex;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import com.newuniverse.nusmp.skilltree.unique.UniqueSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Why: this is the part of the Skill Book that touches a player, kept apart from the item class so a test (and the in-game self-test) can read a book without a right click. Reading a Skill Book TEACHES the
 * player the unique skill of that book ({@link UniqueBooks#grant}), switched on, and uses the book up; a creative reader keeps it; a reader who already owns the skill is told so and nothing is consumed (the
 * book stays closed, so a duplicate is never wasted). An old book of a removed magic, or an old SP Book (Skill Points are gone), is turned into a random book the reader does not own yet. Every decision is
 * {@link BookRules}'s; this class carries it out in the order that cannot lose a book: the skill is taught FIRST and the stack is shrunk only if that really worked, so a refusal from the skill list never eats the
 * book. The reading has a one second cooldown on the item, the unlock show ({@link UnlockFx}) is played for a taught skill, and every message is a translatable key with an English fallback.
 */
public final class BookUse {
    private BookUse() {}

    private static final Logger LOG = LogUtils.getLogger();

    public static final int USE_COOLDOWN_TICKS = 20;

    public enum Result { LEARNED, ALREADY_OWNED, BLANK, REPLACED, NOTHING_LEFT, COOLDOWN, FAILED }

    // ------------------------------------------------------------------ the Skill Book
    public static Result readSkillBook(ServerPlayer player, ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) return Result.COOLDOWN;
            Item item = stack.getItem();                                       // taken now: a stack shrunk to nothing reports the Air item
            if (player.getCooldowns().isOnCooldown(item)) return Result.COOLDOWN;
            Result result = skillBook(player, stack);
            startCooldown(player, item);
            return result;
        } catch (RuntimeException | LinkageError e) {
            say(player, Component.translatableWithFallback("message.nusmp.skillbook.failed", "The book would not open. (A server error was logged.)").withStyle(ChatFormatting.RED));
            LOG.warn("[nusmp] reading a Skill Book failed", e);
            return Result.FAILED;
        }
    }

    private static Result skillBook(ServerPlayer player, ItemStack stack) {
        String bookId = SkillItems.bookIdOf(stack);
        LineIndex.Line line = UniqueSkills.lineOfBook(bookId);
        boolean removed = LegacyIds.isRemovedBook(bookId);
        boolean owned = line != null && UniqueBooks.owns(player, line.bare());
        BookRules.Outcome plan = BookRules.skillBook(removed, line != null, owned);
        boolean creative = player.getAbilities().instabuild;
        switch (plan) {
            case GRANT: {
                UniqueBooks.Grant done = UniqueBooks.grant(player, line.bare(), false);
                if (done == UniqueBooks.Grant.ALREADY) {
                    alreadyOwned(player, line);
                    return Result.ALREADY_OWNED;
                }
                if (done != UniqueBooks.Grant.GRANTED) {
                    say(player, blank());
                    return Result.BLANK;
                }
                if (BookRules.usesUp(plan, creative)) stack.shrink(1);
                tell(player, Component.translatableWithFallback("message.nusmp.skillbook.learned", "You have learned the unique skill: %s.",
                        Component.literal(line.name()).withStyle(Style.EMPTY.withColor(line.color()))).withStyle(ChatFormatting.GOLD));
                UnlockFx.play(player, line.routeId(), line.color(), true);
                return Result.LEARNED;
            }
            case ALREADY_OWNED:
                alreadyOwned(player, line);
                return Result.ALREADY_OWNED;
            case REPLACE:
                return replace(player, stack, creative);
            default:
                say(player, blank());
                return Result.BLANK;
        }
    }

    // ------------------------------------------------------------------ the SP Book (legacy)
    /** An old SP Book is a voucher: it turns into a random Skill Book the reader does not own yet. */
    public static Result readSpBook(ServerPlayer player, ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) return Result.COOLDOWN;
            Item item = stack.getItem();
            if (player.getCooldowns().isOnCooldown(item)) return Result.COOLDOWN;
            Result result = replace(player, stack, player.getAbilities().instabuild);
            startCooldown(player, item);
            return result;
        } catch (RuntimeException | LinkageError e) {
            say(player, Component.translatableWithFallback("message.nusmp.skillbook.failed", "The book would not open. (A server error was logged.)").withStyle(ChatFormatting.RED));
            LOG.warn("[nusmp] reading an old SP Book failed", e);
            return Result.FAILED;
        }
    }

    // ------------------------------------------------------------------ small helpers
    private static Result replace(ServerPlayer player, ItemStack stack, boolean creative) {
        String bare = BookRules.pickUnowned(UniqueSkills.bares(), b -> UniqueBooks.owns(player, b), player.getRandom()::nextDouble);
        LineIndex.Line line = bare == null ? null : UniqueSkills.line(bare);
        if (line == null) {
            say(player, Component.translatableWithFallback("message.nusmp.skillbook.nothing_left", "You already know every skill a book could teach. The book stays closed.").withStyle(ChatFormatting.GRAY));
            return Result.NOTHING_LEFT;
        }
        ItemStack given = SkillBookItem.stackFor(line);
        if (given.isEmpty()) return Result.BLANK;
        if (!creative) stack.shrink(1);
        if (!player.getInventory().add(given)) player.drop(given, false);
        tell(player, Component.translatableWithFallback("message.nusmp.skillbook.replaced", "The old pages crumble and a new book takes their place: %s.",
                Component.literal(line.bookName()).withStyle(Style.EMPTY.withColor(line.color()))).withStyle(ChatFormatting.GOLD));
        sound(player, 0.6f, 1.0f);
        return Result.REPLACED;
    }

    private static void alreadyOwned(ServerPlayer player, LineIndex.Line line) {
        say(player, Component.translatableWithFallback("message.nusmp.skillbook.already_owned", "You already know %s. The book stays closed.",
                line == null ? "" : line.name()).withStyle(ChatFormatting.GRAY));
    }

    private static Component blank() { return Component.translatableWithFallback("message.nusmp.skillbook.blank", "The pages are blank.").withStyle(ChatFormatting.GRAY); }

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
}
