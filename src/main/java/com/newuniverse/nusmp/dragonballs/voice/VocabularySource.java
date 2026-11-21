package com.newuniverse.nusmp.dragonballs.voice;

import java.util.List;

/**
 * Why: the words a voice recogniser should know (the ritual phrases, the wish keywords, race and skill names, the cancel and refused words) are owned by the wish tables, which grow. This one method is the
 * only link: {@link DbVoice#setVocabularySource} swaps the provider, {@link DefaultVocabulary} reads the tables that exist today (the final provider is {@code WishRules.spokenVocabulary()}, wired by
 * whoever owns the wish tables). Words or short phrases; they are split and normalised afterwards. Called on the server thread, about every 10 seconds while the voice feature runs.
 */
@FunctionalInterface
public interface VocabularySource {
    List<String> words();
}
