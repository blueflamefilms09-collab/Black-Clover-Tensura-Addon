package com.newuniverse.nusmp.arc.danmachi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: four of the friends of the Dungeon ask the player for one thing each (Eina has a Guild request for the Minotaur, Welf wants the Black Goliath's hide
 * seen, Lili has her eye on the Amphisbaena's scales, Ais wants to know how strong the player is against Udaeus), and the arc system only knows quest
 * stages 0 to 3 per character; nothing says what makes stage 1 become stage 2. This is that rule as data: which character's request is finished by the fall
 * of which boss. {@code DanMachiQuests} listens for the fall and moves the stage; the dialogue files carry the words (a request is offered only to a player
 * who has not yet beaten the boss, and thanked once it has fallen). Pure, so the pairs are tested against the dialogue files.
 */
public final class QuestBook {
    private QuestBook() {}

    /** One request: the character who asks, and the boss whose fall finishes it. */
    public record Request(String npcId, String bossId) {}

    private static final List<Request> REQUESTS = List.of(
            new Request("eina_tulle", "minotaur"),
            new Request("welf_crozzo", "black_goliath"),
            new Request("liliruca_arde", "amphisbaena"),
            new Request("ais_wallenstein", "udaeus"));

    public static List<Request> requests() { return REQUESTS; }

    /** The characters whose request the fall of this boss finishes (empty for any other boss). */
    public static List<String> askersOf(String bossId) {
        List<String> out = new ArrayList<>();
        if (bossId == null) return out;
        String id = bossId.toLowerCase(Locale.ROOT);
        for (Request r : REQUESTS) if (r.bossId().equals(id)) out.add(r.npcId());
        return out;
    }

    /** The boss a character's request is about, or null when the character asks for nothing. */
    public static String bossOf(String npcId) {
        if (npcId == null) return null;
        String id = npcId.toLowerCase(Locale.ROOT);
        for (Request r : REQUESTS) if (r.npcId().equals(id)) return r.bossId();
        return null;
    }
}
