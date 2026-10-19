package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;

/**
 * Why: Song, Sound, Soul, Soul Corpse and Memory Magic used to be the generic wiki attribute book (WikiBooks.attribute: a bolt, a line, a field, a domain and a guard).
 * The 1.93.0 rebuild gives each its own book class with the SAME five page ids in the SAME order (attribute_bolt, attribute_cut, attribute_field, attribute_domain,
 * attribute_guard), the existing exclusive pages after them, and new pages appended. WikiBooks.attribute asks this one dispatch first (one line there), so the registry
 * of the wiki books is unchanged: it still builds one book per MagicType, only a different class for these five.
 */
public final class SongRebuild {
    private SongRebuild() {}

    /** The rebuilt book of a magic, or null when the magic is not one of the five. */
    public static GrimoireBook book(MagicType magic) {
        return switch (magic) {
            case SONG -> new SongBook();
            case SOUND -> new SoundBook();
            case SOUL -> new SoulBook();
            case SOUL_CORPSE -> new SoulCorpseBook();
            case MEMORY -> new MemoryBook();
            default -> null;
        };
    }
}
