package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.soul.PaladinCasts;

import java.util.List;

/**
 * Why: Soul Magic is a generic wiki attribute, but Lucius Zogratis has spells of his own that nobody else's grimoire should hold.
 * They are appended after the generic five (never before, so no mode number moves) through {@link ExclusiveExt}. This 1.43.0 list is
 * Paladin Conversion: a signature page that purifies a creature's soul into a Paladin that serves the caster, and a recall page that
 * calls the Paladins to the caster (sneak-cast releases them instead). Cooldowns are in ticks, as the neighbours' are.
 */
public final class SoulExt {
    private SoulExt() {}

    private static final List<BookPage> PAGES = List.of(
            BookPage.signature(PaladinCasts.CONVERSION, "Paladin Conversion", PaladinCasts::conversion).withCooldown(300).withAnim("signature"),
            BookPage.mid(PaladinCasts.RECALL, "Paladin Recall", PaladinCasts::recall).withCooldown(200));

    public static List<BookPage> pages() { return PAGES; }
}
