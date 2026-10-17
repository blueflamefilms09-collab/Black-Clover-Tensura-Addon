package com.newuniverse.nusmp.arc.dialogue;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the dialogue selector must be a pure function of "who is talking to whom, and how far the story has gone", so a unit test can prove which line a
 * player hears without a running game. This is that input: the story stage (how many bosses of the arcs the player has beaten), the id of the saga they
 * are in, the ids of the beaten bosses, the quest stage this character has with them, and whether this is their first meeting. The game side builds one
 * from the player's {@code StoryData}; nothing here knows what a player is.
 *
 * @param stage      bosses of the roster the player has defeated
 * @param saga       the id of the saga the player is in ({@code elf_reincarnation}), or empty
 * @param defeated   ids of the defeated characters (lower case)
 * @param questStage the player's quest stage with this character (0 none, 1 offered, 2 done, 3 done and told)
 * @param first      true when the player has never spoken to this character
 */
public record DialogueSituation(int stage, String saga, Set<String> defeated, int questStage, boolean first) {
    public DialogueSituation {
        saga = saga == null ? "" : saga.toLowerCase(Locale.ROOT);
        defeated = defeated == null ? Set.of() : Set.copyOf(defeated);
    }

    /** Builds a situation from loose parts (ids are lower-cased, a null collection is empty). */
    public static DialogueSituation of(int stage, String saga, Collection<String> defeated, int questStage, boolean first) {
        Set<String> ids = new LinkedHashSet<>();
        if (defeated != null) for (String d : defeated) if (d != null) ids.add(d.toLowerCase(Locale.ROOT));
        return new DialogueSituation(stage, saga, ids, questStage, first);
    }
}
