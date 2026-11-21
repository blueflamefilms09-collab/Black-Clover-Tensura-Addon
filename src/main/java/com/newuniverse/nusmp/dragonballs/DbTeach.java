package com.newuniverse.nusmp.dragonballs;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Why: the owner wants players to be TAUGHT the words that call the dragon as they find each Dragon Ball, and to be handed the words ready to copy and paste into chat (docs/dragon_ball_words.md).
 * The first time a player holds a ball (picked up, or found in any bag within five seconds, through the ball's own tick) the dragon whispers the next piece of the set's call in chat, followed by a
 * line with everything known so far and two click buttons: Copy (to the clipboard) and Put in chat (fills the chat box, one Enter away). {@code /dragonwords [set]} shows it again for anybody.
 * It only helps: the summoning check is unchanged, so a player who already knows the words never needs it. Behind {@code dragonBalls.teachWords} (false = nothing is taught, nothing is sent).
 * Server side only; the click events are plain chat actions. Cost: a bit test per ball per five seconds, one saved row per player, two chat lines per new ball; no packets of its own.
 */
public final class DbTeach {
    private DbTeach() {}

    /** The phrase the set teaches: the first entry of its phrase list (the config's, else the built-in default). */
    static String phrase(BallSet set) {
        for (String p : DbConfig.phrases(set)) if (p != null && !p.isBlank()) return p.trim();
        return set.defaultPhrases().get(0);
    }

    // ---------------------------------------------------------------- the hooks
    /** Called from the ball's own tick (every five seconds per ball stack): finds the ball for a player who has not met it yet. Cheap when it is already known. */
    static void teach(ServerPlayer p, BallSet set, int star) {
        if (!DbConfig.teachWords() || p == null || star < 1 || star > DbConfig.count()) return;
        DbLessons l = DbLessons.get(p.server);
        if (PhraseLessons.has(l.mask(p.getUUID(), set), star)) return;
        int n = l.learn(p.getUUID(), set, star);
        if (n == 0) return;
        int balls = DbConfig.count();
        List<String> words = PhraseLessons.words(phrase(set));
        List<String> fresh = PhraseLessons.fresh(words, balls, n);
        if (n >= balls) p.sendSystemMessage(DbMsg.say(set, "words.complete", n, balls));
        else if (fresh.isEmpty()) p.sendSystemMessage(DbMsg.say(set, "words.stir", n, balls));
        else p.sendSystemMessage(DbMsg.say(set, "words.learn", n, balls, String.join(" ", fresh)));
        sendLine(p, set, PhraseLessons.line(PhraseLessons.known(words, balls, n)));
    }

    /** A ball enters the player's inventory by pickup: teach at once (the tick would do it within five seconds anyway). */
    public static void onPickup(ItemEntityPickupEvent.Post e) {
        if (!(e.getPlayer() instanceof ServerPlayer p)) return;
        ItemStack s = e.getCurrentStack().isEmpty() ? e.getOriginalStack() : e.getCurrentStack();
        if (s.getItem() instanceof DragonBallItem b) teach(p, b.set(), b.star());
    }

    // ---------------------------------------------------------------- the line with the buttons
    private static void sendLine(ServerPlayer p, BallSet set, String line) {
        if (line.isEmpty()) return;
        p.sendSystemMessage(Component.translatable(DbMsg.P + "words.sofar", line).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" ")).append(button("words.copy", "words.copy.hover", new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, line), set))
                .append(Component.literal(" ")).append(button("words.chat", "words.chat.hover", new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, line), set)));
    }

    private static MutableComponent button(String key, String hoverKey, ClickEvent click, BallSet set) {
        return Component.translatable(DbMsg.P + key).withStyle(Style.EMPTY.withColor(set.color()).withBold(true).withClickEvent(click)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(DbMsg.P + hoverKey))));
    }

    // ---------------------------------------------------------------- /dragonwords
    public static void registerCommand(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("dragonwords")
                .executes(c -> show(c.getSource(), null))
                .then(Commands.argument("set", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(BallSet.values()).map(BallSet::id), b))
                        .executes(c -> show(c.getSource(), BallSet.byId(StringArgumentType.getString(c, "set"))))));
    }

    private static int show(CommandSourceStack src, BallSet only) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(DbMsg.bad("words.player_only")); return 0; }
        if (!DbConfig.teachWords()) { src.sendFailure(DbMsg.bad("words.off")); return 0; }
        DbLessons l = DbLessons.get(p.server);
        int shown = 0;
        for (BallSet s : BallSet.values()) {
            if (only != null && only != s) continue;
            int n = PhraseLessons.count(l.mask(p.getUUID(), s));
            if (n == 0) continue;
            shown++;
            p.sendSystemMessage(DbMsg.say(s, "words.known", n, DbConfig.count()));
            sendLine(p, s, PhraseLessons.line(PhraseLessons.known(PhraseLessons.words(phrase(s)), DbConfig.count(), n)));
        }
        if (shown == 0) p.sendSystemMessage(DbMsg.info("words.none"));
        return shown;
    }
}
