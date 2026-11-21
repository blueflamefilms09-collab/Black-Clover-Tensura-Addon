package com.newuniverse.nusmp.dragonballs.voice;

import com.newuniverse.nusmp.dragonballs.BallSet;
import com.newuniverse.nusmp.dragonballs.DbConfig;
import com.newuniverse.nusmp.dragonballs.WishRules;

import java.util.ArrayList;
import java.util.List;

/**
 * The vocabulary read from the tables that exist today: the configured ritual phrases of both sets, every wish id and keyword, the skill names, the cancel words and the refused words (a refused word must be
 * heard so that the wish is refused instead of being quietly dropped).
 */
public final class DefaultVocabulary implements VocabularySource {
    /** The words that send the dragon away (the same ones {@code DbSession.wish} reads). */
    public static final List<String> CANCEL = List.of("cancel", "never mind", "nevermind", "farewell", "goodbye");

    @Override public List<String> words() {
        List<String> out = new ArrayList<>();
        for (BallSet s : BallSet.values()) {
            out.addAll(DbConfig.phrases(s));
            for (String id : WishRules.skills(s)) out.add(WishRules.skillName(id));
        }
        for (WishRules.Wish w : WishRules.ALL) {
            out.add(w.id());
            out.addAll(w.keywords());
        }
        out.addAll(CANCEL);
        out.addAll(WishRules.FORBIDDEN);
        return out;
    }
}
