package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.story.StoryData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Why: the owner asked that the last boss of every saga pays a reward ("a saga-clear reward ... a chest or a commemorative item, vanilla and mod items
 * only"), and "the last boss of a saga" is not a place (a saga's last boss may fall in a story arena, as a floor boss of the dungeon, or to a wandering
 * elite) but a state of the player's story: every boss of the saga is defeated. So the reward belongs to the player and the moment the saga closes for them, not
 * to an arena. This pure class decides WHEN a saga is cleared for a story, WHAT it pays (a table of vanilla items and the dungeon's Magic Stone, growing with
 * the saga), and writes the commemorative book (a plain written book that names the saga and the bosses the player beat). The once-only memory is kept in the
 * player's story itself, as a quest stage under a key no character can have (a colon is not allowed in an id), so it travels with the story, survives death
 * and is cleared with it (the story reset command also forgets that a saga was paid, on purpose: a tester starts over).
 */
public final class SagaRewards {
    private SagaRewards() {}

    /** One stack of the reward: an item id ({@code nusmp:magic_stone}) and a count. */
    public record Stack(String item, int count) {}

    /** The key under which the story remembers that a saga's reward was paid; the stage is {@link #PAID}. */
    public static String paidKey(ArcSaga saga) { return "saga:" + saga.id(); }

    public static final int PAID = 1;

    /** Is the reward of this saga already recorded as paid in this story? */
    public static boolean paid(StoryData d, ArcSaga saga) { return d.questStage(paidKey(saga)) >= PAID; }

    /** The bosses of a saga in the roster, in roster order. */
    public static List<ArcCharacter> bossesOf(ArcSaga saga, Collection<ArcCharacter> roster) {
        List<ArcCharacter> out = new ArrayList<>();
        for (ArcCharacter c : roster) if (c.isBoss() && c.saga() == saga) out.add(c);
        return out;
    }

    /** Is every boss of the saga defeated in this story? A saga with no boss is never cleared (there is nothing to clear). */
    public static boolean cleared(StoryData d, ArcSaga saga, Collection<ArcCharacter> roster) {
        List<ArcCharacter> bosses = bossesOf(saga, roster);
        if (bosses.isEmpty()) return false;
        for (ArcCharacter c : bosses) if (!d.hasDefeated(c.id())) return false;
        return true;
    }

    /** The sagas that are cleared in this story and not yet paid, in saga order: what the player is owed right now. */
    public static List<ArcSaga> owed(StoryData d, Collection<ArcCharacter> roster) {
        List<ArcSaga> out = new ArrayList<>();
        for (ArcSaga s : ArcSaga.values()) if (!paid(d, s) && cleared(d, s, roster)) out.add(s);
        return out;
    }

    /** Marks the saga's reward as paid. True when the mark changed (so a caller pays only after a true). */
    public static boolean markPaid(StoryData d, ArcSaga saga) { return d.setQuestStage(paidKey(saga), PAID); }

    /** What a cleared saga pays: Magic Stones, experience and a few fine things, more for the later sagas. Empty for a saga that has no bosses by design. */
    public static List<Stack> reward(ArcSaga saga) {
        return switch (saga) {
            case ELF_REINCARNATION -> List.of(new Stack("nusmp:magic_stone", 12), new Stack("minecraft:experience_bottle", 16), new Stack("minecraft:golden_apple", 2));
            case SPADE_RAID -> List.of(new Stack("nusmp:magic_stone", 16), new Stack("minecraft:experience_bottle", 24), new Stack("minecraft:golden_apple", 4), new Stack("minecraft:diamond", 4));
            case WIZARD_KING -> List.of(new Stack("nusmp:magic_stone", 32), new Stack("minecraft:experience_bottle", 48), new Stack("minecraft:enchanted_golden_apple", 1), new Stack("minecraft:nether_star", 1));
            case SEVEN_DEADLY_SINS -> List.of(new Stack("nusmp:magic_stone", 24), new Stack("minecraft:experience_bottle", 32), new Stack("minecraft:golden_apple", 4), new Stack("minecraft:diamond", 6));
            case DANMACHI -> List.of(new Stack("nusmp:magic_stone", 16), new Stack("minecraft:experience_bottle", 24), new Stack("minecraft:golden_apple", 3), new Stack("minecraft:emerald", 12));
            case ALLIES -> List.of();
        };
    }

    // ---------------------------------------------------------------- the commemorative book
    /** The title of the commemorative book. */
    public static String bookTitle(ArcSaga saga) { return "Chronicle: " + saga.title(); }

    public static final String BOOK_AUTHOR = "The Chronicle";

    /**
     * The pages of the commemorative book: a title page, the bosses of the saga that fell (names as the roster spells them), at most seven to a page so a long
     * saga does not overflow it, and a closing line. Plain text, no formatting codes.
     */
    public static List<String> bookPages(ArcSaga saga, List<String> bossNames) {
        List<String> pages = new ArrayList<>();
        pages.add("The Chronicle records\n\n" + saga.title() + "\n\nCleared.\n\nThe ink is yours to keep.");
        List<String> names = bossNames == null ? List.of() : bossNames;
        for (int i = 0; i < names.size(); i += 7) {
            StringBuilder b = new StringBuilder("They fell before you:\n");
            for (int j = i; j < Math.min(names.size(), i + 7); j++) b.append('\n').append(names.get(j));
            pages.add(b.toString());
        }
        pages.add("Every saga ends, and the next is already being written.\n\nWalk on.");
        return pages;
    }
}
