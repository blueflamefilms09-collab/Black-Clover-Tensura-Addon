package com.newuniverse.nusmp.heroweapon.weapon.book;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.PerPlayer;

import java.util.UUID;

/**
 * Why: the Book keeps one volume per wielder in memory only (the two page decks and how far each has been read): a fresh server starts with an unread book,
 * which is what a freshly summoned book should have, the holder is bounded, and the volume is dropped when the wielder logs out or the server stops, so nothing
 * outlives the server it belongs to (the owner's server rule). Putting the book away does NOT drop it: it is the same book when it is summoned again.
 */
final class BookState {
    private BookState() {}

    static final PerPlayer<Volume> VOLUMES = new PerPlayer<>(Volume::new, 256);

    /** The wielder's volume, with its decks set to the pages the active form holds (the progress is kept when the pages did not change). */
    static Volume volume(HeroCtx c) {
        Volume v = VOLUMES.of(c.player.getUUID());
        v.ensure(BookDecks.offense(c.form.passives()), BookDecks.support(c.form.passives()));
        return v;
    }

    static void forget(UUID wielder) { VOLUMES.forget(wielder); }

    static void clearAll() { VOLUMES.clear(); }
}
