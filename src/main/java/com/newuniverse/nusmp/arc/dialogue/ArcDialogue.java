package com.newuniverse.nusmp.arc.dialogue;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Why: the one place that turns "a player right-clicked a character" into words: it asks the story what the player has done, asks {@link TalkPlan} what
 * the character should talk about, asks the character's {@link DialogueBook} (its own file, else the fallback) for a line, records what the conversation
 * changed (the NPC is now met, an offered quest is now offered) and sends the line. Shops and quest systems call {@link #say} with their own topic. The
 * rules themselves are pure and tested ({@link TalkPlan}, {@link DialogueBook}); this is the thin game-side glue around them.
 */
public final class ArcDialogue {
    private ArcDialogue() {}

    /** A player talks to a character. Returns whether the character said something. */
    public static boolean talk(ServerPlayer p, ArcNpcEntity npc) {
        if (!ArcConfig.enabled()) return false;
        ArcCharacter c = npc.character();
        if (c == null) {
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.blank", "This one has nothing to say yet."));
            return false;
        }
        DialogueSituation s = ArcStory.situation(p, c);
        DialogueBook own = DialogueLoader.bookOrFallback(c.id());
        DialogueBook fallback = DialogueLoader.fallback();
        DialogueContext topic = TalkPlan.next(s.first(), s.questStage(), x -> own.has(x, s));
        DialogueBook book = own;
        if (topic == null && own != fallback) {
            topic = TalkPlan.next(s.first(), s.questStage(), x -> fallback.has(x, s));
            book = fallback;
        }
        if (topic == null) {
            p.sendSystemMessage(Component.literal(c.name() + " says nothing."));
            return false;
        }
        DialogueBook.Pick pick = book.pick(topic, s, npc.lastLine(topic), n -> ThreadLocalRandom.current().nextInt(n));
        if (pick == null) return false;
        npc.rememberLine(topic, pick.index());
        if (s.first()) ArcStory.met(p, c.id());
        int after = TalkPlan.afterSaying(topic, s.questStage());
        if (after != s.questStage()) ArcStory.setQuestStage(p, c.id(), after);
        npc.beginTalk(p);
        NusmpEvents.fire(p, ArcStory.KEY_TALK, c.id());
        DialogueSender.send(p, c, book.voice(), pick.line(), ArcStory.sagaOf(p).title());
        return true;
    }

    /**
     * The character says something about a given topic (a shop opening, a quest being handed in). Returns false when it has no line for that topic that
     * fits the player; nothing else changes (the caller owns the consequences).
     */
    public static boolean say(ServerPlayer p, ArcCharacter c, DialogueContext topic, ArcNpcEntity npcOrNull) {
        if (!ArcConfig.enabled()) return false;
        DialogueSituation s = ArcStory.situation(p, c);
        DialogueBook book = DialogueLoader.bookOrFallback(c.id());
        DialogueBook.Pick pick = book.pick(topic, s, npcOrNull == null ? -1 : npcOrNull.lastLine(topic), n -> ThreadLocalRandom.current().nextInt(n));
        if (pick == null && book != DialogueLoader.fallback()) {
            book = DialogueLoader.fallback();
            pick = book.pick(topic, s, -1, n -> ThreadLocalRandom.current().nextInt(n));
        }
        if (pick == null) return false;
        if (npcOrNull != null) npcOrNull.rememberLine(topic, pick.index());
        DialogueSender.send(p, c, book.voice(), pick.line(), ArcStory.sagaOf(p).title());
        return true;
    }

    /** The character's own farewell (never the generic one: a character without a farewell simply stops talking). */
    public static boolean farewell(ServerPlayer p, ArcNpcEntity npc) {
        ArcCharacter c = npc.character();
        DialogueBook book = c == null ? null : DialogueLoader.book(c.id());
        if (c == null || book == null || !ArcConfig.enabled()) return false;
        DialogueBook.Pick pick = book.pick(DialogueContext.FAREWELL, ArcStory.situation(p, c), npc.lastLine(DialogueContext.FAREWELL), n -> ThreadLocalRandom.current().nextInt(n));
        if (pick == null) return false;
        npc.rememberLine(DialogueContext.FAREWELL, pick.index());
        DialogueSender.send(p, c, book.voice(), pick.line(), ArcStory.sagaOf(p).title());
        return true;
    }
}
